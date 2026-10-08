package com.lipton.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lipton.vpn.ui.theme.LiptonColors
import com.lipton.vpn.ui.theme.LiptonText
import com.lipton.vpn.ui.theme.LiptonTheme

/** Тон главной кнопки: от него зависят оттенок заливки и цвет свечения. */
enum class ButtonTone { ACCENT, WARN, BYPASS }

private fun LiptonColors.glowFor(tone: ButtonTone): Color = when (tone) {
    ButtonTone.ACCENT -> if (isDark) Color(0x7022E58A) else Color(0x520FA968)
    ButtonTone.WARN   -> if (isDark) Color(0x80FF7A1A) else Color(0x6BE8650C)
    ButtonTone.BYPASS -> if (isDark) Color(0x705B63FF) else Color(0x523B5BFF)
}

private fun primaryTint(tone: ButtonTone): Color = when (tone) {
    ButtonTone.ACCENT -> Color(0xFFEFFFF6)
    ButtonTone.WARN   -> Color(0xFFFFF4EA)
    ButtonTone.BYPASS -> Color(0xFFF1F2FF)
}

@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    return if (pressed) 0.97f else 1f
}

/**
 * Главная кнопка-пилюля («Подключить», «Оформить подписку»).
 * Тёмная тема — белая с оттенком тона, тёмный текст и цветное свечение;
 * светлая — графитовая #0E1412 с белым текстом. Кольцо 6dp вокруг.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ButtonTone = ButtonTone.ACCENT,
    icon: ImageVector? = null,
    height: Dp = 56.dp,
    enabled: Boolean = true,
    loading: Boolean = false,
    trailingIcon: ImageVector? = null,
) {
    val c = LiptonTheme.colors
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    val glow = c.glowFor(tone)
    val ring = c.btnPrimaryRing
    val large = height >= 48.dp
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.5f }
            .height(height)
            .drawBehind {
                val r = size.height / 2f
                // box-shadow 0 12px 40px (крупная) / 0 6px 24px (малая)
                softGlow(glow, r, if (large) 20.dp.toPx() else 12.dp.toPx(), offsetY = if (large) 12.dp.toPx() else 6.dp.toPx())
                if (large) {
                    val s = 6.dp.toPx()
                    drawRoundRect(
                        color = ring, topLeft = Offset(-s, -s),
                        size = Size(size.width + 2 * s, size.height + 2 * s),
                        cornerRadius = CornerRadius(r + s),
                    )
                }
            }
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (c.isDark) Brush.verticalGradient(listOf(Color.White, primaryTint(tone)))
                else Brush.verticalGradient(listOf(c.text, c.text))
            )
            .clickable(
                interactionSource = source, indication = null,
                enabled = enabled && !loading, role = Role.Button, onClick = onClick,
            )
            .padding(horizontal = if (large) 28.dp else 22.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val content = c.btnPrimaryText
        if (loading) {
            CircularProgressIndicator(color = content, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        }
        Text(
            text,
            style = if (large) LiptonText.buttonLarge else MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
        )
        if (trailingIcon != null && !loading) {
            Icon(trailingIcon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Тёмная стеклянная пилюля «Отключить» (состояние «подключено»):
 * rgba(5,8,7,.42), рамка .22, свечение акцента.
 */
@Composable
fun GlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ButtonTone = ButtonTone.ACCENT,
    icon: ImageVector? = null,
    height: Dp = 56.dp,
    enabled: Boolean = true,
) {
    val c = LiptonTheme.colors
    if (!c.isDark) {
        // В светлой теме макет рисует ту же графитовую пилюлю, что и главная кнопка.
        PrimaryButton(text, onClick, modifier, tone, icon, height, enabled)
        return
    }
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    val glow = c.glowFor(tone).copy(alpha = 0.28f)
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.5f }
            .height(height)
            .drawBehind { softGlow(glow, size.height / 2f, 18.dp.toPx(), offsetY = 10.dp.toPx()) }   // 0 10px 36px
            .clip(shape)
            .background(c.pillDarkFill)
            .border(1.dp, c.pillDarkBorder, shape)
            .border(1.dp, Brush.verticalGradient(0f to Color(0x24FFFFFF), 0.2f to Color.Transparent), shape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(18.dp))
        Text(text, style = LiptonText.buttonLarge, color = c.text, maxLines = 1)
    }
}

/**
 * Малая стеклянная кнопка 36dp («Продлить», «Выбрать»).
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 36.dp,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    val c = LiptonTheme.colors
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.5f }
            .height(height)
            .clip(shape)
            .background(
                if (c.isDark) Brush.verticalGradient(0f to c.btnGlassTop, 0.5f to c.btnGlassFill)
                else Brush.verticalGradient(listOf(c.btnGlassTop, c.btnGlassFill))
            )
            .border(1.dp, c.btnGlassBorder, shape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize), color = c.text, maxLines = 1)
    }
}

/** Текстовая кнопка 44dp без фона («Ввести промокод», «Пропустить»). */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = Color.Unspecified,
    enabled: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = LiptonTheme.colors
    val tint = if (color == Color.Unspecified) c.text.copy(alpha = if (c.isDark) 0.92f else 0.88f) else color
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1)
        trailing()
    }
}
