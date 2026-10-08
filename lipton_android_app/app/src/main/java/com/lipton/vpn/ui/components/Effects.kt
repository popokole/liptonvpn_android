package com.lipton.vpn.ui.components

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

/**
 * Мягкое свечение/тень вокруг скруглённого прямоугольника размера DrawScope —
 * аналог CSS `box-shadow: 0 offsetY blur color`, где [spread] ≈ blur / 2 (σ гауссианы).
 *
 * Android 9+: настоящее размытие BlurMaskFilter (оно аппаратно ускорено с API 28).
 * Ниже — несколько слоёв с нарастающим отступом и убывающей прозрачностью.
 */
internal fun DrawScope.softGlow(
    color: Color,
    cornerRadius: Float,
    spread: Float,
    offsetY: Float = 0f,
    steps: Int = 6,
) {
    if (color.alpha <= 0f || spread <= 0f) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // radius BlurMaskFilter → σ = radius·0.57735 + 0.5
        val radius = ((spread - 0.5f) / 0.57735f).coerceAtLeast(1f)
        val paint = GlowPaint.get(color.toArgb(), radius)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(
                0f, offsetY, size.width, size.height + offsetY,
                cornerRadius, cornerRadius, paint,
            )
        }
        return
    }
    val a = color.alpha / steps * 1.6f
    for (i in steps downTo 1) {
        val s = spread * i / steps
        drawRoundRect(
            color = color.copy(alpha = (a * (1f - (i - 1f) / steps)).coerceIn(0f, 1f)),
            topLeft = Offset(-s, -s + offsetY),
            size = Size(size.width + 2 * s, size.height + 2 * s),
            cornerRadius = CornerRadius(cornerRadius + s),
        )
    }
}

/** Кэш Paint с BlurMaskFilter: свечения перерисовываются каждый кадр анимаций. */
private object GlowPaint {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private var argb = 0
    private var radius = -1f

    fun get(color: Int, r: Float): android.graphics.Paint {
        if (color != argb) { paint.color = color; argb = color }
        if (r != radius) { paint.maskFilter = BlurMaskFilter(r, BlurMaskFilter.Blur.NORMAL); radius = r }
        return paint
    }
}
