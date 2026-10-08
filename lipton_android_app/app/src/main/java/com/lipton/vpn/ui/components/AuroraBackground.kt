package com.lipton.vpn.ui.components

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lipton.vpn.ui.theme.LiptonColors
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.Tokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
//  Живое свечение «Аврора»: 4–6 размытых пятен, медленный дрейф 17–31 с,
//  «дыхание» ядра, зерно. Палитра зависит от состояния (вкл / выкл / нет
//  подписки / «Обход») и плавно перетекает за 1,6 с.
//
//  Как сделано размытие. В макетах пятно — radial-gradient(цвет → прозрачный
//  на ~W/2) с filter: blur(σ). Здесь профиль такого пятна (конус, свёрнутый
//  с гауссианой σ) считается численно один раз и рисуется радиальным
//  градиентом из 16 точек — результат совпадает с CSS blur на любом API.
//  На Android 12+ дополнительно накладывается небольшой RenderEffect-blur
//  (σ профиля уменьшается на ту же величину), он сглаживает полосы градиента.
//
//  Экономия: кадры свечения обновляются ~30 раз в секунду, анимация стоит на
//  паузе, когда экран скрыт (Lifecycle ON_STOP), и полностью статична при
//  системном «Отключить анимации».
// ─────────────────────────────────────────────────────────────────────────────

/** Состояние, от которого зависит палитра свечения. */
enum class AuroraTone { ON, OFF, NO_SUB, BYPASS }

/** Раскладка пятен — по макетам разных экранов. */
enum class AuroraLayout { HERO, HEADER, NEWS, PROFILE, ONBOARDING }

fun auroraPalette(tone: AuroraTone, dark: Boolean): List<Color> = with(Tokens.Aurora) {
    when (tone) {
        AuroraTone.ON     -> if (dark) onDark else onLight
        AuroraTone.OFF    -> if (dark) offDark else offLight
        AuroraTone.NO_SUB -> if (dark) noSubDark else noSubLight
        AuroraTone.BYPASS -> if (dark) bypassDark else bypassLight
    }
}

/**
 * Фон со свечением. Рисует фон темы, пятна, (для HERO) затемнения сверху и снизу
 * и зерно; [content] кладётся поверх.
 */
@Composable
fun AuroraBackground(
    tone: AuroraTone,
    modifier: Modifier = Modifier,
    layout: AuroraLayout = AuroraLayout.HERO,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val c = LiptonTheme.colors
    val reduceMotion = LiptonTheme.reduceMotion
    val palette = auroraPalette(tone, c.isDark)
    val spec = if (reduceMotion) snap<Color>() else
        tween(Tokens.Motion.PALETTE_CROSSFADE_MS, easing = Tokens.Motion.standard)
    val roleColors: List<State<Color>> = List(4) { i ->
        animateColorAsState(palette[i], spec, label = "aurora_role_$i")
    }

    val time = remember { mutableFloatStateOf(0f) }
    val visible = rememberLifecycleStarted()
    val animate = visible && !reduceMotion
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val offsetNs = (time.floatValue * 1e9f).toLong()
        val start = withFrameNanos { it } - offsetNs
        while (isActive) {
            withFrameNanos { now -> time.floatValue = (now - start) / 1e9f }
            delay(FRAME_PAUSE_MS)
        }
    }

    val blobs = remember(layout) { blobsFor(layout) }
    val caches = remember(layout) { Array(blobs.size) { BlobBrushCache() } }
    val density = LocalDensity.current.density
    // На Android 12+ — небольшой общий blur слоя; σ профиля уменьшаем на столько же.
    val fxSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier.fillMaxSize()) {
        // Слой 1: фон + пятна (offscreen, чтобы Screen/Multiply смешивались с фоном)
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    if (fxSupported) {
                        val k = size.width / DESIGN_WIDTH
                        val sigma = FX_SIGMA_CSS * k
                        val radius = ((sigma - 0.5f) / 0.57735f).coerceAtLeast(0f)
                        renderEffect = if (radius > 0.5f) BlurEffect(radius, radius, TileMode.Clamp) else null
                    }
                }
                .drawBehind {
                    drawRect(c.bg)
                    drawBlobs(blobs, caches, roleColors, time.floatValue, c, fxSupported)
                },
        )
        // Слой 2: затемнения у краёв и зерно (без blur)
        Spacer(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    if (layout == AuroraLayout.HERO) drawHeroFades(c.bg)
                    drawGrain(c, density)
                },
        )
        content()
    }
}

/** true, пока экран виден (между ON_START и ON_STOP). */
@Composable
private fun rememberLifecycleStarted(): Boolean {
    val owner = LocalLifecycleOwner.current
    var started by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> started = true
                Lifecycle.Event.ON_STOP  -> started = false
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return started
}

// ─── Данные пятен ────────────────────────────────────────────────────────────

/** Ширина макета в CSS px: позиции и размеры масштабируются к ширине экрана. */
private const val DESIGN_WIDTH = 390f
/** σ общего RenderEffect-blur на Android 12+ (в CSS px макета). */
private const val FX_SIGMA_CSS = 14f
/** Пауза между кадрами свечения (~30 к/с). */
private const val FRAME_PAUSE_MS = 16L

/**
 * Пятно. [x], [y] — левый верхний угол квадрата [size] в CSS px макета,
 * [blur] — σ из filter: blur(), [opDark]/[opLight] — opacity, [role] — индекс
 * цвета палитры, [motion] — индекс траектории дрейфа.
 */
private class BlobSpec(
    val role: Int, val x: Float, val y: Float, val size: Float, val blur: Float,
    val opDark: Float, val opLight: Float, val motion: Int,
)

/**
 * Дрейф из @keyframes макетов: анимация «a» — сдвиг (ax, ay) за aDur;
 * анимация «b» — сдвиг (bx, by), масштаб bScale и множитель прозрачности
 * за bDur. Обе — ease-in-out, infinite alternate. [breath] — «дыхание» ядра.
 */
private class DriftSpec(
    val ax: Float, val ay: Float, val aDur: Float,
    val bx: Float, val by: Float, val bScale: Float, val bOpacity: Float, val bDur: Float,
    val breath: Boolean = false,
)

private val DRIFTS = arrayOf(
    DriftSpec(46f, -12f, 19f, -20f, 18f, 1.06f, 0.906f, 23f),
    DriftSpec(58f, -24f, 21f, 16f, 30f, 1.12f, 1.138f, 27f),
    DriftSpec(-54f, 22f, 17f, -12f, -28f, 1.10f, 1.16f, 25f),
    DriftSpec(30f, -10f, 18f, -22f, 12f, 1.04f, 1.23f, 22f, breath = true),
    DriftSpec(-50f, 34f, 24f, 22f, -28f, 1.12f, 1.43f, 29f),
    DriftSpec(56f, -30f, 26f, -18f, 36f, 0.92f, 1.375f, 20f),
    DriftSpec(-42f, -40f, 28f, 28f, 18f, 1.14f, 1.5f, 31f),
)

/** В светлой теме пятна при дрейфе чуть бледнеют (макеты: .95 → .87). */
private const val LIGHT_OPACITY_DRIFT = 0.92f

private fun blobsFor(layout: AuroraLayout): List<BlobSpec> = when (layout) {
    // Главная (new-combo-on / off / bypass)
    AuroraLayout.HERO -> listOf(
        BlobSpec(0, -40f, 170f, 470f, 60f, 0.85f, 0.95f, 0),
        BlobSpec(1, -210f, 330f, 440f, 70f, 0.62f, 0.85f, 1),
        BlobSpec(2, 200f, 420f, 380f, 70f, 0.50f, 0.80f, 2),
        BlobSpec(3, 95f, 300f, 200f, 40f, 0.35f, 0.75f, 3),
    )
    // Серверы (new-combo-servers-*)
    AuroraLayout.HEADER -> listOf(
        BlobSpec(0, -130f, -230f, 440f, 70f, 0.50f, 0.85f, 0),
        BlobSpec(1, 140f, -250f, 400f, 70f, 0.36f, 0.75f, 1),
        BlobSpec(2, 220f, -40f, 260f, 70f, 0.24f, 0.55f, 2),
        BlobSpec(0, -170f, 40f, 360f, 80f, 0.10f, 0.42f, 4),
        BlobSpec(2, 190f, 300f, 340f, 80f, 0.18f, 0.45f, 5),
    )
    // Новости (new-combo-news-*)
    AuroraLayout.NEWS -> listOf(
        BlobSpec(0, 120f, -240f, 440f, 70f, 0.46f, 0.82f, 0),
        BlobSpec(1, -170f, -220f, 400f, 70f, 0.34f, 0.72f, 1),
        BlobSpec(2, 60f, -70f, 260f, 70f, 0.20f, 0.50f, 2),
        BlobSpec(1, 210f, 80f, 320f, 80f, 0.12f, 0.45f, 4),
        BlobSpec(0, -170f, 380f, 380f, 80f, 0.12f, 0.45f, 5),
        BlobSpec(2, 220f, 620f, 300f, 80f, 0.12f, 0.42f, 6),
    )
    // Профиль и подэкраны (new-combo-profile-*, new-scr-*)
    AuroraLayout.PROFILE -> listOf(
        BlobSpec(0, -110f, -190f, 420f, 70f, 0.48f, 0.85f, 0),
        BlobSpec(1, 160f, -210f, 400f, 80f, 0.32f, 0.75f, 1),
        BlobSpec(3, 0f, 40f, 200f, 50f, 0.16f, 0.60f, 3),
        BlobSpec(1, 230f, 40f, 300f, 80f, 0.14f, 0.50f, 4),
        BlobSpec(0, -150f, 520f, 380f, 80f, 0.14f, 0.50f, 5),
    )
    // Онбординг и вход (new-onb-*)
    AuroraLayout.ONBOARDING -> listOf(
        BlobSpec(0, -35f, -50f, 460f, 60f, 0.85f, 0.95f, 0),
        BlobSpec(1, -210f, 24f, 420f, 70f, 0.60f, 0.85f, 1),
        BlobSpec(2, 200f, 44f, 380f, 70f, 0.50f, 0.80f, 2),
        BlobSpec(3, 95f, 80f, 200f, 40f, 0.38f, 0.75f, 3),
    )
}

// ─── Рисование ───────────────────────────────────────────────────────────────

/** Кэш кисти пятна: пересоздаётся только при смене цвета (во время кросс-фейда). */
private class BlobBrushCache {
    var color: Color = Color.Unspecified
    var sigmaKey: Int = -1
    var brush: Brush? = null
    var radius: Float = 0f
}

/** Радиус конуса в CSS: 70% от farthest-corner (W/√2) ≈ 0.495·W. */
private const val CONE_RATIO = 0.495f

private fun DrawScope.drawBlobs(
    blobs: List<BlobSpec>,
    caches: Array<BlobBrushCache>,
    roleColors: List<State<Color>>,
    t: Float,
    c: LiptonColors,
    fx: Boolean,
) {
    val k = size.width / DESIGN_WIDTH
    val blend = if (c.isDark) BlendMode.Screen else BlendMode.Multiply
    val fxSigma = if (fx) FX_SIGMA_CSS else 0f
    blobs.forEachIndexed { i, b ->
        val d = DRIFTS[b.motion]
        val ea = wave(t, d.aDur)
        val eb = wave(t, d.bDur)
        val dx = d.ax * ea + d.bx * eb
        val dy = d.ay * ea + d.by * eb
        var scale = 1f + (d.bScale - 1f) * eb
        if (d.breath) scale *= 1f + 0.08f * wave(t, Tokens.Motion.BREATH_MS / 2000f)
        val opBase = if (c.isDark) b.opDark else b.opLight
        val opMul = 1f + ((if (c.isDark) d.bOpacity else LIGHT_OPACITY_DRIFT) - 1f) * eb
        val opacity = (opBase * opMul).coerceIn(0f, 1f)

        val coneR = b.size * CONE_RATIO
        val sigma = sqrt(max(b.blur * b.blur - fxSigma * fxSigma, 1f))
        val s = sigma / coneR
        val color = roleColors[b.role].value
        val cache = caches[i]
        val key = (s * 1000).roundToInt()
        if (cache.brush == null || cache.color != color || cache.sigmaKey != key) {
            val profile = BlurProfiles.get(s)
            val n = profile.size
            val stops = Array(n) { j -> (j / (n - 1f)) to color.copy(alpha = profile[j]) }
            cache.radius = coneR * (1f + 3f * s)
            cache.brush = Brush.radialGradient(colorStops = stops, center = Offset.Zero, radius = cache.radius)
            cache.color = color
            cache.sigmaKey = key
        }
        val cx = (b.x + b.size / 2f + dx) * k
        val cy = (b.y + b.size / 2f + dy) * k
        val brush = cache.brush ?: return@forEachIndexed
        translate(cx, cy) {
            scale(scale * k, scale * k, pivot = Offset.Zero) {
                drawCircle(brush = brush, radius = cache.radius, center = Offset.Zero, alpha = opacity, blendMode = blend)
            }
        }
    }
}

/** Треугольная волна 0 → 1 → 0 с периодом 2·dur и ease-in-out (CSS alternate). */
private fun wave(t: Float, dur: Float): Float {
    if (dur <= 0f) return 0f
    val p = t / dur
    val cycle = floor(p)
    var f = p - cycle
    if ((cycle.toLong() and 1L) == 1L) f = 1f - f
    return Tokens.Motion.easeInOut.transform(f.coerceIn(0f, 1f))
}

/** Затемнения у верхнего и нижнего края на главной (из new-combo-on). */
private fun DrawScope.drawHeroFades(bg: Color) {
    val k = size.width / DESIGN_WIDTH
    val topEnd = 340f * k
    drawRect(
        brush = Brush.verticalGradient(
            0f to bg,
            (120f / 340f) to bg,
            (200f / 340f) to bg.copy(alpha = 0.82f),
            (276f / 340f) to bg.copy(alpha = 0.40f),
            1f to bg.copy(alpha = 0f),
            startY = 0f, endY = topEnd,
        ),
        size = size.copy(height = min(topEnd, size.height)),
    )
    val bottomH = 230f * k
    val top = size.height - bottomH
    drawRect(
        brush = Brush.verticalGradient(
            0f to bg.copy(alpha = 0f),
            (120f / 230f) to bg.copy(alpha = 0.45f),
            1f to bg.copy(alpha = 0.82f),
            startY = top, endY = size.height,
        ),
        topLeft = Offset(0f, top),
        size = size.copy(height = bottomH),
    )
}

private fun DrawScope.drawGrain(c: LiptonColors, density: Float) {
    val brush = Grain.brush(density)
    drawRect(
        brush = brush,
        alpha = if (c.isDark) Tokens.Aurora.GRAIN_ALPHA_DARK else Tokens.Aurora.GRAIN_ALPHA_LIGHT,
        blendMode = if (c.isDark) BlendMode.Overlay else BlendMode.Multiply,
    )
}

/** Тайловый шум (аналог feTurbulence из макетов): серые пиксели ~1 CSS px. */
private object Grain {
    private var cached: ShaderBrush? = null
    private var cachedCell = 0

    fun brush(density: Float): ShaderBrush {
        val cell = density.roundToInt().coerceIn(1, 4)
        cached?.let { if (cachedCell == cell) return it }
        val n = 96
        val rnd = Random(20261008)
        val pixels = IntArray(n * n) {
            val v = rnd.nextInt(256)
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        val small = Bitmap.createBitmap(pixels, n, n, Bitmap.Config.ARGB_8888)
        val big = if (cell > 1) Bitmap.createScaledBitmap(small, n * cell, n * cell, false) else small
        val brush = ShaderBrush(ImageShader(big.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
        cached = brush
        cachedCell = cell
        return brush
    }
}

/**
 * Радиальный профиль пятна: конус alpha = 1 − r/R (R = 1), свёрнутый
 * с двумерной гауссианой σ = s (в долях R). Значения в точках
 * r = i/(N−1)·(1 + 3s). Считается один раз на каждое s.
 */
internal object BlurProfiles {
    private const val N = 16
    private const val NR = 32
    private const val NT = 48
    private val cache = HashMap<Int, FloatArray>()
    private val cosT = FloatArray(NT) { cos((it + 0.5f) * PI.toFloat() / NT) }
    private val sinT = FloatArray(NT) { sin((it + 0.5f) * PI.toFloat() / NT) }

    @Synchronized
    fun get(s: Float): FloatArray {
        val key = (s * 1000).roundToInt()
        return cache.getOrPut(key) { compute(key / 1000f) }
    }

    fun compute(s: Float): FloatArray {
        val out = FloatArray(N)
        val extent = 1f + 3f * s
        if (s < 0.02f) {
            for (i in 0 until N) out[i] = max(0f, 1f - extent * i / (N - 1))
            return out
        }
        val inv2s2 = 1f / (2f * s * s)
        val norm = 1f / (2f * PI.toFloat() * s * s)
        val dr = 1f / NR
        val dt = PI.toFloat() / NT
        for (i in 0 until N - 1) {
            val r = extent * i / (N - 1)
            var acc = 0f
            for (a in 0 until NR) {
                val rho = (a + 0.5f) * dr
                val w = (1f - rho) * rho * dr * dt * 2f   // полуокружность × 2 (симметрия)
                for (b in 0 until NT) {
                    val px = rho * cosT[b] - r
                    val py = rho * sinT[b]
                    acc += w * exp(-(px * px + py * py) * inv2s2)
                }
            }
            out[i] = (acc * norm).coerceIn(0f, 1f)
        }
        out[N - 1] = 0f
        return out
    }
}
