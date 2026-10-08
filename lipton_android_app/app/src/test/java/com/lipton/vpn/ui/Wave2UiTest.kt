package com.lipton.vpn.ui

import com.lipton.vpn.TrialSession
import com.lipton.vpn.UiState
import com.lipton.vpn.service.LiptonVpnService.VpnStatus
import com.lipton.vpn.ui.auth.AuthStep
import com.lipton.vpn.ui.auth.authStepFor
import com.lipton.vpn.ui.auth.isLikelyEmail
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.tabs.activeTrial
import com.lipton.vpn.ui.tabs.auroraToneFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Логика экранов волны 2: точки входа, свечение гостя и «15 минут», проверка почты. */
class Wave2UiTest {

    private val now = System.currentTimeMillis()

    @Test fun authEntryMapsToStep() {
        assertEquals(AuthStep.WELCOME, authStepFor(null))
        assertEquals(AuthStep.START, authStepFor("start"))
        assertEquals(AuthStep.LOGIN, authStepFor("login"))
        assertEquals(AuthStep.LOGIN, authStepFor("email"))
        assertEquals(AuthStep.TELEGRAM, authStepFor("telegram"))
        assertEquals(AuthStep.WELCOME, authStepFor("что-то новое"))
    }

    @Test fun guestAuroraFollowsVpnAndTurnsOrangeWhenEnded() {
        val active = UiState(guest = TrialSession(now + 60_000, 15), status = VpnStatus.CONNECTED)
        assertEquals(AuroraTone.ON, auroraToneFor(active))
        assertEquals(AuroraTone.OFF, auroraToneFor(active.copy(status = VpnStatus.DISCONNECTED)))
        assertEquals(AuroraTone.NO_SUB, auroraToneFor(active.copy(guest = TrialSession(now - 1, 15, ended = true))))
    }

    @Test fun dailyTrialIsNotNoSubscription() {
        val noSub = UiState(isAuthed = true, accountStatus = "none", status = VpnStatus.CONNECTED)
        assertEquals(AuroraTone.NO_SUB, auroraToneFor(noSub))
        assertEquals(AuroraTone.ON, auroraToneFor(noSub.copy(dailyTrial = TrialSession(now + 60_000, 15))))
    }

    @Test fun activeTrialPrefersRunningGuest() {
        assertNull(activeTrial(UiState()))
        val g = TrialSession(now + 1000, 15)
        assertEquals(g, activeTrial(UiState(guest = g)))
        assertNull(activeTrial(UiState(guest = g.copy(ended = true))))
        val d = TrialSession(now + 2000, 15)
        assertEquals(d, activeTrial(UiState(dailyTrial = d)))
    }

    @Test fun emailCheck() {
        assertTrue(isLikelyEmail("user@example.com"))
        assertTrue(isLikelyEmail(" new.name+vpn@mail.ru "))
        assertFalse(isLikelyEmail("user@example"))
        assertFalse(isLikelyEmail("user example.com"))
        assertFalse(isLikelyEmail(""))
    }
}
