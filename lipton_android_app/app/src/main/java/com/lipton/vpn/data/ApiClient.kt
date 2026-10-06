package com.lipton.vpn.data

import android.os.Build
import com.google.gson.Gson
import com.lipton.vpn.BuildConfig
import com.lipton.vpn.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

// Клиент бэкенда Lipton (liptonone.online). Токены хранит в SettingsManager
// (DataStore), прозрачно обновляет access по refresh при 401. Платформа — android.
// Логика повторяет десктопный electron/api-client.js.
class ApiClient(private val settings: SettingsManager) {

    // code — внутренний код клиента (network / unauthorized / invalid);
    // serverCode — error.code из ответа бэкенда (conflict, not_found, ...).
    class ApiException(
        message: String,
        val status: Int = 0,
        val code: String = "",
        val serverCode: String = "",
    ) : Exception(message)

    companion object {
        const val API_BASE = "https://liptonone.online"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val refreshMutex = Mutex()

    private data class Resp(val status: Int, val body: String)

    private fun ua() = "LiptonVPN/${BuildConfig.VERSION_NAME} (Android; ${Build.MODEL})"

    // ─── Низкоуровневый запрос ──────────────────────────────────────────────

    private suspend fun raw(method: String, path: String, bodyObj: Any?, token: String?): Resp =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder()
                .url(API_BASE + path)
                .header("Accept", "application/json")
                .header("User-Agent", ua())
                .header("X-Platform", "android")
                .header("X-Device-Label", Build.MODEL)
            if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")

            val reqBody = if (bodyObj != null) gson.toJson(bodyObj).toRequestBody(JSON) else null
            when (method) {
                "GET"    -> builder.get()
                "POST"   -> builder.post(reqBody ?: "".toRequestBody(JSON))
                "DELETE" -> if (reqBody != null) builder.delete(reqBody) else builder.delete()
                else     -> builder.method(method, reqBody)
            }

            val resp = try {
                client.newCall(builder.build()).execute()
            } catch (e: IOException) {
                throw ApiException("Нет соединения с сервером — проверьте интернет", code = "network")
            }
            resp.use { Resp(it.code, it.body?.string() ?: "") }
        }

    private fun errMsg(r: Resp, fallback: String): String {
        return try {
            val obj = gson.fromJson(r.body, Map::class.java)
            val err = obj?.get("error")
            val msg = when (err) {
                is Map<*, *> -> err["message"] as? String
                else -> obj?.get("message") as? String
            }
            msg ?: fallback
        } catch (_: Exception) { fallback }
    }

    private fun errCode(r: Resp): String {
        return try {
            val obj = gson.fromJson(r.body, Map::class.java)
            val err = obj?.get("error")
            (if (err is Map<*, *>) err["code"] as? String else null) ?: ""
        } catch (_: Exception) { "" }
    }

    private inline fun <reified T> parse(body: String): T = gson.fromJson(body, T::class.java)

    // ─── Токены ─────────────────────────────────────────────────────────────

    private suspend fun saveTokens(t: TokenPair) {
        val access = t.accessToken ?: return
        val refresh = t.refreshToken ?: return
        settings.setAuthTokens(access, refresh, System.currentTimeMillis() + t.expiresIn * 1000L)
    }

    suspend fun isAuthed(): Boolean = settings.getAuthTokens() != null

    suspend fun logout() {
        try { authed("POST", "/auth/logout", null) } catch (_: Exception) {}
        settings.clearAuthTokens()
    }

    // refresh: true = обновлено; ApiException(code="invalid") = токен явно отклонён.
    // Транзиентный сбой (сеть/5xx) НЕ трогает сессию.
    private suspend fun doRefresh(): Boolean = refreshMutex.withLock {
        val t = settings.getAuthTokens() ?: throw ApiException("Сессия истекла", 401, "invalid")
        val r = try {
            raw("POST", "/auth/refresh", mapOf("refresh_token" to t.refresh), null)
        } catch (_: ApiException) {
            return false // сеть — не разлогиниваем
        }
        if (r.status == 200) {
            val pair = parse<TokenPair>(r.body)
            if (pair.accessToken != null) { saveTokens(pair); return true }
        }
        if (r.status == 401 || r.status == 400) throw ApiException("Сессия истекла", 401, "invalid")
        false
    }

    // authed — запрос с Bearer и одной попыткой refresh при 401.
    private suspend fun authed(method: String, path: String, bodyObj: Any?, retry: Boolean = false): String {
        val t = settings.getAuthTokens()
        val r = raw(method, path, bodyObj, t?.access)
        if (r.status == 401 && !retry) {
            val ok = try { doRefresh() } catch (e: ApiException) {
                if (e.code == "invalid") settings.clearAuthTokens()
                throw ApiException("Сессия истекла, войдите снова", 401, "unauthorized")
            }
            if (ok) return authed(method, path, bodyObj, true)
            throw ApiException("Сессия истекла, войдите снова", 401, "unauthorized")
        }
        if (r.status >= 400) throw ApiException(errMsg(r, "Ошибка ${r.status}"), r.status, serverCode = errCode(r))
        return r.body
    }

    // ─── Публичные флоу входа ───────────────────────────────────────────────

    suspend fun deviceExchange(code: String) {
        val r = raw("POST", "/auth/device/exchange", mapOf("code" to code), null)
        val pair = if (r.status < 400) parse<TokenPair>(r.body) else null
        if (r.status >= 400 || pair?.accessToken == null)
            throw ApiException(errMsg(r, "Неверный или истёкший код"), r.status)
        saveTokens(pair)
    }

    suspend fun emailRequest(email: String) {
        val r = raw("POST", "/auth/request-code", mapOf("type" to "email", "identifier" to email), null)
        if (r.status >= 400) throw ApiException(errMsg(r, "Не удалось отправить код"), r.status)
    }

    suspend fun emailVerify(email: String, code: String) {
        val r = raw("POST", "/auth/verify", mapOf("type" to "email", "identifier" to email, "code" to code), null)
        val pair = if (r.status < 400) parse<TokenPair>(r.body) else null
        if (r.status >= 400 || pair?.accessToken == null)
            throw ApiException(errMsg(r, "Неверный код"), r.status)
        saveTokens(pair)
    }

    suspend fun tgInit(): TgInit {
        val r = raw("POST", "/auth/telegram/init", null, null)
        if (r.status >= 400) throw ApiException(errMsg(r, "Не удалось начать вход"), r.status)
        return parse(r.body)
    }

    // tgPoll — авто-вход: бэкенд возвращает токены, как только пользователь нажал
    // Start/подтвердил в боте. Пока нет — done=false.
    suspend fun tgPoll(linkToken: String): Boolean {
        val r = raw("POST", "/auth/telegram/poll", mapOf("link_token" to linkToken), null)
        if (r.status >= 400) throw ApiException(errMsg(r, "Сессия входа истекла"), r.status)
        val pair = if (r.body.isNotBlank()) parse<TokenPair>(r.body) else null
        return if (pair?.accessToken != null) { saveTokens(pair); true } else false
    }

    // ─── Данные аккаунта ────────────────────────────────────────────────────

    suspend fun getSubscription(): MeSubscription = parse(authed("GET", "/me/subscription", null))
    suspend fun getTransactions(): TxList = parse(authed("GET", "/me/transactions", null))

    suspend fun getConfig(): AppConfig {
        val r = raw("GET", "/config", null, null)
        if (r.status >= 400) throw ApiException(errMsg(r, "Не удалось получить конфиг"), r.status)
        return parse(r.body)
    }

    suspend fun deleteCard() { authed("DELETE", "/payments/card", null) }

    // ─── Оплата ─────────────────────────────────────────────────────────────

    suspend fun checkout(tariffCode: String?, periodId: String?, promoCode: String?): CheckoutResult =
        parse(authed("POST", "/payments/checkout", mapOf(
            "tariff_code" to (tariffCode ?: ""),
            "period_id" to (periodId ?: ""),
            "promo_code" to (promoCode ?: ""),
        )))

    suspend fun paymentStatus(txId: String): PaymentStatus =
        parse(authed("GET", "/payments/status/$txId", null))

    // ─── Смена тарифа ───────────────────────────────────────────────────────

    suspend fun changeOptions(): ChangeOptions =
        parse(authed("GET", "/me/subscription/change/options", null))

    suspend fun changePreview(tariffId: String, periodDays: Int): ChangeOption =
        parse(authed("POST", "/me/subscription/change/preview", mapOf(
            "tariff_id" to tariffId,
            "period_days" to periodDays,
        )))

    // idempotencyKey — UUID на одну попытку; expectedSurchargeKopeks — доплата из
    // предпросмотра (если сумма на сервере изменилась, бэкенд ответит 409).
    suspend fun changeTariff(
        tariffId: String,
        periodDays: Int,
        idempotencyKey: String,
        expectedSurchargeKopeks: Long,
    ): ChangeResult =
        parse(authed("POST", "/me/subscription/change", mapOf(
            "tariff_id" to tariffId,
            "period_days" to periodDays,
            "idempotency_key" to idempotencyKey,
            "expected_surcharge_kopeks" to expectedSurchargeKopeks,
        )))

    // ─── Новости ────────────────────────────────────────────────────────────

    suspend fun getNews(): NewsList {
        val r = raw("GET", "/news", null, null)
        if (r.status >= 400) throw ApiException(errMsg(r, "Не удалось загрузить новости"), r.status)
        return parse(r.body)
    }

    // ─── Поддержка / ИИ ──────────────────────────────────────────────────────

    suspend fun getAiDialog(): AiDialog = parse(authed("GET", "/support/ai/dialog", null))
    suspend fun aiChat(message: String): AiReply = parse(authed("POST", "/support/ai", mapOf("message" to message)))
}
