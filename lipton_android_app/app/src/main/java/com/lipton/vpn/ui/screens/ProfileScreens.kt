package com.lipton.vpn.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.UiState
import com.lipton.vpn.data.model.TxItem
import com.lipton.vpn.service.LiptonVpnService.VpnStatus
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.BypassDomainsScreen
import com.lipton.vpn.ui.components.ConfirmDialog
import com.lipton.vpn.ui.components.FlagCircle
import com.lipton.vpn.ui.components.GhostButton
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.GlassGroup
import com.lipton.vpn.ui.components.GroupDivider
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.LiptonSwitch
import com.lipton.vpn.ui.components.LogsScreen
import com.lipton.vpn.ui.components.PrimaryButton
import com.lipton.vpn.ui.components.ScreenHorizontalPadding
import com.lipton.vpn.ui.components.ToneChip
import com.lipton.vpn.ui.components.ToneCircleIcon
import com.lipton.vpn.ui.components.glass
import com.lipton.vpn.ui.components.stateText
import com.lipton.vpn.ui.components.stateTone
import com.lipton.vpn.ui.tabs.parseRfc3339
import com.lipton.vpn.ui.tabs.pluralRu
import com.lipton.vpn.ui.tabs.rubles
import com.lipton.vpn.ui.tabs.ruDayMonth
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.TABULAR_NUMS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  Подэкраны профиля (минимальные, в стиле редизайна). Подробные макеты
//  new-scr-* (домены, логи, платежи, способ оплаты, промокод) — пакет A4 ч.2.
// ─────────────────────────────────────────────────────────────────────────────

/** Каркас подэкрана: «‹» в стеклянном круге, заголовок 22/700, прокрутка, отступы под бары. */
@Composable
fun SubPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scroll: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LiptonTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ScreenHorizontalPadding).padding(top = 8.dp).height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .glass(CircleShape, highlightHeight = 20.dp)
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = "Назад" },
                contentAlignment = Alignment.Center,
            ) { Icon(LiptonIcons.ChevronLeft, null, tint = c.text, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize * 1.1f), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            trailing()
        }
        Column(
            Modifier
                .fillMaxSize()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = ScreenHorizontalPadding)
                .padding(top = 16.dp, bottom = 24.dp),
            content = content,
        )
    }
}

// ─── Свои домены и логи — прежние экраны в новом каркасе ──────────────────────

/** «Свои домены для обхода». TODO(redesign): перерисовать по new-scr-domains-play (A4 ч.2). */
@Composable
fun DomainsRoute(state: UiState, viewModel: MainViewModel, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
        BypassDomainsScreen(
            domains = state.bypassDomains,
            onAdd = { viewModel.addBypassDomain(it) },
            onRemove = { viewModel.removeBypassDomain(it) },
            onBack = onBack,
        )
    }
}

/** «Логи приложения». TODO(redesign): перерисовать по new-scr-logs-play (A4 ч.2). */
@Composable
fun LogsRoute(state: UiState, viewModel: MainViewModel, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
        LogsScreen(logLines = state.logLines, onClear = { viewModel.clearLogs() }, onBack = onBack)
    }
}

// ─── Раздельное туннелирование ───────────────────────────────────────────────

private data class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap?)

/**
 * Раздельное туннелирование: отмеченные приложения идут мимо VPN (режим
 * «все, кроме выбранных», VpnService.Builder.addDisallowedApplication).
 * Список — приложения с иконкой на рабочем столе (<queries> на LAUNCHER).
 * Изменения применяются при выходе с экрана: если VPN включён, он переподключится.
 */
@Composable
fun SplitTunnelScreen(state: UiState, viewModel: MainViewModel, activity: ComponentActivity, onBack: () -> Unit) {
    val c = LiptonTheme.colors
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var selected by remember { mutableStateOf(state.splitTunnelApps.toSet()) }
    var query by remember { mutableStateOf("") }
    val initial = remember { state.splitTunnelApps.toSet() }
    val latest by rememberUpdatedState(selected)

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                .map { it.activityInfo.applicationInfo }
                .distinctBy { it.packageName }
                .filter { it.packageName != ctx.packageName }
                .map { ai ->
                    val icon = try { pm.getApplicationIcon(ai).toBitmap(96, 96).asImageBitmap() } catch (_: Exception) { null }
                    AppEntry(ai.packageName, pm.getApplicationLabel(ai).toString(), icon)
                }
                .sortedBy { it.label.lowercase(Locale.getDefault()) }
        }
    }
    // Применяем при уходе с экрана, только если список изменился.
    DisposableEffect(Unit) {
        onDispose { if (latest != initial) viewModel.setSplitTunnelApps(latest.toList(), activity) }
    }

    SubPage("Раздельное туннелирование", onBack, scroll = false) {
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ToneCircleIcon(LiptonIcons.Apps, c.stateTone(AuroraTone.ON).a, size = 40.dp, iconSize = 18.dp)
                Column(Modifier.weight(1f)) {
                    Text("Все, кроме выбранных", style = MaterialTheme.typography.titleSmall, color = c.text)
                    Text(
                        "Отмеченные приложения пойдут напрямую, мимо VPN. Остальные — через VPN.",
                        style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.72f),
                    )
                }
            }
            if (state.status == VpnStatus.CONNECTED) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Изменения применятся, когда вы вернётесь назад: VPN переподключится.",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = c.stateText(AuroraTone.OFF),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SearchField(query, { query = it }, "Найти приложение")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (selected.isEmpty()) "Все приложения через VPN" else "${selected.size} ${pluralRu(selected.size, "приложение", "приложения", "приложений")} мимо VPN",
                style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.7f), modifier = Modifier.weight(1f),
            )
            if (selected.isNotEmpty()) GhostButton("Сбросить", onClick = { selected = emptySet() })
        }
        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.stateTone(AuroraTone.ON).a, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }
        } else {
            val q = query.trim().lowercase(Locale.getDefault())
            val shown = list.filter { q.isEmpty() || it.label.lowercase(Locale.getDefault()).contains(q) || it.pkg.contains(q) }
                .sortedByDescending { it.pkg in selected }
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)).glass(RoundedCornerShape(24.dp)),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(shown, key = { it.pkg }) { app ->
                    val on = app.pkg in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable(role = Role.Switch) { selected = if (on) selected - app.pkg else selected + app.pkg }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (app.icon != null) Image(app.icon, null, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                        else Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(c.text.copy(alpha = 0.08f)))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (on) "мимо VPN" else "через VPN", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = if (on) c.stateText(AuroraTone.OFF) else c.text.copy(alpha = 0.55f))
                        }
                        LiptonSwitch(checked = on)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .glass(shape, highlightHeight = 24.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(LiptonIcons.Search, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = c.text.copy(alpha = 0.45f))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.text),
                cursorBrush = SolidColor(c.stateTone(AuroraTone.ON).a),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─── Проверка соединения ─────────────────────────────────────────────────────

/** Что видят сайты, ответ Cloudflare, IPv6, DNS и пинг до сервера. */
@Composable
fun ConnectionCheckScreen(state: UiState, viewModel: MainViewModel, onBack: () -> Unit) {
    val c = LiptonTheme.colors
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<MainViewModel.ConnCheckResult?>(null) }
    val run: () -> Unit = {
        if (!running) {
            running = true
            scope.launch {
                result = try { viewModel.runConnectionCheck() } catch (_: Exception) { null }
                running = false
            }
        }
    }
    LaunchedEffect(state.status) { if (state.status == VpnStatus.CONNECTED || state.status == VpnStatus.DISCONNECTED) run() }

    SubPage("Проверка соединения", onBack) {
        val r = result
        val connected = state.status == VpnStatus.CONNECTED
        val good = c.stateTone(AuroraTone.ON).a
        val warn = c.warn
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ToneCircleIcon(if (connected) LiptonIcons.ShieldCheck else LiptonIcons.ShieldOff, if (connected) good else warn, size = 44.dp, iconSize = 20.dp, glow = true)
                Column(Modifier.weight(1f)) {
                    Text(if (connected) "VPN подключён" else "VPN выключен", style = MaterialTheme.typography.titleMedium, color = c.text)
                    Text(
                        if (connected) "Проверяем, что трафик идёт через сервер ${r?.serverName ?: ""}".trim()
                        else "Без VPN сайты и провайдер видят ваш настоящий адрес",
                        style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.72f),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassGroup {
            val ip = r?.ip
            CheckRow(
                LiptonIcons.Eye, "Сайты видят",
                when {
                    running && r == null -> "проверяем…"
                    ip == null -> "нет ответа"
                    else -> listOfNotNull(ip.ip, listOfNotNull(ip.country, ip.city).filter { it.isNotBlank() }.joinToString(", ").ifBlank { null }).joinToString(" · ")
                },
                ok = if (r == null) null else connected && ip != null,
                leading = ip?.countryCode?.let { code -> { FlagCircle(code, size = 32.dp, emoji = ip.flag) } },
            )
            GroupDivider()
            val trace = r?.trace.orEmpty()
            CheckRow(
                LiptonIcons.Globe, "Ответ Cloudflare",
                when {
                    running && r == null -> "проверяем…"
                    trace.isEmpty() -> "нет ответа"
                    else -> listOfNotNull(trace["loc"]?.let { "страна $it" }, trace["ip"]).joinToString(" · ")
                },
                ok = if (r == null) null else connected && trace.isNotEmpty(),
            )
            GroupDivider()
            CheckRow(
                LiptonIcons.Shield, "IPv6",
                when {
                    connected -> "закрыт — весь IPv6 идёт в туннель"
                    r?.ipv6Public == true -> "открыт — адрес IPv6 виден сайтам"
                    else -> "в этой сети IPv6 нет"
                },
                ok = connected || r?.ipv6Public == false,
            )
            GroupDivider()
            CheckRow(
                LiptonIcons.Lock, "DNS",
                if (connected) "через VPN — запросы не видны провайдеру" else "DNS провайдера — он видит, какие сайты вы открываете",
                ok = connected,
            )
            GroupDivider()
            CheckRow(
                LiptonIcons.Timer, "Пинг до сервера",
                when {
                    running && r == null -> "проверяем…"
                    r?.pingMs != null -> "${r.pingMs} мс"
                    else -> "нет ответа"
                },
                ok = r?.pingMs?.let { it <= 160 },
            )
        }
        r?.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = c.stateText(AuroraTone.OFF))
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            if (running) "Проверяем…" else "Проверить ещё раз", onClick = run,
            loading = running, enabled = !running, icon = LiptonIcons.Refresh,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CheckRow(icon: ImageVector, title: String, value: String, ok: Boolean?, leading: (@Composable () -> Unit)? = null) {
    val c = LiptonTheme.colors
    val good = c.stateTone(AuroraTone.ON).a
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (leading != null) leading() else ToneCircleIcon(icon, c.text.copy(alpha = 0.8f), size = 32.dp, iconSize = 15.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
            Text(value, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS), color = c.text.copy(alpha = 0.7f))
        }
        when (ok) {
            true -> ToneCircleIcon(LiptonIcons.Check, good, size = 24.dp, iconSize = 11.dp)
            false -> ToneCircleIcon(LiptonIcons.Exclaim, c.warn, size = 24.dp, iconSize = 11.dp)
            null -> Unit
        }
    }
}

// ─── История платежей ────────────────────────────────────────────────────────

private fun txKindRu(t: TxItem): String = when (t.kind) {
    "initial" -> "Оплата подписки"
    "subscription", "renewal", "renew" -> "Продление"
    "change" -> "Смена тарифа"
    "overlay" -> "Временный тариф"
    "manual" -> "Списание"
    "refund" -> "Возврат"
    else -> t.tariffTitle ?: "Платёж"
}

/** История платежей из /me/transactions. TODO(redesign): вид по new-scr-payments-play (A4 ч.2), поля B3. */
@Composable
fun PaymentsHistoryScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val c = LiptonTheme.colors
    var items by remember { mutableStateOf<List<TxItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try { items = viewModel.api.getTransactions().transactions } catch (e: Exception) { error = e.message ?: "Не удалось загрузить" }
    }
    SubPage("История платежей", onBack) {
        val list = items
        when {
            list == null && error == null -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.stateTone(AuroraTone.ON).a, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }
            list == null -> GlassCard(Modifier.fillMaxWidth()) { Text(error ?: "", style = MaterialTheme.typography.bodyMedium, color = c.text.copy(alpha = 0.72f)) }
            list.isEmpty() -> GlassCard(Modifier.fillMaxWidth()) {
                Text("Платежей пока нет", style = MaterialTheme.typography.titleSmall, color = c.text)
                Text("Чек об оплате ЮKassa присылает на почту.", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.7f))
            }
            else -> GlassGroup {
                list.forEachIndexed { i, t ->
                    if (i > 0) GroupDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ToneCircleIcon(LiptonIcons.Receipt, c.text.copy(alpha = 0.8f), size = 32.dp, iconSize = 15.dp)
                        Column(Modifier.weight(1f)) {
                            Text(txKindRu(t), style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1)
                            val date = t.createdAt?.let { parseRfc3339(it) }?.let { ms ->
                                val y = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.YEAR)
                                "${ruDayMonth(ms)} $y"
                            }
                            Text(
                                listOfNotNull(date, t.cardLast4?.let { "•••• $it" }).joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = c.text.copy(alpha = 0.6f),
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(rubles(t.amountKopeks), style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = TABULAR_NUMS), color = c.text)
                            val (label, color) = when (t.status) {
                                "succeeded" -> "оплачено" to c.stateText(AuroraTone.ON)
                                "pending", "waiting_for_capture" -> "в обработке" to c.text.copy(alpha = 0.6f)
                                else -> "не прошёл" to c.stateText(AuroraTone.OFF)
                            }
                            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
                        }
                    }
                }
            }
        }
        if (!list.isNullOrEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Чеки ЮKassa присылает на почту", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.5f))
        }
    }
}

// ─── Способ оплаты и отвязка карты ───────────────────────────────────────────

/**
 * Способ оплаты: карта •••• 4242 и «Отвязать карту» с кулдауном 24 ч после
 * новой привязки (для всех). Автопродление не переключается — только отвязка.
 * TODO(redesign): вид по new-combo-payment-method / -unlink (A4 ч.2).
 */
@Composable
fun PaymentMethodScreen(state: UiState, viewModel: MainViewModel, onBack: () -> Unit) {
    val c = LiptonTheme.colors
    val me = state.me
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.loadProfile() }
    val hasCard = me?.hasCard == true
    val unlinkAt = me?.cardUnlinkAvailableAt?.let { parseRfc3339(it) }
    val now = System.currentTimeMillis()
    val cooldown = unlinkAt != null && unlinkAt > now
    val periodEnd = ruDayMonth(state.accountPeriodEnd)
    val nextAt = ruDayMonth(me?.nextChargeAt) ?: periodEnd
    val nextSum = me?.nextChargeKopeks?.let { rubles(it) }

    SubPage("Способ оплаты", onBack) {
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ToneCircleIcon(LiptonIcons.Card, c.stateTone(AuroraTone.ON).a, size = 44.dp, iconSize = 20.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        if (hasCard) listOfNotNull(me?.cardBrand?.uppercase(), "•••• ${me?.cardLast4 ?: ""}").joinToString(" ") else "Карта не привязана",
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TABULAR_NUMS), color = c.text,
                    )
                    Text(
                        when {
                            hasCard && me?.cardExp != null -> "действует до ${me.cardExp}"
                            hasCard -> "списания по подписке — с этой карты"
                            else -> "Карта привяжется при следующей оплате картой"
                        },
                        style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.7f),
                    )
                }
                if (hasCard) ToneChip("автопродление", c.stateTone(AuroraTone.ON).a, textColor = c.stateText(AuroraTone.ON), height = 22.dp)
            }
        }
        if (hasCard) {
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    buildString {
                        if (nextAt != null) {
                            append("Если не отвязать, $nextAt подписка продлится")
                            if (nextSum != null) append(" — $nextSum")
                            append(". ")
                        }
                        append("После отвязки автопродление выключится")
                        if (periodEnd != null) append(", подписка будет работать до $periodEnd")
                        append(". Отвязать карту снова получится только через 24 часа после новой привязки.")
                    },
                    style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.72f),
                )
            }
            Spacer(Modifier.height(20.dp))
            if (cooldown) {
                val at = unlinkAt ?: now
                val cal = Calendar.getInstance().apply { timeInMillis = at }
                Text(
                    "Отвязать можно будет ${ruDayMonth(at)} в ${String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))} — через 24 часа после привязки",
                    style = MaterialTheme.typography.bodySmall, color = c.stateText(AuroraTone.OFF),
                )
                Spacer(Modifier.height(12.dp))
            }
            PrimaryButton(
                "Отвязать карту", onClick = { confirm = true }, tone = com.lipton.vpn.ui.components.ButtonTone.WARN,
                enabled = !cooldown && state.profileBusy == null, loading = state.profileBusy == "card",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (confirm) {
        ConfirmDialog(
            title = "Отвязать карту?",
            text = "Автопродление выключится" + (periodEnd?.let { ", подписка будет работать до $it" } ?: "") + ".",
            warning = "Отвязать карту снова получится только через 24 часа после новой привязки.",
            confirmText = "Отвязать карту",
            icon = LiptonIcons.Card,
            danger = true,
            onConfirm = { viewModel.unlinkCard(); confirm = false },
            onDismiss = { confirm = false },
        )
    }
}

// ─── Промокод ────────────────────────────────────────────────────────────────

/**
 * «Ввести промокод»: проверка через /promo/validate; верный код запоминается и
 * уходит в ближайшую оплату. TODO(redesign): экран по new-scr-promo-play (A4 ч.2).
 */
@Composable
fun PromoDialog(viewModel: MainViewModel, onDismiss: () -> Unit, onApplied: () -> Unit) {
    val c = LiptonTheme.colors
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf(viewModel.pendingPromo.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    ConfirmDialog(
        title = "Промокод",
        text = "Скидка или бонусные дни применятся к ближайшей оплате.",
        confirmText = "Применить",
        icon = LiptonIcons.Tag,
        busy = busy,
        onConfirm = {
            if (code.isBlank()) { error = "Введите промокод"; return@ConfirmDialog }
            busy = true; error = null
            scope.launch {
                try {
                    val r = viewModel.validatePromo(code)
                    if (r.valid) {
                        val what = when {
                            r.percentOff != null -> "скидка ${r.percentOff}%"
                            r.bonusDays != null -> "+${r.bonusDays} ${pluralRu(r.bonusDays, "день", "дня", "дней")}"
                            else -> "применится к оплате"
                        }
                        viewModel.showError("Промокод принят: $what")
                        onApplied()
                    } else {
                        error = r.reason ?: "Промокод не подходит"
                    }
                } catch (e: Exception) {
                    error = e.message ?: "Не удалось проверить промокод"
                } finally {
                    busy = false
                }
            }
        },
        onDismiss = onDismiss,
        extra = {
            Spacer(Modifier.height(14.dp))
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(shape)
                    .background(c.text.copy(alpha = 0.05f))
                    .border(1.dp, if (error != null) c.warn.copy(alpha = 0.6f) else c.text.copy(alpha = 0.12f), shape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (code.isEmpty()) Text("Например, LIPTON10", style = MaterialTheme.typography.bodyLarge, color = c.text.copy(alpha = 0.4f))
                BasicTextField(
                    value = code,
                    onValueChange = { code = it.uppercase(Locale.ROOT).take(32); error = null },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.text, fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(c.stateTone(AuroraTone.ON).a),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.stateText(AuroraTone.OFF))
            }
        },
    )
}
