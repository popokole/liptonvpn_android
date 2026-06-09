package com.lipton.vpn.data

import android.util.Base64
import com.lipton.vpn.BuildConfig
import com.lipton.vpn.data.model.Server
import com.lipton.vpn.data.model.Subscription
import com.lipton.vpn.data.model.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val ALLOWED_DOMAIN = "sub.popokole.online"
private const val TRIAL_URL      = "https://sub.popokole.online/rxB74qQu6gGg1JTt"

class SubscriptionManager(private val settings: SettingsManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ─── Validation ───────────────────────────────────────────────────────────

    fun normalizeUrl(url: String): String =
        if (url.startsWith("liptonvpn://")) url.replaceFirst("liptonvpn://", "https://") else url

    fun validateUrl(url: String) {
        val normalized = normalizeUrl(url)
        val parsed = runCatching { java.net.URL(normalized) }.getOrElse {
            throw IllegalArgumentException("Неверный формат ссылки")
        }
        if (parsed.host != ALLOWED_DOMAIN) {
            throw IllegalArgumentException("Разрешены только ссылки с домена $ALLOWED_DOMAIN")
        }
        if (parsed.protocol != "https" && parsed.protocol != "http") {
            throw IllegalArgumentException("Ссылка должна начинаться с https://")
        }
    }

    // ─── Fetch ────────────────────────────────────────────────────────────────

    suspend fun fetchAndParse(url: String): Pair<List<Server>, UserInfo> = withContext(Dispatchers.IO) {
        val hwid = settings.getHwid()
        val appVersion = BuildConfig.VERSION_NAME
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "LiptonVPN/$appVersion (Android; ${android.os.Build.MODEL})")
            .header("X-App-Name", "LiptonVPN")
            .header("X-App-Version", appVersion)
            .header("Accept", "text/plain, application/json, */*")
            .header("X-Hwid", hwid)
            .header("X-Device-OS", "Android")
            .header("X-Ver-OS", android.os.Build.VERSION.RELEASE)
            .header("X-Device-Model", android.os.Build.MODEL)
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (e: UnknownHostException) {
            throw Exception("Нет подключения к интернету")
        } catch (e: SocketTimeoutException) {
            throw Exception("Сервер не отвечает — проверьте интернет")
        } catch (e: javax.net.ssl.SSLException) {
            throw Exception("Ошибка SSL — проверьте дату и время на устройстве")
        } catch (e: IOException) {
            throw Exception("Ошибка сети — проверьте подключение к интернету")
        }

        if (!response.isSuccessful) {
            throw Exception(when (response.code) {
                401, 403 -> "Подписка недействительна (${response.code})"
                404      -> "Подписка не найдена — проверьте ссылку"
                429      -> "Слишком много запросов — подождите немного"
                in 500..599 -> "Ошибка сервера (${response.code}) — попробуйте позже"
                else     -> "Ошибка сервера (${response.code})"
            })
        }

        val userInfo = parseUserInfo(response.header("subscription-userinfo") ?: "")
        val body = response.body?.string() ?: throw Exception("Пустой ответ от сервера")
        val content = decodeContent(body)
        val servers = content.lines().mapNotNull { parseUri(it.trim()) }
        if (servers.isEmpty()) throw Exception("В подписке нет серверов — обратитесь в поддержку")

        Pair(servers, userInfo)
    }

    // ─── Subscription CRUD ────────────────────────────────────────────────────

    suspend fun add(url: String, isTrial: Boolean = false): Subscription {
        val url = normalizeUrl(url)
        validateUrl(url)
        val existing = settings.getSubscriptions()
        if (existing.any { it.url == url }) throw Exception("Такая подписка уже добавлена")

        if (!isTrial && existing.any { !it.isTrial }) {
            throw Exception("Уже есть активная подписка. Удалите её перед добавлением новой.")
        }

        val (servers, userInfo) = fetchAndParse(url)
        val resolvedUserInfo = if (isTrial && userInfo.expire == 0L) {
            userInfo.copy(expire = System.currentTimeMillis() / 1000L + 3600L)
        } else userInfo
        val sub = Subscription(
            id       = UUID.randomUUID().toString(),
            name     = if (isTrial) "Пробный доступ" else "Подписка",
            url      = url,
            isTrial  = isTrial,
            servers  = servers,
            userInfo = resolvedUserInfo,
        )
        val baseList = if (!isTrial) existing.filter { !it.isTrial } else existing
        settings.saveSubscriptions(baseList + sub)
        return sub
    }

    suspend fun refresh(subId: String) {
        val subs = settings.getSubscriptions().toMutableList()
        val idx = subs.indexOfFirst { it.id == subId }
        if (idx < 0) return
        val sub = subs[idx]
        val (newServers, userInfo) = fetchAndParse(sub.url)
        // Preserve id and addedAt for servers that already existed (matched by address+port+protocol)
        val preserved = newServers.map { newSrv ->
            val existing = sub.servers.find {
                it.address == newSrv.address && it.port == newSrv.port && it.protocol == newSrv.protocol
            }
            if (existing != null) newSrv.copy(id = existing.id, addedAt = existing.addedAt) else newSrv
        }
        subs[idx] = sub.copy(servers = preserved, userInfo = userInfo, lastUpdated = System.currentTimeMillis())
        settings.saveSubscriptions(subs)
    }

    suspend fun remove(subId: String) {
        val subs = settings.getSubscriptions().filter { it.id != subId }
        settings.saveSubscriptions(subs)
    }

    // ─── Trial subscription ───────────────────────────────────────────────────

    suspend fun getTrialSubscription(hwid: String, durationMinutes: Int): Subscription =
        withContext(Dispatchers.IO) {
            // Try to get a personalised URL from the trial API; fall back to hardcoded TRIAL_URL on any error
            val subUrl = try {
                val trialApiUrl = "https://sub.popokole.online/trial?hwid=$hwid&duration=${durationMinutes}m"
                val req = Request.Builder()
                    .url(trialApiUrl)
                    .header("User-Agent", "LiptonVPN/${BuildConfig.VERSION_NAME} (Android; ${android.os.Build.MODEL})")
                    .header("X-App-Name", "LiptonVPN")
                    .header("X-App-Version", BuildConfig.VERSION_NAME)
                    .header("X-Hwid", hwid)
                    .build()
                val resp = client.newCall(req).execute()
                val body = resp.body?.string()?.trim() ?: ""
                if (resp.isSuccessful && body.startsWith("http")) body else TRIAL_URL
            } catch (_: Exception) {
                TRIAL_URL
            }
            add(subUrl, isTrial = true)
        }

    // ─── Ping ─────────────────────────────────────────────────────────────────

    suspend fun pingAll(subId: String) = withContext(Dispatchers.IO) {
        val subs = settings.getSubscriptions().toMutableList()
        val idx = subs.indexOfFirst { it.id == subId }
        if (idx < 0) return@withContext

        val sub = subs[idx]
        val pingedServers = sub.servers.map { server ->
            val ping = measurePing(server.address, server.port)
            server.copy(ping = ping)
        }
        subs[idx] = sub.copy(servers = pingedServers)
        settings.saveSubscriptions(subs)
    }

    private fun measurePing(host: String, port: Int): Long? {
        return try {
            val start = System.currentTimeMillis()
            Socket().use { sock ->
                sock.connect(java.net.InetSocketAddress(host, port), 3000)
            }
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            null
        }
    }

    // ─── Parsers ──────────────────────────────────────────────────────────────

    private fun parseUserInfo(raw: String): UserInfo {
        val map = raw.split(";").associate { part ->
            val (k, v) = part.trim().split("=").let {
                it.getOrElse(0) { "" } to it.getOrElse(1) { "0" }
            }
            k.trim() to v.trim().toLongOrNull()
        }
        return UserInfo(
            upload   = map["upload"]   ?: 0,
            download = map["download"] ?: 0,
            total    = map["total"]    ?: 0,
            expire   = map["expire"]   ?: 0,
        )
    }

    private fun decodeContent(raw: String): String {
        val t = raw.trim()
        return try {
            val decoded = String(Base64.decode(t, Base64.DEFAULT), Charsets.UTF_8)
            if (decoded.contains("://")) decoded else t
        } catch (e: Exception) {
            t
        }
    }

    private fun parseUri(line: String): Server? = when {
        line.startsWith("vless://")  -> parseVless(line)
        line.startsWith("vmess://")  -> parseVmess(line)
        line.startsWith("trojan://") -> parseTrojan(line)
        else -> null
    }

    // Splits "key=val=with=equals" on the FIRST '=' only, then URL-decodes the value.
    // Using split("=") would corrupt base64 values (e.g. Reality pbk with '=' padding).
    private fun parseQueryParams(rawQuery: String?): Map<String, String> =
        (rawQuery ?: "").split("&").filter { it.isNotEmpty() }.associate { param ->
            val eq = param.indexOf('=')
            if (eq < 0) param to ""
            else {
                val key = param.substring(0, eq)
                val raw = param.substring(eq + 1)
                key to try { java.net.URLDecoder.decode(raw, "UTF-8") } catch (_: Exception) { raw }
            }
        }

    private fun parseVless(uri: String): Server? = runCatching {
        // Extract remark from fragment manually — java.net.URI throws on unencoded
        // special chars (spaces, CJK, emoji) that VPN panels put in remarks.
        val hashIdx = uri.lastIndexOf('#')
        val cleanUri = if (hashIdx >= 0) uri.substring(0, hashIdx) else uri
        val remark = if (hashIdx >= 0)
            try { java.net.URLDecoder.decode(uri.substring(hashIdx + 1), "UTF-8") }
            catch (_: Exception) { uri.substring(hashIdx + 1) }
        else ""

        val u = java.net.URI(cleanUri)
        val params = parseQueryParams(u.rawQuery)
        val host = u.host ?: return@runCatching null

        Server(
            protocol    = "vless",
            address     = host,
            port        = u.port.takeIf { it > 0 } ?: 443,
            uuid        = u.userInfo ?: "",
            remark      = remark.ifBlank { host },
            network     = params["type"] ?: "tcp",
            security    = params["security"] ?: "none",
            flow        = params["flow"] ?: "",
            sni         = params["sni"] ?: host,
            pbk         = params["pbk"] ?: "",
            sid         = params["sid"] ?: "",
            fp          = params["fp"] ?: "chrome",
            path        = params["path"] ?: "/",
            host        = params["host"] ?: "",
            alpn        = params["alpn"] ?: "",
            serviceName = params["serviceName"] ?: "",
            mode        = params["mode"]?.ifBlank { null },
            headerType  = params["headerType"]?.ifBlank { null },
        )
    }.getOrNull()

    private fun parseVmess(uri: String): Server? = runCatching {
        val json = String(Base64.decode(uri.removePrefix("vmess://"), Base64.DEFAULT))
        val obj = com.google.gson.JsonParser.parseString(json).asJsonObject
        Server(
            protocol  = "vmess",
            address   = obj["add"]?.asString ?: return null,
            port      = obj["port"]?.asString?.toIntOrNull() ?: 443,
            uuid      = obj["id"]?.asString ?: "",
            remark    = obj["ps"]?.asString ?: obj["add"]?.asString ?: "",
            network   = obj["net"]?.asString ?: "tcp",
            security  = obj["tls"]?.asString ?: "none",
            sni       = obj["sni"]?.asString ?: obj["add"]?.asString ?: "",
            path      = obj["path"]?.asString ?: "/",
            host      = obj["host"]?.asString ?: "",
            alterId   = obj["aid"]?.asString?.toIntOrNull() ?: 0,
            cipher    = obj["scy"]?.asString ?: "auto",
        )
    }.getOrNull()

    private fun parseTrojan(uri: String): Server? = runCatching {
        val hashIdx = uri.lastIndexOf('#')
        val cleanUri = if (hashIdx >= 0) uri.substring(0, hashIdx) else uri
        val remark = if (hashIdx >= 0)
            try { java.net.URLDecoder.decode(uri.substring(hashIdx + 1), "UTF-8") }
            catch (_: Exception) { uri.substring(hashIdx + 1) }
        else ""

        val u = java.net.URI(cleanUri)
        val params = parseQueryParams(u.rawQuery)
        val host = u.host ?: return@runCatching null

        Server(
            protocol   = "trojan",
            address    = host,
            port       = u.port.takeIf { it > 0 } ?: 443,
            password   = u.userInfo ?: "",
            remark     = remark.ifBlank { host },
            network    = params["type"] ?: "tcp",
            security   = params["security"] ?: "tls",
            sni        = params["sni"] ?: host,
            fp         = params["fp"] ?: "chrome",
            path       = params["path"] ?: "/",
            host       = params["host"] ?: "",
            mode       = params["mode"]?.ifBlank { null },
            headerType = params["headerType"]?.ifBlank { null },
        )
    }.getOrNull()
}
