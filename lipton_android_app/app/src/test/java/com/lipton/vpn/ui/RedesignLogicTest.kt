package com.lipton.vpn.ui

import com.lipton.vpn.UiState
import com.lipton.vpn.data.model.Server
import com.lipton.vpn.data.model.SubOverlay
import com.lipton.vpn.data.model.displayName
import com.lipton.vpn.data.model.flagEmoji
import com.lipton.vpn.service.LiptonVpnService.VpnStatus
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.tabs.auroraToneFor
import com.lipton.vpn.ui.tabs.daysLeft
import com.lipton.vpn.ui.tabs.hasActiveSubscription
import com.lipton.vpn.ui.tabs.isBypassTariff
import com.lipton.vpn.ui.tabs.pluralDays
import com.lipton.vpn.ui.theme.AppTheme
import com.lipton.vpn.ui.theme.isDark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RedesignLogicTest {

    // ─── Тема ────────────────────────────────────────────────────────────────

    @Test fun themeDefaultsToSystem() {
        assertEquals(AppTheme.SYSTEM, AppTheme.fromStored(null))
        assertEquals(AppTheme.SYSTEM, AppTheme.fromStored("что-то непонятное"))
    }

    @Test fun legacyHackerThemeBecomesDark() {
        assertEquals(AppTheme.DARK, AppTheme.fromStored("HACKER"))
    }

    @Test fun storedThemesRoundTrip() {
        AppTheme.entries.forEach { assertEquals(it, AppTheme.fromStored(it.name)) }
    }

    @Test fun themeTitlesMatchOtherClients() {
        assertEquals(listOf("Тёмная", "Светлая", "Системная"), AppTheme.entries.map { it.title })
    }

    @Test fun systemThemeFollowsSystem() {
        assertTrue(AppTheme.SYSTEM.isDark(systemDark = true))
        assertFalse(AppTheme.SYSTEM.isDark(systemDark = false))
        assertTrue(AppTheme.DARK.isDark(systemDark = false))
        assertFalse(AppTheme.LIGHT.isDark(systemDark = true))
    }

    // ─── Сроки ───────────────────────────────────────────────────────────────

    @Test fun pluralDaysRussian() {
        assertEquals("1 день", pluralDays(1))
        assertEquals("2 дня", pluralDays(2))
        assertEquals("4 дня", pluralDays(4))
        assertEquals("5 дней", pluralDays(5))
        assertEquals("11 дней", pluralDays(11))
        assertEquals("12 дней", pluralDays(12))
        assertEquals("21 день", pluralDays(21))
        assertEquals("24 дня", pluralDays(24))
        assertEquals("0 дней", pluralDays(0))
    }

    @Test fun daysLeftParsesRfc3339() {
        val now = 1_790_000_000_000L   // фиксированное «сейчас»
        val day = 86_400_000L
        val iso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date(now + 24 * day))
        assertEquals(24, daysLeft(iso, now))
        assertEquals(0, daysLeft(iso, now + 30 * day))
        assertNull(daysLeft(null, now))
        assertNull(daysLeft("не дата", now))
    }

    // ─── Состояние подписки → капсула и свечение ─────────────────────────────

    private val active = UiState(loading = false, isAuthed = true, accountStatus = "active", accountTariffCode = "base")

    @Test fun activeStatuses() {
        assertTrue(hasActiveSubscription(active))
        assertTrue(hasActiveSubscription(active.copy(accountStatus = "trial")))
        assertTrue(hasActiveSubscription(active.copy(accountStatus = "grace")))
        assertFalse(hasActiveSubscription(active.copy(accountStatus = "expired")))
        assertFalse(hasActiveSubscription(active.copy(accountStatus = "none")))
    }

    @Test fun auroraToneByState() {
        assertEquals(AuroraTone.OFF, auroraToneFor(active))
        assertEquals(AuroraTone.ON, auroraToneFor(active.copy(status = VpnStatus.CONNECTED)))
        assertEquals(AuroraTone.NO_SUB, auroraToneFor(active.copy(accountStatus = "expired")))
        // Аккаунт ещё не подтянулся — не показываем «нет подписки»
        assertEquals(AuroraTone.OFF, auroraToneFor(active.copy(accountStatus = null)))
        val bypass = active.copy(status = VpnStatus.CONNECTED, accountOverlay = SubOverlay(tariffTitle = "Обход глушилок"))
        assertEquals(AuroraTone.BYPASS, auroraToneFor(bypass))
    }

    @Test fun bypassTariffDetection() {
        assertFalse(isBypassTariff(active))
        assertTrue(isBypassTariff(active.copy(accountTariffCode = "bypass")))
        assertTrue(isBypassTariff(active.copy(accountOverlay = SubOverlay(tariffTitle = "Обход глушилок"))))
    }

    // ─── Флаг сервера ────────────────────────────────────────────────────────

    @Test fun flagIsExtractedAndStripped() {
        val s = Server(protocol = "vless", address = "203.0.113.10", port = 443, remark = "🇩🇪 Германия · Франкфурт")
        assertEquals("🇩🇪", s.flagEmoji())
        assertEquals("Германия · Франкфурт", s.displayName())
    }

    @Test fun remarkWithoutFlag() {
        val s = Server(protocol = "vless", address = "203.0.113.11", port = 443, remark = "Авто-баланс")
        assertEquals("", s.flagEmoji())
        assertEquals("Авто-баланс", s.displayName())
    }
}
