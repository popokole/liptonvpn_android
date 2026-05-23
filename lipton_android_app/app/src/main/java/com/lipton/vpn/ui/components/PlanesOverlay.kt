package com.lipton.vpn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PlaneMode { CONNECT, DISCONNECT }

private val EASE = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

private data class PlaneConfig(
    val leftFrac: Float, val topFrac: Float,
    val delayMs: Long,   val sizeDp: Float,
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

// 8 blast directions in dp, matching the HTML exactly
private val BLAST_DP = listOf(
    Offset(  0f, -55f), Offset( 39f, -39f),
    Offset( 55f,   0f), Offset( 39f,  39f),
    Offset(  0f,  55f), Offset(-39f,  39f),
    Offset(-55f,   0f), Offset(-39f, -39f),
)

private data class SparkDef(
    val cx: Float, val cy: Float,   // absolute px in overlay
    val dx: Float, val dy: Float,   // destination offset px
    val sizePx: Float,
    val uid: Long,
)

@Composable
fun PlanesOverlay(mode: PlaneMode?, modifier: Modifier = Modifier) {
    key(mode) {
        if (mode == null) return@key
        val sparks  = remember { mutableStateListOf<SparkDef>() }
        val density = LocalDensity.current

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }

            PLANES.forEachIndexed { idx, cfg ->
                PlaneParticle(
                    cfg = cfg, mode = mode,
                    wPx = wPx, hPx = hPx,
                    onSpark = if (mode == PlaneMode.DISCONNECT) { cx, cy ->
                        val szPx = with(density) { (if (idx % 2 == 0) 5f else 4f).dp.toPx() }
                        val uid0 = System.nanoTime() + idx.toLong() * 100L
                        BLAST_DP.forEachIndexed { j, dir ->
                            sparks += SparkDef(
                                cx = cx, cy = cy,
                                dx = with(density) { dir.x.dp.toPx() },
                                dy = with(density) { dir.y.dp.toPx() },
                                sizePx = szPx,
                                uid = uid0 + j,
                            )
                        }
                    } else null,
                )
            }

            sparks.forEach { spark ->
                key(spark.uid) { SparkParticle(spark) }
            }
        }
    }
}

// ─── Plane particle ───────────────────────────────────────────────────────────

@Composable
private fun PlaneParticle(
    cfg: PlaneConfig,
    mode: PlaneMode,
    wPx: Float,
    hPx: Float,
    onSpark: ((cx: Float, cy: Float) -> Unit)?,
) {
    val alpha   = remember { Animatable(0f) }
    val txDp    = remember { Animatable(0f) }
    val tyDp    = remember { Animatable(0f) }
    val rotDeg  = remember { Animatable(-40f) }
    val scl     = remember { Animatable(if (mode == PlaneMode.CONNECT) 0.1f else 0.2f) }
    val glowDp  = remember { Animatable(0f) }
    val colorF  = remember { Animatable(0f) }   // 0=green 1=orange 2=red 3=darkRed
    val density = LocalDensity.current

    LaunchedEffect(cfg, mode) {
        delay(cfg.delayMs)

        if (mode == PlaneMode.CONNECT) {
            // ── 0 % → 14 % — 364 ms ──────────────────────────────────────────
            coroutineScope {
                launch { alpha .animateTo(1f,             tween(364, easing = EASE)) }
                launch { txDp  .animateTo(cfg.txDp*.06f, tween(364, easing = EASE)) }
                launch { tyDp  .animateTo(-14f,           tween(364, easing = EASE)) }
                launch { scl   .animateTo(1.05f,          tween(364, easing = EASE)) }
                launch { glowDp.animateTo(8f,             tween(364, easing = EASE)) }
            }
            // ── 14 % → 70 % — 1456 ms ────────────────────────────────────────
            coroutineScope {
                launch { alpha .animateTo(0.9f,           tween(1456, easing = LinearEasing)) }
                launch { txDp  .animateTo(cfg.txDp*.6f,  tween(1456, easing = LinearEasing)) }
                launch { tyDp  .animateTo(-161f,          tween(1456, easing = LinearEasing)) }
                launch { rotDeg.animateTo(-52f,           tween(1456, easing = LinearEasing)) }
                launch { scl   .animateTo(0.7f,           tween(1456, easing = LinearEasing)) }
                launch { glowDp.animateTo(4f,             tween(1456, easing = LinearEasing)) }
            }
            // ── 70 % → 100 % — 780 ms ────────────────────────────────────────
            coroutineScope {
                launch { alpha .animateTo(0f,             tween(780, easing = LinearEasing)) }
                launch { txDp  .animateTo(cfg.txDp,      tween(780, easing = LinearEasing)) }
                launch { tyDp  .animateTo(-230f,          tween(780, easing = LinearEasing)) }
                launch { rotDeg.animateTo(-56f,           tween(780, easing = LinearEasing)) }
                launch { scl   .animateTo(0.5f,           tween(780, easing = LinearEasing)) }
                launch { glowDp.animateTo(2f,             tween(780, easing = LinearEasing)) }
            }

        } else {
            // DISCONNECT ──────────────────────────────────────────────────────

            // Spark fires at plane-local t = 1600 ms, launched concurrently
            // At that moment the plane is at ~ tx=txDp*0.59, ty=32.5 (64 % of 2500 ms)
            launch {
                delay(1600)
                val txPx = with(density) { (cfg.txDp * 0.59f).dp.toPx() }
                val tyPx = with(density) { 32.5f.dp.toPx() }
                onSpark?.invoke(wPx * cfg.leftFrac + txPx, hPx * cfg.topFrac + tyPx)
            }

            // ── 0 % → 12 % — 300 ms ──────────────────────────────────────────
            coroutineScope {
                launch { alpha .animateTo(1f,             tween(300, easing = EASE)) }
                launch { txDp  .animateTo(cfg.txDp*.05f, tween(300, easing = EASE)) }
                launch { tyDp  .animateTo(-16f,           tween(300, easing = EASE)) }
                launch { scl   .animateTo(1.1f,           tween(300, easing = EASE)) }
                launch { glowDp.animateTo(12f,            tween(300, easing = EASE)) }
            }
            // ── 12 % → 28 % — 400 ms — color → orange ────────────────────────
            coroutineScope {
                launch { txDp  .animateTo(cfg.txDp*.18f, tween(400, easing = EASE)) }
                launch { tyDp  .animateTo(-30f,           tween(400, easing = EASE)) }
                launch { rotDeg.animateTo(-12f,           tween(400, easing = EASE)) }
                launch { scl   .animateTo(1.25f,          tween(400, easing = EASE)) }
                launch { glowDp.animateTo(14f,            tween(400, easing = EASE)) }
                launch { colorF.animateTo(1f,             tween(400, easing = LinearEasing)) }
            }
            // ── 28 % → 55 % — 675 ms — color → red ──────────────────────────
            coroutineScope {
                launch { alpha .animateTo(0.95f,          tween(675, easing = LinearEasing)) }
                launch { txDp  .animateTo(cfg.txDp*.59f, tween(675, easing = LinearEasing)) }
                launch { tyDp  .animateTo(32f,            tween(675, easing = LinearEasing)) }
                launch { rotDeg.animateTo(40f,            tween(675, easing = LinearEasing)) }
                launch { scl   .animateTo(0.8f,           tween(675, easing = LinearEasing)) }
                launch { glowDp.animateTo(10f,            tween(675, easing = LinearEasing)) }
                launch { colorF.animateTo(2f,             tween(675, easing = LinearEasing)) }
            }
            // ── 55 % → 100 % — 1125 ms — fall and fade ───────────────────────
            coroutineScope {
                launch { alpha .animateTo(0f,             tween(1125, easing = LinearEasing)) }
                launch { txDp  .animateTo(cfg.txDp,      tween(1125, easing = LinearEasing)) }
                launch { tyDp  .animateTo(95f,            tween(1125, easing = LinearEasing)) }
                launch { rotDeg.animateTo(155f,           tween(1125, easing = LinearEasing)) }
                launch { scl   .animateTo(0.08f,          tween(1125, easing = LinearEasing)) }
                launch { glowDp.animateTo(4f,             tween(1125, easing = LinearEasing)) }
                launch { colorF.animateTo(3f,             tween(1125, easing = LinearEasing)) }
            }
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val txPx   = txDp.value.dp.toPx()
        val tyPx   = tyDp.value.dp.toPx()
        val sizePx = cfg.sizeDp.dp.toPx()
        val glowPx = glowDp.value.dp.toPx()
        val cx     = wPx * cfg.leftFrac + txPx
        val cy     = hPx * cfg.topFrac  + tyPx
        val color  = (if (mode == PlaneMode.CONNECT) Color(0xFF34D058)
                      else lerpPlaneColor(colorF.value))
                     .copy(alpha = alpha.value)

        drawPlane(cx, cy, sizePx, rotDeg.value, scl.value, color, glowPx)
    }
}

// ─── Spark particle ───────────────────────────────────────────────────────────

@Composable
private fun SparkParticle(spark: SparkDef) {
    val alpha = remember { Animatable(0f) }
    val scl   = remember { Animatable(0f) }
    val frac  = remember { Animatable(0f) }   // controls both position and color

    LaunchedEffect(Unit) {
        // ── 0 % → 18 % — 162 ms ──────────────────────────────────────────────
        coroutineScope {
            launch { alpha.animateTo(1f,   tween(162, easing = EASE)) }
            launch { scl  .animateTo(2.2f, tween(162, easing = EASE)) }
            launch { frac .animateTo(0.3f, tween(162, easing = EASE)) }
        }
        // ── 18 % → 60 % — 378 ms ─────────────────────────────────────────────
        coroutineScope {
            launch { scl .animateTo(1.0f,  tween(378, easing = LinearEasing)) }
            launch { frac.animateTo(0.72f, tween(378, easing = LinearEasing)) }
        }
        // ── 60 % → 100 % — 360 ms ────────────────────────────────────────────
        coroutineScope {
            launch { alpha.animateTo(0f,   tween(360, easing = LinearEasing)) }
            launch { scl  .animateTo(0.1f, tween(360, easing = LinearEasing)) }
            launch { frac .animateTo(1f,   tween(360, easing = LinearEasing)) }
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val f  = frac.value
        val x  = spark.cx + spark.dx * f
        val y  = spark.cy + spark.dy * f
        val r  = (spark.sizePx * scl.value / 2f).coerceAtLeast(0.1f)
        val col = lerpSparkColor(f).copy(alpha = alpha.value)

        // Glow
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = col.copy(alpha = col.alpha * 0.7f).toArgb()
                maskFilter = android.graphics.BlurMaskFilter(r * 2.5f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawCircle(x, y, r, paint)
        }
        drawCircle(col, r, Offset(x, y))
    }
}

// ─── Drawing helpers ──────────────────────────────────────────────────────────

private fun DrawScope.drawPlane(
    cx: Float, cy: Float, sizePx: Float,
    rotDeg: Float, scale: Float,
    color: Color, glowPx: Float,
) {
    // Glow pass — blurred circle at plane center
    if (glowPx > 0.5f) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                this.color = color.copy(alpha = color.alpha * 0.55f).toArgb()
                maskFilter = android.graphics.BlurMaskFilter(glowPx * 1.8f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawCircle(cx, cy, sizePx * 0.55f, paint)
        }
    }
    // Main shape
    withTransform({
        translate(cx - sizePx / 2f, cy - sizePx / 2f)
        rotate(rotDeg, Offset(sizePx / 2f, sizePx / 2f))
        scale(scale,   Offset(sizePx / 2f, sizePx / 2f))
    }) {
        val s = sizePx / 24f
        val body = Path().apply {
            moveTo( 2f * s, 12f * s)
            lineTo(22f * s,  2f * s)
            lineTo(14f * s, 22f * s)
            lineTo(11f * s, 13f * s)
            close()
        }
        drawPath(body, color)
        drawLine(
            color = color,
            start = Offset(11f * s, 13f * s),
            end   = Offset(14f * s, 10f * s),
            strokeWidth = s,
            cap = StrokeCap.Round,
        )
    }
}

// ─── Color helpers ────────────────────────────────────────────────────────────

private fun lerpPlaneColor(f: Float): Color {
    val green   = Color(0xFF34D058)
    val orange  = Color(0xFFFF9500)
    val red     = Color(0xFFFF3B30)
    val darkRed = Color(0xFFFF1500)
    return when {
        f <= 1f -> lerp(green,  orange,  f.coerceIn(0f, 1f))
        f <= 2f -> lerp(orange, red,     (f - 1f).coerceIn(0f, 1f))
        else    -> lerp(red,    darkRed, (f - 2f).coerceIn(0f, 1f))
    }
}

// frac is the position fraction (0→1), colour keyframes match time keyframes:
//   frac=0.0 → #ffcc00   frac=0.30 → #ffaa00
//   frac=0.72 → #ff4500  frac=1.0  → #ff1500
private fun lerpSparkColor(frac: Float): Color {
    val gold   = Color(0xFFFFCC00)
    val amber  = Color(0xFFFFAA00)
    val orange = Color(0xFFFF4500)
    val red    = Color(0xFFFF1500)
    return when {
        frac < 0.30f  -> lerp(gold,   amber,  (frac / 0.30f).coerceIn(0f, 1f))
        frac < 0.72f  -> lerp(amber,  orange, ((frac - 0.30f) / 0.42f).coerceIn(0f, 1f))
        else          -> lerp(orange, red,    ((frac - 0.72f) / 0.28f).coerceIn(0f, 1f))
    }
}
