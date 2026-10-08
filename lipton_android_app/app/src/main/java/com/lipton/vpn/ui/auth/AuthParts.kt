package com.lipton.vpn.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.ui.components.AuroraBackground
import com.lipton.vpn.ui.components.AuroraLayout
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.FlagCircle
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.LogoTitle
import com.lipton.vpn.ui.components.glass
import com.lipton.vpn.ui.components.softGlow
import com.lipton.vpn.ui.components.softGlowCircle
import com.lipton.vpn.ui.components.stateText
import com.lipton.vpn.ui.components.stateTone
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.TABULAR_NUMS
import com.lipton.vpn.ui.theme.UnboundedFamily

// ─────────────────────────────────────────────────────────────────────────────
//  Общие части экранов онбординга и входа (макеты new-onb-*, 390×844):
//  свечение сверху, иллюстрация, логотип с точками шагов, двухцветный
//  заголовок, кнопки, поля кода и почты, вкладки способа входа.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Каркас экрана онбординга: свечение [tone] на весь экран, верхняя панель,
 * иллюстрация в свободной верхней части и содержимое у нижнего края.
 * Содержимое прокручивается, если не помещается (клавиатура, маленький экран).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OnbPage(
    tone: AuroraTone,
    topBar: @Composable RowScope.() -> Unit,
    art: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AuroraBackground(tone = tone, layout = AuroraLayout.ONBOARDING) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp).height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = topBar,
            )
            // С клавиатурой иллюстрация уходит, а форма прокручивается в оставшемся месте.
            val ime = WindowInsets.isImeVisible
            if (!ime) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    art?.invoke(this)
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .then(if (ime) Modifier.weight(1f) else Modifier)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                content = content,
            )
        }
    }
}

/** Круглая стеклянная кнопка 40dp («‹» или «×»). */
@Composable
internal fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val c = LiptonTheme.colors
    Box(
        Modifier
            .size(40.dp)
            .glass(CircleShape, highlightHeight = 20.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = c.text, modifier = Modifier.size(18.dp)) }
}

/** Подпись справа в верхней панели («Войти», «Код из письма»); кликабельная — если [onClick]. */
@Composable
internal fun RowScope.TopLabel(text: String, onClick: (() -> Unit)? = null) {
    val c = LiptonTheme.colors
    Spacer(Modifier.weight(1f))
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
        color = c.text.copy(alpha = if (onClick != null) 0.92f else 0.78f),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/** «Lipton VPN» и точки шагов (активная — пилюля в цвете свечения). */
@Composable
internal fun LogoRow(tone: AuroraTone, step: Int? = null, steps: Int = 3) {
    val c = LiptonTheme.colors
    val accent = c.stateTone(tone).a
    Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
        LogoTitle()
        Spacer(Modifier.weight(1f))
        if (step != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                for (i in 0 until steps) {
                    val on = i == step
                    Box(
                        Modifier
                            .width(if (on) 20.dp else 6.dp)
                            .height(6.dp)
                            .then(if (on) Modifier.drawBehind { softGlow(accent.copy(alpha = 0.45f), size.height / 2, 6.dp.toPx()) } else Modifier)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (on) accent else c.text.copy(alpha = 0.28f)),
                    )
                }
            }
        }
    }
}

/** Двухцветный заголовок: первая строка — цвет текста, вторая — цвет свечения. */
@Composable
internal fun OnbTitle(first: String, second: String, tone: AuroraTone, modifier: Modifier = Modifier) {
    val c = LiptonTheme.colors
    Text(
        buildAnnotatedString {
            append(first)
            append("\n")
            withStyle(SpanStyle(color = c.stateText(tone))) { append(second) }
        },
        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp),
        color = c.text,
        modifier = modifier.padding(top = 20.dp),
    )
}

/** Подзаголовок онбординга (15/22, .72). */
@Composable
internal fun OnbSubtitle(text: AnnotatedString, modifier: Modifier = Modifier) {
    val c = LiptonTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
        color = c.text.copy(alpha = 0.78f),
        modifier = modifier.padding(top = 14.dp),
    )
}

@Composable
internal fun OnbSubtitle(text: String, modifier: Modifier = Modifier) = OnbSubtitle(AnnotatedString(text), modifier)

/** Жирные фрагменты в тексте: «Откройте **liptonone.online** → …». */
internal fun boldParts(src: String, boldColor: Color): AnnotatedString = buildAnnotatedString {
    val parts = src.split("**")
    parts.forEachIndexed { i, p ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) { append(p) } else append(p)
    }
}

/** Вторичная пилюля 52dp: стекло, иконка и текст цвета темы («Создать аккаунт», «Создать по почте»). */
@Composable
internal fun SecondaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
) {
    val c = LiptonTheme.colors
    val tint = if (color == Color.Unspecified) c.text else color
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .glass(RoundedCornerShape(999.dp), highlightHeight = 26.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = tint.copy(alpha = if (enabled) 1f else 0.5f), modifier = Modifier.size(18.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            color = tint.copy(alpha = if (enabled) 1f else 0.5f),
            maxLines = 1,
        )
        if (trailingIcon != null) Icon(trailingIcon, null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

/**
 * Белая карточка-кнопка «Попробовать 15 минут / без регистрации» с зелёной
 * круглой стрелкой справа и мягким свечением (new-onb-welcome).
 */
@Composable
internal fun TrialCtaButton(title: String, subtitle: String, onClick: () -> Unit, enabled: Boolean = true) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .drawBehind { if (enabled) softGlow(t.a.copy(alpha = if (c.isDark) 0.30f else 0.22f), size.height / 2, 16.dp.toPx(), offsetY = 8.dp.toPx()) }
            .clip(shape)
            .background(if (c.isDark) Brush.verticalGradient(listOf(Color.White, Color(0xFFE4FFF1))) else Brush.verticalGradient(listOf(Color.White, Color(0xFFF2FFF8))))
            .border(1.dp, if (c.isDark) Color.Transparent else t.a.copy(alpha = 0.25f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = 24.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = Color(0xFF050807).copy(alpha = if (enabled) 1f else 0.5f), maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF050807).copy(alpha = 0.6f), maxLines = 1)
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(t.a, t.b)).takeIf { enabled } ?: SolidColor(Color(0x33050807))),
            contentAlignment = Alignment.Center,
        ) { Icon(LiptonIcons.ArrowRight, null, tint = Color(0xFF04140C), modifier = Modifier.size(20.dp)) }
    }
}

/** «Уже есть аккаунт? **Войти**» — подпись и ссылка в цвете свечения. */
@Composable
internal fun FooterLink(prefix: String, link: String, tone: AuroraTone, onClick: () -> Unit, underline: Boolean = false) {
    val c = LiptonTheme.colors
    Box(Modifier.fillMaxWidth().padding(top = 14.dp), contentAlignment = Alignment.Center) {
        Text(
            buildAnnotatedString {
                if (prefix.isNotEmpty()) {
                    withStyle(SpanStyle(color = c.text.copy(alpha = 0.66f))) { append(prefix) }
                    append(" ")
                }
                withStyle(
                    SpanStyle(
                        color = if (underline) c.text else c.stateText(tone),
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (underline) TextDecoration.Underline else null,
                    ),
                ) { append(link) }
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

/** Мелкая подпись с иконкой по центру («🔒 Пришлём 6-значный код на почту»). */
@Composable
internal fun HintLine(icon: ImageVector, text: String, color: Color = Color.Unspecified, onClick: (() -> Unit)? = null) {
    val c = LiptonTheme.colors
    val tint = if (color == Color.Unspecified) c.text.copy(alpha = 0.6f) else color
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TABULAR_NUMS), color = tint)
    }
}

/** Ошибка под формой. */
@Composable
internal fun ErrorText(text: String?) {
    if (text.isNullOrBlank()) return
    val c = LiptonTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = c.stateText(AuroraTone.OFF),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    )
}

// ─── Вкладки способа входа: Почта / Код с сайта / Telegram ───────────────────

internal enum class LoginTab(val label: String) { EMAIL("Почта"), CODE("Код с сайта"), TELEGRAM("Telegram") }

@Composable
internal fun LoginTabs(selected: LoginTab, onSelect: (LoginTab) -> Unit) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .height(44.dp)
            .glass(shape, highlightHeight = 22.dp)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LoginTab.entries.forEach { tab ->
            val on = tab == selected
            val icon = when (tab) {
                LoginTab.EMAIL -> LiptonIcons.Mail
                LoginTab.CODE -> LiptonIcons.Browser
                LoginTab.TELEGRAM -> LiptonIcons.Send
            }
            Row(
                Modifier
                    .weight(if (tab == LoginTab.CODE) 1.25f else 1f)
                    .fillMaxSize()
                    .clip(shape)
                    .background(if (on) c.text.copy(alpha = if (c.isDark) 0.12f else 0.08f) else Color.Transparent)
                    .border(1.dp, if (on) c.text.copy(alpha = 0.14f) else Color.Transparent, shape)
                    .clickable(role = Role.Tab) { onSelect(tab) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = c.text.copy(alpha = if (on) 0.95f else 0.6f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold),
                    color = c.text.copy(alpha = if (on) 1f else 0.62f),
                    maxLines = 1,
                )
            }
        }
    }
}

// ─── Поля ────────────────────────────────────────────────────────────────────

/** Поле почты 56dp: иконка, текст, галочка при правильном адресе; рамка — цвет свечения в фокусе. */
@Composable
internal fun EmailField(
    value: String,
    onChange: (String) -> Unit,
    tone: AuroraTone,
    onDone: () -> Unit,
    placeholder: String = "you@example.com",
    autoFocus: Boolean = false,
) {
    val c = LiptonTheme.colors
    val accent = c.stateTone(tone).a
    var focused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val valid = isLikelyEmail(value)
    val shape = RoundedCornerShape(18.dp)
    LaunchedEffect(Unit) { if (autoFocus) runCatching { focus.requestFocus() } }
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind { if (focused) softGlow(accent.copy(alpha = 0.18f), 18.dp.toPx(), 10.dp.toPx()) }
            .clip(shape)
            .background(c.text.copy(alpha = if (c.isDark) 0.05f else 0.6f).let { if (c.isDark) it else Color.White.copy(alpha = 0.7f) })
            .border(1.dp, if (focused) accent.copy(alpha = 0.7f) else c.text.copy(alpha = 0.12f), shape)
            .clickable { runCatching { focus.requestFocus() } }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(LiptonIcons.Mail, null, tint = c.text.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), color = c.text.copy(alpha = 0.4f))
            BasicTextField(
                value = value,
                onValueChange = { onChange(it.trim().take(120)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.text),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { focused = it.isFocused },
            )
        }
        if (valid) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).border(1.5.dp, accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(LiptonIcons.Check, null, tint = accent, modifier = Modifier.size(12.dp)) }
        }
    }
}

/** Похоже на адрес почты (проверка перед отправкой кода). */
internal fun isLikelyEmail(s: String): Boolean =
    Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$").matches(s.trim())

/**
 * Код из [length] цифр по ячейкам (new-onb-email-code / site-code). Невидимое поле
 * под ячейками принимает ввод и вставку; текущая ячейка подсвечена цветом свечения.
 */
@Composable
internal fun CodeBoxes(
    value: String,
    length: Int,
    onChange: (String) -> Unit,
    tone: AuroraTone,
    onComplete: () -> Unit = {},
    cellHeight: Dp = if (length <= 4) 64.dp else 56.dp,
    autoFocus: Boolean = true,
) {
    val c = LiptonTheme.colors
    val accent = c.stateTone(tone).a
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (autoFocus) runCatching { focus.requestFocus() } }
    BasicTextField(
        value = value,
        onValueChange = { raw ->
            val digits = raw.filter(Char::isDigit).take(length)
            onChange(digits)
            if (digits.length == length) onComplete()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (value.length == length) onComplete() }),
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = "Код из $length цифр" },
        decorationBox = { inner ->
            Box {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (length <= 4) 12.dp else 8.dp)) {
                    for (i in 0 until length) {
                        val ch = value.getOrNull(i)
                        val current = focused && (i == value.length || (i == length - 1 && value.length == length))
                        val shape = RoundedCornerShape(16.dp)
                        Box(
                            Modifier
                                .weight(1f)
                                .height(cellHeight)
                                .drawBehind { if (current) softGlow(accent.copy(alpha = 0.32f), 16.dp.toPx(), 9.dp.toPx()) }
                                .glass(shape, highlightHeight = 24.dp)
                                .border(1.5.dp, if (current) accent.copy(alpha = 0.85f) else Color.Transparent, shape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                ch?.toString() ?: "",
                                style = MaterialTheme.typography.displaySmall.copy(fontSize = if (length <= 4) 30.sp else 24.sp, fontFeatureSettings = TABULAR_NUMS),
                                color = c.text,
                            )
                        }
                    }
                }
                Box(Modifier.size(1.dp)) { inner() }
            }
        },
    )
}

// ─── Иллюстрации ─────────────────────────────────────────────────────────────

/** Радар с орбитами и флагами стран (new-onb-welcome). */
@Composable
internal fun BoxScope.RadarArt(countries: List<String>) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    val pulse = rememberInfiniteTransition(label = "radar")
    val k by pulse.animateFloat(0.85f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "radar_k")
    Box(Modifier.fillMaxWidth().aspectRatio(1f).align(Alignment.Center)) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val ring = c.text.copy(alpha = if (c.isDark) 0.10f else 0.14f)
            listOf(0.48f, 0.34f, 0.2f).forEach { r -> drawCircle(ring, radius = size.minDimension * r, center = center, style = Stroke(1.dp.toPx())) }
            drawCircle(t.a.copy(alpha = 0.10f * k), radius = size.minDimension * 0.2f, center = center)
            softGlowCircle(t.a.copy(alpha = 0.55f), center, 6.dp.toPx(), 14.dp.toPx())
            drawCircle(Color.White, radius = 5.dp.toPx(), center = center)
            // точки на орбитах
            listOf(Offset(-0.33f, 0.12f) to 0.9f, Offset(-0.15f, 0.12f) to 1f, Offset(0.43f, 0.16f) to 0.6f).forEach { (o, a) ->
                drawCircle(t.b.copy(alpha = a), radius = 2.6.dp.toPx(), center = Offset(center.x + o.x * size.width, center.y + o.y * size.height))
            }
        }
        val flags = countries.take(2)
        if (flags.isNotEmpty()) FlagCircle(flags[0], Modifier.align(Alignment.TopStart).offset(x = 14.dp, y = 76.dp), size = 26.dp, ring = Color.White.copy(alpha = 0.7f))
        if (flags.size > 1) FlagCircle(flags[1], Modifier.align(Alignment.TopEnd).offset(x = (-76).dp, y = 82.dp), size = 26.dp, ring = Color.White.copy(alpha = 0.7f))
    }
}

/** Сообщения бота «/start» и «Аккаунт создан» (new-onb-start). */
@Composable
internal fun BoxScope.ChatArt(botName: String) {
    val c = LiptonTheme.colors
    val warn = c.stateTone(AuroraTone.OFF).a
    Column(
        Modifier.align(Alignment.BottomEnd).padding(end = 44.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val bubble = RoundedCornerShape(16.dp)
        Row(
            Modifier
                .clip(bubble)
                .background(warn.copy(alpha = 0.14f))
                .border(1.dp, warn.copy(alpha = 0.6f), bubble)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("/start", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = c.text)
            Icon(LiptonIcons.CheckDouble, null, tint = c.text.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF2AABEE)),
                contentAlignment = Alignment.Center,
            ) { Icon(LiptonIcons.Send, botName, tint = Color.White, modifier = Modifier.size(14.dp)) }
            Column(
                Modifier
                    .glass(RoundedCornerShape(16.dp), highlightHeight = 24.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Аккаунт создан", style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp), color = c.text)
                    Icon(LiptonIcons.Check, null, tint = warn, modifier = Modifier.size(13.dp))
                }
                Text("Вот ваша ссылка для подключения", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.7f))
            }
        }
    }
}

/** Кольца и плашка с галочкой (new-onb-success). */
@Composable
internal fun BoxScope.SuccessArt() {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    Box(Modifier.size(260.dp).align(Alignment.Center), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(c.text.copy(alpha = 0.08f), radius = size.minDimension * 0.47f, center = center, style = Stroke(1.dp.toPx()))
            drawCircle(t.a.copy(alpha = 0.10f), radius = size.minDimension * 0.33f, center = center)
            drawCircle(t.a.copy(alpha = 0.22f), radius = size.minDimension * 0.33f, center = center, style = Stroke(1.dp.toPx()))
            listOf(Offset(-0.34f, -0.18f), Offset(0.24f, -0.17f), Offset(0.38f, 0.1f), Offset(-0.24f, 0.24f)).forEachIndexed { i, o ->
                drawCircle(if (i % 2 == 0) t.a.copy(alpha = 0.8f) else t.b.copy(alpha = 0.8f), radius = 2.5.dp.toPx(), center = Offset(center.x + o.x * size.width, center.y + o.y * size.height))
            }
        }
        Box(
            Modifier
                .size(56.dp)
                .drawBehind { softGlow(t.a.copy(alpha = 0.5f), 16.dp.toPx(), 18.dp.toPx()) }
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(t.a, t.b))),
            contentAlignment = Alignment.Center,
        ) { Icon(LiptonIcons.Check, "Готово", tint = Color(0xFF04140C), modifier = Modifier.size(26.dp)) }
    }
}

/** Кольцо-таймер «15:00 минут» с делениями (new-onb-trial, new-guest-ended). */
@Composable
internal fun TimerRing(
    label: String,
    caption: String,
    fraction: Float,
    tone: AuroraTone,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
) {
    val c = LiptonTheme.colors
    val t = c.stateTone(tone)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            val center = Offset(r, r)
            // деления
            for (i in 0 until 60) {
                rotate(i * 6f, center) {
                    val long = i % 5 == 0
                    drawLine(
                        c.text.copy(alpha = if (long) 0.45f else 0.22f),
                        Offset(center.x, center.y - r + 2.dp.toPx()),
                        Offset(center.x, center.y - r + (if (long) 9.dp else 5.dp).toPx()),
                        strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round,
                    )
                }
            }
            val ringR = r - 22.dp.toPx()
            val sw = 9.dp.toPx()
            drawCircle(c.text.copy(alpha = 0.08f), radius = ringR, center = center, style = Stroke(sw))
            val f = fraction.coerceIn(0f, 1f)
            if (f > 0f) {
                softGlowCircle(t.a.copy(alpha = 0.25f), center, ringR, 16.dp.toPx())
                drawArc(
                    Brush.sweepGradient(listOf(t.a, t.b, t.a), center),
                    startAngle = -90f, sweepAngle = 360f * f, useCenter = false,
                    topLeft = Offset(center.x - ringR, center.y - ringR), size = Size(ringR * 2, ringR * 2),
                    style = Stroke(sw, cap = StrokeCap.Round),
                )
            }
            // ручка в начале отсчёта
            val knob = Offset(center.x, center.y - ringR)
            softGlowCircle(t.a.copy(alpha = 0.6f), knob, 6.dp.toPx(), 8.dp.toPx())
            drawCircle(if (f > 0f) Color.White else t.a, radius = 6.dp.toPx(), center = knob)
            drawCircle(c.bg.copy(alpha = 0.55f), radius = ringR - sw, center = center)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                style = TextStyle(fontFamily = UnboundedFamily, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.19f).sp, fontFeatureSettings = TABULAR_NUMS),
                color = c.text,
                maxLines = 1,
            )
            Text(caption, style = MaterialTheme.typography.labelMedium, color = c.text.copy(alpha = 0.6f))
        }
    }
}
