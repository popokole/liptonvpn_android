package com.lipton.vpn.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.ui.theme.Green3
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.UiState
import com.lipton.vpn.data.model.displayName
import com.lipton.vpn.service.LiptonVpnService.VpnStatus
import com.lipton.vpn.ui.components.*
import com.lipton.vpn.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    state: UiState,
    viewModel: MainViewModel,
    activity: ComponentActivity,
) {
    val lc = LocalLiptonColors.current

    var showSettings by remember { mutableStateOf(false) }
    var planesMode   by remember { mutableStateOf<PlaneMode?>(null) }
    val prevStatus   = remember { mutableStateOf(state.status) }
    val scope        = rememberCoroutineScope()
    val planesJob    = remember { mutableStateOf<Job?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg, withDismissAction = true)
        viewModel.clearError()
    }

    state.connectionError?.let { error ->
        ConnectionErrorSheet(
            error          = error,
            onDismiss      = { viewModel.clearConnectionError() },
            onRetry        = {
                viewModel.clearConnectionError()
                viewModel.handleConnectToggle(activity)
            },
            onSwitchServer = { viewModel.switchToNextServer(activity) },
            onHelp         = {
                viewModel.clearConnectionError()
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/liptonvpn_bot"))
                )
            },
        )
    }

    // Auto-connect on launch
    LaunchedEffect(state.loading) {
        if (!state.loading && state.autoConnectOnLaunch && state.status == VpnStatus.DISCONNECTED) {
            val serverId = state.activeServerId
                ?: state.subscriptions.flatMap { it.servers }.firstOrNull()?.id
            if (serverId != null) viewModel.connect(activity, serverId)
        }
    }

    // Plane animation triggers
    LaunchedEffect(state.status) {
        val prev = prevStatus.value
        prevStatus.value = state.status
        when {
            (prev == VpnStatus.DISCONNECTED || prev == VpnStatus.ERROR) &&
                    state.status == VpnStatus.CONNECTING -> {
                planesJob.value?.cancel()
                planesJob.value = scope.launch {
                    planesMode = PlaneMode.CONNECT
                    delay(2800)
                    planesMode = null
                }
            }
            prev == VpnStatus.CONNECTED &&
                    (state.status == VpnStatus.DISCONNECTING ||
                     state.status == VpnStatus.DISCONNECTED) -> {
                planesJob.value?.cancel()
                planesJob.value = scope.launch {
                    planesMode = PlaneMode.DISCONNECT
                    delay(2000)
                    planesMode = null
                }
            }
        }
    }

    val bgDeep = lc.bgDeep

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(color = bgDeep)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Green.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width / 2f, -size.height * 0.05f),
                        radius = size.width * 0.85f,
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Green.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.85f),
                        radius = size.width * 0.55f,
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Green.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height * 0.75f),
                        radius = size.width * 0.45f,
                    )
                )
            }
    ) {
        if (state.loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green, modifier = Modifier.size(32.dp), strokeWidth = 2.5.dp)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(onSettings = { showSettings = true })

                // Баннер обновления — прямо под шапкой, над кнопкой подключения
                AnimatedVisibility(
                    visible = state.updateInfo != null,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit  = fadeOut(tween(200)) + shrinkVertically(tween(200)),
                ) {
                    state.updateInfo?.let { _ ->
                        Column {
                            UpdateBanner(
                                version           = state.updateInfo!!.versionName,
                                downloadProgress  = state.downloadProgress,
                                downloadedApkPath = state.downloadedApkPath,
                                onDownload        = { viewModel.downloadUpdate() },
                                onInstall         = { viewModel.installUpdate(activity) },
                                onDismiss         = { viewModel.dismissUpdate() },
                                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, Green.copy(alpha = 0.14f), Color.Transparent)
                                        )
                                    )
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    ConnectSection(
                        state = state,
                        onConnect = { viewModel.handleConnectToggle(activity) },
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Green.copy(alpha = 0.14f), Color.Transparent)
                                )
                            )
                    )

                    val allServers = state.subscriptions.flatMap { it.servers }
                    val isTrialOnly = state.subscriptions.isNotEmpty() && state.subscriptions.all { it.isTrial }
                    ServerList(
                        servers        = allServers,
                        activeServerId = state.activeServerId,
                        pinging        = state.pinging,
                        isTrialOnly    = isTrialOnly,
                        onSelect       = { id -> viewModel.selectServer(activity, id) },
                        onPingAll      = {
                            state.subscriptions.forEach { sub -> viewModel.pingAll(sub.id) }
                        },
                    )

                    SubscriptionPanel(
                        subscriptions       = state.subscriptions,
                        trialUsed           = state.trialUsed,
                        onAdd               = { url -> viewModel.addSubscription(url) },
                        onRemove            = { id -> viewModel.removeSubscription(id) },
                        onRefresh           = { id -> viewModel.refreshSubscription(id) },
                        onGetTrial          = { mins -> viewModel.getTrialSubscription(mins) },
                        onBuyClick          = {
                            activity.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/liptonvpn_bot"))
                            )
                        },
                    )

                }

                Footer(activity = activity)
            }

            PlanesOverlay(mode = planesMode)

            // Snackbar для ошибок
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier  = Modifier.padding(bottom = 16.dp),
                )
            }

            if (showSettings) {
                SettingsPanel(
                    bypassRu             = state.bypassRu,
                    bypassDomains        = state.bypassDomains,
                    autoConnectOnLaunch  = state.autoConnectOnLaunch,
                    logLines             = state.logLines,
                    trialUsed            = state.trialUsed,
                    onBypassRuChange     = { viewModel.setBypassRu(it) },
                    onAddDomain          = { viewModel.addBypassDomain(it) },
                    onRemoveDomain       = { viewModel.removeBypassDomain(it) },
                    onAutoConnectChange  = { viewModel.setAutoConnectOnLaunch(it) },
                    onClearLogs          = { viewModel.clearLogs() },
                    onGetTrial           = { mins -> viewModel.getTrialSubscription(mins) },
                    onBuyClick           = {
                        activity.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/liptonvpn_bot"))
                        )
                    },
                    onReset              = { viewModel.resetProfile(activity) },
                    onClose              = { showSettings = false },
                    onCheckUpdate        = { viewModel.manualCheckUpdate() },
                )
            }
        }
    }
}

@Composable
private fun TopBar(onSettings: () -> Unit) {
    val lc = LocalLiptonColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(Green, Green3))),
                contentAlignment = Alignment.Center,
            ) {
                Text("L", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
            Text(
                text = "LIPTON VPN",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                color = lc.textSecondary,
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(lc.greenCard)
                .border(1.dp, lc.greenBorder, RoundedCornerShape(10.dp))
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Text("⚙", fontSize = 16.sp, color = lc.textSecondary)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Green.copy(alpha = 0.14f), Color.Transparent)
                )
            )
    )
}

@Composable
private fun ConnectSection(state: UiState, onConnect: () -> Unit) {
    val lc = LocalLiptonColors.current
    val allServers   = state.subscriptions.flatMap { it.servers }
    val activeServer = allServers.find { it.id == state.activeServerId }

    val statusLabel = when (state.status) {
        VpnStatus.CONNECTED     -> activeServer?.displayName() ?: "Подключено"
        VpnStatus.CONNECTING    -> "Подключение..."
        VpnStatus.DISCONNECTING -> "Отключение..."
        VpnStatus.DISCONNECTED  -> "Отключено"
        VpnStatus.ERROR         -> "Не удалось подключиться"
    }

    val statusColor by animateColorAsState(
        targetValue = when (state.status) {
            VpnStatus.CONNECTED              -> Green
            VpnStatus.CONNECTING,
            VpnStatus.DISCONNECTING          -> Green
            VpnStatus.ERROR                  -> Red
            else                             -> lc.textPrimary
        },
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "status_color",
    )

    val subText = when (state.status) {
        VpnStatus.CONNECTED -> activeServer?.remark?.replace(
            Regex("[\uD83C][\uDDE6-\uDDFF][\uD83C][\uDDE6-\uDDFF]\\s*"), ""
        )?.trim() ?: ""
        else -> if (allServers.isNotEmpty()) "${allServers.size} серверов доступно"
                else "Добавьте подписку"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        ConnectButton(
            status  = state.status,
            onClick = onConnect,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        AnimatedContent(
            targetState = statusLabel,
            transitionSpec = {
                fadeIn(tween(320, easing = FastOutSlowInEasing))
                    .togetherWith(fadeOut(tween(200)))
            },
            label = "status",
        ) { label ->
            Text(
                text = label,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.4).sp,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        AnimatedContent(
            targetState = subText,
            transitionSpec = { fadeIn(tween(350)).togetherWith(fadeOut(tween(250))) },
            label = "sub_text",
        ) { text ->
            Text(
                text = text,
                fontSize = 12.sp,
                color = lc.textTertiary,
                textAlign = TextAlign.Center,
            )
        }

        // Кнопка "Повторить" при ошибке подключения
        AnimatedVisibility(
            visible = state.status == VpnStatus.ERROR,
            enter   = fadeIn(tween(300)) + slideInVertically { it / 2 },
            exit    = fadeOut(tween(200)),
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Red.copy(alpha = 0.12f))
                    .border(1.dp, Red.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable(onClick = onConnect)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "↻  Повторить",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Red,
                )
            }
        }
    }
}

@Composable
private fun Footer(activity: ComponentActivity) {
    val lc = LocalLiptonColors.current
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Green.copy(alpha = 0.08f)))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable {
                    activity.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/liptonvpn_bot"))
                    )
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("✈", fontSize = 14.sp, color = lc.textSecondary)
            Text("Telegram", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = lc.textSecondary)
        }

        Text("v${com.lipton.vpn.BuildConfig.VERSION_NAME}", fontSize = 11.sp, color = lc.textTertiary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun UpdateBanner(
    version:           String,
    downloadProgress:  Int?,
    downloadedApkPath: String?,
    onDownload:        () -> Unit,
    onInstall:         () -> Unit,
    onDismiss:         () -> Unit,
    modifier:          Modifier = Modifier,
) {
    val lc = LocalLiptonColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(lc.bgCard)
            .border(1.dp, Green.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("⬆", fontSize = 18.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Доступно обновление v$version",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = lc.textPrimary,
                )
                Text(
                    text = when {
                        downloadedApkPath != null -> "Готово к установке"
                        downloadProgress != null  -> "Скачивание $downloadProgress%..."
                        else                      -> "Нажмите чтобы скачать"
                    },
                    fontSize = 11.sp,
                    color = if (downloadedApkPath != null) Green else lc.textTertiary,
                )
            }
            when {
                downloadedApkPath != null -> Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(Green, Green3)))
                        .clickable(onClick = onInstall)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("Установить", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                downloadProgress != null -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Green,
                    strokeWidth = 2.dp,
                )
                else -> Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Green.copy(alpha = 0.12f))
                        .border(1.dp, Green.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onDownload)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("Скачать", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Green)
                }
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = downloadProgress == null, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", fontSize = 12.sp, color = lc.textTertiary)
            }
        }
        if (downloadProgress != null && downloadedApkPath == null) {
            LinearProgressIndicator(
                progress = { downloadProgress / 100f },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color = Green,
                trackColor = Green.copy(alpha = 0.15f),
            )
        }
    }
}
