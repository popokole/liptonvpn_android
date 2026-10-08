package com.lipton.vpn.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lipton.vpn.BuildConfig
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.UiState
import com.lipton.vpn.ui.account.AccountPanel
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.GlassGroup
import com.lipton.vpn.ui.components.GroupDivider
import com.lipton.vpn.ui.components.IconTile
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.ListRow
import com.lipton.vpn.ui.components.ScreenHorizontalPadding
import com.lipton.vpn.ui.components.ScreenTitle
import com.lipton.vpn.ui.components.SectionHeader
import com.lipton.vpn.ui.components.SegmentOption
import com.lipton.vpn.ui.components.Segmented
import com.lipton.vpn.ui.components.TabTopBar
import com.lipton.vpn.ui.components.tabBarBottomPadding
import com.lipton.vpn.ui.theme.AppTheme
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.LocalThemeChange

/**
 * Вкладка «Профиль». Пока — прежняя панель аккаунта (подписка, оплата, смена
 * тарифа, выход), новая настройка темы и входы в настройки VPN, базу знаний и чат.
 * TODO(redesign): A4 — шапка с аватаром, ссылка подписки, устройства, оплата и т. д.
 */
@Composable
fun ProfileTab(
    state: UiState,
    viewModel: MainViewModel,
    onPay: () -> Unit,
    onChangeTariff: () -> Unit,
    onNews: () -> Unit,
    onSupport: () -> Unit,
    onVpnSettings: () -> Unit,
    onFaq: () -> Unit,
) {
    val c = LiptonTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenHorizontalPadding),
    ) {
        TabTopBar { SubscriptionCapsule(state) }
        ScreenTitle("Профиль")
        Spacer(Modifier.height(20.dp))

        AccountPanel(
            state = state,
            onPay = onPay,
            onNews = onNews,
            onSupport = onSupport,
            onLogout = { viewModel.logoutAccount() },
            onRefresh = { viewModel.refreshAccount() },
            onChangeTariff = onChangeTariff,
        )

        SectionHeader("Приложение", Modifier.padding(top = 28.dp))
        Spacer(Modifier.height(8.dp))
        ThemeCard(state.themeMode)

        SectionHeader("VPN и помощь", Modifier.padding(top = 28.dp))
        Spacer(Modifier.height(8.dp))
        GlassGroup {
            ListRow(
                title = "Настройки VPN",
                subtitle = "Автоподключение, обход российских сайтов, логи",
                icon = LiptonIcons.Bolt,
                onClick = onVpnSettings,
            )
            GroupDivider()
            ListRow(
                title = "База знаний",
                subtitle = "Подключение и частые вопросы",
                icon = LiptonIcons.Book,
                onClick = onFaq,
            )
            GroupDivider()
            ListRow(
                title = "Чат поддержки",
                subtitle = "ИИ-помощник отвечает сразу",
                icon = LiptonIcons.Chat,
                onClick = onSupport,
            )
        }

        Text(
            "Lipton VPN · версия ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = c.text3,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )

        Spacer(Modifier.height(tabBarBottomPadding()))
    }
}

/** Карточка «Тема»: Тёмная / Светлая / Системная, смена — раскрытием кругом из варианта. */
@Composable
private fun ThemeCard(current: AppTheme) {
    val c = LiptonTheme.colors
    val changeTheme = LocalThemeChange.current
    val options = listOf(
        SegmentOption(AppTheme.DARK, AppTheme.DARK.title, LiptonIcons.Moon),
        SegmentOption(AppTheme.LIGHT, AppTheme.LIGHT.title, LiptonIcons.Sun),
        SegmentOption(AppTheme.SYSTEM, AppTheme.SYSTEM.title, LiptonIcons.System),
    )
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(LiptonIcons.Palette)
            Text("Тема", style = MaterialTheme.typography.titleSmall, color = c.text)
        }
        Spacer(Modifier.height(12.dp))
        Segmented(
            options = options,
            selected = current,
            onSelect = { theme, center -> changeTheme(theme, center) },
        )
    }
}
