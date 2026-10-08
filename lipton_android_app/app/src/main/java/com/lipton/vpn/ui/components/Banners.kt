package com.lipton.vpn.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lipton.vpn.BuildConfig
import com.lipton.vpn.data.ApiClient
import com.lipton.vpn.data.model.AppBanner
import com.lipton.vpn.ui.theme.LiptonColors
import com.lipton.vpn.ui.theme.LiptonTheme

// ─────────────────────────────────────────────────────────────────────────────
//  Баннеры и экраны из админки (GET /app/banners): баннер в слоте над главной,
//  полноэкранный экран (kind=screen) и блокирующий экран обязательного
//  обновления (kind=update, dismissible=false).
// ─────────────────────────────────────────────────────────────────────────────

/** Адрес установщика Android: 302 на актуальный APK. */
const val DOWNLOAD_ANDROID_URL = ApiClient.API_BASE + "/download/android"

/** Ссылка из админки: относительные пути («/download/android») — от нашего сайта. */
fun bannerUrl(b: AppBanner): String? {
    val u = b.ctaUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?: return if (b.kind == "update") DOWNLOAD_ANDROID_URL else null
    return if (u.startsWith("/")) ApiClient.API_BASE + u else u
}

private fun LiptonColors.bannerColor(b: AppBanner): Color = when {
    b.style == "warning" -> warn
    b.style == "promo" -> if (isDark) bypass else bypassDeep
    else -> if (isDark) accent else accentDeep
}

private fun bannerIcon(b: AppBanner): ImageVector = when {
    b.kind == "update" -> LiptonIcons.Download
    b.style == "warning" -> LiptonIcons.Alert
    b.style == "promo" -> LiptonIcons.Gift
    else -> LiptonIcons.Megaphone
}

/** Баннер в слоте над главной: иконка, заголовок, текст, кнопка и «×» (если можно закрыть). */
@Composable
fun AppBannerCard(banner: AppBanner, onCta: () -> Unit, onClose: () -> Unit) {
    val c = LiptonTheme.colors
    val tone = c.bannerColor(banner)
    val shape = RoundedCornerShape(20.dp)
    val hasCta = bannerUrl(banner) != null
    Column(
        Modifier
            .fillMaxWidth()
            .glass(shape)
            .border(1.dp, tone.copy(alpha = 0.35f), shape)
            .let { if (hasCta) it.clickable(role = Role.Button, onClick = onCta) else it }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToneCircleIcon(bannerIcon(banner), tone, size = 36.dp, iconSize = 16.dp)
            Column(Modifier.weight(1f)) {
                Text(banner.title, style = MaterialTheme.typography.titleSmall, color = c.text)
                banner.text?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.72f), modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (banner.dismissible) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = "Скрыть" },
                    contentAlignment = Alignment.Center,
                ) { Icon(LiptonIcons.Close, null, tint = c.text.copy(alpha = 0.55f), modifier = Modifier.size(13.dp)) }
            }
        }
        if (hasCta) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().padding(start = 48.dp)) {
                GlassButton(banner.ctaText?.takeIf { it.isNotBlank() } ?: if (banner.kind == "update") "Обновить" else "Подробнее", onClick = onCta, height = 32.dp)
            }
        }
    }
}

/** Полноэкранный экран из админки (kind=screen): свечение, иконка, текст, кнопка. */
@Composable
fun AppBannerScreen(banner: AppBanner, onCta: () -> Unit, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = { if (banner.dismissible) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = banner.dismissible, dismissOnClickOutside = false, decorFitsSystemWindows = false),
    ) {
        val tone = when (banner.style) {
            "warning" -> AuroraTone.OFF
            "promo" -> AuroraTone.BYPASS
            else -> AuroraTone.ON
        }
        FullScreenMessage(
            tone = tone,
            icon = bannerIcon(banner),
            title = banner.title,
            text = banner.text,
            cta = banner.ctaText?.takeIf { it.isNotBlank() } ?: if (bannerUrl(banner) != null) "Подробнее" else "Понятно",
            onCta = onCta,
            secondary = if (banner.dismissible && bannerUrl(banner) != null) "Не сейчас" else null,
            onSecondary = onClose,
            onClose = if (banner.dismissible) onClose else null,
        )
    }
}

/**
 * Обязательное обновление (kind=update, dismissible=false): закрыть нельзя,
 * только «Обновить» — ссылка из баннера или /download/android.
 */
@Composable
fun ForceUpdateScreen(banner: AppBanner, onUpdate: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    FullScreenMessage(
        tone = AuroraTone.OFF,
        icon = LiptonIcons.Download,
        title = banner.title.ifBlank { "Обновите приложение" },
        text = banner.text?.takeIf { it.isNotBlank() } ?: "Эта версия больше не поддерживается. Установите новую — подписка и настройки сохранятся.",
        cta = banner.ctaText?.takeIf { it.isNotBlank() } ?: "Обновить",
        onCta = onUpdate,
        footnote = "Версия ${BuildConfig.VERSION_NAME}",
    )
}

@Composable
private fun FullScreenMessage(
    tone: AuroraTone,
    icon: ImageVector,
    title: String,
    text: String?,
    cta: String,
    onCta: () -> Unit,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    footnote: String? = null,
) {
    val c = LiptonTheme.colors
    val t = c.stateTone(tone)
    AuroraBackground(tone = tone, layout = AuroraLayout.ONBOARDING) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.CenterEnd) {
                if (onClose != null) {
                    Box(
                        Modifier.size(40.dp).glass(CircleShape, highlightHeight = 20.dp).clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = "Закрыть" },
                        contentAlignment = Alignment.Center,
                    ) { Icon(LiptonIcons.Close, null, tint = c.text, modifier = Modifier.size(16.dp)) }
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(96.dp)
                    .drawBehind { softGlow(t.a.copy(alpha = 0.45f), size.minDimension / 2, 26.dp.toPx()) }
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(t.a.copy(alpha = 0.3f), t.b.copy(alpha = 0.18f))))
                    .border(1.dp, t.a.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = c.stateText(tone), modifier = Modifier.size(40.dp)) }
            Spacer(Modifier.height(28.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
                color = c.text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            if (!text.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(text, style = MaterialTheme.typography.bodyLarge, color = c.text.copy(alpha = 0.78f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                cta, onClick = onCta, trailingIcon = LiptonIcons.ArrowRight,
                tone = when (tone) { AuroraTone.OFF, AuroraTone.NO_SUB -> ButtonTone.WARN; AuroraTone.BYPASS -> ButtonTone.BYPASS; else -> ButtonTone.ACCENT },
                modifier = Modifier.fillMaxWidth(),
            )
            if (secondary != null) {
                Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    GhostButton(secondary, onClick = onSecondary, color = c.text.copy(alpha = 0.7f))
                }
            }
            if (footnote != null) {
                Text(footnote, style = MaterialTheme.typography.labelMedium, color = c.text.copy(alpha = 0.45f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
