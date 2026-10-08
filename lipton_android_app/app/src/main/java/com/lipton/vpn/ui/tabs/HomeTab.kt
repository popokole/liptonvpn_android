package com.lipton.vpn.ui.tabs

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.UiState
import com.lipton.vpn.data.model.displayName
import com.lipton.vpn.data.model.flagEmoji
import com.lipton.vpn.service.LiptonVpnService.VpnStatus
import com.lipton.vpn.ui.components.BannerSlot
import com.lipton.vpn.ui.components.ButtonTone
import com.lipton.vpn.ui.components.ConnectButton
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.PrimaryButton
import com.lipton.vpn.ui.components.ScreenHorizontalPadding
import com.lipton.vpn.ui.components.TabTopBar
import com.lipton.vpn.ui.components.tabBarBottomPadding
import com.lipton.vpn.ui.theme.Green
import com.lipton.vpn.ui.theme.Green3
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.LocalLiptonColors
import com.lipton.vpn.ui.theme.Red

/**
 * Вкладка «Главная». Пока это прежний экран подключения (кнопка, статус) в новом
 * каркасе: шапка с капсулой срока, слот баннера, карточка сервера → «Серверы».
 * TODO(redesign): A4 — таймер сессии, бенто-статистика, «нет подписки» с тарифами.
 */
@Composable
fun HomeTab(
    state: UiState,
    viewModel: MainViewModel,
    activity: ComponentActivity,
    onOpenServers: () -> Unit,
    onPay: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TabTopBar(Modifier.padding(horizontal = ScreenHorizontalPadding)) {
            SubscriptionCapsule(state)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
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
            }

            ConnectSection(state = state, onConnect = { viewModel.handleConnectToggle(activity) })

            ServerCard(state = state, onClick = onOpenServers)

            if (!hasActiveSubscription(state) && state.accountStatus != null) {
                PrimaryButton(
                    text = "Оформить подписку",
                    onClick = onPay,
                    tone = ButtonTone.WARN,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(tabBarBottomPadding()))
        }
    }
}

// ─── Карточка текущего сервера (переход во вкладку «Серверы») ────────────────

@Composable
private fun ServerCard(state: UiState, onClick: () -> Unit) {
    val c = LiptonTheme.colors
    val servers = state.subscriptions.flatMap { it.servers }
    val active = servers.find { it.id == state.activeServerId } ?: servers.firstOrNull()
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(c.glassFill)
                    .border(1.dp, c.glassBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                val flag = active?.flagEmoji().orEmpty()
                if (flag.isNotEmpty()) Text(flag, fontSize = 18.sp)
                else Icon(LiptonIcons.Globe, null, tint = c.text2, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (state.status == VpnStatus.CONNECTED) "Сейчас через" else "Сервер",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.text2,
                    maxLines = 1,
                )
                Text(
                    active?.displayName() ?: "Серверов пока нет",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(LiptonIcons.ChevronRight, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
    }
}

// ─── Прежний блок подключения (кнопка-сфера и статус) ────────────────────────

@Composable
private fun ConnectSection(state: UiState, onConnect: () -> Unit) {
    val lc = LocalLiptonColors.current
    val allServers = state.subscriptions.flatMap { it.servers }
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
            VpnStatus.CONNECTED,
            VpnStatus.CONNECTING,
            VpnStatus.DISCONNECTING -> Green
            VpnStatus.ERROR         -> Red
            else                    -> lc.textPrimary
        },
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "status_color",
    )

    val subText = when (state.status) {
        VpnStatus.CONNECTED -> "Защищено"
        else -> if (allServers.isNotEmpty()) "${allServers.size} серверов доступно" else "Серверов пока нет"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ConnectButton(
            status = state.status,
            onClick = onConnect,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        AnimatedContent(
            targetState = statusLabel,
            transitionSpec = { fadeIn(tween(320, easing = FastOutSlowInEasing)).togetherWith(fadeOut(tween(200))) },
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

        Spacer(Modifier.height(6.dp))

        AnimatedContent(
            targetState = subText,
            transitionSpec = { fadeIn(tween(350)).togetherWith(fadeOut(tween(250))) },
            label = "sub_text",
        ) { text ->
            Text(text = text, fontSize = 12.sp, color = lc.textTertiary, textAlign = TextAlign.Center)
        }

        // Кнопка «Повторить» при ошибке подключения
        AnimatedVisibility(
            visible = state.status == VpnStatus.ERROR,
            enter = fadeIn(tween(300)) + slideInVertically { it / 2 },
            exit = fadeOut(tween(200)),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Red.copy(alpha = 0.12f))
                    .border(1.dp, Red.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable(onClick = onConnect)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("↻  Повторить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Red)
            }
        }
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
    modifier: Modifier = Modifier,
) {
    val lc = LocalLiptonColors.current
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("⬆", fontSize = 18.sp, color = lc.textPrimary)
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
                        else                      -> "Нажмите, чтобы скачать"
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
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(4.dp)),
                color = Green,
                trackColor = Green.copy(alpha = 0.15f),
            )
        }
    }
}
