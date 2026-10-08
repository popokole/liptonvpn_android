package com.lipton.vpn.ui

import com.lipton.vpn.NewsState
import com.lipton.vpn.data.DayTraffic
import com.lipton.vpn.data.TrafficLedger
import com.lipton.vpn.data.model.AppConfig
import com.lipton.vpn.data.model.DeviceItem
import com.lipton.vpn.data.model.NewsItem
import com.lipton.vpn.data.model.Server
import com.lipton.vpn.data.model.Tariff
import com.lipton.vpn.data.model.TariffPeriod
import com.lipton.vpn.ui.components.countryCodeFromFlag
import com.lipton.vpn.ui.components.flagFromCountryCode
import com.lipton.vpn.ui.components.pingLevel
import com.lipton.vpn.ui.tabs.baseTariff
import com.lipton.vpn.ui.tabs.bypassTariff
import com.lipton.vpn.ui.tabs.cleanName
import com.lipton.vpn.ui.tabs.countryCode
import com.lipton.vpn.ui.tabs.deviceTitle
import com.lipton.vpn.ui.tabs.formatMbps
import com.lipton.vpn.ui.tabs.formatTimer
import com.lipton.vpn.ui.tabs.formatTraffic
import com.lipton.vpn.ui.tabs.isAutoBalance
import com.lipton.vpn.ui.tabs.isBypassServer
import com.lipton.vpn.ui.tabs.isUnread
import com.lipton.vpn.ui.tabs.lastSeenRu
import com.lipton.vpn.ui.tabs.maskSubscriptionUrl
import com.lipton.vpn.ui.tabs.monthlyPeriod
import com.lipton.vpn.ui.tabs.percentVsAverage
import com.lipton.vpn.ui.tabs.periodLabel
import com.lipton.vpn.ui.tabs.periodTotalDays
import com.lipton.vpn.ui.tabs.pingStability
import com.lipton.vpn.ui.tabs.pluralRu
import com.lipton.vpn.ui.tabs.relativeAgo
import com.lipton.vpn.ui.tabs.rubles
import com.lipton.vpn.ui.tabs.savingPercent
import com.lipton.vpn.ui.tabs.titleAndSubtitle
import com.lipton.vpn.ui.tabs.unreadCount
import com.lipton.vpn.ui.tabs.weekRangeLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Логика экранов редизайна (A4): форматирование, тарифы, серверы, трафик, новости, устройства. */
class RedesignScreensTest {

    private val nbsp = " "

    private fun iso(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(ms))

    private val config = AppConfig(
        tariffs = listOf(
            Tariff(code = "base", title = "Базовый", periodDays = 30, priceKopeks = 15900, periods = listOf(
                TariffPeriod("p30", 30, 15900), TariffPeriod("p90", 90, 39900), TariffPeriod("p365", 365, 119900),
            )),
            Tariff(code = "bypass", title = "Обход глушилок", periodDays = 30, priceKopeks = 49900, periods = listOf(TariffPeriod("b30", 30, 49900))),
        ),
    )

    // ─── Форматирование ──────────────────────────────────────────────────────

    @Test fun speedInMbitWithComma() {
        assertEquals("186,4", formatMbps(23_300_000))
        assertEquals("0,0", formatMbps(0))
    }

    @Test fun trafficInGbOrMb() {
        assertEquals("1,8" to "ГБ", formatTraffic(1_800_000_000))
        assertEquals("50" to "МБ", formatTraffic(50_000_000))
        assertEquals("120" to "ГБ", formatTraffic(120_000_000_000))
    }

    @Test fun sessionTimer() {
        assertEquals("00:45:28", formatTimer((45 * 60 + 28) * 1000L))
        assertEquals("00:00:00", formatTimer(-5))
        assertEquals("101:00:00", formatTimer(101 * 3_600_000L))
    }

    @Test fun pingStabilityAndLevels() {
        assertEquals(true to 3L, pingStability(listOf(40, 45, 42)))
        assertEquals(false to 50L, pingStability(listOf(40, 140)))
        assertNull(pingStability(listOf(42)))
        assertEquals(0, pingLevel(null))
        assertEquals(4, pingLevel(38))
        assertEquals(1, pingLevel(400))
    }

    @Test fun rublesWithThousandsSeparator() {
        assertEquals("1${nbsp}199${nbsp}₽", rubles(119900))
        assertEquals("159${nbsp}₽", rubles(15900))
        assertEquals("99,50${nbsp}₽", rubles(9950))
    }

    @Test fun plurals() {
        assertEquals("устройство", pluralRu(1, "устройство", "устройства", "устройств"))
        assertEquals("устройства", pluralRu(3, "устройство", "устройства", "устройств"))
        assertEquals("устройств", pluralRu(11, "устройство", "устройства", "устройств"))
        assertEquals("год", periodLabel(365))
        assertEquals("3 месяца", periodLabel(90))
        assertEquals("30 дней", periodLabel(30))
    }

    @Test fun subscriptionUrlIsMasked() {
        assertEquals("liptonone.online/sub/•••8f3a", maskSubscriptionUrl("https://liptonone.online/sub/4f9c2a7d8f3a"))
        assertEquals("sub.example.org/•••cdef", maskSubscriptionUrl("https://sub.example.org/abcdef?x=1"))
        assertEquals("—", maskSubscriptionUrl(null))
    }

    @Test fun relativeTime() {
        val now = 1_000_000_000L
        assertEquals("только что", relativeAgo(now - 10_000, now))
        assertEquals("5 мин назад", relativeAgo(now - 5 * 60_000, now))
        assertEquals("2 ч назад", relativeAgo(now - 2 * 3_600_000, now))
    }

    // ─── Тарифы ──────────────────────────────────────────────────────────────

    @Test fun tariffsFromConfig() {
        assertEquals("base", config.baseTariff()?.code)
        assertEquals("bypass", config.bypassTariff()?.code)
        val base = config.baseTariff()!!
        assertEquals("p30", base.monthlyPeriod()?.id)
        assertEquals(38, base.savingPercent(365, 119900))
        assertNull(base.savingPercent(30, 15900))
    }

    @Test fun periodLengthFitsRemainingDays() {
        assertEquals(30, periodTotalDays(24, "base", config))
        assertEquals(90, periodTotalDays(80, "base", config))
        assertEquals(400, periodTotalDays(400, "base", config))
        assertEquals(30, periodTotalDays(5, null, null))
    }

    // ─── Серверы и флаги ─────────────────────────────────────────────────────

    private fun srv(remark: String) = Server(protocol = "vless", address = "203.0.113.1", port = 443, remark = remark)

    @Test fun serverGroupsByName() {
        assertTrue(srv("⚖️ Авто-баланс").isAutoBalance())
        assertFalse(srv("🇩🇪 Германия").isAutoBalance())
        assertTrue(srv("🇩🇪 Обход · Германия").isBypassServer())
        assertFalse(srv("🇳🇱 Нидерланды").isBypassServer())
    }

    @Test fun serverTitleAndSubtitle() {
        assertEquals("Германия" to "Франкфурт", srv("🇩🇪 Германия · Франкфурт").titleAndSubtitle())
        assertEquals("Быстрый сервер" to "Германия", srv("🇩🇪 Быстрый сервер").titleAndSubtitle())
        assertEquals("Авто-баланс", srv("⚖️ Авто-баланс").cleanName())
        assertEquals("NL", srv("🇳🇱 Нидерланды").countryCode())
    }

    @Test fun flagsAndCountryCodes() {
        assertEquals("DE", countryCodeFromFlag("🇩🇪"))
        assertNull(countryCodeFromFlag("⚖️"))
        assertEquals("🇩🇪", flagFromCountryCode("de"))
        assertEquals("", flagFromCountryCode("DEU"))
    }

    // ─── Трафик по дням ──────────────────────────────────────────────────────

    @Test fun ledgerKeepsTwoWeeksAndRoundTrips() {
        var map: Map<String, LongArray> = emptyMap()
        for (d in 1..20) map = TrafficLedger.add(map, String.format(Locale.US, "2026-09-%02d", d), 10, 1)
        map = TrafficLedger.add(map, "2026-09-20", 5, 2)
        assertEquals(TrafficLedger.KEEP_DAYS, map.size)
        assertFalse(map.containsKey("2026-09-01"))
        assertEquals(15L, map["2026-09-20"]!![0])
        val back = TrafficLedger.decode(TrafficLedger.encode(map))
        assertEquals(map.keys, back.keys)
        assertEquals(3L, back["2026-09-20"]!![1])
        assertTrue(TrafficLedger.decode("не json").isEmpty())
    }

    @Test fun weekEndsTodayWithZeros() {
        val tz = TimeZone.getTimeZone("UTC")
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = tz }.parse("2026-10-08 12:00")!!.time
        val week = TrafficLedger.week(mapOf("2026-10-08" to longArrayOf(100, 20), "2026-10-02" to longArrayOf(5, 5)), now, tz)
        assertEquals(7, week.size)
        assertEquals("2026-10-02", week.first().day)
        assertEquals("2026-10-08", week.last().day)
        assertEquals(120L, week.last().total)
        assertEquals(0L, week[3].total)
    }

    @Test fun todayVersusAverageAndRange() {
        val days = listOf(DayTraffic("2026-10-06", 100, 0), DayTraffic("2026-10-07", 0, 0), DayTraffic("2026-10-08", 128, 0))
        assertEquals(28, percentVsAverage(days))
        assertNull(percentVsAverage(listOf(DayTraffic("2026-10-08", 10, 0))))
        assertEquals("6–8 октября", weekRangeLabel(days))
        assertEquals("30 сентября – 1 октября", weekRangeLabel(listOf(DayTraffic("2026-09-30", 0, 0), DayTraffic("2026-10-01", 0, 0))))
    }

    // ─── Новости и устройства ────────────────────────────────────────────────

    @Test fun unreadNewsAreFreshAndNotRead() {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val news = NewsState(
            items = listOf(
                NewsItem("a", "свежая", "", null, iso(now - day)),
                NewsItem("b", "прочитанная", "", null, iso(now - day)),
                NewsItem("c", "старая", "", null, iso(now - 40 * day)),
                NewsItem("d", "без даты", "", null, null),
            ),
            readIds = setOf("b"),
        )
        assertTrue(news.isUnread(news.items[0], now))
        assertFalse(news.isUnread(news.items[1], now))
        assertFalse(news.isUnread(news.items[2], now))
        assertEquals(1, news.unreadCount(now))
    }

    @Test fun deviceNamesAndLastSeen() {
        assertEquals("iPhone 15 · Happ", deviceTitle(DeviceItem(hwid = "1", platform = "iOS", model = "iPhone 15", app = "Happ")))
        assertEquals("Pixel 8", deviceTitle(DeviceItem(hwid = "2", platform = "Android", model = "Pixel 8", app = "LiptonVPN")))
        assertEquals("Windows", deviceTitle(DeviceItem(hwid = "3", platform = "Windows")))
        val now = System.currentTimeMillis()
        assertEquals("сейчас", lastSeenRu(iso(now - 60_000), now))
        assertNull(lastSeenRu(null, now))
    }
}
