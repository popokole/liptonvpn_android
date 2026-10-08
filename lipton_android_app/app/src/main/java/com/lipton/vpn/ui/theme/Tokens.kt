package com.lipton.vpn.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
//  Дизайн-токены «Аврора + Бенто».
//  Значения сняты с макетов *.dc.html (390×844) и плана G1.
//  TODO(redesign): когда появится канон scripts/design/tokens.json + gen-tokens.mjs
//  в vpn_beack (пакет G1), заменить этот файл сгенерированной копией. Руками копию
//  после этого не править.
// ─────────────────────────────────────────────────────────────────────────────

object Tokens {

    /** Тёмная тема. */
    object Dark {
        val bg              = Color(0xFF050807)
        val text            = Color(0xFFFFFFFF)
        val text2           = Color(0xB3FFFFFF)   // .70 — подписи
        val text3           = Color(0x8CFFFFFF)   // .55 — заголовки секций, третичный
        val textMuted       = Color(0x73FFFFFF)   // .45

        val accent          = Color(0xFF22E58A)   // изумруд
        val accentBright    = Color(0xFF34F5A3)
        val accentSoft      = Color(0xFF7CF7C4)
        val accentDeep      = Color(0xFF0FA968)
        val onAccent        = Color(0xFF04140C)
        val cyan            = Color(0xFF22D3EE)
        val blue            = Color(0xFF3B82F6)
        val warn            = Color(0xFFFF7A1A)   // «выключено / нет подписки»
        val warnSoft        = Color(0xFFFFA24C)
        val bypass          = Color(0xFF6E8BFF)   // «Обход»
        val bypassDeep      = Color(0xFF3B6BFF)
        val violet          = Color(0xFF8B5CF6)
        val danger          = Color(0xFFFF5A4E)

        // Стекло: градиент-блик .08 → 0 (56dp) поверх заливки .06, рамка .10, inset-блик .10
        val glassFill       = Color(0x0FFFFFFF)
        val glassHighlight  = Color(0x14FFFFFF)
        val glassBorder     = Color(0x1AFFFFFF)
        val glassInset      = Color(0x1AFFFFFF)
        val glassPressed    = Color(0x17FFFFFF)
        val divider         = Color(0x0FFFFFFF)   // .06

        // Иконка-плашка 32dp в строках настроек
        val iconTileTop     = Color(0x1AFFFFFF)
        val iconTileBottom  = Color(0x0AFFFFFF)

        // Нижняя капсула навигации
        // Макет: rgba(16,20,19,.72) + backdrop blur 18. Размытия фона под капсулой нет,
        // поэтому заливка плотнее (.92), иначе текст ленты просвечивает сквозь подписи.
        val navFill         = Color(0xEB101413)
        val navBorder       = Color(0x14FFFFFF)
        val navActiveFill   = Color(0x1AFFFFFF)
        val navActiveText   = Color(0xFFFFFFFF)
        val navInactive     = Color(0x94FFFFFF)   // .58

        // Сегментированный переключатель
        val segTrack        = Color(0x0DFFFFFF)
        val segBorder       = Color(0x14FFFFFF)
        val segThumb        = Color(0x24FFFFFF)   // .14
        val segActiveText   = Color(0xFFFFFFFF)
        val segInactiveText = Color(0x9EFFFFFF)   // .62

        // Тумблер
        val switchOnStart   = Color(0xFF22E58A)
        val switchOnEnd     = Color(0xFF22D3EE)
        val switchOnGlow    = Color(0x6122E58A)   // .38
        val switchOff       = Color(0x24FFFFFF)   // .14
        val switchOffRing   = Color(0x0FFFFFFF)

        // Кнопки
        val btnGlassBorder  = Color(0x2EFFFFFF)   // .18
        val btnGlassFill    = Color(0x12FFFFFF)   // .07
        val btnGlassTop     = Color(0x1AFFFFFF)
        val btnPrimaryText  = Color(0xFF0B0E0D)
        val btnPrimaryRing  = Color(0x0FFFFFFF)   // 0 0 0 6px .06
        val pillDarkFill    = Color(0x6B050807)   // rgba(5,8,7,.42)
        val pillDarkBorder  = Color(0x38FFFFFF)   // .22

        // Опаковые поверхности для старых экранов, шитов и диалогов
        val surface         = Color(0xFF0C110F)
        val surfaceSheet    = Color(0xFF0F1513)
    }

    /** Светлая тема. */
    object Light {
        val bg              = Color(0xFFF3F1EC)
        val text            = Color(0xFF0E1412)
        val text2           = Color(0xA30E1412)   // .64
        val text3           = Color(0x940E1412)   // .58
        val textMuted       = Color(0x800E1412)   // .50

        val accent          = Color(0xFF067A4B)   // акцент текста
        val accentBright    = Color(0xFF12C97C)
        val accentSoft      = Color(0xFF7EF0B8)
        val accentDeep      = Color(0xFF0FA968)
        val onAccent        = Color(0xFFFFFFFF)
        val cyan            = Color(0xFF0891B2)
        val blue            = Color(0xFF2563EB)
        val warn            = Color(0xFFE8650C)
        val warnSoft        = Color(0xFFC95A0E)
        val bypass          = Color(0xFF3B5BFF)
        val bypassDeep      = Color(0xFF3B5BFF)
        val violet          = Color(0xFF7048E8)
        val danger          = Color(0xFFD92D20)

        val glassFill       = Color(0x8CFFFFFF)   // .55
        val glassHighlight  = Color(0x52FFFFFF)   // .32
        val glassBorder     = Color(0xD9FFFFFF)   // .85
        val glassInset      = Color(0xE6FFFFFF)   // .90
        val glassPressed    = Color(0xB3FFFFFF)
        val divider         = Color(0x0F0E1412)

        val iconTileTop     = Color(0xFFFFFFFF)
        val iconTileBottom  = Color(0xB8FFFFFF)

        val navFill         = Color(0xEBFFFFFF)   // макет .70 + blur; без blur — .92
        val navBorder       = Color(0xE6FFFFFF)
        val navActiveFill   = Color(0xFF0E1412)
        val navActiveText   = Color(0xFFFFFFFF)
        val navInactive     = Color(0x940E1412)

        val segTrack        = Color(0x0D0E1412)
        val segBorder       = Color(0x0F0E1412)
        val segThumb        = Color(0xFFFFFFFF)
        val segActiveText   = Color(0xFF0E1412)
        val segInactiveText = Color(0x990E1412)   // .60

        val switchOnStart   = Color(0xFF12C97C)
        val switchOnEnd     = Color(0xFF10B9D4)
        val switchOnGlow    = Color(0x470FA968)   // .28
        val switchOff       = Color(0x1F0E1412)   // .12
        val switchOffRing   = Color(0x0F0E1412)

        val btnGlassBorder  = Color(0x140E1412)   // .08
        val btnGlassFill    = Color(0xDBFFFFFF)   // .86
        val btnGlassTop     = Color(0xFFFFFFFF)
        val btnPrimaryText  = Color(0xFFFFFFFF)
        val btnPrimaryRing  = Color(0x80FFFFFF)   // .50
        val pillDarkFill    = Color(0x8CFFFFFF)
        val pillDarkBorder  = Color(0xD9FFFFFF)

        val surface         = Color(0xFFFFFFFF)
        val surfaceSheet    = Color(0xFFFBFAF7)
    }

    /**
     * Палитры свечения «Аврора» по состоянию. Четыре роли:
     * [0] основное пятно, [1] второе, [2] третье, [3] «ядро» (дышит).
     * В тёмной теме пятна смешиваются Screen, в светлой — Multiply (пастель).
     */
    object Aurora {
        val onDark      = listOf(Color(0xFF22E58A), Color(0xFF22D3EE), Color(0xFF3B82F6), Color(0xFF7CF7C4))
        val onLight     = listOf(Color(0xFF7EF0B8), Color(0xFF6FE3F0), Color(0xFF8DB8FF), Color(0xFF7EF0B8))
        val offDark     = listOf(Color(0xFFFF7A1A), Color(0xFFFFA24C), Color(0xFFFF4E1A), Color(0xFFFFC08A))
        val offLight    = listOf(Color(0xFFFFB57E), Color(0xFFFFC9A0), Color(0xFFFFA285), Color(0xFFFFC9A0))
        val noSubDark   = listOf(Color(0xFFFF7A1A), Color(0xFFFFA24C), Color(0xFFFF6A6A), Color(0xFFFFC08A))
        val noSubLight  = listOf(Color(0xFFFFB57E), Color(0xFFFFC9A0), Color(0xFFFFA8A0), Color(0xFFFFD2AE))
        val bypassDark  = listOf(Color(0xFF3B6BFF), Color(0xFF8B5CF6), Color(0xFF22D3EE), Color(0xFFA9BCFF))
        val bypassLight = listOf(Color(0xFF8DB8FF), Color(0xFFC3B0FF), Color(0xFF6FE3F0), Color(0xFFC3B0FF))

        /** Зерно поверх свечения: overlay .09 в тёмной, multiply .05 в светлой. */
        const val GRAIN_ALPHA_DARK  = 0.09f
        const val GRAIN_ALPHA_LIGHT = 0.05f
    }

    /** Радиусы. */
    object Radius {
        val panel   = 28.dp
        val tile    = 24.dp
        val row     = 16.dp
        val icon    = 10.dp
        val badge   = 9.dp
        val pill    = 999.dp
    }

    /** Отступы. */
    object Space {
        val screen  = 20.dp   // боковые поля экрана (макет: left/right 20)
        val gap     = 12.dp   // зазор между плитками бенто
        val section = 28.dp   // отступ перед заголовком секции
    }

    /** Движение. Анимируются только transform/opacity (и цвет свечения). */
    object Motion {
        /** cubic-bezier(.4,0,.2,1) — основная кривая. */
        val standard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
        /** ease-in-out из CSS — дрейф пятен свечения. */
        val easeInOut: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

        const val PALETTE_CROSSFADE_MS = 1600   // смена палитры свечения (1,4–1,8 с)
        const val THEME_REVEAL_MS      = 1600   // круг смены темы
        const val THEME_REVEAL_DELAY_MS = 350L  // ползунок темы едет первым (~0,5 с)
        const val SEGMENT_THUMB_MS     = 500
        const val BREATH_MS            = 6000   // «дыхание» ядра (scale 1 → 1.08 → 1)
        const val TAB_FADE_MS          = 220
        const val TILE_STAGGER_MS      = 80
    }
}
