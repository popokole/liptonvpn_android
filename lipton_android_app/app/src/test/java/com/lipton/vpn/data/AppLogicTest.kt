package com.lipton.vpn.data

import com.lipton.vpn.data.model.AppBanner
import com.lipton.vpn.data.model.AppConfig
import com.lipton.vpn.data.model.GuestTrialConfig
import com.lipton.vpn.data.model.TxItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Чистая логика волны 2: ошибки API, баннеры, версии, домены, логи, раздельное туннелирование, платежи. */
class AppLogicTest {

    private val msk = TimeZone.getTimeZone("Europe/Moscow")

    private fun iso(s: String): String = s   // для читаемости

    private fun ms(s: String): Long =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(s)!!.time

    // ─── Ошибки API ──────────────────────────────────────────────────────────

    @Test fun errorAsStringWithRetryAt() {
        val e = parseApiError("""{"error":"guest_trial_used","retry_at":"2026-10-09T11:05:00Z"}""")
        assertEquals("guest_trial_used", e.code)
        assertEquals("2026-10-09T11:05:00Z", e.retryAt)
        assertNull(e.message)
    }

    @Test fun errorAsObjectWithAvailableAt() {
        val e = parseApiError("""{"error":{"code":"card_unlink_cooldown","message":"Отвязать можно завтра","available_at":"2026-10-09T10:00:00Z"},"code":"card_unlink_cooldown"}""")
        assertEquals("card_unlink_cooldown", e.code)
        assertEquals("Отвязать можно завтра", e.message)
        assertEquals("2026-10-09T10:00:00Z", e.availableAt)
    }

    @Test fun errorEchoMessageAndGarbage() {
        assertEquals("Not Found", parseApiError("""{"message":"Not Found"}""").message)
        assertEquals("", parseApiError("<html>502</html>").code)
        assertEquals("", parseApiError(null).code)
        assertTrue(isEndpointMissing(404))
        assertTrue(isEndpointMissing(405))
        assertFalse(isEndpointMissing(429))
    }

    // ─── Конфиг: гостевой доступ ─────────────────────────────────────────────

    @Test fun guestTrialFlagFromObjectOrFlatFields() {
        assertFalse(AppConfig().guestEnabled)
        assertEquals(15, AppConfig().guestMinutes)
        assertTrue(AppConfig(guestTrial = GuestTrialConfig(true, 15)).guestEnabled)
        assertTrue(AppConfig(trialGuestEnabled = true, trialGuestMinutes = 10).guestEnabled)
        assertEquals(10, AppConfig(trialGuestEnabled = true, trialGuestMinutes = 10).guestMinutes)
        // объект важнее плоских полей
        assertFalse(AppConfig(guestTrial = GuestTrialConfig(false, 15), trialGuestEnabled = true).guestEnabled)
    }

    // ─── Время ───────────────────────────────────────────────────────────────

    @Test fun countdownFormat() {
        assertEquals("15:00", formatCountdown(15 * 60_000L))
        assertEquals("15:00", formatCountdown(14 * 60_000L + 59_200L))   // округление вверх
        assertEquals("12:34", formatCountdown((12 * 60 + 34) * 1000L))
        assertEquals("0:05", formatCountdown(5_000L))
        assertEquals("0:00", formatCountdown(-3_000L))
        assertEquals("1:02:03", formatCountdown((3600 + 2 * 60 + 3) * 1000L))
    }

    @Test fun ruWhenTodayTomorrowDate() {
        val now = ms("2026-10-08T14:05:00+03:00")
        assertEquals("сегодня в 23:10", ruWhen(ms("2026-10-08T23:10:00+03:00"), now, msk))
        assertEquals("завтра в 14:05", ruWhen(ms("2026-10-09T14:05:00+03:00"), now, msk))
        assertEquals("12 октября в 09:00", ruWhen(ms("2026-10-12T09:00:00+03:00"), now, msk))
    }

    @Test fun isoParse() {
        assertEquals(ms("2026-10-09T11:05:00+00:00"), parseIsoMillis("2026-10-09T11:05:00.123Z"))
        assertNull(parseIsoMillis(""))
        assertNull(parseIsoMillis("завтра"))
    }

    // ─── Баннеры ─────────────────────────────────────────────────────────────

    private val now = ms("2026-10-08T12:00:00+00:00")

    @Test fun bannersFilteredByWindowDismissAndSortedByPriority() {
        val list = listOf(
            AppBanner(id = "a", title = "Старый", endsAt = iso("2026-10-01T00:00:00Z")),
            AppBanner(id = "b", title = "Будущий", startsAt = iso("2026-10-20T00:00:00Z")),
            AppBanner(id = "c", title = "Обычный", priority = 1),
            AppBanner(id = "d", title = "Важный", priority = 5),
            AppBanner(id = "e", title = "Закрытый"),
            AppBanner(id = "", title = "Без id"),
        )
        val active = BannerLogic.active(list, dismissed = setOf("e"), nowMs = now)
        assertEquals(listOf("d", "c"), active.map { it.id })
    }

    @Test fun mandatoryUpdateCannotBeDismissed() {
        val upd = AppBanner(id = "u", kind = "update", title = "Обновите приложение", dismissible = false)
        val active = BannerLogic.active(listOf(upd), dismissed = setOf("u"), nowMs = now)
        assertEquals(upd, BannerLogic.blockingUpdate(active))
        assertTrue(BannerLogic.slot(active).isEmpty())
    }

    @Test fun bannerKindsGoToTheirPlaces() {
        val active = BannerLogic.active(listOf(
            AppBanner(id = "s", kind = "screen", title = "Новый тариф", priority = 3),
            AppBanner(id = "b", kind = "banner", title = "Акция", priority = 2),
            AppBanner(id = "u", kind = "update", title = "Есть обновление", dismissible = true, priority = 1),
        ), emptySet(), now)
        assertEquals("s", BannerLogic.screen(active)?.id)
        assertEquals(listOf("b", "u"), BannerLogic.slot(active).map { it.id })
        assertNull(BannerLogic.blockingUpdate(active))
    }

    // ─── Версии ──────────────────────────────────────────────────────────────

    @Test fun versionsCompareNumerically() {
        assertTrue(compareVersions("1.3.0", "1.10.0") < 0)
        assertEquals(0, compareVersions("1.3", "1.3.0"))
        assertTrue(compareVersions("v2.0.7", "2.0.6") > 0)
        assertEquals(0, compareVersions("1.4.0-beta", "1.4.0"))
    }

    // ─── Домены ──────────────────────────────────────────────────────────────

    @Test fun domainNormalization() {
        assertEquals("example.com", normalizeDomain("https://www.Example.com/path?q=1"))
        assertEquals("corp.example.com", normalizeDomain("  corp.example.com  "))
        assertEquals("steampowered.com", normalizeDomain("*.steampowered.com"))
        assertEquals("example.org", normalizeDomain("user@example.org:8443"))
        assertNull(normalizeDomain(""))
        assertNull(normalizeDomain("localhost"))
        assertNull(normalizeDomain("не домен"))
        assertNull(normalizeDomain("-bad-.com"))
    }

    // ─── Логи ────────────────────────────────────────────────────────────────

    @Test fun ipsMaskedInLogs() {
        val masked = maskIps("Сервер 185.225.31.42:443, локально 127.0.0.1 и 10.10.10.1, v6 2a01:4f8:c0c:1234::1 2001:db8:85a3:0:0:8a2e:370:7334")
        assertTrue(masked.contains("185.225.*.*:443"))
        assertTrue(masked.contains("127.0.0.1"))
        assertTrue(masked.contains("10.10.10.1"))
        assertFalse(masked.contains("31.42"))
        assertFalse(masked.contains("8a2e"))
    }

    // ─── Раздельное туннелирование ───────────────────────────────────────────

    @Test fun splitModeFromStoredAndLegacy() {
        assertEquals(SplitMode.ALL, SplitMode.fromStored(null, hasApps = false))
        assertEquals(SplitMode.BYPASS_SELECTED, SplitMode.fromStored(null, hasApps = true))   // старые версии
        assertEquals(SplitMode.ALL, SplitMode.fromStored("all", hasApps = true))
        assertEquals(SplitMode.BYPASS_SELECTED, SplitMode.fromStored("bypass", hasApps = false))
    }

    @Test fun excludedPackagesDependOnMode() {
        val sel = listOf("org.telegram", "com.lipton.vpn", "org.telegram", "")
        assertTrue(excludedPackages(SplitMode.ALL, sel, "com.lipton.vpn").isEmpty())
        assertEquals(listOf("org.telegram"), excludedPackages(SplitMode.BYPASS_SELECTED, sel, "com.lipton.vpn"))
    }

    // ─── История платежей ────────────────────────────────────────────────────

    private val txs = listOf(
        TxItem(id = "1", kind = "renewal", amountKopeks = 15900, status = "succeeded", createdAt = "2026-10-08T10:00:00Z"),
        TxItem(id = "2", kind = "change", amountKopeks = 31100, status = "succeeded", createdAt = "2026-10-02T10:00:00Z"),
        TxItem(id = "3", kind = "renewal", amountKopeks = 15900, status = "failed", createdAt = "2026-09-13T10:00:00Z"),
        TxItem(id = "4", kind = "refund", amountKopeks = 39900, status = "succeeded", createdAt = "2026-03-12T10:00:00Z"),
        TxItem(id = "5", kind = "initial", amountKopeks = 39900, status = "succeeded", createdAt = "2025-12-01T10:00:00Z"),
    )

    @Test fun paymentsSummary() {
        val s = summarizePayments(txs)
        assertEquals(15900L + 31100 + 39900 - 39900, s.totalKopeks)
        assertEquals(3, s.paid)
        assertEquals(1, s.refunds)
        assertEquals(1, s.failed)
        assertEquals(ms("2025-12-01T10:00:00+00:00"), s.sinceMs)
    }

    @Test fun paymentsGroupedByMonthNewestFirst() {
        val groups = groupPaymentsByMonth(txs, nowMs = ms("2026-10-08T12:00:00+03:00"), tz = msk)
        assertEquals(listOf("Октябрь", "Сентябрь", "Март", "Декабрь 2025"), groups.map { it.first })
        assertEquals(listOf("1", "2"), groups.first().second.map { it.id })
    }
}
