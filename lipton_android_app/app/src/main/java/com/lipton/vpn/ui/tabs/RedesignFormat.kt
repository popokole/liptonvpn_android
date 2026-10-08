package com.lipton.vpn.ui.tabs

import com.lipton.vpn.data.DayTraffic
import com.lipton.vpn.data.model.AppConfig
import com.lipton.vpn.data.model.Server
import com.lipton.vpn.data.model.Tariff
import com.lipton.vpn.data.model.displayName
import com.lipton.vpn.data.model.flagEmoji
import com.lipton.vpn.ui.components.countryCodeFromFlag
import com.lipton.vpn.ui.components.countryNameRu
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
//  Форматирование и разбор для экранов редизайна (чистые функции — под юнит-тесты).
// ─────────────────────────────────────────────────────────────────────────────

private val RU = Locale("ru", "RU")

private val MONTHS_GEN = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)
private val MONTHS_NOM = listOf(
    "январь", "февраль", "март", "апрель", "май", "июнь",
    "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь",
)
private val WEEKDAYS_SHORT = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")

/** Склонение: 1 устройство, 2 устройства, 5 устройств. */
fun pluralRu(n: Int, one: String, few: String, many: String): String {
    val m10 = abs(n) % 10
    val m100 = abs(n) % 100
    return when {
        m10 == 1 && m100 != 11 -> one
        m10 in 2..4 && m100 !in 12..14 -> few
        else -> many
    }
}

/** Мбит/с с одной цифрой после запятой: 186,4. */
fun formatMbps(bytesPerSec: Long): String {
    val mbit = bytesPerSec * 8 / 1_000_000.0
    return String.format(RU, "%.1f", mbit)
}

/** Трафик: «1,8» + «ГБ», мелкие объёмы — в МБ. */
fun formatTraffic(bytes: Long): Pair<String, String> {
    val gb = bytes / 1_000_000_000.0
    return when {
        gb >= 100 -> String.format(RU, "%.0f", gb) to "ГБ"
        gb >= 0.1 -> String.format(RU, "%.1f", gb) to "ГБ"
        else -> String.format(RU, "%.0f", bytes / 1_000_000.0) to "МБ"
    }
}

fun formatTrafficInline(bytes: Long): String = formatTraffic(bytes).let { "${it.first} ${it.second}" }

/** Таймер сессии 00:45:28 (часы не обрезаются после 99). */
fun formatTimer(elapsedMs: Long): String {
    val total = (elapsedMs / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

/** Стабильность пинга за последние замеры: (стабильно?, разброс ±мс). */
fun pingStability(history: List<Long>): Pair<Boolean, Long>? {
    val last = history.takeLast(20)
    if (last.size < 2) return null
    val jitter = ((last.max() - last.min()) / 2.0).roundToInt().toLong()
    return (jitter <= 10) to jitter
}

/** «8 октября». */
fun ruDayMonth(ms: Long, tz: TimeZone = TimeZone.getDefault()): String {
    val cal = Calendar.getInstance(tz).apply { timeInMillis = ms }
    return "${cal.get(Calendar.DAY_OF_MONTH)} ${MONTHS_GEN[cal.get(Calendar.MONTH)]}"
}

/** «1 ноября» из RFC3339; null — дата не разобралась. */
fun ruDayMonth(iso: String?): String? = iso?.let { parseRfc3339(it) }?.let { ruDayMonth(it) }

/** «12 марта 2026». */
fun ruDayMonthYear(iso: String?): String? = iso?.let { parseRfc3339(it) }?.let {
    val cal = Calendar.getInstance().apply { timeInMillis = it }
    "${ruDayMonth(it)} ${cal.get(Calendar.YEAR)}"
}

/** Ключ дня «yyyy-MM-dd» → «Пт». */
fun weekdayShort(dayKey: String): String = try {
    val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dayKey)
    val cal = Calendar.getInstance().apply { time = d!! }
    WEEKDAYS_SHORT[cal.get(Calendar.DAY_OF_WEEK) - 1]
} catch (_: Exception) {
    ""
}

/** «2–8 октября» или «28 сентября – 4 октября». */
fun weekRangeLabel(days: List<DayTraffic>): String {
    if (days.isEmpty()) return ""
    val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return try {
        val a = Calendar.getInstance().apply { time = f.parse(days.first().day)!! }
        val b = Calendar.getInstance().apply { time = f.parse(days.last().day)!! }
        if (a.get(Calendar.MONTH) == b.get(Calendar.MONTH)) {
            "${a.get(Calendar.DAY_OF_MONTH)}–${b.get(Calendar.DAY_OF_MONTH)} ${MONTHS_GEN[b.get(Calendar.MONTH)]}"
        } else {
            "${a.get(Calendar.DAY_OF_MONTH)} ${MONTHS_GEN[a.get(Calendar.MONTH)]} – ${b.get(Calendar.DAY_OF_MONTH)} ${MONTHS_GEN[b.get(Calendar.MONTH)]}"
        }
    } catch (_: Exception) {
        ""
    }
}

/** «сентябрь — октябрь» по датам ленты (от старой к новой). */
fun monthRangeLabel(oldestMs: Long?, newestMs: Long?): String? {
    if (oldestMs == null || newestMs == null) return null
    val a = Calendar.getInstance().apply { timeInMillis = oldestMs }
    val b = Calendar.getInstance().apply { timeInMillis = newestMs }
    val ma = MONTHS_NOM[a.get(Calendar.MONTH)]
    val mb = MONTHS_NOM[b.get(Calendar.MONTH)]
    return if (ma == mb) mb else "$ma — $mb"
}

/** Сегодня против среднего по прошлым дням с трафиком: +28 / −15; null — сравнивать не с чем. */
fun percentVsAverage(days: List<DayTraffic>): Int? {
    if (days.isEmpty()) return null
    val prev = days.dropLast(1).filter { it.total > 0 }
    if (prev.isEmpty()) return null
    val avg = prev.sumOf { it.total }.toDouble() / prev.size
    if (avg <= 0) return null
    return ((days.last().total - avg) / avg * 100).roundToInt()
}

/** Среднее за дни с трафиком. */
fun averagePerDay(days: List<DayTraffic>): Long {
    val withData = days.filter { it.total > 0 }
    return if (withData.isEmpty()) 0L else withData.sumOf { it.total } / withData.size
}

/** «обновлено только что / 1 мин назад / 2 ч назад». */
fun relativeAgo(thenMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val sec = ((nowMs - thenMs) / 1000).coerceAtLeast(0)
    return when {
        sec < 45 -> "только что"
        sec < 3600 -> "${(sec / 60).coerceAtLeast(1)} мин назад"
        sec < 86_400 -> "${sec / 3600} ч назад"
        else -> {
            val d = (sec / 86_400).toInt()
            "$d ${pluralRu(d, "день", "дня", "дней")} назад"
        }
    }
}

/** Ссылка подписки с маской: liptonone.online/sub/•••8f3a. */
fun maskSubscriptionUrl(url: String?): String {
    if (url.isNullOrBlank()) return "—"
    val noScheme = url.substringAfter("://")
    val host = noScheme.substringBefore('/')
    val path = noScheme.substringAfter('/', "")
    val token = path.substringAfterLast('/').substringBefore('?')
    val prefix = path.substringBeforeLast('/', "")
    val tail = token.takeLast(4)
    return buildString {
        append(host)
        append('/')
        if (prefix.isNotEmpty()) { append(prefix); append('/') }
        append("•••")
        append(tail)
    }
}

/** Рубли с разделителем тысяч: 1 199 ₽ (неразрывные пробелы). */
fun rubles(kopeks: Long): String {
    val r = kopeks / 100
    val s = r.toString().reversed().chunked(3).joinToString(" ").reversed()
    val rest = kopeks % 100
    return if (rest == 0L) "$s ₽" else "$s,${String.format(Locale.US, "%02d", rest)} ₽"
}

/** Срок тарифа словами: 30 → «30 дней», 90 → «3 месяца», 365 → «год». */
fun periodLabel(days: Int): String = when {
    days in 360..370 -> "год"
    days % 30 == 0 && days >= 60 -> (days / 30).let { "$it ${pluralRu(it, "месяц", "месяца", "месяцев")}" }
    else -> "$days ${pluralRu(days, "день", "дня", "дней")}"
}

// ─── Тарифы из /config ───────────────────────────────────────────────────────

/** Тариф «Обход глушилок» в /config (по коду или названию). */
fun isBypassTariffCode(code: String?, title: String? = null): Boolean {
    val c = code?.lowercase(Locale.ROOT).orEmpty()
    val t = title?.lowercase(RU).orEmpty()
    return listOf("bypass", "obhod", "obkhod", "glush", "antijam", "anti_jam").any { it in c } ||
        "обход" in t || "глуш" in t
}

fun AppConfig.baseTariff(): Tariff? = tariffs.firstOrNull { !isBypassTariffCode(it.code, it.title) }
fun AppConfig.bypassTariff(): Tariff? = tariffs.firstOrNull { isBypassTariffCode(it.code, it.title) }

/** Месячная цена тарифа (срок 30±3 дня), иначе самый короткий срок. */
fun Tariff.monthlyPeriod() = periods.firstOrNull { it.days in 27..33 } ?: periods.minByOrNull { it.days }

/** Выгода длинного срока к помесячной цене, %; null — выгоды нет. */
fun Tariff.savingPercent(periodDays: Int, priceKopeks: Long): Int? {
    val m = monthlyPeriod() ?: return null
    if (m.days <= 0 || periodDays <= m.days) return null
    val full = m.priceKopeks.toDouble() / m.days * periodDays
    if (full <= 0) return null
    val p = ((1 - priceKopeks / full) * 100).roundToInt()
    return p.takeIf { it >= 5 }
}

/** Название тарифа по коду из /config (или из временного тарифа). */
fun tariffTitleFor(code: String?, config: AppConfig?): String? {
    if (code.isNullOrBlank()) return null
    return config?.tariffs?.firstOrNull { it.code == code }?.title
}

/**
 * Длина текущего периода для «24 из 30 дней»: самый короткий срок тарифа, который
 * вмещает остаток. TODO(redesign): бэкенд должен отдавать начало периода / period_days.
 */
fun periodTotalDays(daysLeft: Int, tariffCode: String?, config: AppConfig?): Int {
    val periods = config?.tariffs?.firstOrNull { it.code == tariffCode }?.periods?.map { it.days }
        ?: config?.tariffs?.flatMap { t -> t.periods.map { it.days } }
        ?: emptyList()
    val fit = periods.filter { it >= daysLeft }.minOrNull()
    return fit ?: maxOf(daysLeft, 30)
}

// ─── Серверы ─────────────────────────────────────────────────────────────────

/** «Авто-баланс» — по названию хоста. TODO(redesign): признак должен приходить с бэкенда. */
fun Server.isAutoBalance(): Boolean {
    val r = remark.lowercase(RU)
    return "баланс" in r || "balance" in r || "auto" in r || "⚖" in remark
}

/** Сервер группы «Обход глушилок» — по названию хоста. TODO(redesign): группа должна приходить с бэкенда. */
fun Server.isBypassServer(): Boolean {
    val r = remark.lowercase(RU)
    return "обход" in r || "bypass" in r
}

/** Название без флага и ведущих значков-эмодзи («⚖️ Авто-баланс» → «Авто-баланс»). */
fun Server.cleanName(): String {
    val name = displayName()
    val i = name.indexOfFirst { it.isLetterOrDigit() }
    return (if (i > 0) name.substring(i) else name).trim()
}

/** ISO-код страны сервера по эмодзи-флагу в названии. */
fun Server.countryCode(): String? = countryCodeFromFlag(flagEmoji())

/** Заголовок и подпись строки сервера: «Германия · Франкфурт» → («Германия», «Франкфурт»). */
fun Server.titleAndSubtitle(): Pair<String, String?> {
    val name = cleanName().ifBlank { address }
    val parts = name.split(" · ", " — ", " - ", " | ", ", ").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size >= 2) return parts[0] to parts.drop(1).joinToString(" · ")
    val country = countryNameRu(countryCode())
    return name to country?.takeIf { !it.equals(name, ignoreCase = true) }
}
