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
import java.net.InetSocketAddress
import java.net.Proxy
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
        // Когда можно повторить (429 guest_trial_used / daily_trial_used) и когда
        // откроется действие (409 card_unlink_cooldown) — RFC 3339 из ответа.
        val retryAt: String? = null,
        val availableAt: String? = null,
    ) : Exception(message) {
        /** Ручки нет на этом бэкенде (ещё не выложена) — блок надо тихо скрыть. */
        val endpointMissing: Boolean get() = isEndpointMissing(status)
    }

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

    private fun errMsg(r: Resp, fallback: String): String = parseApiError(r.body).message ?: fallback

    /** Исключение по ответу с ошибкой: текст, код сервера и даты из тела. */
    private fun failure(r: Resp, fallback: String): ApiException {
        val e = parseApiError(r.body)
        return ApiException(e.message ?: fallback, r.status, serverCode = e.code, retryAt = e.retryAt, availableAt = e.availableAt)
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
        if (r.status >= 400) throw failure(r, "Ошибка ${r.status}")
        return r.body
    }

    /** Запрос с авторизацией, если вход выполнен, иначе — без неё (баннеры). */
    private suspend fun optionalAuthed(method: String, path: String, bodyObj: Any?): String {
        if (settings.getAuthTokens() != null) return authed(method, path, bodyObj)
        val r = raw(method, path, bodyObj, null)
        if (r.status >= 400) throw failure(r, "Ошибка ${r.status}")
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

    // Отвязка карты. 409 + Retry-After — кулдаун 24 ч после новой привязки (текст — от сервера).
    suspend fun deleteCard(): CardUnlinkResult =
        authed("DELETE", "/payments/card", null).let { if (it.isBlank()) CardUnlinkResult() else parse(it) }

    // ─── Профиль, ссылка, устройства (редизайн) ─────────────────────────────

    suspend fun getMe(): MeProfile = parse(authed("GET", "/me", null))

    suspend fun getDevices(): DevicesResponse = parse(authed("GET", "/me/devices", null))

    suspend fun revokeDevice(hwid: String) {
        authed("POST", "/me/devices/revoke", mapOf("hwid" to hwid))
    }

    // «Отвязать все»: освобождает все места, ссылка не меняется (кулдаун — на сервере).
    suspend fun revokeAllDevices() {
        authed("POST", "/me/devices/revoke-all", null)
    }

    // «Обновить ссылку»: ответ — тот же View, что у /me/subscription, уже с новой ссылкой.
    // expectedVersion — link_version, которую видел пользователь (защита от двойного нажатия).
    suspend fun relinkSubscription(expectedVersion: Int?): MeSubscription =
        parse(authed("POST", "/me/subscription/relink",
            if (expectedVersion != null && expectedVersion > 0) mapOf("expected_version" to expectedVersion) else emptyMap<String, Any>()))

    // Отмена подписки: сразу, остаток не возвращается, карта удаляется.
    suspend fun cancelSubscription() {
        authed("POST", "/me/subscription/cancel", mapOf("confirm" to true))
    }

    suspend fun validatePromo(code: String): PromoResult =
        parse(authed("POST", "/promo/validate", mapOf("code" to code)))

    // ─── Статус серверов и «как сайты видят вас» (публичные) ────────────────

    suspend fun getServerStatus(): ServerStatusList {
        val r = raw("GET", "/status/servers", null, null)
        if (r.status >= 400) throw ApiException(errMsg(r, "Не удалось получить статус серверов"), r.status)
        return parse(r.body)
    }

    // Само приложение исключено из туннеля, поэтому при подключённом VPN адрес
    // «как его видят сайты» спрашиваем через локальный SOCKS ядра (socksPort).
    suspend fun ipCheck(socksPort: Int?): IpInfo = withContext(Dispatchers.IO) {
        val c = if (socksPort != null) {
            client.newBuilder()
                .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socksPort)))
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()
        } else client
        val req = Request.Builder()
            .url("$API_BASE/ipcheck")
            .header("Accept", "application/json")
            .header("User-Agent", ua())
            .header("X-Platform", "android")
            .get()
            .build()
        val body = try {
            c.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw ApiException("Ошибка ${resp.code}", resp.code)
                resp.body?.string() ?: ""
            }
        } catch (e: IOException) {
            throw ApiException("Нет соединения с сервером — проверьте интернет", code = "network")
        }
        parse<IpInfo>(body)
    }

    // Проверка соединения: ответ Cloudflare /cdn-cgi/trace (ip, loc, colo…) — через
    // SOCKS ядра, если VPN включён. Ключи и значения — как в ответе («ip=…»).
    suspend fun cloudflareTrace(socksPort: Int?): Map<String, String> = withContext(Dispatchers.IO) {
        val c = if (socksPort != null) {
            client.newBuilder()
                .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socksPort)))
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()
        } else client
        val req = Request.Builder().url("https://www.cloudflare.com/cdn-cgi/trace").header("User-Agent", ua()).get().build()
        try {
            c.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyMap()
                (resp.body?.string() ?: "").lines().mapNotNull { line ->
                    val i = line.indexOf('=')
                    if (i <= 0) null else line.substring(0, i).trim() to line.substring(i + 1).trim()
                }.toMap()
            }
        } catch (e: IOException) {
            throw ApiException("Нет ответа — проверьте интернет", code = "network")
        }
    }

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

    // «Помогло / Не помогло» под ответом ИИ.
    suspend fun aiFeedback(messageId: String, helpful: Boolean) {
        authed("POST", "/support/ai/feedback", mapOf("message_id" to messageId, "helpful" to helpful))
    }

    // «Позвать оператора»: диалог переходит в ручной режим.
    suspend fun aiOperator(dialogId: String?) {
        authed("POST", "/support/ai/operator", if (dialogId.isNullOrBlank()) emptyMap<String, Any>() else mapOf("dialog_id" to dialogId))
    }

    // Логи приложения в поддержку (IP маскируются до отправки).
    suspend fun aiLogs(logs: String, note: String) {
        authed("POST", "/support/ai/logs", mapOf("logs" to logs, "note" to note))
    }

    // ─── Гостевой доступ и 15 минут в день ──────────────────────────────────

    // Без авторизации: 200 — ссылка и срок; 429 guest_trial_used (+retry_at); 403 guest_trial_disabled.
    suspend fun guestTrial(deviceId: String, appVersion: String): GuestTrialResponse {
        val r = raw("POST", "/guest/trial", mapOf(
            "device_id" to deviceId,
            "platform" to "android",
            "app_version" to appVersion,
        ), null)
        if (r.status >= 400) throw failure(r, "Не удалось включить пробный доступ")
        return parse(r.body)
    }

    // Вошедшим без подписки: 200 — ссылка и срок; 409 has_subscription; 429 daily_trial_used (+retry_at).
    suspend fun dailyTrial(): DailyTrialResponse = parse(authed("POST", "/me/daily-trial", null))

    // ─── Баннеры и экраны из админки ────────────────────────────────────────

    suspend fun getBanners(version: String): List<AppBanner> {
        val v = java.net.URLEncoder.encode(version, "UTF-8")
        val body = optionalAuthed("GET", "/app/banners?platform=android&version=$v", null)
        if (body.isBlank()) return emptyList()
        return parse<BannersResponse>(body).banners.orEmpty()
    }

    // ─── Уведомления (три переключателя) ────────────────────────────────────

    suspend fun getNotificationPrefs(): NotificationPrefs = parse(authed("GET", "/me/notifications", null))

    suspend fun putNotificationPrefs(p: NotificationPrefs): NotificationPrefs {
        val body = authed("PUT", "/me/notifications", p)
        return if (body.isBlank()) p else try { parse(body) } catch (_: Exception) { p }
    }

    // ─── Смена и привязка почты ─────────────────────────────────────────────

    // Код на новый адрес (тот же запрос, что при привязке почты).
    suspend fun emailLinkRequestCode(email: String) {
        authed("POST", "/auth/link/request-code", mapOf("type" to "email", "identifier" to email))
    }

    // Смена почты. Если адрес был у другого аккаунта, аккаунты объединяются и приходят
    // токены «выжившего» — сохраняем их. true — токены заменены.
    suspend fun changeEmail(newEmail: String, code: String): Boolean {
        val body = authed("POST", "/auth/email/change", mapOf("new_email" to newEmail, "code" to code))
        val pair = try { parse<TokenPair>(body) } catch (_: Exception) { null }
        if (pair?.accessToken != null && pair.refreshToken != null) { saveTokens(pair); return true }
        return false
    }

    // Привязка почты, если её ещё нет (вход был через Telegram).
    suspend fun linkEmailVerify(email: String, code: String) {
        authed("POST", "/auth/link/verify", mapOf("type" to "email", "identifier" to email, "code" to code))
    }

    // ─── База знаний ────────────────────────────────────────────────────────

    suspend fun getFaq(): List<FaqEntry> {
        val r = raw("GET", "/faq", null, null)
        if (r.status >= 400) throw failure(r, "Не удалось загрузить базу знаний")
        return parse<FaqList>(r.body).items.orEmpty()
    }

    // Статьи блога как база знаний. Ответ — массив (или объект с articles/items).
    suspend fun getArticles(category: String? = null): List<ArticleSummary> {
        val q = if (category.isNullOrBlank()) "" else "?category=" + java.net.URLEncoder.encode(category, "UTF-8")
        val r = raw("GET", "/content/articles$q", null, null)
        if (r.status >= 400) throw failure(r, "Не удалось загрузить статьи")
        val el = com.google.gson.JsonParser.parseString(r.body.ifBlank { "[]" })
        val arr = when {
            el.isJsonArray -> el.asJsonArray
            el.isJsonObject -> el.asJsonObject.let { o -> (o.get("articles") ?: o.get("items"))?.takeIf { it.isJsonArray }?.asJsonArray }
            else -> null
        } ?: return emptyList()
        return arr.mapNotNull { runCatching { gson.fromJson(it, ArticleSummary::class.java) }.getOrNull() }
            .filter { it.slug.isNotBlank() && it.title.isNotBlank() }
    }

    suspend fun getArticle(slug: String): ArticleDetail {
        val r = raw("GET", "/content/articles/" + java.net.URLEncoder.encode(slug, "UTF-8"), null, null)
        if (r.status >= 400) throw failure(r, "Не удалось открыть статью")
        return parse(r.body)
    }
}
