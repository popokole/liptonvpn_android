package com.lipton.vpn.ui.tabs

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.NewsState
import com.lipton.vpn.UiState
import com.lipton.vpn.data.model.NewsItem
import com.lipton.vpn.data.model.ServerStatus
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.FlagCircle
import com.lipton.vpn.ui.components.GhostButton
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.ScreenHorizontalPadding
import com.lipton.vpn.ui.components.ScreenTitle
import com.lipton.vpn.ui.components.StatusDot
import com.lipton.vpn.ui.components.TabTopBar
import com.lipton.vpn.ui.components.TileHeader
import com.lipton.vpn.ui.components.TileHint
import com.lipton.vpn.ui.components.glass
import com.lipton.vpn.ui.components.stateText
import com.lipton.vpn.ui.components.stateTone
import com.lipton.vpn.ui.components.tabBarBottomPadding
import com.lipton.vpn.ui.components.topAccentLine
import com.lipton.vpn.ui.theme.LiptonTheme
import kotlinx.coroutines.flow.StateFlow

/** Свежая новость (за 14 дней) без отметки «прочитано» — для точки и счётчика. */
fun NewsState.isUnread(item: NewsItem, nowMs: Long = System.currentTimeMillis()): Boolean {
    if (item.id.isBlank() || item.id in readIds) return false
    val at = item.publishedAt?.let { parseRfc3339(it) } ?: return false
    return nowMs - at <= 14L * 86_400_000L
}

fun NewsState.unreadCount(nowMs: Long = System.currentTimeMillis()): Int = items.count { isUnread(it, nowMs) }

/**
 * Вкладка «Новости» по макету new-combo-news-play: «Прочитать все» и счётчик
 * непрочитанных, плитка «Статус серверов» (GET /status/servers), лента (GET /news)
 * с датой, точкой «не прочитано» и «Подробнее».
 * TODO(redesign): B3 — теги «Обновление / Тариф / Совет» и анонсы продукта
 * (/announcements с published_at и tag); пока лента без тегов.
 */
@Composable
fun NewsTab(
    state: UiState,
    viewModel: MainViewModel,
    onOpenStatus: () -> Unit,
    newsFlow: StateFlow<NewsState> = viewModel.news,
) {
    val c = LiptonTheme.colors
    val news by newsFlow.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadNews() }
    val unread = news.unreadCount()
    val tone = AuroraTone.ON

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = ScreenHorizontalPadding),
    ) {
        TabTopBar { SubscriptionCapsule(state) }
        ScreenTitle("Новости") {
            if (unread > 0) {
                GhostButton(
                    "Прочитать все",
                    onClick = { viewModel.markAllNewsRead() },
                    icon = LiptonIcons.CheckDouble,
                    color = c.stateText(tone),
                    modifier = Modifier.padding(end = 0.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.height(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusDot(if (unread > 0) c.stateTone(tone).a else c.text.copy(alpha = 0.35f), glow = unread > 0)
            Text(
                buildAnnotatedString {
                    if (unread > 0) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = c.text)) { append("$unread") }
                        append(" ${pluralRu(unread, "непрочитанная", "непрочитанных", "непрочитанных")}")
                    } else append("Всё прочитано")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = c.text.copy(alpha = 0.7f),
            )
        }
        Spacer(Modifier.height(24.dp))

        ServerStatusCard(news, onOpenStatus)

        Spacer(Modifier.height(32.dp))
        val dates = news.items.mapNotNull { it.publishedAt?.let { d -> parseRfc3339(d) } }
        Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Лента", style = MaterialTheme.typography.titleLarge, color = c.text, modifier = Modifier.weight(1f))
            monthRangeLabel(dates.minOrNull(), dates.maxOrNull())?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.75f))
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            news.items.isEmpty() && news.loading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.stateTone(tone).a, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }
            news.items.isEmpty() -> GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    news.error ?: "Пока новостей нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.text.copy(alpha = 0.72f),
                )
                if (news.error != null) {
                    Spacer(Modifier.height(8.dp))
                    GhostButton("Повторить", onClick = { viewModel.loadNews(force = true) }, icon = LiptonIcons.Refresh)
                }
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                news.items.forEach { item ->
                    NewsCard(item, unread = news.isUnread(item), onRead = { viewModel.markNewsRead(item.id) })
                }
            }
        }

        if (news.items.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(LiptonIcons.Check, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text("Это все новости", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.5f))
            }
        }
        Spacer(Modifier.height(tabBarBottomPadding()))
    }
}

/** «Статус серверов»: общий итог и линия узлов (точка статуса + флаг). */
@Composable
private fun ServerStatusCard(news: NewsState, onOpen: () -> Unit) {
    val c = LiptonTheme.colors
    val status = news.status
    val servers = status?.servers.orEmpty()
    val down = servers.count { it.status == "down" }
    val ok = servers.isNotEmpty() && down == 0
    val good = c.stateTone(AuroraTone.ON)
    val bad = c.stateTone(AuroraTone.OFF)
    val head = if (ok || servers.isEmpty()) good else bad
    GlassCard(
        Modifier
            .fillMaxWidth()
            .topAccentLine(head)
            .clip(RoundedCornerShape(24.dp))
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(head.a.copy(alpha = if (c.isDark) 0.16f else 0.10f), Color.Transparent), center = Offset(30.dp.toPx(), 64.dp.toPx()), radius = 200.dp.toPx()))
            },
        onClick = onOpen,
    ) {
        TileHeader(LiptonIcons.Pulse, "Статус серверов") {
            if (news.statusAt > 0) TileHint("обновлено ${relativeAgo(news.statusAt)}")
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .padding(start = 2.dp)
                    .size(10.dp)
                    .drawBehind {
                        drawCircle(head.a.copy(alpha = 0.06f), radius = size.minDimension / 2 + 8.dp.toPx())
                        drawCircle(head.a.copy(alpha = 0.16f), radius = size.minDimension / 2 + 4.dp.toPx())
                    }
                    .clip(CircleShape)
                    .background(head.a),
            )
            Text(
                when {
                    servers.isEmpty() -> if (news.loading) "Проверяем серверы…" else "Нет данных о серверах"
                    ok -> "Все серверы работают"
                    down == servers.size -> "Серверы недоступны"
                    else -> "Не работает: $down из ${servers.size}"
                },
                style = MaterialTheme.typography.titleLarge,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(c.text.copy(alpha = 0.06f))
                    .border(1.dp, c.text.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(LiptonIcons.ChevronRight, "Подробнее", tint = c.text.copy(alpha = 0.8f), modifier = Modifier.size(14.dp)) }
        }
        if (servers.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            StatusLine(servers.take(6))
        }
    }
}

@Composable
private fun StatusLine(servers: List<ServerStatus>) {
    val c = LiptonTheme.colors
    val good = c.stateTone(AuroraTone.ON)
    val warn = c.stateTone(AuroraTone.OFF).a
    Box(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .semantics { contentDescription = "Серверы: " + servers.joinToString { "${it.name} — ${statusRu(it.status)}" } },
    ) {
        // соединительная линия между узлами
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .padding(top = 3.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Brush.horizontalGradient(listOf(good.a.copy(alpha = 0.45f), good.b.copy(alpha = 0.35f)))),
        )
        Row(Modifier.fillMaxWidth()) {
            servers.forEach { s ->
                val color = when (s.status) {
                    "up" -> good.a
                    "down" -> warn
                    else -> c.text.copy(alpha = 0.35f)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    StatusDot(color, size = 8.dp, glow = s.status == "up")
                    Spacer(Modifier.height(8.dp))
                    val isBalance = s.name.contains("баланс", ignoreCase = true) || s.name.contains("balance", ignoreCase = true)
                    if (isBalance || s.country.isNullOrBlank()) {
                        Box(
                            Modifier.size(20.dp).clip(CircleShape).background(Brush.linearGradient(listOf(good.a, good.b))),
                            contentAlignment = Alignment.Center,
                        ) { Icon(LiptonIcons.Shuffle, null, tint = Color.White, modifier = Modifier.size(11.dp)) }
                    } else {
                        FlagCircle(s.country, size = 20.dp)
                    }
                }
            }
        }
    }
}

private fun statusRu(s: String): String = when (s) {
    "up" -> "работает"
    "down" -> "не работает"
    else -> "нет данных"
}

/** Карточка новости: дата, точка «не прочитано», заголовок, кратко, «Подробнее» (раскрывает текст и отмечает прочитанной). */
@Composable
private fun NewsCard(item: NewsItem, unread: Boolean, onRead: () -> Unit) {
    val c = LiptonTheme.colors
    val accent = c.stateTone(AuroraTone.ON).a
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    val date = ruDayMonth(item.publishedAt)
    Column(
        Modifier
            .fillMaxWidth()
            .glass(shape)
            .border(1.dp, if (unread) accent.copy(alpha = 0.24f) else Color.Transparent, shape)
            .clickable(role = Role.Button) { expanded = !expanded; onRead() }
            .animateContentSize(tween(260))
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (date != null) {
                Text(date, style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.65f))
            }
            item.sourceName?.takeIf { it.isNotBlank() }?.let { src ->
                Text("· $src", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
            Spacer(Modifier.weight(1f))
            if (unread) StatusDot(accent, size = 8.dp)
        }
        Spacer(Modifier.height(12.dp))
        Text(item.title, style = MaterialTheme.typography.titleMedium, color = c.text)
        if (item.body.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                item.body,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                color = c.text.copy(alpha = 0.72f),
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            Modifier.padding(top = 6.dp).height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(if (expanded) "Свернуть" else "Подробнее", style = MaterialTheme.typography.labelLarge, color = c.stateText(AuroraTone.ON))
            Icon(
                if (expanded) LiptonIcons.ChevronLeft else LiptonIcons.ChevronRight, null,
                tint = c.stateText(AuroraTone.ON), modifier = Modifier.size(14.dp),
            )
        }
    }
}
