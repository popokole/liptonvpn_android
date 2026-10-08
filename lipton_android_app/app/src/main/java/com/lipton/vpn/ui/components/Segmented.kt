package com.lipton.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.Tokens
import kotlin.math.roundToInt

/** Один вариант сегментированного переключателя. */
data class SegmentOption<T>(val value: T, val label: String, val icon: ImageVector? = null)

/**
 * Сегментированный переключатель по макету (тема «Тёмная / Светлая / Системная»):
 * дорожка 40dp с полем 3, бегунок 32dp едет к выбранному варианту за 0,5 с.
 * Выбор отображается сразу (оптимистично), [onSelect] получает и центр нажатого
 * варианта в координатах корня — для раскрытия темы кругом.
 */
@Composable
fun <T> Segmented(
    options: List<SegmentOption<T>>,
    selected: T,
    onSelect: (value: T, centerInRoot: Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(999.dp)
    var local by remember(selected) { mutableStateOf(selected) }
    val index = options.indexOfFirst { it.value == local }.coerceAtLeast(0)
    val coords = remember(options.size) { arrayOfNulls<LayoutCoordinates>(options.size) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .background(c.segTrack)
            .border(1.dp, c.segBorder, shape)
            .padding(3.dp),
    ) {
        val gap = 4.dp
        val itemWidth = (maxWidth - gap * (options.size - 1)) / options.size
        val itemWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { (itemWidth + gap).toPx() }
        val thumbX = remember { Animatable(index.toFloat()) }
        LaunchedEffect(index) {
            thumbX.animateTo(index.toFloat(), tween(Tokens.Motion.SEGMENT_THUMB_MS, easing = Tokens.Motion.standard))
        }

        // Бегунок
        Box(
            Modifier
                .offset { IntOffset((thumbX.value * itemWidthPx).roundToInt(), 0) }
                .width(itemWidth)
                .fillMaxHeight()
                .drawBehind {
                    val r = CornerRadius(size.height / 2f)
                    // тень 0 4 12
                    softGlow(Color.Black.copy(alpha = if (c.isDark) 0.18f else 0.08f), size.height / 2f, 5.dp.toPx(), 3.dp.toPx(), 4)
                    drawRoundRect(color = c.segThumb, cornerRadius = r)
                    drawRoundRect(
                        brush = Brush.verticalGradient(0f to c.glassInset.copy(alpha = if (c.isDark) 0.16f else 1f), 0.15f to Color.Transparent),
                        cornerRadius = r,
                    )
                },
        )

        Row(
            Modifier.fillMaxWidth().fillMaxHeight().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            options.forEachIndexed { i, opt ->
                val isSel = i == index
                val color by animateColorAsState(
                    if (isSel) c.segActiveText else c.segInactiveText,
                    tween(Tokens.Motion.SEGMENT_THUMB_MS, easing = Tokens.Motion.standard),
                    label = "seg_color",
                )
                Row(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(shape)
                        .onGloballyPositioned { coords[i] = it }
                        .selectable(
                            selected = isSel,
                            role = Role.RadioButton,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (local != opt.value) {
                                local = opt.value
                                val lc = coords[i]
                                val center = if (lc != null && lc.isAttached) {
                                    lc.positionInRoot() + Offset(lc.size.width / 2f, lc.size.height / 2f)
                                } else Offset.Unspecified
                                onSelect(opt.value, center)
                            }
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (opt.icon != null) {
                        Icon(opt.icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                        Box(Modifier.width(6.dp))
                    }
                    Text(
                        opt.label,
                        color = color,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
