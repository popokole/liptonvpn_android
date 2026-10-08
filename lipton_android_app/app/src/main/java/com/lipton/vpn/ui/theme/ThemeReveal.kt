package com.lipton.vpn.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.hypot
import kotlin.math.max

// ─────────────────────────────────────────────────────────────────────────────
//  Смена темы «раскрытием кругом» из переключателя (new-demo-theme).
//
//  Как сделано: бегунок переключателя сначала доезжает до нового варианта
//  (THEME_REVEAL_DELAY_MS), затем снимок окна (PixelCopy, Android 8+) кладётся
//  поверх интерфейса, тема переключается под ним, и в снимке за 1,6 с
//  «прорезается» растущий круг с центром в нажатом варианте — новая тема
//  проступает изнутри круга. Снимок — обычная картинка, поэтому рисовать его
//  дёшево, а сам интерфейс под ним живёт как обычно.
//
//  Без анимации (сразу) — на Android 7, при «Отключить анимации», если снимок
//  не получился или тёмность темы не меняется (например, «Системная» при
//  тёмной системе после «Тёмной»).
// ─────────────────────────────────────────────────────────────────────────────

/** Запрос смены темы: новая тема и центр нажатого варианта в координатах корня Compose. */
fun interface ThemeChangeRequest {
    operator fun invoke(theme: AppTheme, centerInRoot: Offset)
}

/** Смена темы с раскрытием кругом. Вне [ThemeRevealHost] — пустышка. */
val LocalThemeChange = staticCompositionLocalOf { ThemeChangeRequest { _, _ -> } }

/**
 * Обёртка над всем содержимым окна. [currentTheme] — действующая тема,
 * [onApply] — сохранить новую (например, `viewModel.setThemeMode`).
 * Должна лежать внутри [LiptonTheme], чтобы видеть системное «Отключить анимации».
 */
@Composable
fun ThemeRevealHost(
    currentTheme: AppTheme,
    onApply: (AppTheme) -> Unit,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val reduceMotion by rememberUpdatedState(LiptonTheme.reduceMotion)
    val systemDark by rememberUpdatedState(isSystemInDarkTheme())
    val current by rememberUpdatedState(currentTheme)
    val apply by rememberUpdatedState(onApply)

    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var viewOffset by remember { mutableStateOf(IntOffset.Zero) }
    val radius = remember { Animatable(0f) }
    var job by remember { mutableStateOf<Job?>(null) }

    val request = remember(view) {
        ThemeChangeRequest { theme, centerInRoot ->
            job?.cancel()
            job = scope.launch {
                val sameLook = theme.isDark(systemDark) == current.isDark(systemDark)
                val window = view.context.findActivity()?.window
                if (reduceMotion || sameLook || window == null || !centerInRoot.isSpecified() ||
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                ) {
                    apply(theme)
                    return@launch
                }
                try {
                    delay(Tokens.Motion.THEME_REVEAL_DELAY_MS)
                    val bmp = captureWindow(window, view)
                    if (bmp == null) {
                        apply(theme)
                        return@launch
                    }
                    val loc = IntArray(2).also { view.getLocationInWindow(it) }
                    viewOffset = IntOffset(loc[0], loc[1])
                    center = centerInRoot
                    radius.snapTo(0f)
                    snapshot = bmp.asImageBitmap()
                    withFrameNanos { }          // снимок уже на экране — можно менять тему под ним
                    apply(theme)
                    val w = view.width.toFloat()
                    val h = view.height.toFloat()
                    val maxR = max(
                        max(hypot(center.x, center.y), hypot(w - center.x, center.y)),
                        max(hypot(center.x, h - center.y), hypot(w - center.x, h - center.y)),
                    )
                    radius.animateTo(maxR, tween(Tokens.Motion.THEME_REVEAL_MS, easing = Tokens.Motion.standard))
                } finally {
                    // Bitmap не recycle(): его ещё может держать список отрисовки — отдаём GC.
                    snapshot = null
                }
            }
        }
    }

    CompositionLocalProvider(LocalThemeChange provides request) {
        Box(Modifier.fillMaxSize()) {
            content()
            val img = snapshot
            if (img != null) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                ) {
                    drawImage(img, topLeft = Offset(-viewOffset.x.toFloat(), -viewOffset.y.toFloat()))
                    drawCircle(Color.Black, radius = radius.value, center = center, blendMode = BlendMode.Clear)
                }
            }
        }
    }
}

private fun Offset.isSpecified(): Boolean = this != Offset.Unspecified && x.isFinite() && y.isFinite()

internal fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Снимок окна целиком (вместе с размытием RenderEffect, которого нет в software-отрисовке). */
@RequiresApi(Build.VERSION_CODES.O)
private suspend fun captureWindow(window: Window, view: View): Bitmap? {
    val root = window.decorView
    val w = root.width.takeIf { it > 0 } ?: view.width
    val h = root.height.takeIf { it > 0 } ?: view.height
    if (w <= 0 || h <= 0) return null
    val bmp = runCatching { Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) }.getOrNull() ?: return null
    return suspendCancellableCoroutine { cont ->
        try {
            PixelCopy.request(window, bmp, { result ->
                if (!cont.isActive) { bmp.recycle(); return@request }
                if (result == PixelCopy.SUCCESS) cont.resume(bmp) else { bmp.recycle(); cont.resume(null) }
            }, Handler(Looper.getMainLooper()))
        } catch (_: IllegalArgumentException) {
            bmp.recycle()
            cont.resume(null)
        }
    }
}
