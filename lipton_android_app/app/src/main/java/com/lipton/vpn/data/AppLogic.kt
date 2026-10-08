package com.lipton.vpn.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lipton.vpn.data.model.AppBanner
import com.lipton.vpn.data.model.TxItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// ─────────────────────────────────────────────────────────────────────────────
//  Чистая логика редизайна (без Android): разбор ошибок API, выбор баннеров,
//  сравнение версий, домены, маскировка IP в логах, режим раздельного
//  туннелирования, история платежей. Покрыта юнит-тестами (JVM).
// ─────────────────────────────────────────────────────────────────────────────

/** Код и подробности ошибки из тела ответа бэкенда. */
data class ApiErrorInfo(
    val code: String = "",
    val message: String? = null,
    val retryAt: String? = null,
    val availableAt: String? = null,
)

/**
 * Разбор тела ошибки. Бэкенд отвечает в нескольких форматах:
 *  - `{"error": {"code": "...", "message": "...", "available_at": "..."}, "code": "..."}`;
 *  - `{"error": "guest_trial_used", "retry_at": "..."}` (новые ручки редизайна);
 *  - `{"message": "..."}` (ошибки echo).
 */
fun parseApiError(body: String?): ApiErrorInfo {
    if (body.isNullOrBlank()) return ApiErrorInfo()
    val obj: JsonObject = try {
        JsonParser.parseString(body).takeIf { it.isJsonObject }?.asJsonObject ?: return ApiErrorInfo()
    } catch (_: Exception) {
        return ApiErrorInfo()
    }
    fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }

    val err = obj.get("error")
    var code: String? = null
    var message: String? = null
    var retryAt: String? = obj.str("retry_at")
    var availableAt: String? = obj.str("available_at")
    when {
        err == null || err.isJsonNull -> Unit
        err.isJsonPrimitive -> code = err.asString
        err.isJsonObject -> {
            val e = err.asJsonObject
            code = e.str("code")
            message = e.str("message")
            retryAt = retryAt ?: e.str("retry_at")
            availableAt = availableAt ?: e.str("available_at")
        }
    }
    code = code ?: obj.str("code")
    message = message ?: obj.str("message")
    return ApiErrorInfo(code.orEmpty(), message, retryAt, availableAt)
}

/** Ручка ещё не выложена на бэкенд (или выключена) — блок надо тихо скрыть. */
fun isEndpointMissing(status: Int): Boolean = status == 404 || status == 405 || status == 501

// ─── Даты ────────────────────────────────────────────────────────────────────

/** RFC 3339 → мс; null, если строки нет или она не разбирается. */
fun parseIsoMillis(src: String?): Long? {
    val s = src?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return try {
        val clean = s.replace(Regex("\\.\\d+"), "").replace("Z", "+00:00")
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(clean)?.time
    } catch (_: Exception) {
        null
    }
}

/** «завтра в 14:05» / «сегодня в 23:10» / «12 октября в 09:00». */
fun ruWhen(ms: Long, nowMs: Long = System.currentTimeMillis(), tz: TimeZone = TimeZone.getDefault()): String {
    val at = Calendar.getInstance(tz).apply { timeInMillis = ms }
    val now = Calendar.getInstance(tz).apply { timeInMillis = nowMs }
    val hm = String.format(Locale.US, "%02d:%02d", at.get(Calendar.HOUR_OF_DAY), at.get(Calendar.MINUTE))
    fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    if (sameDay(at, now)) return "сегодня в $hm"
    now.add(Calendar.DAY_OF_YEAR, 1)
    if (sameDay(at, now)) return "завтра в $hm"
    val months = arrayOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
    return "${at.get(Calendar.DAY_OF_MONTH)} ${months[at.get(Calendar.MONTH)]} в $hm"
}

/** Обратный отсчёт «12:34» (мин:сек), от часа — «1:02:03»; отрицательное — «0:00». */
fun formatCountdown(remainingMs: Long): String {
    val total = (remainingMs.coerceAtLeast(0) + 999) / 1000   // округляем вверх: 14:59.2 → «15:00»
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

// ─── Баннеры из админки ──────────────────────────────────────────────────────

object BannerLogic {
    /**
     * Баннеры, которые сейчас надо показывать: в окне starts_at…ends_at, не закрытые
     * пользователем (закрыть можно только dismissible), по убыванию priority.
     */
    fun active(banners: List<AppBanner>, dismissed: Set<String>, nowMs: Long): List<AppBanner> =
        banners
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .filter { b ->
                val from = parseIsoMillis(b.startsAt)
                val to = parseIsoMillis(b.endsAt)
                (from == null || from <= nowMs) && (to == null || nowMs < to)
            }
            .filter { !(it.dismissible && it.id in dismissed) }
            .sortedByDescending { it.priority }

    /** Обязательное обновление: kind=update, закрыть нельзя. */
    fun blockingUpdate(active: List<AppBanner>): AppBanner? =
        active.firstOrNull { it.kind == "update" && !it.dismissible }

    /** Полноэкранный экран из админки (kind=screen) — показываем по одному. */
    fun screen(active: List<AppBanner>): AppBanner? = active.firstOrNull { it.kind == "screen" }

    /** Баннеры в слоте над главной: kind=banner и необязательные обновления. */
    fun slot(active: List<AppBanner>): List<AppBanner> =
        active.filter { it.kind == "banner" || (it.kind == "update" && it.dismissible) }
}

/** Сравнение версий «1.3.0» и «1.10.2»: <0, 0, >0. Хвосты вроде «-beta» игнорируются. */
fun compareVersions(a: String, b: String): Int {
    fun parts(v: String) = v.trim().removePrefix("v").split('.', '-', '+')
        .map { p -> p.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
    val pa = parts(a)
    val pb = parts(b)
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val d = (pa.getOrElse(i) { 0 }).compareTo(pb.getOrElse(i) { 0 })
        if (d != 0) return d
    }
    return 0
}

// ─── Свои домены для обхода ──────────────────────────────────────────────────

const val MAX_BYPASS_DOMAINS = 50

private val DOMAIN_RE = Regex("^(?=.{1,253}$)([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9-]{2,63}$")

/**
 * Приводит ввод к домену: «https://www.Example.com/path» → «example.com».
 * null — не домен (пустая строка, пробелы, IP без точки и т. п.).
 */
fun normalizeDomain(input: String): String? {
    var s = input.trim().lowercase(Locale.ROOT)
    if (s.isEmpty()) return null
    s = s.substringAfter("://")
    s = s.substringBefore('/').substringBefore('?').substringBefore('#')
    s = s.substringAfterLast('@').substringBefore(':')
    s = s.removePrefix("*.").removePrefix("www.").trimEnd('.')
    return s.takeIf { DOMAIN_RE.matches(it) }
}

// ─── Логи: маскировка адресов перед отправкой ───────────────────────────────

private val IPV4_RE = Regex("\\b(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\b")
private val IPV6_RE = Regex("\\b(?:[0-9a-fA-F]{1,4}:){3,7}[0-9a-fA-F]{1,4}\\b")

/** IPv4 → «185.225.*.*», IPv6 → «2a01:…». Локальные 127.x / 10.x оставляем как есть. */
fun maskIps(text: String): String {
    val v4 = IPV4_RE.replace(text) { m ->
        val (a, b) = m.destructured
        if (a == "127" || a == "10" || (a == "192" && b == "168")) m.value else "$a.$b.*.*"
    }
    return IPV6_RE.replace(v4) { m -> m.value.substringBefore(':') + ":…" }
}

// ─── Раздельное туннелирование ───────────────────────────────────────────────

/** Режим: всё через VPN или отмеченные приложения мимо VPN. */
enum class SplitMode(val stored: String) {
    ALL("all"),
    BYPASS_SELECTED("bypass");

    companion object {
        /** Сохранённого режима нет (старые версии) — по списку: есть приложения → «мимо VPN». */
        fun fromStored(raw: String?, hasApps: Boolean): SplitMode =
            entries.firstOrNull { it.stored == raw } ?: if (hasApps) BYPASS_SELECTED else ALL
    }
}

/** Какие пакеты исключить из туннеля при этом режиме (само приложение исключается всегда). */
fun excludedPackages(mode: SplitMode, selected: List<String>, ownPackage: String): List<String> = when (mode) {
    SplitMode.ALL -> emptyList()
    SplitMode.BYPASS_SELECTED -> selected.filter { it.isNotBlank() && it != ownPackage }.distinct()
}

// ─── История платежей ────────────────────────────────────────────────────────

data class PaymentsSummary(
    val totalKopeks: Long,
    val sinceMs: Long?,
    val paid: Int,
    val refunds: Int,
    val failed: Int,
)

fun TxItem.isRefund(): Boolean = kind == "refund" || status == "refunded"
fun TxItem.isPaid(): Boolean = !isRefund() && status == "succeeded"
fun TxItem.isFailed(): Boolean = !isRefund() && status in setOf("failed", "canceled", "cancelled")

/** «Всего оплачено»: успешные минус возвраты, с даты самой ранней операции. */
fun summarizePayments(items: List<TxItem>): PaymentsSummary {
    val paid = items.filter { it.isPaid() }
    val refunds = items.filter { it.isRefund() }
    val total = paid.sumOf { it.amountKopeks } - refunds.sumOf { it.amountKopeks }
    return PaymentsSummary(
        totalKopeks = total.coerceAtLeast(0),
        sinceMs = items.mapNotNull { parseIsoMillis(it.createdAt) }.minOrNull(),
        paid = paid.size,
        refunds = refunds.size,
        failed = items.count { it.isFailed() },
    )
}

/** Группы по месяцам (новые сверху): «Октябрь», «Сентябрь»; другой год — «Июль 2025». */
fun groupPaymentsByMonth(
    items: List<TxItem>,
    nowMs: Long = System.currentTimeMillis(),
    tz: TimeZone = TimeZone.getDefault(),
): List<Pair<String, List<TxItem>>> {
    val months = arrayOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
    val nowYear = Calendar.getInstance(tz).apply { timeInMillis = nowMs }.get(Calendar.YEAR)
    val sorted = items.sortedByDescending { parseIsoMillis(it.createdAt) ?: 0L }
    val out = LinkedHashMap<String, MutableList<TxItem>>()
    for (t in sorted) {
        val ms = parseIsoMillis(t.createdAt)
        val key = if (ms == null) "Без даты" else {
            val c = Calendar.getInstance(tz).apply { timeInMillis = ms }
            val y = c.get(Calendar.YEAR)
            months[c.get(Calendar.MONTH)] + if (y != nowYear) " $y" else ""
        }
        out.getOrPut(key) { mutableListOf() }.add(t)
    }
    return out.map { it.key to it.value.toList() }
}
