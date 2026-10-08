package com.lipton.vpn.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

// ─── Тема приложения ─────────────────────────────────────────────────────────

/** Тёмная / Светлая / Системная — одинаковые названия во всех клиентах. */
enum class AppTheme(val title: String) {
    DARK("Тёмная"),
    LIGHT("Светлая"),
    SYSTEM("Системная");

    companion object {
        val DEFAULT = SYSTEM

        /** Разбор сохранённого значения. Старая тема HACKER переводится в DARK. */
        fun fromStored(raw: String?): AppTheme = when (raw) {
            null       -> DEFAULT
            "HACKER"   -> DARK
            else       -> entries.firstOrNull { it.name == raw } ?: DEFAULT
        }
    }
}

/** Итоговая тёмность для выбранной темы. */
fun AppTheme.isDark(systemDark: Boolean): Boolean = when (this) {
    AppTheme.DARK   -> true
    AppTheme.LIGHT  -> false
    AppTheme.SYSTEM -> systemDark
}

// ─── Цвета ───────────────────────────────────────────────────────────────────

/**
 * Цвета темы. Верхний блок — новые токены редизайна, нижний — поля старых
 * экранов (bgDeep, cardBg, …): им заданы значения из новых токенов, чтобы
 * старые экраны уже выглядели в новой палитре, пока их не перерисуют (A4).
 */
@Immutable
data class LiptonColors(
    val isDark:          Boolean,
    // Новые токены
    val bg:              Color,
    val text:            Color,
    val text2:           Color,
    val text3:           Color,
    val textMuted:       Color,
    val accent:          Color,
    val accentBright:    Color,
    val accentSoft:      Color,
    val accentDeep:      Color,
    val onAccent:        Color,
    val cyan:            Color,
    val blue:            Color,
    val warn:            Color,
    val warnSoft:        Color,
    val bypass:          Color,
    val bypassDeep:      Color,
    val violet:          Color,
    val danger:          Color,
    val glassFill:       Color,
    val glassHighlight:  Color,
    val glassBorder:     Color,
    val glassInset:      Color,
    val glassPressed:    Color,
    val divider:         Color,
    val iconTileTop:     Color,
    val iconTileBottom:  Color,
    val navFill:         Color,
    val navBorder:       Color,
    val navActiveFill:   Color,
    val navActiveText:   Color,
    val navInactive:     Color,
    val segTrack:        Color,
    val segBorder:       Color,
    val segThumb:        Color,
    val segActiveText:   Color,
    val segInactiveText: Color,
    val switchOnStart:   Color,
    val switchOnEnd:     Color,
    val switchOnGlow:    Color,
    val switchOff:       Color,
    val switchOffRing:   Color,
    val btnGlassBorder:  Color,
    val btnGlassFill:    Color,
    val btnGlassTop:     Color,
    val btnPrimaryText:  Color,
    val btnPrimaryRing:  Color,
    val pillDarkFill:    Color,
    val pillDarkBorder:  Color,
    val surface:         Color,
    val surfaceSheet:    Color,
    // ── Поля старых экранов (до перерисовки в A4) ──
    val bgDeep:          Color,
    val bgCard:          Color,
    val bgSheet:         Color,
    val cardBg:          Color,
    val cardBorder:      Color,
    val cardHover:       Color,
    val greenCard:       Color,
    val greenBorder:     Color,
    val textPrimary:     Color,
    val textSecondary:   Color,
    val textTertiary:    Color,
)

fun darkLiptonColors(): LiptonColors = with(Tokens.Dark) {
    LiptonColors(
        isDark = true,
        bg = bg, text = text, text2 = text2, text3 = text3, textMuted = textMuted,
        accent = accent, accentBright = accentBright, accentSoft = accentSoft, accentDeep = accentDeep,
        onAccent = onAccent, cyan = cyan, blue = blue, warn = warn, warnSoft = warnSoft,
        bypass = bypass, bypassDeep = bypassDeep, violet = violet, danger = danger,
        glassFill = glassFill, glassHighlight = glassHighlight, glassBorder = glassBorder,
        glassInset = glassInset, glassPressed = glassPressed, divider = divider,
        iconTileTop = iconTileTop, iconTileBottom = iconTileBottom,
        navFill = navFill, navBorder = navBorder, navActiveFill = navActiveFill,
        navActiveText = navActiveText, navInactive = navInactive,
        segTrack = segTrack, segBorder = segBorder, segThumb = segThumb,
        segActiveText = segActiveText, segInactiveText = segInactiveText,
        switchOnStart = switchOnStart, switchOnEnd = switchOnEnd, switchOnGlow = switchOnGlow,
        switchOff = switchOff, switchOffRing = switchOffRing,
        btnGlassBorder = btnGlassBorder, btnGlassFill = btnGlassFill, btnGlassTop = btnGlassTop,
        btnPrimaryText = btnPrimaryText, btnPrimaryRing = btnPrimaryRing,
        pillDarkFill = pillDarkFill, pillDarkBorder = pillDarkBorder,
        surface = surface, surfaceSheet = surfaceSheet,
        bgDeep = bg, bgCard = surface, bgSheet = surfaceSheet,
        cardBg = glassFill, cardBorder = glassBorder, cardHover = glassPressed,
        greenCard = accent.copy(alpha = 0.08f), greenBorder = accent.copy(alpha = 0.32f),
        textPrimary = text, textSecondary = text2, textTertiary = text3,
    )
}

fun lightLiptonColors(): LiptonColors = with(Tokens.Light) {
    LiptonColors(
        isDark = false,
        bg = bg, text = text, text2 = text2, text3 = text3, textMuted = textMuted,
        accent = accent, accentBright = accentBright, accentSoft = accentSoft, accentDeep = accentDeep,
        onAccent = onAccent, cyan = cyan, blue = blue, warn = warn, warnSoft = warnSoft,
        bypass = bypass, bypassDeep = bypassDeep, violet = violet, danger = danger,
        glassFill = glassFill, glassHighlight = glassHighlight, glassBorder = glassBorder,
        glassInset = glassInset, glassPressed = glassPressed, divider = divider,
        iconTileTop = iconTileTop, iconTileBottom = iconTileBottom,
        navFill = navFill, navBorder = navBorder, navActiveFill = navActiveFill,
        navActiveText = navActiveText, navInactive = navInactive,
        segTrack = segTrack, segBorder = segBorder, segThumb = segThumb,
        segActiveText = segActiveText, segInactiveText = segInactiveText,
        switchOnStart = switchOnStart, switchOnEnd = switchOnEnd, switchOnGlow = switchOnGlow,
        switchOff = switchOff, switchOffRing = switchOffRing,
        btnGlassBorder = btnGlassBorder, btnGlassFill = btnGlassFill, btnGlassTop = btnGlassTop,
        btnPrimaryText = btnPrimaryText, btnPrimaryRing = btnPrimaryRing,
        pillDarkFill = pillDarkFill, pillDarkBorder = pillDarkBorder,
        surface = surface, surfaceSheet = surfaceSheet,
        bgDeep = bg, bgCard = surface, bgSheet = surfaceSheet,
        cardBg = glassFill, cardBorder = Color(0x140E1412), cardHover = glassPressed,
        greenCard = accentDeep.copy(alpha = 0.08f), greenBorder = accentDeep.copy(alpha = 0.40f),
        textPrimary = text, textSecondary = text2, textTertiary = text3,
    )
}

val LocalLiptonColors = staticCompositionLocalOf { darkLiptonColors() }

/** true — пользователь отключил анимации (ANIMATOR_DURATION_SCALE = 0): свечение статичное. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Удобный доступ: `LiptonTheme.colors.accent`. */
object LiptonTheme {
    val colors: LiptonColors
        @Composable @ReadOnlyComposable get() = LocalLiptonColors.current
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
}

// ─── Старые акцентные константы ──────────────────────────────────────────────
// Старые экраны используют их напрямую (~150 мест). Чтобы в светлой теме не было
// неконтрастного изумруда на светлом фоне, они читают текущую тёмность темы.
// TODO(redesign): A4 — перевести экраны на LiptonTheme.colors и удалить этот блок.

private object LegacyPalette {
    var dark by mutableStateOf(true)
}

private fun legacy(dark: Color, light: Color): Color = if (LegacyPalette.dark) dark else light

val Green:     Color get() = legacy(Tokens.Dark.accent, Tokens.Light.accentDeep)
// Старые кнопки — градиент Green → Green3 с чёрным текстом: в светлой теме
// второй цвет светлее (#12C97C), иначе чёрный текст на тёмно-зелёном не читается.
val Green2:    Color get() = legacy(Tokens.Dark.accentDeep, Tokens.Light.accentBright)
val Green3:    Color get() = legacy(Tokens.Dark.accentDeep, Tokens.Light.accentBright)
val GreenGlow: Color get() = Green.copy(alpha = 0.32f)
val GreenSoft: Color get() = Green.copy(alpha = 0.10f)
val GreenMid:  Color get() = Green.copy(alpha = 0.18f)
val Red:       Color get() = legacy(Tokens.Dark.danger, Tokens.Light.danger)
val RedSoft:   Color get() = Red.copy(alpha = 0.12f)
val Amber:     Color get() = legacy(Tokens.Dark.warnSoft, Tokens.Light.warnSoft)
val AmberSoft: Color get() = Amber.copy(alpha = 0.12f)
val Blue:      Color get() = legacy(Tokens.Dark.cyan, Tokens.Light.cyan)
val BlueSoft:  Color get() = Blue.copy(alpha = 0.20f)

// ─── LiptonTheme ─────────────────────────────────────────────────────────────

@Composable
fun LiptonTheme(appTheme: AppTheme = AppTheme.DEFAULT, content: @Composable () -> Unit) {
    val dark = appTheme.isDark(isSystemInDarkTheme())
    val lc = remember(dark) { if (dark) darkLiptonColors() else lightLiptonColors() }
    SideEffect { LegacyPalette.dark = dark }

    // Окно рисуется edge-to-edge: иконки статус-бара и навигации — тёмные в светлой теме.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    val m3 = remember(lc) {
        if (lc.isDark) darkColorScheme(
            primary = lc.accent, onPrimary = lc.onAccent,
            primaryContainer = lc.greenCard, onPrimaryContainer = lc.text,
            secondary = lc.cyan, onSecondary = lc.onAccent,
            tertiary = lc.bypass,
            background = lc.bg, onBackground = lc.text,
            surface = lc.surface, onSurface = lc.text,
            surfaceVariant = lc.surfaceSheet, onSurfaceVariant = lc.text2,
            surfaceContainer = lc.surfaceSheet, surfaceContainerHigh = lc.surfaceSheet,
            error = lc.danger, onError = Color.White,
            outline = lc.glassBorder, outlineVariant = lc.divider,
        ) else lightColorScheme(
            primary = lc.accentDeep, onPrimary = Color.White,
            primaryContainer = lc.greenCard, onPrimaryContainer = lc.text,
            secondary = lc.cyan, onSecondary = Color.White,
            tertiary = lc.bypass,
            background = lc.bg, onBackground = lc.text,
            surface = lc.surface, onSurface = lc.text,
            surfaceVariant = lc.surfaceSheet, onSurfaceVariant = lc.text2,
            surfaceContainer = lc.surfaceSheet, surfaceContainerHigh = lc.surfaceSheet,
            error = lc.danger, onError = Color.White,
            outline = Color(0x1F0E1412), outlineVariant = lc.divider,
        )
    }

    CompositionLocalProvider(
        LocalLiptonColors provides lc,
        LocalReduceMotion provides rememberSystemReduceMotion(),
    ) {
        MaterialTheme(colorScheme = m3, typography = LiptonTypography, content = content)
    }
}

/** Системное «Отключить анимации». Перечитывается при возврате в приложение. */
@Composable
private fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    fun read(): Boolean = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)
    var reduce by remember { mutableStateOf(read()) }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) reduce = read() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return reduce
}
