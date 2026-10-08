package com.lipton.vpn.ui.tabs

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.UiState
import com.lipton.vpn.ui.components.ScreenHorizontalPadding
import com.lipton.vpn.ui.components.ScreenTitle
import com.lipton.vpn.ui.components.ServerList
import com.lipton.vpn.ui.components.TabTopBar
import com.lipton.vpn.ui.components.tabBarBottomPadding

/**
 * Вкладка «Серверы». Пока — прежний список серверов (пинг, выбор) на всю высоту.
 * TODO(redesign): A4 — «Авто-баланс» сверху, группы, секция «Обход глушилок».
 */
@Composable
fun ServersTab(state: UiState, viewModel: MainViewModel, activity: ComponentActivity) {
    val servers = state.subscriptions.flatMap { it.servers }
    val isTrialOnly = state.subscriptions.isNotEmpty() && state.subscriptions.all { it.isTrial }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = ScreenHorizontalPadding),
    ) {
        TabTopBar { SubscriptionCapsule(state) }
        ScreenTitle("Серверы")
        Spacer(Modifier.height(16.dp))
        ServerList(
            servers = servers,
            activeServerId = state.activeServerId,
            pinging = state.pinging,
            isTrialOnly = isTrialOnly,
            onSelect = { id -> viewModel.selectServer(activity, id) },
            onPingAll = { state.subscriptions.forEach { sub -> viewModel.pingAll(sub.id) } },
            modifier = Modifier.weight(1f, fill = false),
            maxListHeight = Dp.Unspecified,
        )
        Spacer(Modifier.height(tabBarBottomPadding()))
    }
}
