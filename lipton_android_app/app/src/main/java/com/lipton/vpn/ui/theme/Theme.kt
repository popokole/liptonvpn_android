package com.lipton.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ─── Accent / status colors (never change between themes) ────────────────────

val Green         = Color(0xFF34D058)
val Green2        = Color(0xFF25A244)
val Green3        = Color(0xFF1A7A32)
val GreenGlow     = Color(0x5234D058)
val GreenSoft     = Color(0x1A34D058)
val GreenMid      = Color(0x2E34D058)
val Red           = Color(0xFFFF453A)
val RedSoft       = Color(0x1FFF453A)
val Amber         = Color(0xFFFF9F0A)
val AmberSoft     = Color(0x1FFF9F0A)
val Blue          = Color(0xFF40B3FF)
val BlueSoft      = Color(0x330088CC)

// ─── Theme-dependent colors ───────────────────────────────────────────────────

data class LiptonColors(
    val bgDeep:        Color,
    val bgCard:        Color,
    val bgSheet:       Color,
    val cardBg:        Color,
    val cardBorder:    Color,
    val cardHover:     Color,
    val greenCard:     Color,
    val greenBorder:   Color,
    val textPrimary:   Color,
    val textSecondary: Color,
    val textTertiary:  Color,
    val isDark:        Boolean,
)

fun darkLiptonColors() = LiptonColors(
    bgDeep        = Color(0xFF060C08),
    bgCard        = Color(0xFF0B1410),
    bgSheet       = Color(0xFF111A13),
    cardBg        = Color(0x0D34D058),
    cardBorder    = Color(0x1F34D058),
    cardHover     = Color(0x1734D058),
    greenCard     = Color(0x0F34D058),
    greenBorder   = Color(0x2434D058),
    textPrimary   = Color(0xFFEDFFF2),
    textSecondary = Color(0x8CB4F0C3),
    textTertiary  = Color(0x4D78BE8C),
    isDark        = true,
)

fun lightLiptonColors() = LiptonColors(
    bgDeep        = Color(0xFFF2FBF4),
    bgCard        = Color(0xFFE4F2E8),
    bgSheet       = Color(0xFFEFF8F2),
    cardBg        = Color(0x2834D058),
    cardBorder    = Color(0x4034D058),
    cardHover     = Color(0x3034D058),
    greenCard     = Color(0x2034D058),
    greenBorder   = Color(0x4A34D058),
    textPrimary   = Color(0xFF0C1F10),
    textSecondary = Color(0xFF1F5228),
    textTertiary  = Color(0xFF4D8C5B),
    isDark        = false,
)

val LocalLiptonColors = staticCompositionLocalOf { darkLiptonColors() }

// ─── App theme enum ───────────────────────────────────────────────────────────

enum class AppTheme { SYSTEM, DARK, LIGHT }

// ─── LiptonTheme ─────────────────────────────────────────────────────────────

@Composable
fun LiptonTheme(appTheme: AppTheme = AppTheme.SYSTEM, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (appTheme) {
        AppTheme.DARK   -> true
        AppTheme.LIGHT  -> false
        AppTheme.SYSTEM -> systemDark
    }
    val lc = if (isDark) darkLiptonColors() else lightLiptonColors()
    val m3 = if (isDark) {
        darkColorScheme(
            primary = Green, onPrimary = Color.Black,
            primaryContainer = lc.greenCard, secondary = Green2,
            background = lc.bgDeep, surface = lc.bgCard,
            onBackground = lc.textPrimary, onSurface = lc.textPrimary,
            error = Red, outline = lc.cardBorder,
        )
    } else {
        lightColorScheme(
            primary = Green2, onPrimary = Color.White,
            primaryContainer = lc.greenCard, secondary = Green2,
            background = lc.bgDeep, surface = lc.bgCard,
            onBackground = lc.textPrimary, onSurface = lc.textPrimary,
            error = Red, outline = lc.cardBorder,
        )
    }
    CompositionLocalProvider(LocalLiptonColors provides lc) {
        MaterialTheme(colorScheme = m3, typography = Typography(), content = content)
    }
}
