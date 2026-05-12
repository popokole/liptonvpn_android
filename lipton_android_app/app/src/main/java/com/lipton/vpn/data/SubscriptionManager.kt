package com.lipton.vpn.data

import android.util.Base64
import com.lipton.vpn.data.model.Server
import com.lipton.vpn.data.model.Subscription
import com.lipton.vpn.data.model.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Socket
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val ALLOWED_DOMAIN = "sub.popokole.online"
private const val APP_VERSION   = "1.0.0"
private const val TRIAL_URL     = "https://sub.popokole.online/rxB74qQu6gGg1JTt"

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
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "LiptonVPN/$APP_VERSION (Android)")
            .header("X-App-Name", "LiptonVPN")
            .header("X-App-Version", APP_VERSION)
            .header("Accept", "text/plain, application/json, */*")
            .header("X-Hwid", hwid)
            .header("X-Device-OS", "Android")
            .header("X-Ver-OS", android.os.Build.VERSION.RELEASE)
            .header("X-Device-Model", android.os.Build.MODEL)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Ошибка сервера (${response.code})")

        val userInfo = parseUserInfo(response.header("subscription-userinfo") ?: "")
        val body = response.body?.string() ?: throw Exception("Пустой ответ")
        val content = decodeContent(body)
        val servers = content.lines().mapNotNull { parseUri(it.trim()) }
        if (servers.isEmpty()) throw Exception("В подписке не найдено серверов")

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
        val (servers, userInfo) = fetchAndParse(sub.url)
        subs[idx] = sub.copy(servers = servers, userInfo = userInfo, lastUpdated = System.currentTimeMillis())
        settings.saveSubscriptions(subs)
    }

    suspend fun remove(subId: String) {
        val subs = settings.getSubscriptions().filter { it.id != subId }
        settings.saveSubscriptions(subs)
    }

    // ─── Trial subscription ───────────────────────────────────────────────────

    suspend fun getTrialSubscription(hwid: String, durationMinutes: Int): Subscription =
        withContext(Dispatchers.IO) {
            add(TRIAL_URL, isTrial = true)
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

    private fun parseVless(uri: String): Server? = runCatching {
        val u = java.net.URI(uri)
        val params = (u.query ?: "").split("&").associate {
            val (k, v) = it.split("=").let { kv ->
                kv.getOrElse(0) { "" } to kv.getOrElse(1) { "" }
            }
            k to java.net.URLDecoder.decode(v, "UTF-8")
        }
        Server(
            protocol  = "vless",
            address   = u.host,
            port      = u.port.takeIf { it > 0 } ?: 443,
            uuid      = u.userInfo ?: "",
            remark    = u.fragment?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: u.host,
            network   = params["type"] ?: "tcp",
            security  = params["security"] ?: "none",
            flow      = params["flow"] ?: "",
            sni       = params["sni"] ?: u.host,
            pbk       = params["pbk"] ?: "",
            sid       = params["sid"] ?: "",
            fp        = params["fp"] ?: "chrome",
            path      = params["path"] ?: "/",
            host      = params["host"] ?: "",
            alpn      = params["alpn"] ?: "",
            serviceName = params["serviceName"] ?: "",
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
        val u = java.net.URI(uri)
        val params = (u.query ?: "").split("&").associate {
            val parts = it.split("=")
            parts.getOrElse(0) { "" } to java.net.URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
        }
        Server(
            protocol  = "trojan",
            address   = u.host,
            port      = u.port.takeIf { it > 0 } ?: 443,
            password  = u.userInfo ?: "",
            remark    = u.fragment?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: u.host,
            network   = params["type"] ?: "tcp",
            security  = params["security"] ?: "tls",
            sni       = params["sni"] ?: u.host,
            path      = params["path"] ?: "/",
            host      = params["host"] ?: "",
        )
    }.getOrNull()
}
