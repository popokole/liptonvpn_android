package com.lipton.vpn.ui.tabs

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.StatsState
import com.lipton.vpn.UiState
import com.lipton.vpn.data.TrafficLedger
import com.lipton.vpn.data.model.Tariff
import com.lipton.vpn.data.model.flagEmoji
import com.lipton.vpn.service.LiptonVpnService.VpnStatus
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.BannerSlot
import com.lipton.vpn.ui.components.BentoColumn
import com.lipton.vpn.ui.components.BentoRow
import com.lipton.vpn.ui.components.BigValue
import com.lipton.vpn.ui.components.ButtonTone
import com.lipton.vpn.ui.components.FlagCircle
import com.lipton.vpn.ui.components.GhostButton
import com.lipton.vpn.ui.components.GlassButton
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.GlassPillButton
import com.lipton.vpn.ui.components.GradientProgress
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.MiniBars
import com.lipton.vpn.ui.components.PrimaryButton
import com.lipton.vpn.ui.components.ScreenHorizontalPadding
import com.lipton.vpn.ui.components.SignalBars
import com.lipton.vpn.ui.components.Sparkline
import com.lipton.vpn.ui.components.StateTone
import com.lipton.vpn.ui.components.StatusDot
import com.lipton.vpn.ui.components.TabTopBar
import com.lipton.vpn.ui.components.TileHeader
import com.lipton.vpn.ui.components.TileHint
import com.lipton.vpn.ui.components.ToneChip
import com.lipton.vpn.ui.components.ToneCircleIcon
import com.lipton.vpn.ui.components.pingColor
import com.lipton.vpn.ui.components.pingLevel
import com.lipton.vpn.ui.components.accentDeepOrAccent
import com.lipton.vpn.ui.components.softGlow
import com.lipton.vpn.ui.components.stateText
import com.lipton.vpn.ui.components.stateTone
import com.lipton.vpn.ui.components.tabBarBottomPadding
import com.lipton.vpn.ui.components.topAccentLine
import com.lipton.vpn.ui.theme.LiptonText
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.TABULAR_NUMS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

/**
 * Вкладка «Главная» по макетам new-combo-stats/off/nosub:
 * шапка с капсулой срока → слот баннера → статус, таймер сессии, скорость/IP,
 * кнопка-пилюля и чип сервера → бенто-статистика (сайты видят вас, пинг,
 * скорость, тариф, сегодня, защита, неделя). Без подписки — «Подключите защиту»
 * и тарифы из /config.
 */
@Composable
fun HomeTab(
    state: UiState,
    viewModel: MainViewModel,
    activity: ComponentActivity,
    onOpenServers: () -> Unit,
    onPay: (periodId: String?) -> Unit,
    onPromo: () -> Unit,
    statsFlow: StateFlow<StatsState> = viewModel.stats,
    onAuth: (String) -> Unit = {},
    banners: @Composable () -> Unit = {},
) {
    val stats by statsFlow.collectAsState()
    val tone = auroraToneFor(state)
    val guest = state.guest
    val daily = state.dailyTrial
    val noSub = guest == null && daily == null && state.accountStatus != null && !hasActiveSubscription(state)

    LaunchedEffect(Unit) {
        viewModel.loadConfigIfNeeded()
        if (stats.ip == null && !stats.ipLoading) viewModel.refreshIpInfo()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = ScreenHorizontalPadding),
    ) {
        TabTopBar { SubscriptionCapsule(state) }

        // Слот баннера над содержимым главной (обновление приложения; позже — баннеры из админки)
        BannerSlot(Modifier.padding(top = 12.dp)) {
            AnimatedVisibility(
                visible = state.updateInfo != null,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(220)),
            ) {
                val info = state.updateInfo
                if (info != null) {
                    UpdateBanner(
                        version = info.versionName,
                        downloadProgress = state.downloadProgress,
                        downloadedApkPath = state.downloadedApkPath,
                        onDownload = { viewModel.downloadUpdate() },
                        onInstall = { viewModel.installUpdate(activity) },
                        onDismiss = { viewModel.dismissUpdate() },
                    )
                }
            }
            // Баннеры и обновления из админки (GET /app/banners)
            banners()
        }

        val screenH = LocalConfiguration.current.screenHeightDp
        if (guest != null && guest.ended) {
            // Гостевые 15 минут прошли
            GuestEndedHero(state, onCreate = { onAuth("start") }, onLogin = { onAuth("login") })
        } else if (guest != null || daily != null) {
            // Пробный доступ: обычный блок подключения + карточка с отсчётом + статистика сессии
            ConnectHero(
                state = state,
                stats = stats,
                tone = tone,
                onToggle = { viewModel.handleConnectToggle(activity) },
                onOpenServers = onOpenServers,
            )
            Spacer(Modifier.height(32.dp))
            TrialAccessCard(
                session = guest ?: daily!!,
                guest = guest != null,
                onPrimary = { if (guest != null) onAuth("start") else onPay(null) },
                onSecondary = if (guest != null) ({ onAuth("login") }) else null,
            )
            Spacer(Modifier.height(32.dp))
            StatsSection(state = state, stats = stats, tone = tone, onRenew = { onPay(null) }, limited = true)
        } else if (noSub) {
            NoSubHero(
                onSubscribe = { onPay(null) }, onPromo = onPromo,
                trial = if (state.appConfig?.guestEnabled == true) ({ DailyTrialButton(state) { viewModel.startDailyTrial(activity) } }) else null,
            )
            Spacer(Modifier.height(((screenH - 740).coerceIn(24, 88)).dp))
            NoSubTariffs(state, onPay)
        } else {
            ConnectHero(
                state = state,
                stats = stats,
                tone = tone,
                onToggle = { viewModel.handleConnectToggle(activity) },
                onOpenServers = onOpenServers,
            )
            Spacer(Modifier.height(((screenH - 650).coerceIn(32, 136)).dp))
            StatsSection(state = state, stats = stats, tone = tone, onRenew = { onPay(null) })
        }

        Spacer(Modifier.height(tabBarBottomPadding()))
    }
}

// ─── Верх: статус, таймер, скорость / IP, кнопка, сервер ─────────────────────

@Composable
private fun ConnectHero(
    state: UiState,
    stats: StatsState,
    tone: AuroraTone,
    onToggle: () -> Unit,
    onOpenServers: () -> Unit,
) {
    val c = LiptonTheme.colors
    val st = state.status
    val connected = st == VpnStatus.CONNECTED
    val busy = st == VpnStatus.CONNECTING || st == VpnStatus.DISCONNECTING
    val stateTone = c.stateTone(tone)
    val eyebrowColor = c.stateText(tone)

    Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // «● ЗАЩИЩЕНО»
        val eyebrow = when (st) {
            VpnStatus.CONNECTED -> "Защищено"
            VpnStatus.CONNECTING -> "Подключение…"
            VpnStatus.DISCONNECTING -> "Отключение…"
            VpnStatus.ERROR -> "Не удалось подключиться"
            VpnStatus.DISCONNECTED -> "Не защищено"
        }
        Row(Modifier.height(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusDot(stateTone.a)
            AnimatedContent(eyebrow, transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) }, label = "eyebrow") { t ->
                Text(t.uppercase(), style = LiptonText.eyebrow, color = eyebrowColor, maxLines = 1)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Таймер сессии
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(stats.connectedAt) {
            while (stats.connectedAt > 0) {
                now = System.currentTimeMillis()
                delay(1000 - now % 1000)
            }
        }
        val elapsed = if (connected && stats.connectedAt > 0) now - stats.connectedAt else 0L
        Text(
            formatTimer(elapsed),
            style = MaterialTheme.typography.displayLarge.copy(
                shadow = if (c.isDark && connected) Shadow(stateTone.a.copy(alpha = 0.28f), Offset.Zero, 36f) else null,
            ),
            color = if (connected) c.text else c.text.copy(alpha = if (c.isDark) 0.38f else 0.32f),
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(64.dp).semantics { contentDescription = "Время сессии" },
        )
        Spacer(Modifier.height(8.dp))

        // Скорость (подключено) или IP «виден провайдеру»
        Box(Modifier.height(20.dp), contentAlignment = Alignment.Center) {
            if (connected) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val numStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR_NUMS)
                    Icon(LiptonIcons.ArrowDown, null, tint = stateTone.a, modifier = Modifier.size(14.dp))
                    Text(formatMbps(stats.downBps), style = numStyle, color = c.text.copy(alpha = 0.88f))
                    Spacer(Modifier.width(8.dp))
                    Icon(LiptonIcons.ArrowUp, null, tint = stateTone.b, modifier = Modifier.size(14.dp))
                    Text(formatMbps(stats.upBps), style = numStyle, color = c.text.copy(alpha = 0.88f))
                    Spacer(Modifier.width(4.dp))
                    Text("Мбит/с", style = MaterialTheme.typography.bodyMedium, color = c.text.copy(alpha = 0.62f))
                }
            } else if (!busy) {
                val ip = stats.ip?.ip?.takeIf { it.isNotBlank() && !stats.ipViaVpn }
                Text(
                    buildAnnotatedString {
                        if (ip != null) {
                            withStyle(SpanStyle(color = c.text.copy(alpha = 0.55f))) { append("IP ") }
                            withStyle(SpanStyle(color = c.text.copy(alpha = 0.88f))) { append(ip) }
                            withStyle(SpanStyle(color = c.text.copy(alpha = 0.45f))) { append("  ·  ") }
                            withStyle(SpanStyle(color = c.stateText(AuroraTone.OFF))) { append("виден провайдеру") }
                        } else {
                            withStyle(SpanStyle(color = c.stateText(AuroraTone.OFF))) { append("Без VPN адрес виден провайдеру") }
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR_NUMS),
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(32.dp))

        // Кнопка-пилюля
        val btnMod = Modifier.widthIn(min = 200.dp)
        when {
            connected -> GlassPillButton(
                "Отключить", onClick = onToggle, icon = LiptonIcons.Power,
                tone = if (tone == AuroraTone.BYPASS) ButtonTone.BYPASS else ButtonTone.ACCENT, modifier = btnMod,
            )
            busy -> PrimaryButton(
                if (st == VpnStatus.CONNECTING) "Подключение…" else "Отключение…", onClick = {},
                tone = ButtonTone.WARN, loading = true, enabled = false, modifier = btnMod,
            )
            st == VpnStatus.ERROR -> PrimaryButton("Повторить", onClick = onToggle, icon = LiptonIcons.Refresh, tone = ButtonTone.WARN, modifier = btnMod)
            else -> PrimaryButton("Подключить", onClick = onToggle, icon = LiptonIcons.Power, tone = ButtonTone.WARN, modifier = btnMod)
        }
        Spacer(Modifier.height(16.dp))

        ServerChip(state = state, onClick = onOpenServers)
    }
}

/** Чип текущего сервера (флаг, название, ›) → вкладка «Серверы». */
@Composable
private fun ServerChip(state: UiState, onClick: () -> Unit) {
    val c = LiptonTheme.colors
    val servers = state.subscriptions.flatMap { it.servers }
    val active = servers.find { it.id == state.activeServerId } ?: servers.firstOrNull()
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .height(44.dp)
            .clip(shape)
            .background(Brush.verticalGradient(0f to Color.White.copy(alpha = if (c.isDark) 0.08f else 0.4f), 0.5f to Color.Transparent))
            .background(c.pillDarkFill)
            .border(1.dp, if (c.isDark) Color.White.copy(alpha = 0.18f) else c.pillDarkBorder, shape)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 14.dp)
            .semantics { contentDescription = "Сменить сервер" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (active == null) {
            Icon(LiptonIcons.Globe, null, tint = c.text2, modifier = Modifier.size(20.dp))
            Text("Серверов пока нет", style = MaterialTheme.typography.labelLarge, color = c.text)
        } else {
            if (active.isAutoBalance()) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(c.stateTone(AuroraTone.ON).a.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(LiptonIcons.Shuffle, null, tint = c.text, modifier = Modifier.size(13.dp)) }
            } else {
                FlagCircle(active.countryCode(), size = 24.dp, emoji = active.flagEmoji())
            }
            Text(
                active.cleanName(),
                style = MaterialTheme.typography.labelLarge,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 220.dp),
            )
        }
        Icon(LiptonIcons.ChevronRight, null, tint = c.text.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
    }
}

// ─── Бенто-статистика ────────────────────────────────────────────────────────

@Composable
private fun StatsSection(state: UiState, stats: StatsState, tone: AuroraTone, onRenew: () -> Unit, limited: Boolean = false) {
    val c = LiptonTheme.colors
    val connected = state.status == VpnStatus.CONNECTED
    val t = c.stateTone(tone)
    val nowMs = System.currentTimeMillis()
    val week = remember(stats.trafficDays, nowMs / 60_000) { TrafficLedger.week(stats.trafficDays, nowMs) }

    if (limited) {
        // Пробный доступ: только текущая сессия; тариф и трафик — после входа / оплаты.
        SectionTitle("Статистика", "текущая сессия")
        Spacer(Modifier.height(16.dp))
        BentoColumn {
            SeesYouTile(state, stats, connected, t)
            BentoRow(height = 192.dp) {
                PingTile(stats, connected, t, Modifier.weight(1f))
                SpeedTile(stats, connected, t, Modifier.weight(1f))
            }
            if (state.guest != null) AfterLoginTile()
        }
        return
    }

    SectionTitle("Статистика", "сегодня, ${ruDayMonth(nowMs)}")
    Spacer(Modifier.height(16.dp))
    BentoColumn {
        SeesYouTile(state, stats, connected, t)
        BentoRow(height = 192.dp) {
            PingTile(stats, connected, t, Modifier.weight(1f))
            SpeedTile(stats, connected, t, Modifier.weight(1f))
        }
        TariffTile(state, t, onRenew)
        BentoRow(height = 192.dp) {
            TodayTile(week, t, Modifier.weight(1f))
            ProtectionTile(connected, stats, t, tone, Modifier.weight(1f))
        }
        WeekTile(week, t)
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LiptonIcons.Refresh, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "Трафик — на этом устройстве · за 7 дней",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = c.text.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun SectionTitle(title: String, hint: String?) {
    val c = LiptonTheme.colors
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = c.text, modifier = Modifier.weight(1f), maxLines = 1)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.75f), maxLines = 1)
        }
    }
}

private val TilePadding = PaddingValues(16.dp)

/** «Сайты видят вас»: страна и IP глазами сайтов; при VPN — пинг, без VPN — «IP открыт». */
@Composable
private fun SeesYouTile(state: UiState, stats: StatsState, connected: Boolean, t: StateTone) {
    val c = LiptonTheme.colors
    val ip = stats.ip?.takeIf { stats.ipViaVpn == connected }
    val servers = state.subscriptions.flatMap { it.servers }
    val active = servers.find { it.id == state.activeServerId } ?: servers.firstOrNull()
    val code = ip?.countryCode ?: if (connected) active?.countryCode() else null
    val place = listOfNotNull(ip?.country?.takeIf { it.isNotBlank() }, ip?.city?.takeIf { it.isNotBlank() })
        .joinToString(" · ")
        .ifBlank { if (connected) active?.cleanName().orEmpty() else "" }
    GlassCard(
        Modifier.fillMaxWidth().height(112.dp).then(if (connected) Modifier.topAccentLine(t) else Modifier),
        contentPadding = TilePadding,
    ) {
        TileHeader(LiptonIcons.Eye, "Сайты видят вас")
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FlagCircle(code, size = 40.dp, emoji = ip?.flag, ring = t.a)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    when {
                        place.isNotBlank() -> place
                        stats.ipLoading -> "Проверяем…"
                        else -> "Нет данных"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 24.sp),
                    color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = c.text.copy(alpha = 0.55f))) { append("IP  ") }
                        append(ip?.ip?.takeIf { it.isNotBlank() } ?: "—")
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS),
                    color = c.text.copy(alpha = 0.75f), maxLines = 1,
                )
            }
            if (connected) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val ping = stats.pingMs
                    SignalBars(pingLevel(ping), c.pingColor(ping), glow = true)
                    Text(
                        ping?.let { "$it мс" } ?: "—",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, lineHeight = 16.sp, fontFeatureSettings = TABULAR_NUMS),
                        color = c.text, maxLines = 1,
                    )
                }
            } else {
                ToneChip("IP открыт", c.warn, icon = LiptonIcons.Unlock, textColor = c.stateText(AuroraTone.OFF), height = 28.dp)
            }
        }
    }
}

@Composable
private fun PingTile(stats: StatsState, connected: Boolean, t: StateTone, modifier: Modifier) {
    val c = LiptonTheme.colors
    GlassCard(modifier.fillMaxSize(), contentPadding = TilePadding) {
        TileHeader(LiptonIcons.Timer, "Пинг") { TileHint("за час") }
        Spacer(Modifier.height(14.dp))
        val ping = if (connected) stats.pingMs else null
        BigValue(ping?.toString() ?: "—", if (ping != null) "мс" else null, dim = ping == null)
        Spacer(Modifier.height(4.dp))
        val stab = if (connected) pingStability(stats.pingHistory) else null
        Text(
            buildAnnotatedString {
                when {
                    !connected -> append("нет подключения")
                    stab == null -> append("замеряем…")
                    else -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = if (stab.first) t.a else c.stateText(AuroraTone.OFF))) {
                            append(if (stab.first) "стабильно" else "скачет")
                        }
                        append(" · ±${stab.second} мс")
                    }
                }
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR_NUMS),
            color = c.text.copy(alpha = 0.7f), maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        Sparkline(if (connected) stats.pingHistory.takeLast(40) else emptyList(), t.a, Modifier.fillMaxWidth().height(48.dp))
    }
}

@Composable
private fun SpeedTile(stats: StatsState, connected: Boolean, t: StateTone, modifier: Modifier) {
    val c = LiptonTheme.colors
    GlassCard(modifier.fillMaxSize(), contentPadding = TilePadding) {
        TileHeader(LiptonIcons.Gauge, "Скорость") {
            if (connected) StatusDot(t.a) else Box(Modifier.size(7.dp).border(1.dp, c.text.copy(alpha = 0.45f), CircleShape))
        }
        Spacer(Modifier.height(14.dp))
        BigValue(if (connected) formatMbps(stats.downBps) else "—", null, dim = !connected)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().height(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val small = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TABULAR_NUMS)
            Text("Мбит/с", style = small.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.7f))
            if (connected) {
                Icon(LiptonIcons.ArrowDown, null, tint = t.a, modifier = Modifier.padding(start = 4.dp).size(11.dp))
                Spacer(Modifier.weight(1f))
                Icon(LiptonIcons.ArrowUp, null, tint = t.b, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text(formatMbps(stats.upBps), style = small, color = c.text.copy(alpha = 0.7f))
            } else {
                Spacer(Modifier.weight(1f))
                Text("нет данных", style = small.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.55f))
            }
        }
        Spacer(Modifier.weight(1f))
        MiniBars(if (connected) stats.speedHistory else emptyList(), t, Modifier.fillMaxWidth().height(48.dp))
    }
}

/** «Тариф»: название, остаток, «Продлить», полоса «24 из 30 дней». */
@Composable
private fun TariffTile(state: UiState, t: StateTone, onRenew: () -> Unit) {
    val c = LiptonTheme.colors
    val left = daysLeft(state.accountPeriodEnd)
    val total = left?.let { periodTotalDays(it, state.accountTariffCode, state.appConfig) }
    val overlay = state.accountOverlay
    val title = when {
        state.accountStatus == "trial" -> "Пробный период"
        overlay != null && !overlay.tariffTitle.isNullOrBlank() -> overlay.tariffTitle
        else -> tariffTitleFor(state.accountTariffCode, state.appConfig) ?: "Подписка"
    }
    val until = ruDayMonth(state.accountPeriodEnd)
    val sub = buildString {
        if (left != null) append("осталось ${pluralDays(left)}")
        if (until != null) { if (isNotEmpty()) append(" · "); append("до $until") }
        if (isEmpty()) append(if (hasActiveSubscription(state)) "активна" else "загружаем…")
    }
    GlassCard(Modifier.fillMaxWidth().height(128.dp), contentPadding = TilePadding) {
        TileHeader(LiptonIcons.Tag, "Тариф") {
            if (left != null && total != null) TileHint("$left из ${pluralDays(total)}")
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 24.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sub, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS), color = c.text.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            GlassButton("Продлить", onClick = onRenew)
        }
        Spacer(Modifier.height(14.dp))
        GradientProgress(if (left != null && total != null && total > 0) left.toFloat() / total else 0f, t)
    }
}

/** «Сегодня»: трафик за день на этом устройстве, сравнение со средним, приём/отдача. */
@Composable
private fun TodayTile(week: List<com.lipton.vpn.data.DayTraffic>, t: StateTone, modifier: Modifier) {
    val c = LiptonTheme.colors
    val today = week.lastOrNull()
    val pct = percentVsAverage(week)
    val avg = averagePerDay(week.dropLast(1))
    GlassCard(modifier.fillMaxSize(), contentPadding = TilePadding) {
        Box(Modifier.fillMaxWidth()) {
            Column {
                TileHeader(LiptonIcons.ArrowsUpDown, "Сегодня")
                Spacer(Modifier.height(14.dp))
                val (v, u) = formatTraffic(today?.total ?: 0L)
                BigValue(v, u)
                Spacer(Modifier.height(4.dp))
                Text(
                    buildAnnotatedString {
                        if (pct != null) {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = if (pct >= 0) t.a else c.stateText(AuroraTone.OFF))) {
                                append(if (pct >= 0) "+$pct%" else "−${-pct}%")
                            }
                            append(" к среднему")
                        } else append("на этом устройстве")
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR_NUMS),
                    color = c.text.copy(alpha = 0.7f), maxLines = 1,
                )
            }
            // Кольцо «доля от среднего дня»
            val ratio = if (avg > 0 && today != null) (today.total.toFloat() / avg).coerceIn(0f, 1f) else 0f
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .drawBehind {
                        val sw = 3.dp.toPx()
                        drawCircle(c.text.copy(alpha = 0.1f), radius = size.minDimension / 2 - sw / 2, style = Stroke(sw))
                        if (ratio > 0f) {
                            drawArc(
                                Brush.sweepGradient(listOf(t.a, t.b, t.a)), -90f, 360f * ratio, false,
                                topLeft = Offset(sw / 2, sw / 2),
                                size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
                                style = Stroke(sw, cap = StrokeCap.Round),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(LiptonIcons.Pulse, null, tint = t.a, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TrafficLine(LiptonIcons.ArrowDown, t.a, formatTrafficInline(today?.rx ?: 0L), "приём")
            TrafficLine(LiptonIcons.ArrowUp, t.b, formatTrafficInline(today?.tx ?: 0L), "отдача")
        }
    }
}

@Composable
private fun TrafficLine(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, value: String, label: String) {
    val c = LiptonTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToneCircleIcon(icon, color, size = 24.dp, iconSize = 12.dp)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR_NUMS), color = c.text, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f), maxLines = 1)
    }
}

/** «Защита»: при VPN IPv6 и DNS идут через туннель; без VPN — открытый IP/IPv6 и DNS провайдера. */
@Composable
private fun ProtectionTile(connected: Boolean, stats: StatsState, t: StateTone, tone: AuroraTone, modifier: Modifier) {
    val c = LiptonTheme.colors
    val warn = c.warn
    val items: List<Pair<String, Boolean>> = if (connected) {
        listOf("IPv6 закрыт" to true, "DNS через VPN" to true)
    } else {
        listOf((if (stats.ipv6Available) "IPv6 открыт" else "IP открыт") to false, "DNS провайдера" to false)
    }
    val leaks = items.count { !it.second }
    GlassCard(modifier.fillMaxSize(), contentPadding = TilePadding) {
        TileHeader(if (connected) LiptonIcons.ShieldCheck else LiptonIcons.ShieldOff, "Защита") {
            StatusDot(if (connected) t.a else warn)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (connected) "Активна" else "Не активна",
            style = MaterialTheme.typography.titleLarge.copy(lineHeight = 32.sp, letterSpacing = (-0.4).sp),
            color = c.text, maxLines = 1, modifier = Modifier.height(32.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            buildAnnotatedString {
                if (leaks == 0) append("Утечек нет")
                else {
                    append("Найдено ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = c.stateText(AuroraTone.OFF))) {
                        append("$leaks ${pluralRu(leaks, "утечка", "утечки", "утечек")}")
                    }
                }
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = c.text.copy(alpha = 0.7f), maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEachIndexed { i, (label, ok) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToneCircleIcon(
                        if (ok) LiptonIcons.Check else LiptonIcons.Exclaim,
                        if (ok) (if (i == 0) t.a else t.b) else warn,
                        size = 24.dp, iconSize = 11.dp,
                    )
                    Text(label, style = MaterialTheme.typography.bodySmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** «За неделю»: сумма, среднее (пунктир), столбики по дням, сегодня — ярким со значением. */
@Composable
private fun WeekTile(week: List<com.lipton.vpn.data.DayTraffic>, t: StateTone) {
    val c = LiptonTheme.colors
    val total = week.sumOf { it.total }
    val avg = averagePerDay(week)
    GlassCard(Modifier.fillMaxWidth().height(280.dp), contentPadding = TilePadding) {
        TileHeader(LiptonIcons.BarChart, "За неделю") { TileHint(weekRangeLabel(week)) }
        Spacer(Modifier.height(14.dp))
        val (v, u) = formatTraffic(total)
        BigValue(v, u)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.height(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.width(16.dp).height(1.dp).drawBehind {
                    drawLine(
                        c.text.copy(alpha = 0.6f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())),
                    )
                },
            )
            Text(
                if (avg > 0) "в среднем ${formatTrafficInline(avg)} в день" else "трафик появится после подключения",
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS),
                color = c.text.copy(alpha = 0.72f), maxLines = 1,
            )
        }
        Spacer(Modifier.height(26.dp))
        WeekBars(week, avg, t, Modifier.fillMaxWidth().height(112.dp))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(16.dp)) {
            week.forEachIndexed { i, d ->
                val today = i == week.lastIndex
                Text(
                    weekdayShort(d.day),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (today) FontWeight.Bold else FontWeight.Medium),
                    color = if (today) c.text else c.text.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WeekBars(week: List<com.lipton.vpn.data.DayTraffic>, avg: Long, t: StateTone, modifier: Modifier) {
    val c = LiptonTheme.colors
    val max = (week.maxOfOrNull { it.total } ?: 0L).coerceAtLeast(1L)
    val labelSpace = 22.dp
    BoxWithConstraints(modifier) {
        val barArea = maxHeight - labelSpace
        // Пунктир среднего
        if (avg > 0) {
            val y = barArea * (1f - avg.toFloat() / max) + labelSpace
            Box(
                Modifier.fillMaxWidth().padding(top = y).height(1.dp).drawBehind {
                    drawLine(
                        c.text.copy(alpha = 0.32f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    )
                },
            )
        }
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
            week.forEachIndexed { i, d ->
                val today = i == week.lastIndex
                val h: Dp = if (d.total <= 0) 4.dp else (barArea * (d.total.toFloat() / max)).coerceAtLeast(6.dp)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (today && d.total > 0) {
                        Text(
                            formatTraffic(d.total).first,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR_NUMS),
                            color = c.text, maxLines = 1,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Box(
                        Modifier
                            .width(28.dp)
                            .height(h)
                            .then(if (today && d.total > 0) Modifier.drawBehind { softGlow(t.a.copy(alpha = 0.45f), 8.dp.toPx(), 9.dp.toPx()) } else Modifier)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(
                                if (today) Brush.verticalGradient(listOf(t.a, t.b))
                                else Brush.verticalGradient(listOf(t.a.copy(alpha = 0.35f), t.a.copy(alpha = 0.35f)))
                            ),
                    )
                }
            }
        }
    }
}

// ─── Нет подписки: «Подключите защиту», тарифы, что входит ───────────────────

@Composable
private fun NoSubHero(onSubscribe: () -> Unit, onPromo: () -> Unit, trial: (@Composable () -> Unit)? = null) {
    val c = LiptonTheme.colors
    val warn = c.stateTone(AuroraTone.NO_SUB)
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.height(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusDot(warn.a)
            Text("НЕ ЗАЩИЩЕНО", style = LiptonText.eyebrow, color = c.stateText(AuroraTone.NO_SUB))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Подключите\nзащиту",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
                fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 42.sp,
                shadow = if (c.isDark) Shadow(warn.a.copy(alpha = 0.3f), Offset.Zero, 40f) else null,
            ),
            color = c.text,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "до 5 устройств · без лимита трафика",
            style = MaterialTheme.typography.bodyLarge,
            color = c.text.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        PrimaryButton("Оформить подписку", onClick = onSubscribe, tone = ButtonTone.WARN, modifier = Modifier.widthIn(min = 248.dp), trailingIcon = LiptonIcons.ArrowRight)
        if (trial != null) {
            Spacer(Modifier.height(16.dp))
            trial()
        }
        Spacer(Modifier.height(12.dp))
        GhostButton("Ввести промокод", onClick = onPromo, icon = LiptonIcons.Tag)
    }
}

@Composable
private fun NoSubTariffs(state: UiState, onPay: (String?) -> Unit) {
    val c = LiptonTheme.colors
    val cfg = state.appConfig
    val base = cfg?.baseTariff()
    val bypass = cfg?.bypassTariff()
    val warnTone = c.stateTone(AuroraTone.NO_SUB)

    if (base != null || bypass != null) {
        SectionTitle("Тарифы", "до 5 устройств в каждом")
        Spacer(Modifier.height(16.dp))
        BentoColumn {
            if (base != null) BaseTariffCard(base, warnTone, onPay)
            if (bypass != null) BypassTariffCard(bypass, onPay)
        }
        Spacer(Modifier.height(32.dp))
    } else if (cfg == null) {
        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = warnTone.a, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        }
    }

    SectionTitle("Что входит", "в любой тариф")
    Spacer(Modifier.height(16.dp))
    BentoRow(height = 136.dp) {
        IncludedTile(LiptonIcons.Monitor, "5 устройств", "на одной подписке", warnTone, Modifier.weight(1f))
        IncludedTile(LiptonIcons.Infinity, "Безлимит", "без лимита трафика", warnTone, Modifier.weight(1f))
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LiptonIcons.Lock, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(6.dp))
        Text("Безопасная оплата через ЮKassa", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.5f))
    }
}

@Composable
private fun BaseTariffCard(t: Tariff, tone: StateTone, onPay: (String?) -> Unit) {
    val c = LiptonTheme.colors
    val monthly = t.monthlyPeriod()
    val others = t.periods.filter { it.id != monthly?.id }.sortedBy { it.days }.take(2)
    GlassCard(Modifier.fillMaxWidth().topAccentLine(StateTone(tone.a, Color(0xFFFFC08A))), contentPadding = TilePadding) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TileHeader(LiptonIcons.ShieldCheck, t.title.ifBlank { "Базовый" })
                PriceLine(monthly?.priceKopeks ?: t.priceKopeks, monthly?.days ?: t.periodDays)
            }
            SmallPrimary("Купить", ButtonTone.WARN) { onPay(monthly?.id) }
        }
        if (others.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.text.copy(alpha = 0.10f)))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = c.text.copy(alpha = 0.6f))) { append("Другие сроки: ") }
                        others.forEachIndexed { i, p ->
                            if (i > 0) append(" ·\n")
                            append("${periodLabel(p.days)} — ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.text)) { append(rubles(p.priceKopeks)) }
                            // Выгоду показываем у самого длинного срока (как в макете — «год … (выгода 38%)»)
                            if (i == others.lastIndex) t.savingPercent(p.days, p.priceKopeks)?.let { s ->
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = if (c.isDark) Color(0xFFFFC08A) else c.warnSoft)) { append(" (выгода $s%)") }
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS),
                    color = c.text.copy(alpha = 0.72f),
                    modifier = Modifier.weight(1f),
                )
                GlassButton("Выбрать", onClick = { onPay(others.first().id) })
            }
        }
    }
}

@Composable
private fun BypassTariffCard(t: Tariff, onPay: (String?) -> Unit) {
    val c = LiptonTheme.colors
    val monthly = t.monthlyPeriod()
    GlassCard(Modifier.fillMaxWidth(), contentPadding = TilePadding) {
        TileHeader(LiptonIcons.Bolt, t.title.ifBlank { "Обход глушилок" }) { BypassBadge("Усиленный") }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                PriceLine(monthly?.priceKopeks ?: t.priceKopeks, monthly?.days ?: t.periodDays)
                Text("когда обычный VPN не работает", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SmallPrimary("Купить", ButtonTone.BYPASS) { onPay(monthly?.id) }
        }
    }
}

/** Бейдж «Усиленный» / «Ваш тариф» с градиентной рамкой синий → фиолетовый. */
@Composable
fun BypassBadge(text: String, modifier: Modifier = Modifier) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier
            .height(20.dp)
            .clip(shape)
            .background(if (c.isDark) Color(0xEB0C0E1A) else Color(0xF2FFFFFF))
            .border(1.dp, Brush.horizontalGradient(listOf(Color(0xFF3B6BFF), Color(0xFF8B5CF6))), shape)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF3B6BFF), Color(0xFF8B5CF6)))))
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (c.isDark) Color(0xFFC8D1FF) else Color(0xFF3B5BFF), maxLines = 1)
    }
}

@Composable
private fun PriceLine(kopeks: Long, days: Int) {
    val perMonth = days in 27..33
    BigValue(rubles(kopeks).removeSuffix(" ₽"), if (perMonth) "₽ / мес" else "₽ / ${periodLabel(days)}")
}

@Composable
private fun SmallPrimary(text: String, tone: ButtonTone, onClick: () -> Unit) {
    PrimaryButton(text, onClick = onClick, tone = tone, height = 40.dp)
}

@Composable
private fun IncludedTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, tone: StateTone, modifier: Modifier) {
    val c = LiptonTheme.colors
    GlassCard(modifier.fillMaxSize(), contentPadding = TilePadding) {
        ToneCircleIcon(icon, tone.a, size = 40.dp, iconSize = 18.dp, glow = true)
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 24.sp), color = c.text, maxLines = 1)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.72f), maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

// ─── Баннер обновления приложения (в слоте баннеров) ─────────────────────────

@Composable
private fun UpdateBanner(
    version: String,
    downloadProgress: Int?,
    downloadedApkPath: String?,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LiptonTheme.colors
    val accent = c.accentDeepOrAccent()
    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToneCircleIcon(LiptonIcons.ArrowUp, accent, size = 32.dp, iconSize = 14.dp)
            Column(Modifier.weight(1f)) {
                Text("Доступно обновление v$version", style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp), color = c.text)
                Text(
                    when {
                        downloadedApkPath != null -> "Готово к установке"
                        downloadProgress != null -> "Скачивание $downloadProgress%…"
                        else -> "Нажмите, чтобы скачать"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (downloadedApkPath != null) accent else c.text3,
                )
            }
            when {
                downloadedApkPath != null -> GlassButton("Установить", onClick = onInstall, height = 32.dp)
                downloadProgress != null -> CircularProgressIndicator(Modifier.size(22.dp), color = accent, strokeWidth = 2.dp)
                else -> GlassButton("Скачать", onClick = onDownload, height = 32.dp)
            }
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(enabled = downloadProgress == null, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LiptonIcons.Close, "Скрыть", tint = c.text3, modifier = Modifier.size(14.dp))
            }
        }
        if (downloadProgress != null && downloadedApkPath == null) {
            LinearProgressIndicator(
                progress = { downloadProgress / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.15f),
            )
        }
    }
}
