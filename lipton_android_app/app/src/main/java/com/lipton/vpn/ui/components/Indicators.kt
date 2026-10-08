package com.lipton.vpn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.ui.theme.LiptonColors
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.TABULAR_NUMS

// ─────────────────────────────────────────────────────────────────────────────
//  Мелкие индикаторы экранов редизайна: точка-статус, шкала пинга, полоса
//  прогресса, спарклайн, столбики, шапка плитки бенто, чип.
// ─────────────────────────────────────────────────────────────────────────────

/** Пара цветов состояния: основной и второй (градиенты плиток, столбиков). */
data class StateTone(val a: Color, val b: Color)

/** Цвета состояния под палитру свечения: вкл — изумруд/бирюза, выкл — рыжий, «Обход» — синий/фиолетовый. */
fun LiptonColors.stateTone(tone: AuroraTone): StateTone = when (tone) {
    AuroraTone.ON -> if (isDark) StateTone(Color(0xFF22E58A), Color(0xFF22D3EE)) else StateTone(Color(0xFF0FA968), Color(0xFF0891B2))
    AuroraTone.OFF, AuroraTone.NO_SUB ->
        if (isDark) StateTone(Color(0xFFFF7A1A), Color(0xFFFFA24C)) else StateTone(Color(0xFFE8650C), Color(0xFFC95A0E))
    AuroraTone.BYPASS -> if (isDark) StateTone(Color(0xFF6E8BFF), Color(0xFF8B5CF6)) else StateTone(Color(0xFF3B5BFF), Color(0xFF7048E8))
}

/** Цвет текста-акцента состояния (в светлой теме — темнее, для контраста). */
fun LiptonColors.stateText(tone: AuroraTone): Color = when (tone) {
    AuroraTone.ON -> if (isDark) Color(0xFF22E58A) else Color(0xFF067A4B)
    AuroraTone.OFF, AuroraTone.NO_SUB -> if (isDark) Color(0xFFFFA24C) else Color(0xFFC2510A)
    AuroraTone.BYPASS -> if (isDark) Color(0xFFA9BCFF) else Color(0xFF3B5BFF)
}

/** Точка-статус 6dp с кольцом .18 и свечением. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Dp = 6.dp, glow: Boolean = true) {
    Box(
        modifier
            .size(size)
            .drawBehind {
                if (glow) {
                    val r = this.size.minDimension / 2f
                    drawCircle(color.copy(alpha = 0.18f), radius = r + 3.dp.toPx())
                    softGlow(color.copy(alpha = 0.6f), r, 5.dp.toPx())
                }
            }
            .clip(CircleShape)
            .background(color),
    )
}

/** Уровень шкалы пинга 0..4 (0 — нет данных). */
fun pingLevel(ms: Long?): Int = when {
    ms == null || ms <= 0 -> 0
    ms <= 60 -> 4
    ms <= 100 -> 3
    ms <= 160 -> 2
    else -> 1
}

/** Цвет пинга: быстро — акцент, средне — янтарный, медленно — оранжево-красный. */
fun LiptonColors.pingColor(ms: Long?): Color = when {
    ms == null || ms <= 0 -> text.copy(alpha = 0.35f)
    ms <= 100 -> if (isDark) Color(0xFF22E58A) else Color(0xFF0FA968)
    ms <= 160 -> if (isDark) Color(0xFFFFB547) else Color(0xFFB86E00)
    else -> if (isDark) Color(0xFFFF8A3D) else Color(0xFFD9480F)
}

/** Шкала пинга: 4 столбика 3dp (высоты 6/9/12/15 или 4/7/10/13). */
@Composable
fun SignalBars(
    level: Int,
    color: Color,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    glow: Boolean = false,
) {
    val c = LiptonTheme.colors
    val heights = if (small) listOf(4, 7, 10, 13) else listOf(6, 9, 12, 15)
    val off = c.text.copy(alpha = if (c.isDark) 0.22f else 0.18f)
    Row(
        modifier.height(heights.last().dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEachIndexed { i, h ->
            val on = i < level
            Box(
                Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .drawBehind { if (on && glow) softGlow(color.copy(alpha = 0.45f), 2.dp.toPx(), 3.dp.toPx()) }
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (on) color else off),
            )
        }
    }
}

/** Полоса прогресса 8dp: дорожка .08, градиент тона со свечением. */
@Composable
fun GradientProgress(fraction: Float, tone: StateTone, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val c = LiptonTheme.colors
    val f = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(c.text.copy(alpha = if (c.isDark) 0.08f else 0.07f)),
    ) {
        if (f > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(Brush.horizontalGradient(listOf(tone.a, tone.b))),
            )
        }
    }
}

/**
 * Спарклайн пинга: линия + заливка градиентом + точка на последнем значении.
 * Пустой список — пунктир «нет подключения».
 */
@Composable
fun Sparkline(values: List<Long>, color: Color, modifier: Modifier = Modifier) {
    val c = LiptonTheme.colors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (values.size < 2) {
            val y = h * 0.6f
            drawLine(
                c.text.copy(alpha = 0.35f), Offset(0f, y), Offset(w - 8.dp.toPx(), y),
                strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
            )
            drawCircle(c.text.copy(alpha = 0.45f), radius = 3.dp.toPx(), center = Offset(w - 4.dp.toPx(), y), style = Stroke(1.5.dp.toPx()))
            return@Canvas
        }
        val min = values.min().toFloat()
        val max = values.max().toFloat()
        val span = (max - min).coerceAtLeast(6f)
        val pad = 4.dp.toPx()
        val pts = values.mapIndexed { i, v ->
            val x = pad + (w - 2 * pad) * i / (values.size - 1)
            val y = pad + (h - 2 * pad) * (1f - (v - min) / span) * 0.8f + (h - 2 * pad) * 0.1f
            Offset(x, y)
        }
        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val p0 = pts[i - 1]; val p1 = pts[i]
                val mx = (p0.x + p1.x) / 2
                cubicTo(mx, p0.y, mx, p1.y, p1.x, p1.y)
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(pts.last().x, h); lineTo(pts.first().x, h); close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.32f), color.copy(alpha = 0f))))
        drawPath(line, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color.copy(alpha = 0.25f), radius = 6.dp.toPx(), center = pts.last())
        drawCircle(color, radius = 3.dp.toPx(), center = pts.last())
    }
}

/** Столбики скорости: последний — яркий со свечением, остальные — с нарастающей прозрачностью. */
@Composable
fun MiniBars(values: List<Long>, tone: StateTone, modifier: Modifier = Modifier, count: Int = 12) {
    val c = LiptonTheme.colors
    Canvas(modifier) {
        val gap = 4.dp.toPx()
        val bw = (size.width - gap * (count - 1)) / count
        val data = values.takeLast(count)
        val max = (data.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
        for (i in 0 until count) {
            val idx = i - (count - data.size)
            val x = i * (bw + gap)
            if (idx < 0) {
                // нет данных — низкие серые «заглушки»
                val hh = 8.dp.toPx()
                drawRoundRect(c.text.copy(alpha = 0.12f), Offset(x, size.height - hh), Size(bw, hh), CornerRadius(2.dp.toPx()))
                continue
            }
            val v = data[idx]
            val hh = (size.height * (v / max)).coerceAtLeast(4.dp.toPx())
            val last = idx == data.size - 1
            val alpha = if (last) 1f else 0.35f + 0.6f * (i.toFloat() / count)
            drawRoundRect(
                Brush.verticalGradient(listOf(tone.a, tone.b.copy(alpha = if (last) 1f else 0.35f)), startY = size.height - hh, endY = size.height),
                Offset(x, size.height - hh), Size(bw, hh), CornerRadius(3.dp.toPx()), alpha = alpha,
            )
        }
    }
}

/** Шапка плитки бенто: иконка 14 + подпись 12/600 (.7) слева, [trailing] справа. */
@Composable
fun TileHeader(icon: ImageVector, title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    val c = LiptonTheme.colors
    Row(modifier.fillMaxWidth().height(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = c.text.copy(alpha = if (c.isDark) 0.7f else 0.64f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = c.text.copy(alpha = if (c.isDark) 0.7f else 0.64f),
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

/** Подпись справа в шапке плитки («за час», «24 из 30 дней»). */
@Composable
fun TileHint(text: String) {
    val c = LiptonTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR_NUMS),
        color = c.text.copy(alpha = 0.55f),
        maxLines = 1,
    )
}

/** Чип 20–24dp: рамка тона .32–.45, заливка .10–.14, текст 12/600. */
@Composable
fun ToneChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    textColor: Color = color,
    icon: ImageVector? = null,
    dot: Boolean = false,
    height: Dp = 24.dp,
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier
            .height(height)
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.38f), shape)
            .padding(start = if (icon != null || dot) 8.dp else 10.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot) StatusDot(color, size = 6.dp, glow = false)
        if (icon != null) Icon(icon, null, tint = textColor, modifier = Modifier.size(13.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = textColor, maxLines = 1)
    }
}

/** Иконка в круге 24/40dp с заливкой тона .14 и рамкой .32. */
@Composable
fun ToneCircleIcon(icon: ImageVector, color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp, iconSize: Dp = 12.dp, glow: Boolean = false) {
    Box(
        modifier
            .size(size)
            .drawBehind { if (glow) softGlow(color.copy(alpha = 0.22f), this.size.minDimension / 2, 10.dp.toPx()) }
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.32f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(iconSize))
    }
}

/** Блик-линия тона по верхней кромке карточки (left/right 28, 1dp). */
fun Modifier.topAccentLine(tone: StateTone): Modifier = drawBehind {
    val inset = 28.dp.toPx()
    drawRect(
        Brush.horizontalGradient(
            0f to tone.a.copy(alpha = 0f), 0.28f to tone.a.copy(alpha = 0.85f),
            0.72f to tone.b.copy(alpha = 0.85f), 1f to tone.b.copy(alpha = 0f),
            startX = inset, endX = size.width - inset,
        ),
        topLeft = Offset(inset, 0f), size = Size(size.width - 2 * inset, 1.dp.toPx()),
    )
}

/** Крупная цифра плитки (Unbounded 30) + единица (14/600 .7). */
@Composable
fun BigValue(value: String, unit: String?, modifier: Modifier = Modifier, dim: Boolean = false) {
    val c = LiptonTheme.colors
    Row(modifier.height(32.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.displayMedium, color = if (dim) c.text.copy(alpha = 0.45f) else c.text, maxLines = 1)
        if (unit != null) {
            Text(
                unit,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                color = c.text.copy(alpha = 0.7f),
                maxLines = 1,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

/** Ряд плиток бенто равной ширины с зазором 12. */
@Composable
fun BentoRow(modifier: Modifier = Modifier, height: Dp, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

/** Вертикальный стек с зазором 12 (бенто). */
@Composable
fun BentoColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
}
