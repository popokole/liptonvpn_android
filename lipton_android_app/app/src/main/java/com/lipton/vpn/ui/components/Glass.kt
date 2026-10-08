package com.lipton.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lipton.vpn.ui.theme.LiptonColors
import com.lipton.vpn.ui.theme.LiptonText
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.Tokens

/**
 * Стекло по макету: заливка (.06 тёмная / .55 светлая), сверху блик-градиент
 * (.08 → 0 на 56dp), рамка 1dp (.10 / .85) и inset-блик по верхней кромке.
 *
 * Настоящего backdrop-blur нет: на minSdk 24 его не сделать без дорогих
 * захватов фона, а на слабых телефонах это заметно тормозит. Под стеклом
 * лежит только размытое свечение «Аврора», поэтому разница почти не видна.
 */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(Tokens.Radius.tile),
    highlightHeight: Dp = 56.dp,
    pressed: Boolean = false,
): Modifier = composed {
    val c = LiptonTheme.colors
    glassWith(c, shape, highlightHeight, pressed)
}

internal fun Modifier.glassWith(
    c: LiptonColors,
    shape: Shape,
    highlightHeight: Dp = 56.dp,
    pressed: Boolean = false,
): Modifier = this
    .clip(shape)
    .background(if (pressed) c.glassPressed else c.glassFill)
    .drawBehind {
        val h = highlightHeight.toPx().coerceAtMost(size.height)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(c.glassHighlight, Color.Transparent),
                startY = 0f, endY = h,
            ),
            size = size.copy(height = h),
            topLeft = Offset.Zero,
        )
    }
    .border(1.dp, c.glassBorder, shape)
    .border(1.dp, Brush.verticalGradient(0f to c.glassInset, 0.12f to Color.Transparent), shape)

/** Стеклянная карточка/плитка бенто (радиус 24, внутренние поля 16). */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Tokens.Radius.tile),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = if (pressed && onClick != null) 0.985f else 1f
    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .glass(shape = shape, pressed = pressed && onClick != null)
            .let { m ->
                if (onClick != null) m.clickable(interactionSource = interaction, indication = null, onClick = onClick) else m
            }
            .padding(contentPadding),
        content = content,
    )
}

/** Плитка — то же стекло, но с Box-раскладкой. */
@Composable
fun GlassTile(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Tokens.Radius.tile),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.glass(shape = shape).padding(contentPadding),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/** Иконка в плашке 32dp с радиусом 10 (строки настроек). */
@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = LiptonTheme.colors.accentDeepOrAccent()) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(Tokens.Radius.icon)
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c.iconTileTop, c.iconTileBottom)))
            .border(1.dp, if (c.isDark) c.glassBorder else Color(0x0F0E1412), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

/** Акцент для иконок: изумруд в тёмной, #0FA968 в светлой (как в макетах). */
fun LiptonColors.accentDeepOrAccent(): Color = if (isDark) accent else accentDeep

/** Заголовок секции: 13sp, 600, трекинг 0.08em, верхний регистр. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = LiptonText.section,
        color = LiptonTheme.colors.text3,
        modifier = modifier.padding(start = 16.dp),
        maxLines = 1,
    )
}
