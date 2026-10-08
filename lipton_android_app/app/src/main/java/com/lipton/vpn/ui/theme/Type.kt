package com.lipton.vpn.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as GoogleFontEntry
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lipton.vpn.R

// ─────────────────────────────────────────────────────────────────────────────
//  Шрифты: Onest (интерфейс), Unbounded (крупные цифры и заголовки-цифры),
//  JetBrains Mono (логи). Грузятся через Downloadable Google Fonts (провайдер GMS).
//  Если Google Play Services нет или загрузка не удалась — Compose берёт
//  следующий шрифт цепочки (системный sans-serif / monospace того же веса).
// ─────────────────────────────────────────────────────────────────────────────

private val gmsFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

private fun googleFamily(name: String, fallback: String, weights: List<FontWeight>): FontFamily {
    val gf = GoogleFont(name)
    val fonts = weights.map { w -> GoogleFontEntry(googleFont = gf, fontProvider = gmsFontProvider, weight = w) } +
        weights.map { w -> Font(familyName = DeviceFontFamilyName(fallback), weight = w) }
    return FontFamily(fonts)
}

/** Onest 400–800 — основной шрифт интерфейса. */
val OnestFamily: FontFamily = googleFamily(
    "Onest", "sans-serif",
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold),
)

/** Unbounded 400–700 — таймер сессии, крупные цифры плиток. */
val UnboundedFamily: FontFamily = googleFamily(
    "Unbounded", "sans-serif",
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold),
)

/** JetBrains Mono — логи. */
val MonoFamily: FontFamily = googleFamily(
    "JetBrains Mono", "monospace",
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold),
)

/** Табличные цифры (font-variant-numeric: tabular-nums). */
const val TABULAR_NUMS = "tnum"

private val base = TextStyle(fontFamily = OnestFamily)

/**
 * Типографика по макетам (размер/интерлиньяж/трекинг в CSS px = sp).
 * display* — Unbounded с табличными цифрами; остальное — Onest.
 */
val LiptonTypography = Typography(
    // Таймер сессии 00:45:28
    displayLarge = TextStyle(
        fontFamily = UnboundedFamily, fontWeight = FontWeight.Medium,
        fontSize = 54.sp, lineHeight = 64.sp, letterSpacing = (-0.02).em,
        fontFeatureSettings = TABULAR_NUMS,
    ),
    // Крупные цифры плиток бенто (186,4)
    displayMedium = TextStyle(
        fontFamily = UnboundedFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp, lineHeight = 32.sp, letterSpacing = (-0.03).em,
        fontFeatureSettings = TABULAR_NUMS,
    ),
    displaySmall = TextStyle(
        fontFamily = UnboundedFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.03).em,
        fontFeatureSettings = TABULAR_NUMS,
    ),
    // Заголовок онбординга
    headlineLarge = base.copy(
        fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.025).em,
    ),
    // Заголовок экрана («Серверы», «Профиль»)
    headlineMedium = base.copy(
        fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em,
    ),
    headlineSmall = base.copy(
        fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.02).em,
    ),
    titleLarge = base.copy(
        fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.01).em,
    ),
    titleMedium = base.copy(
        fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em,
    ),
    // Заголовок строки настроек
    titleSmall = base.copy(
        fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.005).em,
    ),
    bodyLarge = base.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = base.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = base.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    // Кнопки
    labelLarge = base.copy(
        fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em,
    ),
    // Подписи плиток, капсулы
    labelMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = base.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 12.sp),
)

/** Дополнительные стили, которых нет в Material3. */
object LiptonText {
    /** Заголовок секции: 13/18, 600, 0.08em, ВЕРХНИЙ РЕГИСТР. */
    val section = base.copy(
        fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.08.em,
    )
    /** Статус над таймером «ЗАЩИЩЕНО»: 12/16, 600, 0.28em. */
    val eyebrow = base.copy(
        fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.28.em,
    )
    /** Крупная кнопка (56dp): 16/20, 600, 0.01em. */
    val buttonLarge = base.copy(
        fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.01.em,
    )
    /** Логи. */
    val mono = TextStyle(fontFamily = MonoFamily, fontSize = 12.sp, lineHeight = 18.sp)
}
