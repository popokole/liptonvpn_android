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
import com.lipton.vpn.data.SplitMode
import com.lipton.vpn.ui.components.GlassButton
import com.lipton.vpn.ui.components.SegmentOption
import com.lipton.vpn.ui.components.Segmented
import com.lipton.vpn.ui.components.tabBarBottomPadding
import com.lipton.vpn.UiState
import com.lipton.vpn.data.model.TxItem
import com.lipton.vpn.service.LiptonVpnService.VpnStatus
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.ConfirmDialog
import com.lipton.vpn.ui.components.FlagCircle
import com.lipton.vpn.ui.components.GhostButton
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.GlassGroup
import com.lipton.vpn.ui.components.GroupDivider
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.LiptonSwitch
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
//  Подэкраны профиля: раздельное туннелирование и проверка соединения.
//  Остальные подэкраны — ProfileSubScreens.kt, PaymentScreens.kt, SupportScreens.kt.
// ─────────────────────────────────────────────────────────────────────────────

// ─── Раздельное туннелирование ───────────────────────────────────────────────

private data class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap?)

/**
 * Раздельное туннелирование по приложениям: режим «Все через VPN» или «Выбранные
 * мимо VPN» (VpnService.Builder.addDisallowedApplication). Список — приложения с
 * иконкой на рабочем столе (<queries> на LAUNCHER), с поиском. Изменения
 * применяются при следующем подключении; если VPN включён — можно переподключиться.
 */
@Composable
fun SplitTunnelScreen(state: UiState, viewModel: MainViewModel, activity: ComponentActivity, onBack: () -> Unit) {
    val c = LiptonTheme.colors
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var query by remember { mutableStateOf("") }
    val initialApps = remember { state.splitTunnelApps.toSet() }
    val initialMode = remember { state.splitTunnelMode }
    val selected = state.splitTunnelApps.toSet()
    val mode = state.splitTunnelMode
    val changed = selected != initialApps || mode != initialMode
    val connected = state.status == VpnStatus.CONNECTED

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

    ProfileSubPage(state, "Раздельное туннелирование", onBack, scroll = false) {
        SubIntro("Выберите, какие приложения пойдут мимо VPN. Применится при следующем подключении.")
        Spacer(Modifier.height(16.dp))
        Segmented(
            options = listOf(
                SegmentOption(SplitMode.ALL, "Все через VPN", LiptonIcons.ShieldCheck),
                SegmentOption(SplitMode.BYPASS_SELECTED, "Выбранные мимо VPN", LiptonIcons.Apps),
            ),
            selected = mode,
            onSelect = { m, _ -> viewModel.setSplitTunnelMode(m) },
        )
        if (changed && connected) {
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(LiptonIcons.Info, null, tint = c.stateText(AuroraTone.OFF), modifier = Modifier.size(16.dp))
                    Text("Изменения применятся при следующем подключении", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
                    GlassButton("Переподключить", onClick = { viewModel.reconnectIfConnected(activity) }, height = 34.dp)
                }
            }
        }
        if (mode == SplitMode.ALL) {
            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToneCircleIcon(LiptonIcons.ShieldCheck, c.stateTone(AuroraTone.ON).a, size = 40.dp, iconSize = 18.dp)
                    Column(Modifier.weight(1f)) {
                        Text("Все приложения через VPN", style = MaterialTheme.typography.titleSmall, color = c.text)
                        Text(
                            if (selected.isEmpty()) "Чтобы пустить приложение напрямую, выберите «Выбранные мимо VPN»."
                            else "Список из ${selected.size} ${pluralRu(selected.size, "приложения", "приложений", "приложений")} сохранён — он заработает в режиме «Выбранные мимо VPN».",
                            style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.72f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(tabBarBottomPadding()))
        } else {
            Spacer(Modifier.height(12.dp))
            SearchField(query, { query = it }, "Найти приложение")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (selected.isEmpty()) "Отметьте приложения, которые пойдут напрямую" else "${selected.size} ${pluralRu(selected.size, "приложение", "приложения", "приложений")} мимо VPN",
                    style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.7f), modifier = Modifier.weight(1f),
                )
                if (selected.isNotEmpty()) GhostButton("Сбросить", onClick = { viewModel.setSplitTunnelApps(emptyList()) })
            }
            val list = apps
            if (list == null) {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.stateTone(AuroraTone.ON).a, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                }
            } else {
                val q = query.trim().lowercase(Locale.getDefault())
                val shown = list.filter { q.isEmpty() || it.label.lowercase(Locale.getDefault()).contains(q) || it.pkg.contains(q) }
                    .sortedByDescending { it.pkg in initialApps }
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)).glass(RoundedCornerShape(24.dp)),
                    contentPadding = PaddingValues(top = 4.dp, bottom = tabBarBottomPadding()),
                ) {
                    items(shown, key = { it.pkg }) { app ->
                        val on = app.pkg in selected
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clickable(role = Role.Switch) {
                                    viewModel.setSplitTunnelApps((if (on) selected - app.pkg else selected + app.pkg).toList())
                                }
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

    ProfileSubPage(state, "Проверка соединения", onBack) {
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

