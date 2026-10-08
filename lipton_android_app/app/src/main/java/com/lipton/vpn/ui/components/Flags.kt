package com.lipton.vpn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lipton.vpn.ui.theme.LiptonTheme
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  Круглые флаги как в макетах: полосатые флаги рисуются полосами (DE, NL, RU…),
//  скандинавские — крестом; остальные — эмодзи, обрезанное кругом.
// ─────────────────────────────────────────────────────────────────────────────

/** ISO-код страны из эмодзи-флага (пара Regional Indicator): «🇩🇪» → «DE». */
fun countryCodeFromFlag(flag: String): String? {
    val cps = flag.codePoints().toArray()
    if (cps.size < 2) return null
    val a = cps[0] - 0x1F1E6
    val b = cps[1] - 0x1F1E6
    if (a !in 0..25 || b !in 0..25) return null
    return "${'A' + a}${'A' + b}"
}

/** Эмодзи-флаг из ISO-кода: «NL» → «🇳🇱». */
fun flagFromCountryCode(code: String?): String {
    val c = code?.trim()?.uppercase(Locale.ROOT) ?: return ""
    if (c.length != 2 || !c.all { it in 'A'..'Z' }) return ""
    return String(Character.toChars(0x1F1E6 + (c[0] - 'A'))) + String(Character.toChars(0x1F1E6 + (c[1] - 'A')))
}

/** Название страны по-русски для подписи сервера, если в названии его нет. */
fun countryNameRu(code: String?): String? = when (code?.uppercase(Locale.ROOT)) {
    "DE" -> "Германия"; "NL" -> "Нидерланды"; "FI" -> "Финляндия"; "SE" -> "Швеция"
    "NO" -> "Норвегия"; "DK" -> "Дания"; "FR" -> "Франция"; "GB" -> "Великобритания"
    "US" -> "США"; "PL" -> "Польша"; "LV" -> "Латвия"; "LT" -> "Литва"; "EE" -> "Эстония"
    "AT" -> "Австрия"; "CH" -> "Швейцария"; "IT" -> "Италия"; "ES" -> "Испания"
    "TR" -> "Турция"; "KZ" -> "Казахстан"; "RU" -> "Россия"; "JP" -> "Япония"
    "SG" -> "Сингапур"; "HK" -> "Гонконг"; "AE" -> "ОАЭ"; "CA" -> "Канада"; "BG" -> "Болгария"
    "RO" -> "Румыния"; "HU" -> "Венгрия"; "CZ" -> "Чехия"; "MD" -> "Молдова"; "GE" -> "Грузия"
    "AM" -> "Армения"; "RS" -> "Сербия"; "BE" -> "Бельгия"; "IE" -> "Ирландия"; "PT" -> "Португалия"
    else -> null
}

private sealed interface FlagArt
private data class HStripes(val colors: List<Color>) : FlagArt
private data class VStripes(val colors: List<Color>) : FlagArt
private data class NordicCross(val field: Color, val cross: Color, val inner: Color? = null) : FlagArt

private fun artFor(code: String?): FlagArt? = when (code?.uppercase(Locale.ROOT)) {
    "DE" -> HStripes(listOf(Color(0xFF000000), Color(0xFFDD0000), Color(0xFFFFCE00)))
    "NL" -> HStripes(listOf(Color(0xFFAE1C28), Color(0xFFFFFFFF), Color(0xFF21468B)))
    "RU" -> HStripes(listOf(Color(0xFFFFFFFF), Color(0xFF0039A6), Color(0xFFD52B1E)))
    "AT" -> HStripes(listOf(Color(0xFFED2939), Color(0xFFFFFFFF), Color(0xFFED2939)))
    "EE" -> HStripes(listOf(Color(0xFF0072CE), Color(0xFF000000), Color(0xFFFFFFFF)))
    "LT" -> HStripes(listOf(Color(0xFFFDB913), Color(0xFF006A44), Color(0xFFC1272D)))
    "LV" -> HStripes(listOf(Color(0xFF9E3039), Color(0xFF9E3039), Color(0xFFFFFFFF), Color(0xFF9E3039), Color(0xFF9E3039)))
    "HU" -> HStripes(listOf(Color(0xFFCD2A3E), Color(0xFFFFFFFF), Color(0xFF436F4D)))
    "BG" -> HStripes(listOf(Color(0xFFFFFFFF), Color(0xFF00966E), Color(0xFFD62612)))
    "PL" -> HStripes(listOf(Color(0xFFFFFFFF), Color(0xFFDC143C)))
    "UA" -> HStripes(listOf(Color(0xFF0057B7), Color(0xFFFFD700)))
    "FR" -> VStripes(listOf(Color(0xFF0055A4), Color(0xFFFFFFFF), Color(0xFFEF4135)))
    "IT" -> VStripes(listOf(Color(0xFF009246), Color(0xFFFFFFFF), Color(0xFFCE2B37)))
    "IE" -> VStripes(listOf(Color(0xFF169B62), Color(0xFFFFFFFF), Color(0xFFFF883E)))
    "BE" -> VStripes(listOf(Color(0xFF000000), Color(0xFFFAE042), Color(0xFFED2939)))
    "RO" -> VStripes(listOf(Color(0xFF002B7F), Color(0xFFFCD116), Color(0xFFCE1126)))
    "MD" -> VStripes(listOf(Color(0xFF0046AE), Color(0xFFFFD200), Color(0xFFCC092F)))
    "FI" -> NordicCross(Color(0xFFFFFFFF), Color(0xFF002F6C))
    "SE" -> NordicCross(Color(0xFF006AA7), Color(0xFFFECC02))
    "DK" -> NordicCross(Color(0xFFC8102E), Color(0xFFFFFFFF))
    "NO" -> NordicCross(Color(0xFFBA0C2F), Color(0xFFFFFFFF), Color(0xFF00205B))
    else -> null
}

/**
 * Круглый флаг [size]. [code] — ISO-код страны, [emoji] — запасной вариант.
 * [glow] — цветное свечение-кольцо вокруг (как у активного сервера в макете).
 */
@Composable
fun FlagCircle(
    code: String?,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    emoji: String? = null,
    ring: Color? = null,
) {
    val c = LiptonTheme.colors
    val art = artFor(code)
    val borderColor = if (c.isDark) Color.White.copy(alpha = 0.16f) else Color(0x1F0E1412)
    val glowMod = if (ring != null) Modifier.border(3.dp, ring.copy(alpha = 0.16f), CircleShape) else Modifier
    Box(modifier.size(size + if (ring != null) 6.dp else 0.dp).then(glowMod), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (c.isDark) Color(0x1FFFFFFF) else Color(0x140E1412))
                .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (art) {
                is HStripes -> Canvas(Modifier.size(size)) {
                    val h = this.size.height / art.colors.size
                    art.colors.forEachIndexed { i, col -> drawRect(col, Offset(0f, i * h), Size(this.size.width, h + 0.5f)) }
                }
                is VStripes -> Canvas(Modifier.size(size)) {
                    val w = this.size.width / art.colors.size
                    art.colors.forEachIndexed { i, col -> drawRect(col, Offset(i * w, 0f), Size(w + 0.5f, this.size.height)) }
                }
                is NordicCross -> Canvas(Modifier.size(size)) {
                    val s = this.size
                    drawRect(art.field)
                    val t = s.height * 0.26f
                    val cx = s.width * 0.40f
                    drawRect(art.cross, Offset(cx - t / 2, 0f), Size(t, s.height))
                    drawRect(art.cross, Offset(0f, s.height / 2 - t / 2), Size(s.width, t))
                    art.inner?.let { inner ->
                        val ti = t * 0.5f
                        drawRect(inner, Offset(cx - ti / 2, 0f), Size(ti, s.height))
                        drawRect(inner, Offset(0f, s.height / 2 - ti / 2), Size(s.width, ti))
                    }
                }
                null -> {
                    val e = emoji?.takeIf { it.isNotBlank() } ?: flagFromCountryCode(code)
                    if (e.isNotEmpty()) {
                        val fs = with(LocalDensity.current) { (size * 1.25f).toSp() }
                        Text(e, fontSize = fs, maxLines = 1, softWrap = false)
                    } else {
                        Icon(LiptonIcons.Globe, null, tint = c.text2, modifier = Modifier.size(size * 0.55f))
                    }
                }
            }
        }
    }
}
