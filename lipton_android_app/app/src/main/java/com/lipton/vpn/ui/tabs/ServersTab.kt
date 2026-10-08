package com.lipton.vpn.ui.tabs

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.UiState
import com.lipton.vpn.data.model.Server
import com.lipton.vpn.data.model.flagEmoji
import com.lipton.vpn.service.LiptonVpnService.VpnStatus
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.FlagCircle
import com.lipton.vpn.ui.components.GlassButton
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.ScreenHorizontalPadding
import com.lipton.vpn.ui.components.ScreenTitle
import com.lipton.vpn.ui.components.SignalBars
import com.lipton.vpn.ui.components.StateTone
import com.lipton.vpn.ui.components.StatusDot
import com.lipton.vpn.ui.components.TabTopBar
import com.lipton.vpn.ui.components.ToneChip
import com.lipton.vpn.ui.components.glass
import com.lipton.vpn.ui.components.pingColor
import com.lipton.vpn.ui.components.pingLevel
import com.lipton.vpn.ui.components.softGlow
import com.lipton.vpn.ui.components.stateText
import com.lipton.vpn.ui.components.stateTone
import com.lipton.vpn.ui.components.tabBarBottomPadding
import com.lipton.vpn.ui.components.topAccentLine
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.TABULAR_NUMS
import kotlinx.coroutines.delay

/**
 * Вкладка «Серверы» по макетам new-combo-servers(-bypass)-play:
 * «Пинг», «Подключено: …», карточка «Рекомендуем · Авто-баланс», список
 * серверов со шкалой пинга. На «Базовом» внизу — предложение «Обход глушилок»;
 * на тарифе «Обход» — сначала серверы «Обход», без предложения.
 */
@Composable
fun ServersTab(
    state: UiState,
    viewModel: MainViewModel,
    activity: ComponentActivity,
    onBuyBypass: () -> Unit,
) {
    val c = LiptonTheme.colors
    val servers = state.subscriptions.flatMap { it.servers }
    val active = servers.find { it.id == state.activeServerId } ?: servers.firstOrNull()
    val onBypassTariff = isBypassTariff(state)
    val auto = servers.firstOrNull { it.isAutoBalance() && !it.isBypassServer() }
    val bypassServers = servers.filter { it.isBypassServer() }
    val regular = servers.filter { it != auto && !it.isBypassServer() }
    val select: (Server) -> Unit = { viewModel.selectServer(activity, it.id) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = ScreenHorizontalPadding),
    ) {
        TabTopBar { SubscriptionCapsule(state) }
        ScreenTitle("Серверы") {
            GlassButton(
                if (state.pinging) "Пинг…" else "Пинг",
                onClick = { viewModel.pingAllServers() },
                icon = LiptonIcons.Pulse,
                enabled = !state.pinging && servers.isNotEmpty(),
            )
        }
        Spacer(Modifier.height(4.dp))
        ConnectedLine(state, active)
        Spacer(Modifier.height(24.dp))

        when {
            servers.isEmpty() -> EmptyServers(state)
            onBypassTariff && bypassServers.isNotEmpty() -> {
                // ── Тариф «Обход»: сначала серверы «Обход» ──
                GroupTitle("Обход глушилок", dotTone = AuroraTone.BYPASS) { BypassBadge("Ваш тариф") }
                Spacer(Modifier.height(12.dp))
                val featured = bypassServers.find { it.id == active?.id } ?: bypassServers.first()
                BypassFeatureCard(featured, selected = featured.id == active?.id, connected = state.status == VpnStatus.CONNECTED) { select(featured) }
                bypassServers.filter { it != featured }.forEachIndexed { i, s ->
                    Spacer(Modifier.height(8.dp))
                    ServerRow(s, selected = s.id == active?.id, hue = c.stateTone(AuroraTone.BYPASS).b, bypassStyle = true, onClick = { select(s) })
                }
                Spacer(Modifier.height(32.dp))
                GroupTitle("Обычные серверы", hint = "без маскировки")
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOfNotNull(auto).plus(regular).forEachIndexed { i, s ->
                        ServerRow(s, selected = s.id == active?.id, hue = rowHue(i, c.isDark), muted = true, onClick = { select(s) })
                    }
                }
                FootNote(LiptonIcons.Info, "При глушилках работают только серверы «Обход»")
            }
            else -> {
                if (auto != null) {
                    AutoBalanceCard(auto, selected = auto.id == active?.id) { select(auto) }
                    Spacer(Modifier.height(32.dp))
                }
                val list = regular + bypassServers
                if (list.isNotEmpty()) {
                    GroupTitle(if (auto != null) "Все серверы" else "Серверы", legend = true)
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        list.forEachIndexed { i, s ->
                            ServerRow(s, selected = s.id == active?.id, hue = rowHue(i, c.isDark), onClick = { select(s) })
                        }
                    }
                }
                if (!onBypassTariff && hasActiveSubscription(state)) {
                    Spacer(Modifier.height(32.dp))
                    GroupTitle("Обход глушилок", hint = "отдельный тариф", dotTone = AuroraTone.BYPASS)
                    Spacer(Modifier.height(12.dp))
                    BypassUpsellCard(state, bypassServers.firstOrNull(), onBuyBypass)
                }
                PingUpdated(state.lastPingAt, state.pinging)
            }
        }
        Spacer(Modifier.height(tabBarBottomPadding()))
    }
}

/** Оттенок свечения строки (как в макете — у каждой строки свой). */
private fun rowHue(i: Int, dark: Boolean): Color {
    val hues = if (dark) listOf(Color(0xFF22D3EE), Color(0xFF3B82F6), Color(0xFFF5B342), Color(0xFFFF8A3D), Color(0xFF8B5CF6))
    else listOf(Color(0xFF0891B2), Color(0xFF2563EB), Color(0xFFB7791F), Color(0xFFD9480F), Color(0xFF7048E8))
    return hues[i % hues.size]
}

@Composable
private fun ConnectedLine(state: UiState, active: Server?) {
    val c = LiptonTheme.colors
    val connected = state.status == VpnStatus.CONNECTED
    val tone = auroraToneFor(state)
    Row(Modifier.height(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusDot(if (connected) c.stateTone(tone).a else c.warn)
        Text(
            buildAnnotatedString {
                append(if (connected) "Подключено: " else "Выбран: ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = c.text)) { append(active?.cleanName() ?: "—") }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = c.text.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GroupTitle(
    title: String,
    hint: String? = null,
    legend: Boolean = false,
    dotTone: AuroraTone? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LiptonTheme.colors
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        if (dotTone != null) {
            StatusDot(c.stateTone(dotTone).a)
            Spacer(Modifier.width(10.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = c.text, maxLines = 1, modifier = Modifier.weight(1f))
        when {
            trailing != null -> trailing()
            legend -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.height(10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                    listOf(4, 7, 10).forEach { h ->
                        Box(Modifier.width(2.dp).height(h.dp).clip(RoundedCornerShape(1.dp)).background(c.text.copy(alpha = 0.6f)))
                    }
                }
                Text("пинг", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f))
            }
            hint != null -> Text(hint, style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.65f), maxLines = 1)
        }
    }
}

/** Карточка «Рекомендуем · Авто-баланс» (140dp): пинг, галочка выбора, иконка-перемешивание. */
@Composable
private fun AutoBalanceCard(server: Server, selected: Boolean, onClick: () -> Unit) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    val shape = RoundedCornerShape(24.dp)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Column(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .graphicsLayer { val s = if (pressed) 0.985f else 1f; scaleX = s; scaleY = s }
            .drawBehind { softGlow(t.a.copy(alpha = if (c.isDark) 0.14f else 0.10f), 24.dp.toPx(), 22.dp.toPx(), offsetY = 16.dp.toPx()) }
            .glass(shape)
            .background(
                Brush.radialGradient(
                    listOf(t.a.copy(alpha = if (c.isDark) 0.30f else 0.18f), Color.Transparent),
                    center = Offset(46f * 3, 94f * 3), radius = 180f * 3,
                ),
            )
            .border(1.dp, t.a.copy(alpha = if (selected) 0.42f else 0.22f), shape)
            .topAccentLine(t)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .padding(20.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
            ToneChip("Рекомендуем", t.a, icon = LiptonIcons.Star, textColor = if (c.isDark) Color(0xFF7CF7C4) else c.accentDeep)
            Spacer(Modifier.weight(1f))
            SignalBars(pingLevel(server.ping), c.pingColor(server.ping), glow = true)
            Spacer(Modifier.width(6.dp))
            Text(
                server.ping?.let { "$it мс" } ?: "—",
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR_NUMS),
                color = c.text,
            )
            Spacer(Modifier.width(12.dp))
            SelectCheck(selected, t.a)
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(t.a.copy(alpha = 0.32f), t.b.copy(alpha = 0.16f))))
                    .border(1.dp, t.a.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LiptonIcons.Shuffle, null, tint = if (c.isDark) Color.White else c.accentDeep, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Авто-баланс",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, letterSpacing = (-0.44).sp),
                    color = c.text, maxLines = 1,
                )
                Text(
                    "Сам выбирает самый быстрый сервер",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.text.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Кружок-галочка выбранного сервера (28dp); не выбран — пустое кольцо. */
@Composable
private fun SelectCheck(selected: Boolean, color: Color, size: Dp = 28.dp) {
    val c = LiptonTheme.colors
    if (selected) {
        Box(
            Modifier
                .size(size)
                .drawBehind {
                    drawCircle(color.copy(alpha = 0.16f), radius = this.size.minDimension / 2 + 4.dp.toPx())
                    softGlow(color.copy(alpha = 0.5f), this.size.minDimension / 2, 8.dp.toPx())
                }
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(LiptonIcons.Check, "Выбран", tint = if (c.isDark) Color(0xFF04140C) else Color.White, modifier = Modifier.size(size * 0.5f))
        }
    } else {
        Box(Modifier.size(size).border(1.5.dp, c.text.copy(alpha = 0.25f), CircleShape))
    }
}

/** Строка сервера 64dp: свечение-полоса слева, круглый флаг, название/город, шкала пинга. */
@Composable
private fun ServerRow(
    server: Server,
    selected: Boolean,
    hue: Color,
    onClick: () -> Unit,
    muted: Boolean = false,
    bypassStyle: Boolean = false,
) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val accent = if (bypassStyle) c.stateTone(AuroraTone.BYPASS).a else c.stateTone(AuroraTone.ON).a
    val (title, sub) = if (bypassStyle) server.cleanName() to countryNameOrNull(server) else server.titleAndSubtitle()
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer { val s = if (pressed) 0.985f else 1f; scaleX = s; scaleY = s; alpha = if (muted && !selected) 0.78f else 1f }
            .glass(shape, highlightHeight = 32.dp)
            .background(Brush.horizontalGradient(0f to hue.copy(alpha = if (c.isDark) 0.10f else 0.07f), 0.4f to Color.Transparent))
            .then(if (selected) Modifier.border(1.dp, accent.copy(alpha = 0.5f), shape) else Modifier)
            .drawBehind {
                // полоса-свечение слева
                val top = 14.dp.toPx()
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, hue, Color.Transparent), startY = top, endY = size.height - top),
                    topLeft = Offset(0f, top), size = Size(2.dp.toPx(), size.height - 2 * top),
                )
            }
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (server.isAutoBalance()) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(c.text.copy(alpha = 0.08f)).border(1.dp, c.text.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(LiptonIcons.Shuffle, null, tint = c.text.copy(alpha = 0.8f), modifier = Modifier.size(15.dp)) }
        } else {
            FlagCircle(server.countryCode(), size = 32.dp, emoji = server.flagEmoji())
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = if (server.isAutoBalance()) "Сам выбирает сервер" else sub
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.65f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.height(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val ping = server.ping
                SignalBars(pingLevel(ping), if (bypassStyle && ping != null) accent else c.pingColor(ping), small = true)
                Text(
                    ping?.let { "$it мс" } ?: "—",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR_NUMS),
                    color = if (ping == null) c.text.copy(alpha = 0.45f) else if (bypassStyle || muted) c.text.copy(alpha = 0.85f) else c.pingColor(ping),
                    maxLines = 1,
                )
            }
            // TODO(redesign): полоса нагрузки сервера — нет данных на бэкенде (B4: «нагрузку в процентах не делаем»).
            if (selected) {
                Text("выбран", style = MaterialTheme.typography.labelMedium, color = accent, maxLines = 1)
            }
        }
    }
}

private fun countryNameOrNull(server: Server): String? =
    com.lipton.vpn.ui.components.countryNameRu(server.countryCode())

/** Большая карточка выбранного сервера «Обход» (тариф «Обход»). */
@Composable
private fun BypassFeatureCard(server: Server, selected: Boolean, connected: Boolean, onClick: () -> Unit) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.BYPASS)
    val shape = RoundedCornerShape(24.dp)
    val (_, city) = server.titleAndSubtitle()
    Column(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .drawBehind { softGlow(t.a.copy(alpha = if (c.isDark) 0.16f else 0.10f), 24.dp.toPx(), 22.dp.toPx(), offsetY = 14.dp.toPx()) }
            .glass(shape)
            .background(Brush.radialGradient(listOf(t.a.copy(alpha = if (c.isDark) 0.28f else 0.16f), Color.Transparent), center = Offset(140f, 280f), radius = 560f))
            .border(1.dp, t.a.copy(alpha = 0.45f), shape)
            .topAccentLine(t)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(20.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
            ToneChip(if (selected && connected) "Обход активен" else "Обход", t.a, icon = LiptonIcons.Bolt, textColor = c.stateText(AuroraTone.BYPASS))
            Spacer(Modifier.weight(1f))
            SignalBars(pingLevel(server.ping), if (server.ping != null) t.a else c.pingColor(null), glow = true)
            Spacer(Modifier.width(6.dp))
            Text(server.ping?.let { "$it мс" } ?: "—", style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR_NUMS), color = c.text)
            Spacer(Modifier.width(12.dp))
            SelectCheck(selected, t.a)
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(t.a.copy(alpha = 0.32f), t.b.copy(alpha = 0.18f))))
                    .border(1.dp, t.a.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(LiptonIcons.ShieldCheck, null, tint = if (c.isDark) Color.White else t.a, modifier = Modifier.size(24.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        server.cleanName(),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, letterSpacing = (-0.44).sp),
                        color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    FlagCircle(server.countryCode(), size = 18.dp, emoji = server.flagEmoji())
                }
                Text(
                    listOfNotNull(city?.takeIf { it.isNotBlank() }, "маскировка трафика").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Предложение «Обход глушилок» для тарифа «Базовый». */
@Composable
private fun BypassUpsellCard(state: UiState, sample: Server?, onBuy: () -> Unit) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.BYPASS)
    val tariff = state.appConfig?.bypassTariff()
    val monthly = tariff?.monthlyPeriod()
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .glass(shape)
            .background(Brush.radialGradient(listOf(Color(0xFF3B82F6).copy(alpha = if (c.isDark) 0.26f else 0.12f), Color.Transparent), center = Offset(110f, 110f), radius = 600f))
            .border(1.dp, Color(0xFF7C9BFF).copy(alpha = 0.30f), shape)
            .topAccentLine(StateTone(Color(0xFF7C9BFF), Color(0xFF22D3EE)))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5B7BFF).copy(alpha = 0.16f))
                    .border(1.dp, Color(0xFF7C9BFF).copy(alpha = 0.42f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(LiptonIcons.Lock, null, tint = c.stateText(AuroraTone.BYPASS), modifier = Modifier.size(18.dp)) }
            Column(Modifier.weight(1f)) {
                Row(Modifier.height(22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        sample?.cleanName() ?: (tariff?.title ?: "Обход глушилок"),
                        style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (sample != null) FlagCircle(sample.countryCode(), size = 16.dp, emoji = sample.flagEmoji())
                }
                Text(
                    "В тарифе «Обход» — когда обычный VPN не работает",
                    style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
            if (monthly != null) {
                Text(
                    rubles(monthly.priceKopeks),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TABULAR_NUMS), color = c.text,
                )
                Text(" / ${pluralDays(monthly.days)}", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.65f))
            }
            Spacer(Modifier.weight(1f))
            val bshape = RoundedCornerShape(999.dp)
            Box(
                Modifier
                    .height(36.dp)
                    .clip(bshape)
                    .background(Brush.verticalGradient(0f to Color.White.copy(alpha = if (c.isDark) 0.12f else 0.5f), 0.5f to Color.Transparent))
                    .background(Color(0xFF5B7BFF).copy(alpha = if (c.isDark) 0.22f else 0.14f))
                    .border(1.dp, Color(0xFF7C9BFF).copy(alpha = 0.5f), bshape)
                    .clickable(role = Role.Button, onClick = onBuy)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Купить тариф", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = if (c.isDark) Color.White else t.a)
            }
        }
    }
}

@Composable
private fun EmptyServers(state: UiState) {
    val c = LiptonTheme.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Серверов пока нет", style = MaterialTheme.typography.titleMedium, color = c.text)
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                state.accountSyncing -> "Загружаем подписку…"
                hasActiveSubscription(state) -> "Подписка активна, но список серверов не загрузился. Проверьте интернет и откройте вкладку ещё раз."
                else -> "Серверы появятся после оформления подписки."
            },
            style = MaterialTheme.typography.bodySmall,
            color = c.text.copy(alpha = 0.72f),
        )
        if (state.accountSyncing) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(color = c.stateTone(AuroraTone.ON).a, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PingUpdated(at: Long?, pinging: Boolean) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(at) {
        while (true) { now = System.currentTimeMillis(); delay(30_000) }
    }
    val text = when {
        pinging -> "Измеряем пинг…"
        at != null -> "Пинг обновлён ${relativeAgo(at, now)}"
        else -> "Нажмите «Пинг», чтобы измерить задержку"
    }
    FootNote(LiptonIcons.Refresh, text)
}

@Composable
private fun FootNote(icon: ImageVector, text: String) {
    val c = LiptonTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.5f))
    }
}
