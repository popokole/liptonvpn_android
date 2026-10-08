package com.lipton.vpn.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.Tokens

/**
 * Тумблер 46×28 по макету: включён — градиент изумруд → бирюза со свечением,
 * выключен — полупрозрачная дорожка; белый бегунок 22dp.
 * Если [onCheckedChange] = null, тумблер только показывает состояние
 * (клик обрабатывает строка-родитель).
 */
@Composable
fun LiptonSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = LiptonTheme.colors
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(250, easing = Tokens.Motion.standard),
        label = "switch",
    )
    val base = modifier.size(width = 46.dp, height = 28.dp)
    val m = if (onCheckedChange != null) {
        base.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
    } else base

    Canvas(m) {
        val r = CornerRadius(size.height / 2f)
        val alpha = if (enabled) 1f else 0.45f
        // Свечение включённого тумблера
        if (progress > 0f) {
            softGlow(
                color = c.switchOnGlow.copy(alpha = c.switchOnGlow.alpha * progress * alpha),
                cornerRadius = size.height / 2f,
                spread = 7.dp.toPx(),
                offsetY = if (c.isDark) 0f else 2.dp.toPx(),
            )
        }
        // Дорожка «выкл»
        drawRoundRect(color = c.switchOff.copy(alpha = c.switchOff.alpha * (1f - progress) * alpha), cornerRadius = r)
        drawRoundRect(
            color = c.switchOffRing.copy(alpha = c.switchOffRing.alpha * (1f - progress) * alpha),
            cornerRadius = r, style = Stroke(1.dp.toPx()),
        )
        // Дорожка «вкл»
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(c.switchOnStart, c.switchOnEnd)),
            alpha = progress * alpha,
            cornerRadius = r,
        )
        // Бегунок: left 3 → 21, top 3, 22dp, тень 0 2 6
        val thumb = 22.dp.toPx()
        val x = 3.dp.toPx() + (18.dp.toPx() * progress)
        val y = 3.dp.toPx()
        drawCircle(
            color = Color.Black.copy(alpha = if (c.isDark) 0.28f else 0.14f),
            radius = thumb / 2f + 1.dp.toPx(),
            center = Offset(x + thumb / 2f, y + thumb / 2f + 1.5.dp.toPx()),
        )
        drawCircle(
            color = Color.White.copy(alpha = if (enabled) 1f else 0.7f),
            radius = thumb / 2f,
            center = Offset(x + thumb / 2f, y + thumb / 2f),
        )
    }
}
