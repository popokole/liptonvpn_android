package com.lipton.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.UiState
import com.lipton.vpn.data.ApiClient
import com.lipton.vpn.data.MAX_BYPASS_DOMAINS
import com.lipton.vpn.data.normalizeDomain
import com.lipton.vpn.ui.auth.CodeBoxes
import com.lipton.vpn.ui.auth.isLikelyEmail
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.ButtonTone
import com.lipton.vpn.ui.components.GhostButton
import com.lipton.vpn.ui.components.GlassButton
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.GlassGroup
import com.lipton.vpn.ui.components.GroupDivider
import com.lipton.vpn.ui.components.IconTile
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.LiptonSwitch
import com.lipton.vpn.ui.components.PrimaryButton
import com.lipton.vpn.ui.components.SectionHeader
import com.lipton.vpn.ui.components.ToneChip
import com.lipton.vpn.ui.components.ToneCircleIcon
import com.lipton.vpn.ui.components.glass
import com.lipton.vpn.ui.components.softGlow
import com.lipton.vpn.ui.components.stateText
import com.lipton.vpn.ui.components.stateTone
import com.lipton.vpn.ui.tabs.baseTariff
import com.lipton.vpn.ui.tabs.monthlyPeriod
import com.lipton.vpn.ui.tabs.pluralRu
import com.lipton.vpn.ui.tabs.ruDayMonth
import com.lipton.vpn.ui.tabs.rubles
import com.lipton.vpn.ui.theme.LiptonText
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.TABULAR_NUMS
import com.lipton.vpn.ui.theme.UnboundedFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  Подэкраны профиля по макетам new-scr-*: свои домены, смена почты, логи,
//  политика конфиденциальности, промокод, уведомления.
// ─────────────────────────────────────────────────────────────────────────────

// ─── Свои домены для обхода (new-scr-domains) ────────────────────────────────

@Composable
fun DomainsScreen(state: UiState, viewModel: MainViewModel, onBack: () -> Unit) {
    val c = LiptonTheme.colors
    val accent = c.stateTone(AuroraTone.ON).a
    var input by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val domains = state.bypassDomains
    val add: () -> Unit = {
        val d = normalizeDomain(input)
        error = when {
            input.isBlank() -> "Введите домен, например example.com"
            d == null -> "Это не похоже на домен — пример: example.com"
            d in domains -> "Этот домен уже в списке"
            domains.size >= MAX_BYPASS_DOMAINS -> "Можно добавить до $MAX_BYPASS_DOMAINS доменов"
            else -> null
        }
        if (error == null && d != null) { viewModel.addBypassDomain(d); input = "" }
    }
    ProfileSubPage(state, "Свои домены для обхода", onBack) {
        SubIntro("Трафик к этим сайтам пойдёт напрямую, мимо VPN. Применится при следующем подключении.")
        Spacer(Modifier.height(20.dp))
        GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            Text("Домен", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = c.text.copy(alpha = 0.7f))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassInput(
                    input, { input = it.take(253); error = null }, "example.com", LiptonIcons.Globe,
                    modifier = Modifier.weight(1f), keyboardType = KeyboardType.Uri, error = error != null, onDone = add,
                )
                PrimaryButton("Добавить", onClick = add, icon = LiptonIcons.Plus, height = 52.dp)
            }
            Spacer(Modifier.height(10.dp))
            if (error != null) Text(error!!, style = MaterialTheme.typography.bodySmall, color = c.stateText(AuroraTone.OFF))
            else NoteLine(LiptonIcons.Info, "До $MAX_BYPASS_DOMAINS доменов · поддомены — тоже напрямую")
        }
        Row(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SectionHeader("Ваши домены", Modifier.weight(1f))
            Text("${domains.size} из $MAX_BYPASS_DOMAINS", style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TABULAR_NUMS), color = c.text.copy(alpha = 0.6f), modifier = Modifier.padding(end = 16.dp))
        }
        if (domains.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Пока пусто", style = MaterialTheme.typography.titleSmall, color = c.text)
                Text("Добавьте сайт, который должен открываться без VPN — например, корпоративный портал.", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.66f))
            }
        } else {
            GlassGroup {
                domains.forEachIndexed { i, d ->
                    if (i > 0) GroupDivider()
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ToneCircleIcon(LiptonIcons.Globe, accent, size = 32.dp, iconSize = 15.dp)
                        Column(Modifier.weight(1f)) {
                            Text(d, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            state.bypassDomainDates[d]?.let { at ->
                                Text("Добавлен ${ruDayMonth(at)}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.55f))
                            }
                        }
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(c.text.copy(alpha = 0.05f)).border(1.dp, c.text.copy(alpha = 0.10f), CircleShape)
                                .clickable(role = Role.Button) { viewModel.removeBypassDomain(d) }
                                .semantics { contentDescription = "Удалить $d" },
                            contentAlignment = Alignment.Center,
                        ) { Icon(LiptonIcons.Close, null, tint = c.text.copy(alpha = 0.7f), modifier = Modifier.size(13.dp)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        GlassGroup {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconTile(LiptonIcons.Branch)
                Text(
                    if (state.bypassRu) "Российские сайты уже идут напрямую — их добавлять не нужно"
                    else "Включите обход — и российские сайты пойдут напрямую",
                    style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f),
                )
            }
            GroupDivider()
            SwitchRow("Обход российских сайтов", "Банки, Госуслуги, маркетплейсы", LiptonIcons.ShieldCheck, state.bypassRu) { viewModel.setBypassRu(it) }
        }
    }
}

// ─── Смена почты (new-scr-email) ─────────────────────────────────────────────

@Composable
fun EmailScreen(state: UiState, viewModel: MainViewModel, onBack: () -> Unit, onOpenSite: () -> Unit) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    val scope = rememberCoroutineScope()
    val fx = LocalScreenFixtures.current
    val current = state.me?.email?.takeIf { it.isNotBlank() }
    var newEmail by rememberSaveable { mutableStateOf(fx?.emailStep2.orEmpty()) }
    var sentTo by rememberSaveable { mutableStateOf(fx?.emailStep2) }
    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resendAt by remember { mutableLongStateOf(if (fx?.emailStep2 != null) System.currentTimeMillis() + 41_000 else 0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(resendAt) { while (now < resendAt) { delay(500); now = System.currentTimeMillis() } }

    val request: () -> Unit = {
        val e = newEmail.trim().lowercase(Locale.ROOT)
        when {
            !isLikelyEmail(e) -> error = "Проверьте адрес почты"
            e == current -> error = "Это ваша текущая почта"
            !busy -> scope.launch {
                busy = true; error = null
                try {
                    viewModel.api.emailLinkRequestCode(e)
                    sentTo = e; newEmail = e; code = ""
                    resendAt = System.currentTimeMillis() + 60_000L; now = System.currentTimeMillis()
                } catch (ex: ApiClient.ApiException) {
                    if (ex.endpointMissing) onOpenSite() else error = ex.message ?: "Не удалось отправить код"
                } catch (ex: Exception) {
                    error = ex.message ?: "Не удалось отправить код"
                } finally { busy = false }
            }
        }
    }
    val confirm: () -> Unit = {
        val to = sentTo
        if (to != null && code.length == 6 && !busy) scope.launch {
            busy = true; error = null
            try {
                val merged = if (current == null) { viewModel.api.linkEmailVerify(to, code); false } else viewModel.api.changeEmail(to, code)
                if (merged) viewModel.refreshAccount() else viewModel.loadProfile()
                viewModel.showError(if (current == null) "Почта привязана" else "Почта изменена")
                onBack()
            } catch (ex: ApiClient.ApiException) {
                error = ex.message ?: "Неверный код"; code = ""
            } catch (ex: Exception) {
                error = ex.message ?: "Не удалось подтвердить"
            } finally { busy = false }
        }
    }

    ProfileSubPage(state, if (current == null) "Привязать почту" else "Изменение почты", onBack) {
        GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(LiptonIcons.Mail)
                Column(Modifier.weight(1f)) {
                    Text("Текущая почта", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f))
                    Text(current ?: "не привязана", style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (current != null) ToneChip("Подтверждена", t.a, icon = LiptonIcons.Check, textColor = c.stateText(AuroraTone.ON), height = 24.dp)
            }
        }
        Spacer(Modifier.height(12.dp))
        StepCard(1, "Новая почта", active = sentTo == null, done = sentTo != null) {
            GlassInput(
                newEmail, { newEmail = it.trim().take(120); error = null; if (sentTo != null && it.trim() != sentTo) sentTo = null },
                "new@example.com", LiptonIcons.Mail, Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Email, onDone = request,
                trailing = if (isLikelyEmail(newEmail)) ({ ToneCircleIcon(LiptonIcons.Check, t.a, size = 22.dp, iconSize = 11.dp) }) else null,
            )
            Spacer(Modifier.height(8.dp))
            Text("Код придёт на новый адрес", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f))
            if (sentTo == null) {
                Spacer(Modifier.height(14.dp))
                PrimaryButton(
                    if (busy) "Отправляем…" else "Отправить код", onClick = request, trailingIcon = LiptonIcons.ArrowRight,
                    loading = busy, enabled = isLikelyEmail(newEmail) && !busy, height = 52.dp, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        StepCard(2, "Код из письма", active = sentTo != null, done = false) {
            Text(
                if (sentTo != null) "Введите 6 цифр из письма на $sentTo" else "Сначала отправьте код на новую почту",
                style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.7f),
            )
            if (sentTo != null) {
                Spacer(Modifier.height(14.dp))
                CodeBoxes(code, 6, { code = it; error = null }, AuroraTone.ON, onComplete = confirm, cellHeight = 52.dp, autoFocus = fx == null)
                Spacer(Modifier.height(12.dp))
                val left = ((resendAt - now) / 1000).coerceAtLeast(0)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    GhostButton(
                        if (left > 0) "Отправить код ещё раз · ${left / 60}:${String.format(Locale.US, "%02d", left % 60)}" else "Отправить код ещё раз",
                        onClick = request, icon = LiptonIcons.Refresh, enabled = left == 0L && !busy,
                        color = c.text.copy(alpha = if (left > 0) 0.55f else 0.85f),
                    )
                }
                Spacer(Modifier.height(4.dp))
                PrimaryButton(
                    if (busy) "Проверяем…" else "Подтвердить", onClick = confirm, icon = LiptonIcons.Check,
                    loading = busy, enabled = code.length == 6 && !busy, height = 52.dp, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = c.stateText(AuroraTone.OFF))
        }
        if (state.me?.telegramLinked == true) {
            Spacer(Modifier.height(16.dp))
            NoteLine(LiptonIcons.Send, "Вход через Telegram продолжит работать")
        }
    }
}

@Composable
private fun StepCard(n: Int, title: String, active: Boolean, done: Boolean, content: @Composable () -> Unit) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .glass(shape)
            .border(1.dp, if (active) t.a.copy(alpha = 0.35f) else Color.Transparent, shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(24.dp).clip(CircleShape)
                    .background(if (active || done) Brush.linearGradient(listOf(t.a, t.b)) else Brush.linearGradient(listOf(c.text.copy(alpha = 0.08f), c.text.copy(alpha = 0.08f)))),
                contentAlignment = Alignment.Center,
            ) {
                if (done) Icon(LiptonIcons.Check, null, tint = Color(0xFF04140C), modifier = Modifier.size(12.dp))
                else Text("$n", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (active) Color(0xFF04140C) else c.text.copy(alpha = 0.6f))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text.copy(alpha = if (active || done) 1f else 0.6f), modifier = Modifier.weight(1f))
            Text("Шаг $n из 2", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.5f))
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth()) { Column { content() } }
    }
}

// ─── Логи приложения (new-scr-logs) ──────────────────────────────────────────

@Composable
fun LogsPage(state: UiState, viewModel: MainViewModel, onBack: () -> Unit) {
    val c = LiptonTheme.colors
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var sending by remember { mutableStateOf(false) }
    val lines = state.logLines.takeLast(150)
    ProfileSubPage(state, "Логи приложения", onBack) {
        GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ToneCircleIcon(LiptonIcons.ShieldCheck, c.stateTone(AuroraTone.ON).a, size = 32.dp, iconSize = 15.dp)
                Text(
                    buildAnnotatedString {
                        append("Пригодятся поддержке, если что-то не работает. ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.text)) { append("IP-адреса при отправке скрываются.") }
                    },
                    style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.78f),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton("Обновить", onClick = { viewModel.logDiagnostics() }, icon = LiptonIcons.Refresh, height = 48.dp, modifier = Modifier.weight(1f))
            GlassButton(
                "Копировать",
                onClick = { clipboard.setText(AnnotatedString(viewModel.logsForSupport())); viewModel.showError("Логи скопированы") },
                icon = LiptonIcons.Copy, height = 48.dp, modifier = Modifier.weight(1f),
            )
        }
        if (state.isAuthed) {
            Spacer(Modifier.height(10.dp))
            val accent = c.stateTone(AuroraTone.ON).a
            val shape = RoundedCornerShape(999.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(shape)
                    .background(accent.copy(alpha = 0.08f))
                    .border(1.dp, accent.copy(alpha = 0.55f), shape)
                    .clickable(enabled = !sending && lines.isNotEmpty(), role = Role.Button) {
                        sending = true
                        scope.launch {
                            try { viewModel.sendLogsToSupport(); viewModel.showError("Логи отправлены в поддержку") }
                            catch (e: Exception) { viewModel.showError(e.message ?: "Не удалось отправить логи") }
                            finally { sending = false }
                        }
                    },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(LiptonIcons.Send, null, tint = c.stateText(AuroraTone.ON), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (sending) "Отправляем…" else "Отправить в поддержку", style = MaterialTheme.typography.titleSmall, color = c.stateText(AuroraTone.ON))
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(LiptonIcons.Doc, null, tint = c.text.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("lipton-vpn.log", style = LiptonText.mono.copy(fontWeight = FontWeight.SemiBold), color = c.text, modifier = Modifier.weight(1f))
                Box(Modifier.size(6.dp).clip(CircleShape).background(c.stateTone(AuroraTone.ON).a))
                Spacer(Modifier.width(6.dp))
                Text("сегодня · ${state.logLines.size} ${pluralRu(state.logLines.size, "запись", "записи", "записей")}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(12.dp))
            if (lines.isEmpty()) {
                Text("Логов пока нет — они появятся после подключения", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.6f))
            } else {
                lines.forEach { LogLine(it) }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassGroup {
            SwitchRow("Подробные логи", "Больше деталей для разбора — включайте по просьбе поддержки", LiptonIcons.ListIcon, state.verboseLogs) {
                viewModel.setVerboseLogs(it)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GhostButton("Очистить логи", onClick = { viewModel.clearLogs() }, color = c.stateText(AuroraTone.OFF))
        }
    }
}

/** Строка лога: время приглушённо, уровень цветом, предупреждения подсвечены. */
@Composable
private fun LogLine(line: String) {
    val c = LiptonTheme.colors
    val lower = line.lowercase(Locale.ROOT)
    val isErr = "error" in lower || "ошибк" in lower || "[err" in lower
    val isWarn = !isErr && ("warn" in lower)
    val warn = c.stateText(AuroraTone.OFF)
    val time = Regex("^\\[\\d{2}:\\d{2}:\\d{2}]").find(line)?.value
    val rest = if (time != null) line.removePrefix(time) else line
    Text(
        buildAnnotatedString {
            if (time != null) withStyle(SpanStyle(color = c.text.copy(alpha = 0.45f))) { append(time) }
            append(rest)
        },
        style = LiptonText.mono.copy(fontSize = 11.sp, lineHeight = 17.sp),
        color = when {
            isErr -> c.danger
            isWarn -> warn
            else -> c.text.copy(alpha = 0.82f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isWarn || isErr) Modifier.clip(RoundedCornerShape(6.dp)).background((if (isErr) c.danger else warn).copy(alpha = 0.10f))
                    .border(1.dp, (if (isErr) c.danger else warn).copy(alpha = 0.25f), RoundedCornerShape(6.dp)).padding(horizontal = 4.dp)
                else Modifier,
            )
            .padding(vertical = 1.dp),
    )
}

// ─── Политика конфиденциальности (new-scr-privacy) ───────────────────────────

/**
 * Коротко о политике и ссылка на полную версию. TODO(redesign): тексты из макета
 * сверить с юристом и полной редакцией на liptonone.online/legal?doc=privacy.
 */
@Composable
fun PrivacyScreen(state: UiState, onBack: () -> Unit, onOpenFull: () -> Unit) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    ProfileSubPage(state, "Политика конфиденциальности", onBack) {
        ToneChip("Кратко — полная версия на сайте", t.a, icon = LiptonIcons.Doc, textColor = c.text.copy(alpha = 0.85f), height = 28.dp)
        SectionHeader("Коротко", Modifier.padding(top = 22.dp, bottom = 8.dp))
        val facts = listOf(
            "Не храним историю посещённых сайтов",
            "Не продаём данные",
            "Платежи — через ЮKassa, карту мы не видим",
            "Можно отменить подписку и выйти в любой момент",
        )
        facts.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { f ->
                    GlassCard(Modifier.weight(1f).height(128.dp), contentPadding = PaddingValues(16.dp)) {
                        ToneCircleIcon(LiptonIcons.Check, t.a, size = 26.dp, iconSize = 13.dp, glow = true)
                        Spacer(Modifier.weight(1f))
                        Text(f, style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, lineHeight = 19.sp), color = c.text)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        PolicyCard("Какие данные мы собираем") {
            Text("Только то, без чего сервис не работает:", style = MaterialTheme.typography.bodyMedium, color = c.text.copy(alpha = 0.78f))
            Spacer(Modifier.height(6.dp))
            BulletLine(bold("Почта или Telegram-ID", " — для входа в аккаунт", c.text))
            BulletLine(bold("Число устройств", " — чтобы соблюдать лимит устройств", c.text))
            BulletLine(bold("Объём трафика", " — для статистики в приложении", c.text))
            BulletLine(bold("Технические логи", " — только по вашему запросу", c.text))
        }
        PolicyCard("Зачем") {
            Text("Чтобы вы входили в аккаунт с любого устройства, а мы проверяли подписку и лимит устройств. Для рекламы данные не используем.", style = MaterialTheme.typography.bodyMedium, color = c.text.copy(alpha = 0.78f))
        }
        PolicyCard("Где хранятся") {
            Text("На серверах Lipton VPN, доступ есть только у команды сервиса. Платёжные данные обрабатывает ЮKassa — номер карты к нам не попадает.", style = MaterialTheme.typography.bodyMedium, color = c.text.copy(alpha = 0.78f))
        }
        PolicyCard("Ваши права") {
            Text("Можно узнать, какие данные о вас хранятся, исправить их или удалить аккаунт целиком — напишите в поддержку или боту @liptonvpn_bot.", style = MaterialTheme.typography.bodyMedium, color = c.text.copy(alpha = 0.78f))
        }
        Spacer(Modifier.height(8.dp))
        GlassButton("Полная версия на liptonone.online", onClick = onOpenFull, icon = LiptonIcons.External, height = 48.dp, modifier = Modifier.fillMaxWidth())
    }
}

private fun bold(b: String, rest: String, color: Color): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) { append(b) }
    append(rest)
}

@Composable
private fun PolicyCard(title: String, content: @Composable () -> Unit) {
    val c = LiptonTheme.colors
    GlassCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), contentPadding = PaddingValues(18.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = c.text)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

// ─── Промокод (new-scr-promo) ────────────────────────────────────────────────

@Composable
fun PromoScreen(state: UiState, viewModel: MainViewModel, onBack: () -> Unit, onPay: () -> Unit) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var applied by remember { mutableStateOf(viewModel.pendingPromo) }
    var info by remember { mutableStateOf(viewModel.pendingPromoInfo) }
    val apply: () -> Unit = {
        if (code.isBlank()) error = "Введите промокод"
        else if (!busy) scope.launch {
            busy = true; error = null
            try {
                val r = viewModel.validatePromo(code)
                if (r.valid) { applied = code.trim(); info = r; code = "" } else error = r.reason ?: "Промокод не подходит"
            } catch (e: Exception) {
                error = e.message ?: "Не удалось проверить промокод"
            } finally { busy = false }
        }
    }
    val value = when {
        info?.percentOff != null -> "−${info?.percentOff}%"
        info?.bonusDays != null -> "+${info?.bonusDays} ${pluralRu(info?.bonusDays ?: 0, "день", "дня", "дней")}"
        else -> null
    }
    ProfileSubPage(state, "Промокод", onBack) {
        // Билет: значение промокода, пунктир и «подарок»
        val shape = RoundedCornerShape(24.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .height(168.dp)
                .drawBehind { softGlow(t.a.copy(alpha = if (c.isDark) 0.22f else 0.12f), 24.dp.toPx(), 16.dp.toPx()) }
                .clip(shape)
                .background(Brush.linearGradient(listOf(t.a.copy(alpha = if (c.isDark) 0.22f else 0.14f), t.b.copy(alpha = if (c.isDark) 0.10f else 0.08f))))
                .border(1.dp, Brush.linearGradient(listOf(t.a.copy(alpha = 0.6f), t.b.copy(alpha = 0.4f))), shape)
                .drawBehind {
                    val x = size.width * 0.72f
                    drawLine(c.text.copy(alpha = 0.25f), Offset(x, 14.dp.toPx()), Offset(x, size.height - 14.dp.toPx()), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
                    drawCircle(c.bg, radius = 10.dp.toPx(), center = Offset(x, 0f))
                    drawCircle(c.bg, radius = 10.dp.toPx(), center = Offset(x, size.height))
                }
                .padding(20.dp),
        ) {
            Column(Modifier.weight(0.72f).padding(end = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(LiptonIcons.Tag, null, tint = c.stateText(AuroraTone.ON), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ПРОМОКОД", style = LiptonText.section.copy(fontSize = 12.sp), color = c.stateText(AuroraTone.ON))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    value ?: "Скидка",
                    style = androidx.compose.ui.text.TextStyle(fontFamily = UnboundedFamily, fontWeight = FontWeight.SemiBold, fontSize = if (value != null) 44.sp else 34.sp, fontFeatureSettings = TABULAR_NUMS),
                    color = c.text, maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (value != null) "на ближайшую оплату" else "или бонусные дни к подписке",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = c.text.copy(alpha = 0.72f),
                )
            }
            Column(Modifier.weight(0.28f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(t.a.copy(alpha = 0.16f)).border(1.dp, t.a.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(LiptonIcons.Gift, null, tint = c.stateText(AuroraTone.ON), modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("подарок", style = MaterialTheme.typography.labelMedium, color = c.text.copy(alpha = 0.7f))
            }
        }
        Text("Промокод", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = c.text.copy(alpha = 0.7f), modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
        GlassInput(
            code, { code = it.uppercase(Locale.ROOT).filter { ch -> !ch.isWhitespace() }.take(32); error = null },
            "Например, LIPTON10", LiptonIcons.Tag, Modifier.fillMaxWidth(),
            capitalization = KeyboardCapitalization.Characters, error = error != null, onDone = apply,
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton(if (busy) "Проверяем…" else "Применить", onClick = apply, trailingIcon = LiptonIcons.ArrowRight, loading = busy, enabled = !busy && code.isNotBlank(), modifier = Modifier.fillMaxWidth())
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = c.stateText(AuroraTone.OFF))
        }
        val ap = applied
        if (ap != null) {
            Spacer(Modifier.height(12.dp))
            val sh = RoundedCornerShape(20.dp)
            Row(
                Modifier.fillMaxWidth().clip(sh).background(t.a.copy(alpha = 0.10f)).border(1.dp, t.a.copy(alpha = 0.4f), sh).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(listOf(t.a, t.b))), contentAlignment = Alignment.Center) {
                    Icon(LiptonIcons.Check, null, tint = Color(0xFF04140C), modifier = Modifier.size(16.dp))
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(ap, style = MaterialTheme.typography.titleMedium.copy(fontFamily = UnboundedFamily, fontSize = 15.sp), color = c.text)
                        ToneChip("применён", t.a, textColor = c.stateText(AuroraTone.ON), height = 20.dp)
                    }
                    Text(
                        (value ?: "скидка") + " на следующую оплату",
                        style = MaterialTheme.typography.labelMedium, color = c.stateText(AuroraTone.ON),
                    )
                }
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(c.text.copy(alpha = 0.06f)).clickable(role = Role.Button) {
                        viewModel.clearPendingPromo(); applied = null; info = null
                    }.semantics { contentDescription = "Убрать промокод" },
                    contentAlignment = Alignment.Center,
                ) { Icon(LiptonIcons.Close, null, tint = c.text.copy(alpha = 0.7f), modifier = Modifier.size(12.dp)) }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Перейти к оплате", onClick = onPay, tone = ButtonTone.ACCENT, trailingIcon = LiptonIcons.ArrowRight, height = 52.dp, modifier = Modifier.fillMaxWidth())
        }
        SectionHeader("Как это работает", Modifier.padding(top = 28.dp, bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val base = state.appConfig?.baseTariff()?.monthlyPeriod()
            val pct = info?.percentOff
            val example = if (base != null && pct != null) {
                "Например, ${rubles(base.priceKopeks)} → ${rubles(base.priceKopeks * (100 - pct) / 100)} на «${state.appConfig?.baseTariff()?.title ?: "Базовом"}»"
            } else "Сумма со скидкой — на экране оплаты"
            HowTile(Modifier.weight(1f), LiptonIcons.Percent, "Скидка применится к следующей оплате", example)
            HowTile(Modifier.weight(1f), LiptonIcons.Calendar, "Бонусные дни — вместе с оплатой", "Прибавятся к сроку подписки")
        }
    }
}

@Composable
private fun HowTile(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String) {
    val c = LiptonTheme.colors
    GlassCard(modifier.heightIn(min = 156.dp), contentPadding = PaddingValues(16.dp)) {
        IconTile(icon)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, lineHeight = 19.sp), color = c.text)
        Spacer(Modifier.height(4.dp))
        Text(sub, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f))
    }
}

// ─── Уведомления: три переключателя ──────────────────────────────────────────

/**
 * Уведомления по GET/PUT /me/notifications. Если ручки нет — остаётся один
 * локальный тумблер напоминаний на телефоне. Сообщения о списаниях и оплатах
 * не отключаются.
 */
@Composable
fun NotificationsScreen(state: UiState, viewModel: MainViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { viewModel.loadNotificationPrefs() }
    val p = state.notifPrefs
    ProfileSubPage(state, "Уведомления", onBack) {
        SubIntro("Выберите, о чём напоминать. Настройки сохраняются в аккаунте и действуют на всех устройствах.")
        Spacer(Modifier.height(20.dp))
        GlassGroup {
            if (p != null) {
                SwitchRow("Напоминания об оплате", "Скоро закончится подписка или будет списание", LiptonIcons.Calendar, p.paymentReminders) { v ->
                    viewModel.updateNotificationPrefs { it.copy(paymentReminders = v) }
                }
                GroupDivider()
                SwitchRow("Новости", "Обновления приложения, тарифы и советы", LiptonIcons.Megaphone, p.news) { v ->
                    viewModel.updateNotificationPrefs { it.copy(news = v) }
                }
                GroupDivider()
                SwitchRow("Сообщения в Telegram", "Напоминания и новости от бота", LiptonIcons.Send, p.telegramMessages) { v ->
                    viewModel.updateNotificationPrefs { it.copy(telegramMessages = v) }
                }
            } else {
                SwitchRow("Уведомления на телефоне", "О сроке подписки и трафике", LiptonIcons.Bell, state.notificationsEnabled) { viewModel.setNotificationsEnabled(it) }
            }
        }
        Spacer(Modifier.height(14.dp))
        NoteLine(LiptonIcons.Info, "Сообщения о списаниях и оплатах приходят всегда — их отключить нельзя.")
    }
}
