package com.lipton.vpn.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lipton.vpn.BuildConfig
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.UiState
import com.lipton.vpn.service.LiptonVpnService.VpnStatus
import com.lipton.vpn.ui.account.NewsScreen
import com.lipton.vpn.ui.account.PaymentScreen
import com.lipton.vpn.ui.account.SupportScreen
import com.lipton.vpn.ui.account.TariffChangeScreen
import com.lipton.vpn.ui.components.AuroraBackground
import com.lipton.vpn.ui.components.AuroraLayout
import com.lipton.vpn.ui.components.ConnectionErrorSheet
import com.lipton.vpn.ui.components.LiptonTab
import com.lipton.vpn.ui.components.LiptonTabBar
import com.lipton.vpn.ui.components.SettingsPanel
import com.lipton.vpn.ui.components.tabBarBottomPadding
import com.lipton.vpn.ui.tabs.HomeTab
import com.lipton.vpn.ui.tabs.ProfileTab
import com.lipton.vpn.ui.tabs.ServersTab
import com.lipton.vpn.ui.tabs.auroraToneFor
import com.lipton.vpn.ui.theme.Green
import com.lipton.vpn.ui.theme.Green3
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.LocalLiptonColors
import com.lipton.vpn.ui.theme.Tokens

private const val BOT_URL = "https://t.me/liptonvpn_bot"

/** Подэкраны поверх вкладок (своё «назад», без капсулы навигации). */
object SubRoutes {
    const val PAYMENT = "payment"
    const val TARIFF_CHANGE = "tariff-change"
    const val SUPPORT = "support"
}

/**
 * Каркас приложения после входа: свечение «Аврора» на весь экран, четыре
 * вкладки (Главная, Серверы, Новости, Профиль) в NavHost, плавающая капсула
 * навигации, подэкраны (оплата, смена тарифа, поддержка) отдельными маршрутами
 * с системной «Назад». Общие шиты и уведомления — здесь же.
 */
@Composable
fun MainScreen(
    state: UiState,
    viewModel: MainViewModel,
    activity: ComponentActivity,
    startTab: LiptonTab = LiptonTab.HOME,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val currentTab = LiptonTab.fromRoute(entry?.destination?.route)

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showFaq      by rememberSaveable { mutableStateOf(false) }
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
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BOT_URL)))
            },
        )
    }

    // Автоподключение при запуске
    LaunchedEffect(state.loading) {
        if (!state.loading && state.autoConnectOnLaunch && state.status == VpnStatus.DISCONNECTED) {
            val serverId = state.activeServerId
                ?: state.subscriptions.flatMap { it.servers }.firstOrNull()?.id
            if (serverId != null) viewModel.connect(activity, serverId)
        }
    }

    val openTab: (LiptonTab) -> Unit = { tab -> nav.navigateToTab(tab) }

    Box(Modifier.fillMaxSize()) {
        // Свечение под всеми вкладками; раскладка пятен — по вкладке, смена — кросс-фейдом.
        Crossfade(
            targetState = auroraLayoutFor(currentTab),
            animationSpec = tween(420, easing = Tokens.Motion.standard),
            label = "aurora_layout",
        ) { layout ->
            AuroraBackground(tone = auroraToneFor(state), layout = layout)
        }

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green, modifier = Modifier.size(32.dp), strokeWidth = 2.5.dp)
            }
        } else {
            NavHost(
                navController = nav,
                startDestination = startTab.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(Tokens.Motion.TAB_FADE_MS)) },
                exitTransition = { fadeOut(tween(Tokens.Motion.TAB_FADE_MS)) },
                popEnterTransition = { fadeIn(tween(Tokens.Motion.TAB_FADE_MS)) },
                popExitTransition = { fadeOut(tween(Tokens.Motion.TAB_FADE_MS)) },
            ) {
                composable(LiptonTab.HOME.route) {
                    HomeTab(
                        state = state,
                        viewModel = viewModel,
                        activity = activity,
                        onOpenServers = { openTab(LiptonTab.SERVERS) },
                        onPay = { nav.navigate(SubRoutes.PAYMENT) },
                    )
                }
                composable(LiptonTab.SERVERS.route) {
                    ServersTab(state = state, viewModel = viewModel, activity = activity)
                }
                composable(LiptonTab.NEWS.route) {
                    NewsScreen(vm = viewModel)
                }
                composable(LiptonTab.PROFILE.route) {
                    ProfileTab(
                        state = state,
                        viewModel = viewModel,
                        onPay = { nav.navigate(SubRoutes.PAYMENT) },
                        onChangeTariff = { nav.navigate(SubRoutes.TARIFF_CHANGE) },
                        onNews = { openTab(LiptonTab.NEWS) },
                        onSupport = { nav.navigate(SubRoutes.SUPPORT) },
                        onVpnSettings = { showSettings = true },
                        onFaq = { showFaq = true },
                    )
                }
                composable(
                    SubRoutes.PAYMENT,
                    enterTransition = { subEnter() }, exitTransition = { fadeOut(tween(160)) },
                    popEnterTransition = { fadeIn(tween(160)) }, popExitTransition = { subExit() },
                ) {
                    PaymentScreen(
                        vm = viewModel,
                        onClose = { nav.popBackStack() },
                        onOpenChange = {
                            nav.navigate(SubRoutes.TARIFF_CHANGE) {
                                popUpTo(SubRoutes.PAYMENT) { inclusive = true }
                            }
                        },
                    )
                }
                composable(
                    SubRoutes.TARIFF_CHANGE,
                    enterTransition = { subEnter() }, exitTransition = { fadeOut(tween(160)) },
                    popEnterTransition = { fadeIn(tween(160)) }, popExitTransition = { subExit() },
                ) {
                    TariffChangeScreen(vm = viewModel, onClose = { nav.popBackStack() })
                }
                composable(
                    SubRoutes.SUPPORT,
                    enterTransition = { subEnter() }, exitTransition = { fadeOut(tween(160)) },
                    popEnterTransition = { fadeIn(tween(160)) }, popExitTransition = { subExit() },
                ) {
                    SupportScreen(vm = viewModel, onClose = { nav.popBackStack() })
                }
            }

            // Плавающая капсула навигации — только на вкладках
            AnimatedVisibility(
                visible = currentTab != null,
                enter = fadeIn(tween(Tokens.Motion.TAB_FADE_MS)) + slideInVertically(tween(320, easing = Tokens.Motion.standard)) { it / 2 },
                exit = fadeOut(tween(160)) + slideOutVertically(tween(220, easing = Tokens.Motion.standard)) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 26.dp),
            ) {
                LiptonTabBar(
                    selected = currentTab ?: LiptonTab.HOME,
                    onSelect = openTab,
                )
            }

            // Уведомления об ошибках — над капсулой
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (currentTab != null) tabBarBottomPadding() else 16.dp)
                    .navigationBarsPadding(),
            )

            ClipboardImportBanner(state = state, viewModel = viewModel, activity = activity)

            if (showFaq) {
                FaqScreen(
                    onClose = { showFaq = false },
                    onOpenTelegram = {
                        showFaq = false
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BOT_URL)))
                    },
                )
            }

            if (showSettings) {
                SettingsPanel(
                    bypassRu             = state.bypassRu,
                    bypassDomains        = state.bypassDomains,
                    autoConnectOnLaunch  = state.autoConnectOnLaunch,
                    logLines             = state.logLines,
                    trialUsed            = state.trialUsed,
                    hapticEnabled        = state.hapticEnabled,
                    themeMode            = state.themeMode,
                    onBypassRuChange     = { viewModel.setBypassRu(it) },
                    onAddDomain          = { viewModel.addBypassDomain(it) },
                    onRemoveDomain       = { viewModel.removeBypassDomain(it) },
                    onAutoConnectChange  = { viewModel.setAutoConnectOnLaunch(it) },
                    onHapticChange       = { viewModel.setHapticEnabled(it) },
                    onThemeChange        = { viewModel.setThemeMode(it) },
                    onClearLogs          = { viewModel.clearLogs() },
                    onGetTrial           = { mins -> viewModel.getTrialSubscription(mins) },
                    onBuyClick           = {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BOT_URL)))
                    },
                    onFaq                = { showSettings = false; showFaq = true },
                    onReset              = { viewModel.resetProfile(activity) },
                    onClose              = { showSettings = false },
                    onCheckUpdate        = { viewModel.manualCheckUpdate() },
                )
            }
        }

        // «Что нового» — поверх всего
        AnimatedVisibility(
            visible = !state.loading && state.showWhatsNew,
            enter = slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize().zIndex(10f),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(LiptonTheme.colors.bg)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                WhatsNewScreen(version = BuildConfig.VERSION_NAME, onDismiss = { viewModel.dismissWhatsNew() })
            }
        }
    }
}

/** Переход на вкладку: одна копия каждой вкладки, состояние вкладок сохраняется, «Назад» — на Главную. */
private fun NavHostController.navigateToTab(tab: LiptonTab) {
    if (currentDestination?.route == tab.route) return
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun auroraLayoutFor(tab: LiptonTab?): AuroraLayout = when (tab) {
    LiptonTab.HOME, null -> AuroraLayout.HERO
    LiptonTab.SERVERS    -> AuroraLayout.HEADER
    LiptonTab.NEWS       -> AuroraLayout.NEWS
    LiptonTab.PROFILE    -> AuroraLayout.PROFILE
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.subEnter(): EnterTransition =
    slideInHorizontally(tween(320, easing = Tokens.Motion.standard)) { it / 4 } + fadeIn(tween(220))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.subExit(): ExitTransition =
    slideOutHorizontally(tween(260, easing = Tokens.Motion.standard)) { it / 4 } + fadeOut(tween(200))

// ─── Импорт ссылки из буфера обмена (наследие ручных подписок) ───────────────

@Composable
private fun ClipboardImportBanner(state: UiState, viewModel: MainViewModel, activity: ComponentActivity) {
    AnimatedVisibility(
        visible = state.clipboardUrl != null,
        enter = fadeIn(tween(180)) + slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { it },
        modifier = Modifier.fillMaxSize().zIndex(5f),
    ) {
        val url = state.clipboardUrl ?: return@AnimatedVisibility
        val lc = LocalLiptonColors.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = { viewModel.dismissClipboard() }),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(lc.bgSheet)
                    .navigationBarsPadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("📋 Найдена ссылка на подписку", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
                Text(url.take(60) + if (url.length > 60) "…" else "", fontSize = 12.sp, color = lc.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Green, Green3)))
                            .clickable { viewModel.importClipboardUrl(activity) }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Добавить", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(lc.cardBg)
                            .border(1.dp, lc.cardBorder, RoundedCornerShape(12.dp))
                            .clickable { viewModel.dismissClipboard() }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Пропустить", fontSize = 14.sp, color = lc.textSecondary) }
                }
            }
        }
    }
}
