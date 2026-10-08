package com.lipton.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lipton.vpn.ui.theme.LiptonColors
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.TABULAR_NUMS

/** Тон капсулы: совпадает с палитрами свечения. */
enum class CapsuleTone { ACCENT, WARN, BYPASS, NEUTRAL }

/** Цвет тона для рамки/иконки (в светлой теме — более насыщенный, как в макетах). */
fun LiptonColors.toneColor(tone: CapsuleTone): Color = when (tone) {
    CapsuleTone.ACCENT  -> if (isDark) accent else accentDeep
    CapsuleTone.WARN    -> warn
    CapsuleTone.BYPASS  -> if (isDark) bypass else bypassDeep
    CapsuleTone.NEUTRAL -> if (isDark) Color.White else text
}

/**
 * Капсула-статус «Базовый · 24 дня» / «Нет подписки» / «15 мин»:
 * высота 28, рамка тона .5, заливка тона .08, текст 12sp.
 */
@Composable
fun Capsule(
    text: String,
    modifier: Modifier = Modifier,
    tone: CapsuleTone = CapsuleTone.ACCENT,
    secondary: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = LiptonTheme.colors
    val toneColor = c.toneColor(tone)
    val shape = RoundedCornerShape(999.dp)
    val borderAlpha = if (tone == CapsuleTone.NEUTRAL) 0.18f else 0.5f
    val fillAlpha = if (tone == CapsuleTone.NEUTRAL) 0.07f else 0.08f
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(shape)
            .background(toneColor.copy(alpha = fillAlpha))
            .border(1.dp, toneColor.copy(alpha = borderAlpha), shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(PaddingValues(start = if (icon != null) 10.dp else 12.dp, end = 12.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TABULAR_NUMS)
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = toneColor, modifier = Modifier.size(14.dp))
        }
        Text(text, style = style, color = c.text, maxLines = 1)
        if (secondary != null) {
            Text("·", style = style, color = c.text.copy(alpha = 0.5f), maxLines = 1)
            Text(
                secondary,
                style = style.copy(fontWeight = FontWeight.Medium),
                color = c.text.copy(alpha = 0.72f),
                maxLines = 1,
            )
        }
    }
}
