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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
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
import com.lipton.vpn.NewsState
import com.lipton.vpn.StatsState
import com.lipton.vpn.ui.account.PaymentScreen
import com.lipton.vpn.ui.account.TariffChangeScreen
import com.lipton.vpn.ui.components.AuroraBackground
import com.lipton.vpn.ui.components.AuroraLayout
import com.lipton.vpn.ui.components.ConnectionErrorSheet
import com.lipton.vpn.ui.components.LiptonTab
import com.lipton.vpn.ui.components.LiptonTabBar
import com.lipton.vpn.ui.components.tabBarBottomPadding
import com.lipton.vpn.ui.screens.ArticleScreen
import com.lipton.vpn.ui.screens.ConnectionCheckScreen
import com.lipton.vpn.ui.screens.DomainsScreen
import com.lipton.vpn.ui.screens.EmailScreen
import com.lipton.vpn.ui.screens.KnowledgeBaseScreen
import com.lipton.vpn.ui.screens.LocalScreenFixtures
import com.lipton.vpn.ui.screens.LogsPage
import com.lipton.vpn.ui.screens.NotificationsScreen
import com.lipton.vpn.ui.screens.PaymentMethodScreen
import com.lipton.vpn.ui.screens.PaymentsHistoryScreen
import com.lipton.vpn.ui.screens.PrivacyScreen
import com.lipton.vpn.ui.screens.PromoScreen
import com.lipton.vpn.ui.screens.ScreenFixtures
import com.lipton.vpn.ui.screens.SplitTunnelScreen
import com.lipton.vpn.ui.screens.SupportChatScreen
import com.lipton.vpn.ui.tabs.HomeTab
import com.lipton.vpn.ui.tabs.NewsTab
import com.lipton.vpn.ui.tabs.ProfileActions
import com.lipton.vpn.ui.tabs.ProfileTab
import com.lipton.vpn.ui.tabs.SITE_URL
import com.lipton.vpn.ui.tabs.ServersTab
import com.lipton.vpn.ui.tabs.auroraToneFor
import com.lipton.vpn.ui.tabs.bypassTariff
import com.lipton.vpn.ui.tabs.monthlyPeriod
import com.lipton.vpn.ui.tabs.unreadCount
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import com.lipton.vpn.ui.theme.Green
import com.lipton.vpn.ui.theme.Green3
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.LocalLiptonColors
import com.lipton.vpn.ui.theme.Tokens

private const val BOT_URL = "https://t.me/liptonvpn_bot"

/** Бот поддержки из /config (support_bot_url), иначе — основной бот. */
private fun botUrl(state: UiState): String =
    state.appConfig?.supportBotUrl?.takeIf { it.startsWith("https://") } ?: BOT_URL

/** Подэкраны поверх вкладок (своё «назад», без капсулы навигации). */
object SubRoutes {
    const val PAYMENT = "payment"
    const val TARIFF_CHANGE = "tariff-change"
    const val SUPPORT = "support"
    const val DOMAINS = "domains"
    const val LOGS = "logs"
    const val SPLIT_TUNNEL = "split-tunnel"
    const val CONNECTION_CHECK = "connection-check"
    const val PAYMENTS_HISTORY = "payments-history"
    const val PAYMENT_METHOD = "payment-method"
    const val KB = "kb"
    const val KB_ARTICLE = "kb/{slug}"
    const val EMAIL = "email"
    const val PRIVACY = "privacy"
    const val PROMO = "promo"
    const val NOTIFICATIONS = "notifications"

    /** Подэкраны в стиле профиля: под ними остаётся капсула навигации. */
    val WITH_TAB_BAR = setOf(
        DOMAINS, LOGS, SPLIT_TUNNEL, CONNECTION_CHECK, PAYMENTS_HISTORY, PAYMENT_METHOD,
        SUPPORT, KB, KB_ARTICLE, EMAIL, PRIVACY, PROMO, NOTIFICATIONS,
    )
}

/**
 * Каркас приложения после входа: свечение «Аврора» на весь экран, четыре
 * вкладки (Главная, Серверы, Новости, Профиль) в NavHost, плавающая капсула
 * навигации, подэкраны (оплата, смена тарифа, поддержка) отдельными маршрутами
 * с системной «Назад». Общие шиты и уведомления — здесь же.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    state: UiState,
    viewModel: MainViewModel,
    activity: ComponentActivity,
    startTab: LiptonTab = LiptonTab.HOME,
    statsFlow: StateFlow<StatsState> = viewModel.stats,
    newsFlow: StateFlow<NewsState> = viewModel.news,
    startRoute: String? = null,
    fixtures: ScreenFixtures? = null,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val routeTab = LiptonTab.fromRoute(route)
    // На подэкранах профиля капсула навигации остаётся; подсвечена вкладка, с которой пришли.
    val onSubWithBar = routeTab == null && route in SubRoutes.WITH_TAB_BAR
    val currentTab = routeTab ?: if (onSubWithBar) (LiptonTab.fromRoute(nav.previousBackStackEntry?.destination?.route) ?: LiptonTab.PROFILE) else null
    val imeVisible = WindowInsets.isImeVisible
    val showTabBar = currentTab != null && !imeVisible
    val news by newsFlow.collectAsState()
    val newsUnread = news.unreadCount() > 0
    val snackbarHostState = remember { SnackbarHostState() }

    // debug-витрина: сразу открыть подэкран поверх вкладки
    LaunchedEffect(startRoute) { if (startRoute != null) nav.navigate(startRoute) }

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
                try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(botUrl(state)))) } catch (_: Exception) {}
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

    val openTab: (LiptonTab) -> Unit = { tab ->
        // С подэкрана нажали на свою же вкладку — назад к ней, иначе обычный переход.
        if (onSubWithBar && tab == currentTab) nav.popBackStack(tab.route, inclusive = false) else nav.navigateToTab(tab)
    }
    val openUrl: (String) -> Unit = { url ->
        try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
    }
    val openPayment: (String?) -> Unit = { periodId ->
        viewModel.paymentPreselectPeriod = periodId
        nav.navigate(SubRoutes.PAYMENT) { launchSingleTop = true }
    }
    val sub: (String) -> Unit = { r -> nav.navigate(r) { launchSingleTop = true } }
    val onAuth: (String) -> Unit = { e -> viewModel.openAuth(e) }
    // Чат поддержки — только с аккаунтом; гостю открываем бота.
    val openSupport: () -> Unit = { if (state.isAuthed) sub(SubRoutes.SUPPORT) else openUrl(botUrl(state)) }

    CompositionLocalProvider(LocalScreenFixtures provides fixtures) {
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
                        onPay = openPayment,
                        onPromo = { sub(SubRoutes.PROMO) },
                        statsFlow = statsFlow,
                        onAuth = onAuth,
                    )
                }
                composable(LiptonTab.SERVERS.route) {
                    ServersTab(
                        state = state,
                        viewModel = viewModel,
                        activity = activity,
                        onBuyBypass = {
                            // Есть оплаченный «Базовый» — смена тарифа (в т. ч. временный «Обход»); иначе — оплата «Обхода».
                            val paid = state.accountStatus == "active" || state.accountStatus == "grace"
                            if (paid) sub(SubRoutes.TARIFF_CHANGE)
                            else openPayment(state.appConfig?.bypassTariff()?.monthlyPeriod()?.id)
                        },
                    )
                }
                composable(LiptonTab.NEWS.route) {
                    NewsTab(
                        state = state,
                        viewModel = viewModel,
                        onOpenStatus = { openUrl(SITE_URL + "/status") },
                        newsFlow = newsFlow,
                    )
                }
                composable(LiptonTab.PROFILE.route) {
                    ProfileTab(
                        state = state,
                        viewModel = viewModel,
                        activity = activity,
                        actions = ProfileActions(
                            onPay = { openPayment(null) },
                            onChangeTariff = { sub(SubRoutes.TARIFF_CHANGE) },
                            onSupport = openSupport,
                            onFaq = { sub(SubRoutes.KB) },
                            onLogs = { sub(SubRoutes.LOGS) },
                            onDomains = { sub(SubRoutes.DOMAINS) },
                            onSplitTunnel = { sub(SubRoutes.SPLIT_TUNNEL) },
                            onConnectionCheck = { sub(SubRoutes.CONNECTION_CHECK) },
                            onPaymentsHistory = { sub(SubRoutes.PAYMENTS_HISTORY) },
                            onPaymentMethod = { sub(SubRoutes.PAYMENT_METHOD) },
                            onOpenUrl = openUrl,
                            onAuth = onAuth,
                            onEmail = { sub(SubRoutes.EMAIL) },
                            onNotifications = { sub(SubRoutes.NOTIFICATIONS) },
                            onPrivacy = { sub(SubRoutes.PRIVACY) },
                        ),
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
                val back: () -> Unit = { nav.popBackStack() }
                subScreen(SubRoutes.SUPPORT) { SupportChatScreen(state, viewModel, onBack = back, onOpenBot = { openUrl(botUrl(state)) }) }
                subScreen(SubRoutes.DOMAINS) { DomainsScreen(state, viewModel, onBack = back) }
                subScreen(SubRoutes.LOGS) { LogsPage(state, viewModel, onBack = back) }
                subScreen(SubRoutes.SPLIT_TUNNEL) { SplitTunnelScreen(state, viewModel, activity, onBack = back) }
                subScreen(SubRoutes.CONNECTION_CHECK) { ConnectionCheckScreen(state, viewModel, onBack = back) }
                subScreen(SubRoutes.PAYMENTS_HISTORY) { PaymentsHistoryScreen(state, viewModel, onBack = back) }
                subScreen(SubRoutes.PAYMENT_METHOD) { PaymentMethodScreen(state, viewModel, onBack = back, onPay = { openPayment(null) }) }
                subScreen(SubRoutes.EMAIL) { EmailScreen(state, viewModel, onBack = back, onOpenSite = { openUrl("$SITE_URL/app/settings") }) }
                subScreen(SubRoutes.PRIVACY) { PrivacyScreen(state, onBack = back, onOpenFull = { openUrl("$SITE_URL/legal?doc=privacy") }) }
                subScreen(SubRoutes.PROMO) {
                    PromoScreen(state, viewModel, onBack = back, onPay = {
                        nav.navigate(SubRoutes.PAYMENT) { popUpTo(SubRoutes.PROMO) { inclusive = true } }
                    })
                }
                subScreen(SubRoutes.NOTIFICATIONS) { NotificationsScreen(state, viewModel, onBack = back) }
                subScreen(SubRoutes.KB) {
                    KnowledgeBaseScreen(state, viewModel, onBack = back, onArticle = { slug -> sub("kb/$slug") }, onSupport = openSupport)
                }
                subScreen(SubRoutes.KB_ARTICLE) { e ->
                    val slug = e.arguments?.getString("slug").orEmpty()
                    ArticleScreen(state, viewModel, slug, onBack = back, onOpenSite = { s -> openUrl("$SITE_URL/blog/$s") })
                }
            }

            // Подложка под статус-бар: прокрученное содержимое не налезает на системные значки
            run {
                val bg = LiptonTheme.colors.bg
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(bg.copy(alpha = 0.92f), bg.copy(alpha = 0f))))
                        .statusBarsPadding()
                        .height(10.dp),
                )
            }

            // Затемнение у нижнего края под капсулой навигации (как в макетах: 120dp к фону)
            AnimatedVisibility(
                visible = showTabBar,
                enter = fadeIn(tween(Tokens.Motion.TAB_FADE_MS)),
                exit = fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                val bg = LiptonTheme.colors.bg
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Brush.verticalGradient(0f to bg.copy(alpha = 0f), 0.45f to bg.copy(alpha = 0.62f), 1f to bg)),
                )
            }

            // Плавающая капсула навигации — только на вкладках
            AnimatedVisibility(
                visible = showTabBar,
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
                    badges = if (newsUnread) setOf(LiptonTab.NEWS) else emptySet(),
                )
            }

            // Уведомления об ошибках — над капсулой
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showTabBar) tabBarBottomPadding() else 16.dp)
                    .navigationBarsPadding(),
            )

            ClipboardImportBanner(state = state, viewModel = viewModel, activity = activity)
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

/** Подэкран со своим «назад» и сдвигом сбоку. */
private fun androidx.navigation.NavGraphBuilder.subScreen(route: String, content: @Composable (NavBackStackEntry) -> Unit) {
    composable(
        route,
        enterTransition = { subEnter() }, exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(160)) }, popExitTransition = { subExit() },
    ) { e -> content(e) }
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
