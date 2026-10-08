package com.lipton.vpn.ui.tabs

import androidx.compose.runtime.Composable
import com.lipton.vpn.UiState
import com.lipton.vpn.service.LiptonVpnService.VpnStatus
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.Capsule
import com.lipton.vpn.ui.components.CapsuleTone
import com.lipton.vpn.ui.components.LiptonIcons
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

// ─────────────────────────────────────────────────────────────────────────────
//  Состояние подписки для каркаса: капсула срока в шапке и цвет свечения.
//  Тексты статусов — как в боте: «активна», «пробный период», «истекла».
// ─────────────────────────────────────────────────────────────────────────────

/** Подписка действует (оплачена, пробный период или льготные дни). */
fun hasActiveSubscription(state: UiState): Boolean =
    state.accountStatus == "active" || state.accountStatus == "trial" || state.accountStatus == "grace"

/**
 * Тариф «Обход глушилок» (в том числе временный поверх «Базового»).
 * TODO(redesign): признак «Обход» должен приходить из /config или /me/subscription;
 * пока — временный тариф (overlay) или код тарифа, отличный от «base», со словом «обход».
 */
fun isBypassTariff(state: UiState): Boolean {
    if (state.accountOverlay != null) return true
    val code = state.accountTariffCode?.lowercase(Locale.ROOT) ?: return false
    return listOf("bypass", "obhod", "obkhod", "glush", "antijam", "anti_jam").any { it in code }
}

/** Палитра свечения: выкл — рыжая, подключено — изумрудная, «Обход» — сине-фиолетовая. */
fun auroraToneFor(state: UiState): AuroraTone {
    // Пока аккаунт не подтянулся (accountStatus == null), но подписка в кэше есть — не пугаем рыжим.
    val known = state.accountStatus != null
    if (known && !hasActiveSubscription(state)) return AuroraTone.NO_SUB
    return when (state.status) {
        VpnStatus.CONNECTED -> if (isBypassTariff(state)) AuroraTone.BYPASS else AuroraTone.ON
        else -> AuroraTone.OFF
    }
}

/** Капсула срока в шапке: «24 дня» / «пробный период · 2 дня» / «истекла» / «Нет подписки». */
@Composable
fun SubscriptionCapsule(state: UiState) {
    val days = daysLeft(state.accountPeriodEnd)
    // Цвет капсулы — как у свечения: подключено — изумруд, «Обход» — синий, выключено — рыжий.
    val tone = when (auroraToneFor(state)) {
        AuroraTone.ON -> CapsuleTone.ACCENT
        AuroraTone.BYPASS -> CapsuleTone.BYPASS
        AuroraTone.OFF, AuroraTone.NO_SUB -> CapsuleTone.WARN
    }
    when (state.accountStatus) {
        "active", "grace" -> Capsule(
            text = days?.let { pluralDays(it) } ?: "активна",
            tone = tone,
            icon = LiptonIcons.Calendar,
        )
        "trial" -> Capsule(
            text = "пробный период",
            secondary = days?.let { pluralDays(it) },
            tone = tone,
            icon = LiptonIcons.Calendar,
        )
        "expired" -> Capsule(text = "истекла", tone = CapsuleTone.WARN)
        null -> Unit   // аккаунт ещё грузится
        else -> Capsule(text = "Нет подписки", tone = CapsuleTone.WARN)
    }
}

/** Сколько полных (округление вверх) дней до даты RFC3339; null — даты нет или она не разобралась. */
fun daysLeft(iso: String?, nowMs: Long = System.currentTimeMillis()): Int? {
    val src = iso?.takeIf { it.isNotBlank() } ?: return null
    val endMs = parseRfc3339(src) ?: return null
    val diff = endMs - nowMs
    if (diff <= 0) return 0
    return ceil(diff.toDouble() / TimeUnit.DAYS.toMillis(1)).toInt()
}

internal fun parseRfc3339(src: String): Long? = try {
    val clean = src.replace(Regex("\\.\\d+"), "").replace("Z", "+00:00")
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(clean)?.time
} catch (_: Exception) {
    null
}

/** 1 день, 2 дня, 5 дней, 21 день… */
fun pluralDays(n: Int): String {
    val m10 = n % 10
    val m100 = n % 100
    val word = when {
        m10 == 1 && m100 != 11 -> "день"
        m10 in 2..4 && m100 !in 12..14 -> "дня"
        else -> "дней"
    }
    return "$n $word"
}
