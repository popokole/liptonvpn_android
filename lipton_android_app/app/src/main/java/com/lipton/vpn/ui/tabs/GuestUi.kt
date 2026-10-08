package com.lipton.vpn.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.TrialSession
import com.lipton.vpn.UiState
import com.lipton.vpn.data.formatCountdown
import com.lipton.vpn.data.ruWhen
import com.lipton.vpn.ui.auth.TimerRing
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.ButtonTone
import com.lipton.vpn.ui.components.Capsule
import com.lipton.vpn.ui.components.CapsuleTone
import com.lipton.vpn.ui.components.GlassButton
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.GradientProgress
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.PrimaryButton
import com.lipton.vpn.ui.components.StatusDot
import com.lipton.vpn.ui.components.ToneCircleIcon
import com.lipton.vpn.ui.components.glass
import com.lipton.vpn.ui.components.softGlow
import com.lipton.vpn.ui.components.stateText
import com.lipton.vpn.ui.components.stateTone
import com.lipton.vpn.ui.theme.LiptonText
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.TABULAR_NUMS
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
//  Гостевой режим и «15 минут бесплатно» (макеты new-guest-*): капсула с
//  обратным отсчётом, карточка «Создайте аккаунт», экран «15 минут прошли».
// ─────────────────────────────────────────────────────────────────────────────

/** Текущее время, обновляется раз в секунду (для обратного отсчёта). */
@Composable
fun rememberNowTicker(active: Boolean = true): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active) {
        while (active) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }
    return now
}

/** Действующая пробная сессия (гостевая или дневная), которую показываем отсчётом. */
fun activeTrial(state: UiState): TrialSession? =
    state.guest?.takeIf { !it.ended } ?: state.dailyTrial

/** Капсула в шапке: «⏱ 12:34» (зелёная) или «0:00» (рыжая, время вышло). */
@Composable
fun TrialCapsule(session: TrialSession) {
    val now = rememberNowTicker(!session.ended)
    val left = if (session.ended) 0L else session.expiresAt - now
    Capsule(
        text = formatCountdown(left),
        tone = if (left > 0) CapsuleTone.ACCENT else CapsuleTone.WARN,
        icon = LiptonIcons.Timer,
    )
}

/**
 * Карточка пробного доступа под кнопкой подключения: «Пробный доступ · осталось 12:34
 * из 15:00», полоса, призыв. Гость — «Создать аккаунт» / «Войти»; вошедший — «Оформить подписку».
 */
@Composable
fun TrialAccessCard(
    session: TrialSession,
    guest: Boolean,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)?,
) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    val now = rememberNowTicker()
    val total = session.minutes * 60_000L
    val left = (session.expiresAt - now).coerceAtLeast(0)
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind { softGlow(t.a.copy(alpha = if (c.isDark) 0.16f else 0.10f), 24.dp.toPx(), 14.dp.toPx()) }
            .glass(shape)
            .border(1.dp, Brush.linearGradient(listOf(t.a.copy(alpha = 0.55f), t.b.copy(alpha = 0.35f))), shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(999.dp)).border(1.5.dp, t.a, RoundedCornerShape(999.dp)))
            Spacer(Modifier.width(8.dp))
            Text(
                buildAnnotatedString {
                    append(if (guest) "Пробный доступ · осталось " else "15 минут бесплатно · осталось ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.text)) { append(formatCountdown(left)) }
                },
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TABULAR_NUMS),
                color = c.text.copy(alpha = 0.78f), modifier = Modifier.weight(1f), maxLines = 1,
            )
            Text("из ${formatCountdown(total)}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR_NUMS), color = c.text.copy(alpha = 0.55f))
        }
        Spacer(Modifier.height(10.dp))
        GradientProgress(if (total > 0) left.toFloat() / total else 0f, t, height = 4.dp)
        Spacer(Modifier.height(14.dp))
        Text(
            if (guest) "Создайте аккаунт,\nчтобы не потерять доступ" else "Оформите подписку,\nчтобы не потерять доступ",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 24.sp),
            color = c.text,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(
                if (guest) "Создать аккаунт" else "Оформить подписку", onClick = onPrimary,
                icon = if (guest) LiptonIcons.UserPlus else null, height = 44.dp,
                modifier = Modifier.weight(1f),
            )
            if (onSecondary != null) GlassButton("Войти", onClick = onSecondary, height = 44.dp, contentPadding = PaddingValues(horizontal = 24.dp))
        }
    }
}

/** Плитка вместо тарифа и трафика у гостя: «Статистика и тариф появятся после входа». */
@Composable
fun AfterLoginTile() {
    val c = LiptonTheme.colors
    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(LiptonIcons.Lock, null, tint = c.text.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text("Тариф и трафик", style = MaterialTheme.typography.labelMedium, color = c.text.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
            Text("после входа", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.5f))
        }
        Spacer(Modifier.height(12.dp))
        Text("Статистика и тариф появятся после входа", style = MaterialTheme.typography.titleSmall, color = c.text)
        Text("Трафик за неделю и срок подписки", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.66f))
        Spacer(Modifier.height(10.dp))
        Text("Без аккаунта история не сохраняется", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.5f))
    }
}

/**
 * «15 минут прошли» (new-guest-ended): кольцо 0:00, «Создать аккаунт», «Войти»,
 * тарифы «от 159 ₽ / мес», когда следующая бесплатная попытка.
 */
@Composable
fun GuestEndedHero(state: UiState, onCreate: () -> Unit, onLogin: () -> Unit) {
    val c = LiptonTheme.colors
    val warn = c.stateTone(AuroraTone.NO_SUB)
    val minutes = state.guest?.minutes ?: state.appConfig?.guestMinutes ?: 15
    Column(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        TimerRing("0:00", "из ${formatCountdown(minutes * 60_000L)}", 0f, AuroraTone.NO_SUB, size = 168.dp)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.height(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusDot(warn.a)
            Text("ПРОБНЫЙ ДОСТУП ЗАКОНЧИЛСЯ", style = LiptonText.eyebrow.copy(letterSpacing = LiptonText.eyebrow.letterSpacing * 0.6f), color = c.stateText(AuroraTone.NO_SUB))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "$minutes минут\nпрошли",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = MaterialTheme.typography.displayLarge.fontFamily, fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp, lineHeight = 42.sp,
                shadow = if (c.isDark) Shadow(warn.a.copy(alpha = 0.3f), Offset.Zero, 40f) else null,
            ),
            color = c.text, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Создайте аккаунт или войдите —\nи выберите тариф.\nСнова попробовать бесплатно можно завтра.",
            style = MaterialTheme.typography.bodyLarge, color = c.text.copy(alpha = 0.75f), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Создать аккаунт", onClick = onCreate, tone = ButtonTone.WARN, trailingIcon = LiptonIcons.ArrowRight, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().height(52.dp).glass(RoundedCornerShape(999.dp), highlightHeight = 26.dp).clickable(role = Role.Button, onClick = onLogin),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(LiptonIcons.LogIn, null, tint = c.text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Войти", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = c.text)
        }
        Spacer(Modifier.height(20.dp))
        val from = state.appConfig?.baseTariff()?.monthlyPeriod()
        val limit = state.appConfig?.baseTariff()?.deviceLimit ?: 5
        GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), onClick = onCreate) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ToneCircleIcon(LiptonIcons.Tag, warn.a, size = 40.dp, iconSize = 18.dp)
                Column(Modifier.weight(1f)) {
                    Text("Тарифы", style = MaterialTheme.typography.titleSmall, color = c.text)
                    Text("до $limit устройств", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.62f))
                }
                if (from != null) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = c.text.copy(alpha = 0.6f))) { append("от ") }
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = c.text)) { append(rubles(from.priceKopeks)) }
                            withStyle(SpanStyle(color = c.text.copy(alpha = 0.6f))) { append(" / мес") }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS),
                    )
                }
                Icon(LiptonIcons.ChevronRight, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(LiptonIcons.Timer, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            val retry = state.guestRetryAt?.takeIf { it > System.currentTimeMillis() }
            Text(
                if (retry != null) "Следующая бесплатная попытка — ${ruWhen(retry)}" else "Следующая бесплатная попытка — завтра",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = c.text.copy(alpha = 0.55f),
            )
        }
    }
}

/** Кнопка «15 минут бесплатно» для вошедших без подписки (POST /me/daily-trial). */
@Composable
fun DailyTrialButton(state: UiState, onStart: () -> Unit) {
    val retry = state.dailyTrialRetryAt?.takeIf { it > System.currentTimeMillis() }
    val minutes = state.appConfig?.guestMinutes ?: 15
    GlassButton(
        if (retry != null) "$minutes минут — снова ${ruWhen(retry)}" else "$minutes минут бесплатно",
        onClick = onStart,
        icon = LiptonIcons.Timer,
        height = 44.dp,
        enabled = retry == null && state.profileBusy != "daily",
        modifier = Modifier.widthIn(min = 220.dp),
    )
}
