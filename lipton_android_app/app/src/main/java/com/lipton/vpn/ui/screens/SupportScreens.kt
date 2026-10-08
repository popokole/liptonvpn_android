package com.lipton.vpn.ui.screens

import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.UiState
import com.lipton.vpn.data.ApiClient
import com.lipton.vpn.data.model.AiMessage
import com.lipton.vpn.data.model.ArticleDetail
import com.lipton.vpn.data.model.ArticleSummary
import com.lipton.vpn.data.model.FaqEntry
import com.lipton.vpn.data.parseIsoMillis
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.GlassGroup
import com.lipton.vpn.ui.components.GroupDivider
import com.lipton.vpn.ui.components.IconTile
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.PrimaryButton
import com.lipton.vpn.ui.components.SectionHeader
import com.lipton.vpn.ui.components.ToneChip
import com.lipton.vpn.ui.components.glass
import com.lipton.vpn.ui.components.stateText
import com.lipton.vpn.ui.components.stateTone
import com.lipton.vpn.ui.components.tabBarBottomPadding
import com.lipton.vpn.ui.theme.LiptonTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  Чат поддержки (new-scr-support) и база знаний (new-scr-kb): ИИ-помощник с
//  «Помогло / Не помогло» и «Позвать оператора»; статьи блога по разделам,
//  а если ручки статей ещё нет — вопросы из /faq.
// ─────────────────────────────────────────────────────────────────────────────

private fun hm(iso: String?): String? = parseIsoMillis(iso)?.let {
    val c = Calendar.getInstance().apply { timeInMillis = it }
    String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
}

private fun nowIso(): String {
    val f = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    return f.format(java.util.Date())
}

// ─── Чат поддержки ───────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupportChatScreen(state: UiState, viewModel: MainViewModel, onBack: () -> Unit, onOpenBot: () -> Unit) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    val fx = LocalScreenFixtures.current
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf(fx?.dialog ?: emptyList()) }
    var mode by remember { mutableStateOf("auto") }
    var dialogId by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(fx != null) }
    var input by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var feedbackSupported by remember { mutableStateOf(true) }
    val feedback = remember { mutableStateMapOf<String, Boolean>() }
    var note by remember { mutableStateOf<String?>(null) }
    val list = rememberLazyListState()

    suspend fun reload() {
        try {
            val d = viewModel.api.getAiDialog()
            messages = d.messages
            mode = d.mode ?: mode
            dialogId = d.id ?: dialogId
        } catch (_: Exception) {}
        loaded = true
    }
    LaunchedEffect(Unit) { if (fx == null) reload() }
    // Ручной режим: отвечает оператор — подтягиваем новые сообщения раз в 6 с.
    LaunchedEffect(mode) {
        if (fx != null) return@LaunchedEffect
        while (mode == "manual") { delay(6_000); reload() }
    }
    LaunchedEffect(messages.size, busy) { if (messages.isNotEmpty()) list.animateScrollToItem(messages.size + 1) }

    val send: () -> Unit = {
        val text = input.trim()
        if (text.isNotEmpty() && !busy) {
            input = ""
            messages = messages + AiMessage("user", text, nowIso())
            scope.launch {
                busy = true
                try {
                    val r = viewModel.api.aiChat(text)
                    if (r.reply.isNotBlank()) messages = messages + AiMessage("assistant", r.reply, nowIso())
                    mode = r.mode.ifBlank { mode }
                    if (r.mode == "manual" && r.reply.isBlank()) note = "Сообщение передано оператору — он ответит здесь"
                } catch (e: ApiClient.ApiException) {
                    note = e.message ?: "Не удалось отправить — попробуйте ещё раз"
                } catch (_: Exception) {
                    note = "Не удалось отправить — попробуйте ещё раз"
                } finally { busy = false }
            }
        }
    }
    val callOperator: () -> Unit = {
        scope.launch {
            try {
                viewModel.api.aiOperator(dialogId)
                mode = "manual"
                note = "Позвали оператора — он ответит здесь"
            } catch (e: ApiClient.ApiException) {
                if (e.endpointMissing) onOpenBot() else note = e.message ?: "Не удалось позвать оператора"
            } catch (_: Exception) {
                note = "Не удалось позвать оператора"
            }
        }
    }
    val sendFeedback: (String, Boolean) -> Unit = { id, helpful ->
        feedback[id] = helpful
        scope.launch {
            try { viewModel.api.aiFeedback(id, helpful) }
            catch (e: ApiClient.ApiException) { if (e.endpointMissing) feedbackSupported = false }
            catch (_: Exception) {}
        }
    }
    val attachLogs: () -> Unit = {
        scope.launch {
            try { viewModel.sendLogsToSupport(); note = "Логи приложения отправлены в поддержку" }
            catch (e: Exception) { note = e.message ?: "Не удалось отправить логи" }
        }
    }

    ProfileSubPage(state, "Чат поддержки", onBack, scroll = false) {
        // Шапка: кто отвечает
        GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AiAvatar(40.dp, online = true)
                Column {
                    Text(if (mode == "manual") "Оператор в чате" else "ИИ-помощник отвечает сразу", style = MaterialTheme.typography.titleSmall, color = c.text)
                    Text(
                        if (mode == "manual") "Ответит здесь, обычно за несколько минут" else "Оператор подключится при необходимости",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.62f),
                    )
                }
            }
        }
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            state = list,
            contentPadding = PaddingValues(top = 14.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!loaded) {
                item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = t.a, strokeWidth = 2.dp, modifier = Modifier.size(22.dp)) } }
            } else if (messages.isEmpty()) {
                item {
                    Text(
                        "Опишите, что не работает: на каком устройстве, что пишет приложение. Ответит ИИ-помощник, при необходимости подключится оператор.",
                        style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.66f), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 12.dp),
                    )
                }
            } else {
                item { DayDivider("Сегодня") }
            }
            val lastAssistant = messages.indexOfLast { it.role != "user" }
            itemsIndexed(messages) { i, m ->
                if (m.role == "user") UserBubble(m) else {
                    Column {
                        AssistantBubble(m)
                        if (i == lastAssistant && mode != "manual") {
                            Spacer(Modifier.height(8.dp))
                            FlowRow(Modifier.padding(start = 34.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val key = m.id ?: "idx:$i"
                                val fb = feedback[key]
                                if (feedbackSupported && m.id != null) {
                                    if (fb == null) {
                                        ChatChip("Помогло", LiptonIcons.Check, accent = true) { sendFeedback(key, true) }
                                        ChatChip("Не помогло", null) { sendFeedback(key, false) }
                                    } else {
                                        ChatChip(if (fb) "Спасибо за отзыв" else "Жаль — позовите оператора", if (fb) LiptonIcons.Check else null, accent = fb) {}
                                    }
                                }
                                ChatChip("Позвать оператора", LiptonIcons.Headset) { callOperator() }
                            }
                            Text(
                                listOfNotNull("ИИ-помощник", hm(m.at)).joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 34.dp, top = 6.dp),
                            )
                        }
                    }
                }
            }
            if (busy) item { TypingBubble() }
            note?.let { n ->
                item {
                    Text(n, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
            }
        }
        // Строка ввода: скрепка (логи), поле, отправка
        val shape = RoundedCornerShape(999.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = 10.dp)
                .heightIn(min = 52.dp)
                .glass(shape, highlightHeight = 26.dp)
                .padding(start = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = attachLogs).semantics { contentDescription = "Отправить логи приложения" },
                contentAlignment = Alignment.Center,
            ) { Icon(LiptonIcons.Paperclip, null, tint = c.text.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) }
            Box(Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 12.dp)) {
                if (input.isEmpty()) Text("Сообщение…", style = MaterialTheme.typography.bodyLarge, color = c.text.copy(alpha = 0.45f))
                BasicTextField(
                    value = input, onValueChange = { input = it.take(2000) }, maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.text),
                    cursorBrush = SolidColor(t.a), modifier = Modifier.fillMaxWidth(),
                )
            }
            val can = input.isNotBlank() && !busy
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(if (can) Brush.linearGradient(listOf(t.a, t.b)) else SolidColor(c.text.copy(alpha = 0.08f)))
                    .clickable(enabled = can, role = Role.Button, onClick = send)
                    .semantics { contentDescription = "Отправить" },
                contentAlignment = Alignment.Center,
            ) { Icon(LiptonIcons.ArrowUp, null, tint = if (can) Color(0xFF04140C) else c.text.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) }
        }
        if (!WindowInsets.isImeVisible) Spacer(Modifier.height(tabBarBottomPadding()))
    }
}

@Composable
private fun AiAvatar(size: androidx.compose.ui.unit.Dp, online: Boolean = false) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    Box(Modifier.size(size)) {
        Box(
            Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(t.a, t.b))),
            contentAlignment = Alignment.Center,
        ) { Icon(LiptonIcons.Sparkle, null, tint = Color(0xFF04140C), modifier = Modifier.size(size * 0.48f)) }
        if (online) Box(Modifier.align(Alignment.BottomEnd).size(size * 0.3f).clip(CircleShape).background(c.bg).padding(2.dp).clip(CircleShape).background(t.a))
    }
}

@Composable
private fun DayDivider(text: String) {
    val c = LiptonTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(c.text.copy(alpha = 0.08f)))
        Text(text, style = MaterialTheme.typography.labelMedium, color = c.text.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(c.text.copy(alpha = 0.08f)))
    }
}

@Composable
private fun UserBubble(m: AiMessage) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Box(
            Modifier
                .widthIn(max = 290.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp))
                .background(Brush.linearGradient(listOf(t.a, t.b)))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) { Text(m.content, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp), color = Color(0xFF04140C)) }
        hm(m.at)?.let { time ->
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(time, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.5f))
                Spacer(Modifier.width(4.dp))
                Icon(LiptonIcons.CheckDouble, null, tint = c.stateText(AuroraTone.ON), modifier = Modifier.size(12.dp))
            }
        }
    }
}

/** Ответ ИИ: строки «1. …» показываются шагами с номерами, остальное — абзацами. */
@Composable
private fun AssistantBubble(m: AiMessage) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AiAvatar(24.dp)
        Column(
            Modifier
                .widthIn(max = 290.dp)
                .glass(RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp), highlightHeight = 28.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val step = Regex("^\\s*(\\d{1,2})[.)]\\s+(.*)$")
            m.content.trim().lines().filter { it.isNotBlank() }.forEach { line ->
                val mm = step.find(line)
                if (mm != null) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.padding(top = 1.dp).size(20.dp).clip(CircleShape).border(1.dp, t.a.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                            Text(mm.groupValues[1], style = MaterialTheme.typography.labelSmall, color = c.stateText(AuroraTone.ON))
                        }
                        Text(mm.groupValues[2], style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp), color = c.text)
                    }
                } else {
                    Text(line.trim(), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold), color = c.text)
                }
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    val c = LiptonTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AiAvatar(24.dp)
        Row(
            Modifier.glass(RoundedCornerShape(16.dp), highlightHeight = 20.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(color = c.stateTone(AuroraTone.ON).a, strokeWidth = 1.5.dp, modifier = Modifier.size(12.dp))
            Text("ИИ-помощник печатает…", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun ChatChip(text: String, icon: ImageVector?, accent: Boolean = false, onClick: () -> Unit) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .height(34.dp)
            .clip(shape)
            .background(if (accent) t.a.copy(alpha = 0.10f) else c.text.copy(alpha = 0.04f))
            .border(1.dp, if (accent) t.a.copy(alpha = 0.5f) else c.text.copy(alpha = 0.16f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = if (accent) c.stateText(AuroraTone.ON) else c.text.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = if (accent) c.stateText(AuroraTone.ON) else c.text)
    }
}

// ─── База знаний ─────────────────────────────────────────────────────────────

private fun categoryIcon(cat: String?): ImageVector {
    val s = cat?.lowercase(Locale.ROOT).orEmpty()
    return when {
        "оплат" in s || "тариф" in s -> LiptonIcons.Card
        "устрой" in s -> LiptonIcons.Phone
        "обход" in s || "блок" in s -> LiptonIcons.Shuffle
        else -> LiptonIcons.Bolt
    }
}

private val CATEGORY_ORDER = listOf("Подключение", "Оплата", "Устройства", "Обход блокировок")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KnowledgeBaseScreen(state: UiState, viewModel: MainViewModel, onBack: () -> Unit, onArticle: (String) -> Unit, onSupport: () -> Unit) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    val fx = LocalScreenFixtures.current
    var articles by remember { mutableStateOf(fx?.articles) }
    var faq by remember { mutableStateOf(fx?.faq) }
    var loading by remember { mutableStateOf(fx == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val open = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        if (fx != null) return@LaunchedEffect
        // Статьи блога; нет ручки или пусто — вопросы из /faq одним списком.
        articles = try { viewModel.api.getArticles().takeIf { it.isNotEmpty() } } catch (_: Exception) { null }
        if (articles == null) faq = try { viewModel.api.getFaq() } catch (e: Exception) { error = e.message ?: "Не удалось загрузить базу знаний"; null }
        loading = false
    }

    ProfileSubPage(state, "База знаний", onBack) {
        GlassInput(query, { query = it.take(80) }, "Найти ответ", LiptonIcons.Search, Modifier.fillMaxWidth())
        val q = query.trim().lowercase(Locale.ROOT)
        val arts = articles
        when {
            loading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = t.a, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }
            arts != null -> {
                val cats = arts.mapNotNull { it.category?.takeIf { s -> s.isNotBlank() } }.distinct()
                    .sortedBy { CATEGORY_ORDER.indexOf(it).let { i -> if (i < 0) 99 else i } }
                if (cats.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                        cats.forEach { cat ->
                            val on = cat == category
                            val sh = RoundedCornerShape(999.dp)
                            Row(
                                Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .glass(sh, highlightHeight = 22.dp)
                                    .border(1.dp, if (on) t.a.copy(alpha = 0.6f) else Color.Transparent, sh)
                                    .clickable(role = Role.Tab) { category = if (on) null else cat },
                                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(categoryIcon(cat), null, tint = c.stateText(AuroraTone.ON), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(cat, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                val shown = arts.filter { a ->
                    (category == null || a.category == category) &&
                        (q.isEmpty() || a.title.lowercase(Locale.ROOT).contains(q) || a.excerpt.orEmpty().lowercase(Locale.ROOT).contains(q))
                }
                Spacer(Modifier.height(16.dp))
                GlassGroup {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(LiptonIcons.Pulse, null, tint = c.stateText(AuroraTone.ON), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(category ?: if (q.isEmpty()) "Популярное" else "Найдено", style = MaterialTheme.typography.labelMedium, color = c.text.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
                        Text(if (category == null && q.isEmpty()) "чаще всего читают" else "${shown.size}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.55f))
                    }
                    if (shown.isEmpty()) {
                        Text("Ничего не нашли — спросите в чате поддержки", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.6f), modifier = Modifier.padding(16.dp))
                    }
                    shown.take(if (category == null && q.isEmpty()) 6 else 50).forEachIndexed { i, a ->
                        if (i > 0) GroupDivider()
                        ArticleRow(a) { onArticle(a.slug) }
                    }
                }
            }
            faq != null -> {
                val items = faq.orEmpty().filter { q.isEmpty() || it.question.lowercase(Locale.ROOT).contains(q) || it.answer.lowercase(Locale.ROOT).contains(q) }
                Spacer(Modifier.height(16.dp))
                GlassGroup {
                    if (items.isEmpty()) Text("Ничего не нашли — спросите в чате поддержки", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.6f), modifier = Modifier.padding(16.dp))
                    items.forEachIndexed { i, f ->
                        if (i > 0) GroupDivider()
                        FaqRow(f, open[f.id] == true) { open[f.id] = open[f.id] != true }
                    }
                }
            }
            else -> GlassCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text(error ?: "Не удалось загрузить базу знаний", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.7f))
            }
        }
        Spacer(Modifier.height(16.dp))
        GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AiAvatar(40.dp, online = true)
                Column {
                    Text("Не нашли ответ?", style = MaterialTheme.typography.titleSmall, color = c.text)
                    Text("Обычно отвечаем за минуту", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("ИИ-помощник ответит сразу, а если нужно — подключится оператор.", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.72f))
            Spacer(Modifier.height(14.dp))
            PrimaryButton("Написать в поддержку", onClick = onSupport, icon = LiptonIcons.Chat, height = 48.dp, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ArticleRow(a: ArticleSummary, onClick: () -> Unit) {
    val c = LiptonTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(categoryIcon(a.category))
        Column(Modifier.weight(1f)) {
            Text(a.title, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(a.category, a.minutes?.takeIf { it > 0 }?.let { "$it мин" }).joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.58f))
        }
        Icon(LiptonIcons.ChevronRight, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun FaqRow(f: FaqEntry, expanded: Boolean, onToggle: () -> Unit) {
    val c = LiptonTheme.colors
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onToggle).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(f.question, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
            Icon(LiptonIcons.ChevronRight, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
        AnimatedVisibility(expanded) {
            Text(f.answer, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp), color = c.text.copy(alpha = 0.75f), modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** Статья базы знаний: HTML из /content/articles/:slug, ссылки открываются в браузере. */
@Composable
fun ArticleScreen(state: UiState, viewModel: MainViewModel, slug: String, onBack: () -> Unit, onOpenSite: (String) -> Unit) {
    val c = LiptonTheme.colors
    val fx = LocalScreenFixtures.current
    var article by remember(slug) { mutableStateOf<ArticleDetail?>(fx?.article) }
    var error by remember(slug) { mutableStateOf<String?>(null) }
    LaunchedEffect(slug) {
        if (fx?.article != null) return@LaunchedEffect
        try { article = viewModel.api.getArticle(slug) } catch (e: Exception) { error = e.message ?: "Не удалось открыть статью" }
    }
    val a = article
    ProfileSubPage(state, a?.title ?: "Статья", onBack) {
        when {
            a != null -> {
                val meta = listOfNotNull(a.category, a.minutes?.takeIf { it > 0 }?.let { "$it мин чтения" }).joinToString(" · ")
                if (meta.isNotBlank()) ToneChip(meta, c.stateTone(AuroraTone.ON).a, icon = categoryIcon(a.category), textColor = c.text.copy(alpha = 0.85f))
                Spacer(Modifier.height(14.dp))
                val html = a.html ?: a.excerpt.orEmpty()
                val textColor = c.text.copy(alpha = 0.86f).toArgb()
                val linkColor = c.stateText(AuroraTone.ON).toArgb()
                GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                movementMethod = LinkMovementMethod.getInstance()
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                                setLineSpacing(0f, 1.25f)
                            }
                        },
                        update = { tv ->
                            tv.setTextColor(textColor)
                            tv.setLinkTextColor(linkColor)
                            tv.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            error != null -> {
                GlassCard(Modifier.fillMaxWidth()) { Text(error ?: "", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.7f)) }
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Открыть на сайте", onClick = { onOpenSite(slug) }, icon = LiptonIcons.External, modifier = Modifier.fillMaxWidth())
            }
            else -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.stateTone(AuroraTone.ON).a, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }
        }
    }
}
