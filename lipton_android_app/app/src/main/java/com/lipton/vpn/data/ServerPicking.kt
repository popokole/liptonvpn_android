package com.lipton.vpn.data

import com.lipton.vpn.data.model.Server
import com.lipton.vpn.data.model.ServerStatusList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  Выбор и проверка серверов (без Android, покрыто юнит-тестами). Повторяет
//  логику Windows-клиента: сервер по умолчанию, скрытие офлайн-серверов и
//  замер пинга без случайного разброса.
// ─────────────────────────────────────────────────────────────────────────────

private val RU = Locale("ru")

private fun isAutoBalanceRemark(remark: String): Boolean {
    val r = remark.lowercase(RU)
    return "баланс" in r || "balance" in r || "auto" in r || "авто" in r || "⚖" in remark
}

private fun isBypassRemark(remark: String): Boolean {
    val r = remark.lowercase(RU)
    return "обход" in r || "bypass" in r
}

/**
 * Сервер по умолчанию, когда человек сам ничего не выбирал: «Авто-баланс»
 * (он сам ведёт на быстрый узел), иначе с наименьшим пингом, иначе первый.
 */
fun List<Server>.defaultServer(): Server? =
    firstOrNull { isAutoBalanceRemark(it.remark) && !isBypassRemark(it.remark) }
        ?: filter { it.ping != null }.minByOrNull { it.ping!! }
        ?: firstOrNull()

/** Название для сверки со статусом: без флага и значков, до « · » («🇩🇪 Германия · Берлин» → «германия»). */
fun serverTitle(remark: String): String {
    val i = remark.indexOfFirst { it.isLetterOrDigit() }
    val name = if (i > 0) remark.substring(i) else remark
    return name.split(Regex("\\s+[·|—–-]\\s+")).first().trim().lowercase(RU)
}

/**
 * Серверы, которые бэкенд (GET /status/servers) отметил «down», не показываем.
 * «unknown», нет статуса или он устарел (stale) — показываем: не знаем — не прячем.
 * Выбранный сервер оставляем; если спрятать пришлось бы всё — показываем всё.
 */
fun hideOffline(servers: List<Server>, status: ServerStatusList?, keepId: String?): List<Server> {
    if (status == null || status.stale) return servers
    val down = status.servers.orEmpty()
        .filter { it.status == "down" }
        .map { it.name.trim().lowercase(RU) }
        .filter { it.isNotEmpty() }
        .toSet()
    if (down.isEmpty()) return servers
    val visible = servers.filter { it.id == keepId || serverTitle(it.remark) !in down }
    return visible.ifEmpty { servers }
}

/** Один «адрес» для замера: сервер, порт и SNI (разные SNI — разные инбаунды). */
fun endpointKey(s: Server): String = "${s.address}:${s.port}:${s.sni}"

/**
 * Пинг серверов без случайного разброса: одинаковые адреса меряются один раз,
 * на адрес [samples] замеров подряд (берётся лучший), одновременно не больше
 * [concurrency] адресов. Раньше один и тот же сервер под разными названиями
 * показывал то 124, то 438 мс.
 *
 * @return id сервера → мс или null
 */
suspend fun measureServers(
    servers: List<Server>,
    samples: Int = 3,
    concurrency: Int = 3,
    measureOnce: suspend (Server) -> Long?,
): Map<String, Long?> = coroutineScope {
    val groups = servers.groupBy(::endpointKey)
    val gate = Semaphore(concurrency.coerceAtLeast(1))
    val best = groups.map { (key, list) ->
        async {
            gate.withPermit {
                var min: Long? = null
                repeat(samples) {
                    val ms = try { measureOnce(list.first()) } catch (_: Exception) { null }
                    if (ms != null && (min == null || ms < min!!)) min = ms
                }
                key to min
            }
        }
    }.awaitAll().toMap()
    servers.associate { it.id to best[endpointKey(it)] }
}
