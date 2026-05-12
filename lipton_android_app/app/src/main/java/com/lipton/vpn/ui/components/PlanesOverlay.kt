package com.lipton.vpn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.lipton.vpn.ui.theme.Green
import com.lipton.vpn.ui.theme.Red
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PlaneMode { CONNECT, DISCONNECT }

private data class PlaneConfig(
    val leftFrac: Float,
    val topFrac: Float,
    val delayMs: Long,
    val sizeDp: Float,
    val txDp: Float,
)

private val PLANES = listOf(
    PlaneConfig(0.28f, 0.50f,   0L, 28f, -55f),
    PlaneConfig(0.50f, 0.56f, 140L, 22f,   5f),
    PlaneConfig(0.16f, 0.47f, 240L, 18f, -75f),
    PlaneConfig(0.65f, 0.53f,  90L, 24f,  60f),
    PlaneConfig(0.40f, 0.62f, 300L, 16f, -25f),
    PlaneConfig(0.73f, 0.44f, 180L, 20f,  42f),
)

@Composable
fun PlanesOverlay(mode: PlaneMode?, modifier: Modifier = Modifier) {
    if (mode != null) {
        Box(modifier = modifier.fillMaxSize()) {
            PLANES.forEachIndexed { i, cfg ->
                key(mode, i) {
                    PlaneParticle(cfg = cfg, mode = mode)
                }
            }
        }
    }
}

@Composable
private fun PlaneParticle(cfg: PlaneConfig, mode: PlaneMode) {
    val alpha = remember { Animatable(0f) }
    val tx    = remember { Animatable(0f) }
    val ty    = remember { Animatable(0f) }
    val rot   = remember { Animatable(if (mode == PlaneMode.CONNECT) -40f else -20f) }
    val scl   = remember { Animatable(0.1f) }

    LaunchedEffect(cfg, mode) {
        delay(cfg.delayMs)
        if (mode == PlaneMode.CONNECT) {
            // Phase 1: appear and lift off
            coroutineScope {
                launch { alpha.animateTo(1f,              tween(380, easing = FastOutSlowInEasing)) }
                launch { tx.animateTo(cfg.txDp * 0.06f,  tween(380, easing = FastOutSlowInEasing)) }
                launch { ty.animateTo(-14f,               tween(380, easing = FastOutSlowInEasing)) }
                launch { scl.animateTo(1.0f,              tween(380, easing = FastOutSlowInEasing)) }
            }
            // Phase 2: climb
            coroutineScope {
                launch { alpha.animateTo(0.88f,           tween(1500, easing = LinearEasing)) }
                launch { tx.animateTo(cfg.txDp * 0.6f,   tween(1500, easing = LinearEasing)) }
                launch { ty.animateTo(-148f,              tween(1500, easing = LinearEasing)) }
                launch { rot.animateTo(-50f,              tween(1500, easing = LinearEasing)) }
                launch { scl.animateTo(0.72f,             tween(1500, easing = LinearEasing)) }
            }
            // Phase 3: fade into clouds
            coroutineScope {
                launch { alpha.animateTo(0f,              tween(820, easing = FastOutSlowInEasing)) }
                launch { tx.animateTo(cfg.txDp,           tween(820, easing = LinearEasing)) }
                launch { ty.animateTo(-235f,              tween(820, easing = LinearEasing)) }
                launch { rot.animateTo(-56f,              tween(820, easing = LinearEasing)) }
                launch { scl.animateTo(0.44f,             tween(820, easing = LinearEasing)) }
            }
        } else {
            // DISCONNECT: appear, wobble, then fall and fade (red planes)
            // Phase 1: appear
            coroutineScope {
                launch { alpha.animateTo(1f,              tween(220, easing = FastOutSlowInEasing)) }
                launch { ty.animateTo(-10f,               tween(220, easing = FastOutSlowInEasing)) }
                launch { scl.animateTo(1.05f,             tween(220, easing = FastOutSlowInEasing)) }
            }
            // Phase 2: wobble and start falling
            coroutineScope {
                launch { alpha.animateTo(0.95f,           tween(550)) }
                launch { tx.animateTo(cfg.txDp * 0.22f,  tween(550, easing = FastOutSlowInEasing)) }
                launch { ty.animateTo(30f,                tween(550, easing = FastOutSlowInEasing)) }
                launch { rot.animateTo(52f,               tween(550, easing = FastOutSlowInEasing)) }
                launch { scl.animateTo(0.88f,             tween(550)) }
            }
            // Phase 3: EXPLODE — scatter outward, pop scale, fast fade
            coroutineScope {
                launch { alpha.animateTo(0f,                         tween(360, easing = FastOutSlowInEasing)) }
                launch { tx.animateTo(cfg.txDp * 2.6f,               tween(360, easing = FastOutSlowInEasing)) }
                launch { ty.animateTo(30f - cfg.txDp * 0.55f,        tween(360, easing = FastOutSlowInEasing)) }
                launch { rot.animateTo(300f,                         tween(360, easing = LinearEasing)) }
                launch { scl.animateTo(2.2f,                         tween(280, easing = FastOutSlowInEasing)) }
            }
        }
    }

    val planeColor = if (mode == PlaneMode.DISCONNECT) Red else Green

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx     = size.width  * cfg.leftFrac
        val cy     = size.height * cfg.topFrac
        val sizePx = cfg.sizeDp.dp.toPx()
        val txPx   = tx.value.dp.toPx()
        val tyPx   = ty.value.dp.toPx()

        translate(cx + txPx - sizePx / 2f, cy + tyPx - sizePx / 2f) {
            rotate(rot.value, pivot = Offset(sizePx / 2f, sizePx / 2f)) {
                scale(scl.value, pivot = Offset(sizePx / 2f, sizePx / 2f)) {
                    drawPlaneSvg(planeColor.copy(alpha = alpha.value), sizePx)
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlaneSvg(
    color: Color,
    sizePx: Float,
) {
    val s = sizePx / 24f
    val body = Path().apply {
        moveTo(2f * s, 12f * s)
        lineTo(22f * s,  2f * s)
        lineTo(14f * s, 22f * s)
        lineTo(11f * s, 13f * s)
        close()
    }
    drawPath(body, color = color)
    drawLine(
        color = color,
        start = Offset(11f * s, 13f * s),
        end   = Offset(14f * s, 10f * s),
        strokeWidth = s,
        cap = StrokeCap.Round,
    )
}
