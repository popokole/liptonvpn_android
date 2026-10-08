package com.lipton.vpn.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.NewsState
import com.lipton.vpn.StatsState
import com.lipton.vpn.TrialSession
import com.lipton.vpn.UiState
import com.lipton.vpn.data.SplitMode
import com.lipton.vpn.data.model.AiMessage
import com.lipton.vpn.data.model.AppBanner
import com.lipton.vpn.ui.components.ForceUpdateScreen
import com.lipton.vpn.data.model.ArticleDetail
import com.lipton.vpn.data.model.ArticleSummary
import com.lipton.vpn.data.model.NotificationPrefs
import com.lipton.vpn.data.model.TxItem
import com.lipton.vpn.ui.screens.ScreenFixtures
import com.lipton.vpn.data.TrafficLedger
import com.lipton.vpn.data.model.AppConfig
import com.lipton.vpn.data.model.DeviceItem
import com.lipton.vpn.data.model.IpInfo
import com.lipton.vpn.data.model.MeProfile
import com.lipton.vpn.data.model.NewsItem
import com.lipton.vpn.data.model.Server
import com.lipton.vpn.data.model.ServerStatus
import com.lipton.vpn.data.model.ServerStatusList
import com.lipton.vpn.data.model.SubOverlay
import com.lipton.vpn.data.model.Subscription
import com.lipton.vpn.data.model.Tariff
import com.lipton.vpn.data.model.TariffPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import com.lipton.vpn.service.LiptonVpnService.VpnStatus
import com.lipton.vpn.ui.MainScreen
import com.lipton.vpn.ui.auth.AuthFlow
import com.lipton.vpn.ui.auth.AuthStep
import com.lipton.vpn.ui.auth.LoginSuccessScreen
import com.lipton.vpn.data.model.GuestTrialConfig
import com.lipton.vpn.ui.components.AuroraBackground
import com.lipton.vpn.ui.components.AuroraLayout
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.ButtonTone
import com.lipton.vpn.ui.components.Capsule
import com.lipton.vpn.ui.components.CapsuleTone
import com.lipton.vpn.ui.components.GhostButton
import com.lipton.vpn.ui.components.GlassButton
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.GlassGroup
import com.lipton.vpn.ui.components.GlassPillButton
import com.lipton.vpn.ui.components.GroupDivider
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.LiptonLogo
import com.lipton.vpn.ui.components.LiptonSwitch
import com.lipton.vpn.ui.components.LiptonTab
import com.lipton.vpn.ui.components.ListRow
import com.lipton.vpn.ui.components.PrimaryButton
import com.lipton.vpn.ui.components.ScreenHorizontalPadding
import com.lipton.vpn.ui.components.ScreenTitle
import com.lipton.vpn.ui.components.SectionHeader
import com.lipton.vpn.ui.components.SegmentOption
import com.lipton.vpn.ui.components.Segmented
import com.lipton.vpn.ui.components.TabTopBar
import com.lipton.vpn.ui.theme.AppTheme
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.LocalThemeChange
import com.lipton.vpn.ui.theme.ThemeRevealHost
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Витрина каркаса редизайна (только debug): вкладки на тестовых данных без входа
 * и компоненты дизайн-системы. Для скриншотов и ручной проверки:
 *
 *   adb shell am start -n com.lipton.vpn/.debug.DesignPreviewActivity \
 *       --es screen home|servers|news|profile|components \
 *       --es theme dark|light|system --es state on|off|nosub|bypass
 *
 * Онбординг и вход: screen = onb-welcome | onb-howto | onb-start | onb-login | onb-signup |
 * onb-site-code | onb-email-code | onb-telegram | onb-trial | onb-success (state=noguest —
 * гостевой доступ выключен в /config).
 *
 * Тестовые данные: профиль, устройства, тарифы, статистика, новости, статус серверов
 * (адреса — из документационных диапазонов RFC 5737).
 */
class DesignPreviewActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val screen = intent.getStringExtra("screen") ?: "home"
        val initialTheme = when (intent.getStringExtra("theme")) {
            "light" -> AppTheme.LIGHT
            "system" -> AppTheme.SYSTEM
            else -> AppTheme.DARK
        }
        val stateName = intent.getStringExtra("state") ?: "on"
        val routeArg = intent.getStringExtra("route")
        val route = when (routeArg) {
            "unlink" -> "payment-method"
            "email2" -> "email"
            "article" -> "kb/vpn-ne-podklyuchaetsya"
            else -> routeArg
        }
        val fixtures = fakeFixtures(routeArg)

        setContent {
            var theme by remember { mutableStateOf(initialTheme) }
            LiptonTheme(appTheme = theme) {
                ThemeRevealHost(currentTheme = theme, onApply = { theme = it }) {
                    if (screen == "components") {
                        ComponentsGallery(stateName)
                    } else if (stateName == "update") {
                        ForceUpdateScreen(fakeBanners.first { it.kind == "update" && !it.dismissible }, onUpdate = {}, onBack = {})
                    } else if (screen == "onb-success") {
                        LoginSuccessScreen(state = fakeState(stateName, theme), onContinue = {})
                    } else if (screen.startsWith("onb-")) {
                        val step = when (screen) {
                            "onb-howto" -> AuthStep.HOWTO
                            "onb-start" -> AuthStep.START
                            "onb-login", "onb-signup" -> AuthStep.LOGIN
                            "onb-site-code" -> AuthStep.SITE_CODE
                            "onb-email-code" -> AuthStep.EMAIL_CODE
                            "onb-telegram" -> AuthStep.TELEGRAM
                            "onb-trial" -> AuthStep.TRIAL
                            else -> AuthStep.WELCOME
                        }
                        AuthFlow(
                            state = UiState(loading = false, themeMode = theme, appConfig = fakeConfig.copy(guestTrial = GuestTrialConfig(stateName != "noguest", 15), supportBot = "@liptonvpn_bot")),
                            vm = viewModel,
                            activity = this,
                            start = step,
                            signupStart = screen == "onb-signup",
                            newsFlow = remember { MutableStateFlow(fakeNews()) },
                            previewEmail = "user@example.com",
                            offline = true,
                        )
                    } else {
                        val tab = LiptonTab.fromRoute(screen) ?: LiptonTab.HOME
                        MainScreen(
                            state = fakeState(stateName, theme),
                            viewModel = viewModel,
                            activity = this,
                            startTab = tab,
                            statsFlow = remember(stateName) { MutableStateFlow(fakeStats(stateName)) },
                            newsFlow = remember { MutableStateFlow(fakeNews()) },
                            startRoute = route,
                            fixtures = fixtures,
                        )
                    }
                }
            }
        }
    }
}

private fun isoIn(days: Int): String {
    val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    return f.format(Date(System.currentTimeMillis() + days * 86_400_000L + 3_600_000L))
}

// Адреса — из документационного диапазона TEST-NET-3 (RFC 5737), не реальные.
private val fakeServers = listOf(
    Server(id = "s4", protocol = "vless", address = "203.0.113.13", port = 443, remark = "⚖️ Авто-баланс", ping = 38),
    Server(id = "s1", protocol = "vless", address = "203.0.113.10", port = 443, remark = "🇩🇪 Быстрый сервер", ping = 41),
    Server(id = "s2", protocol = "vless", address = "203.0.113.11", port = 443, remark = "🇩🇪 Германия · Франкфурт", ping = 42),
    Server(id = "s3", protocol = "vless", address = "203.0.113.12", port = 443, remark = "🇳🇱 Нидерланды · Амстердам", ping = 142),
)

private val fakeBypassServers = listOf(
    Server(id = "b1", protocol = "vless", address = "203.0.113.20", port = 443, remark = "🇩🇪 Обход · Германия", ping = 46),
    Server(id = "b2", protocol = "vless", address = "203.0.113.21", port = 443, remark = "🇳🇱 Обход · Нидерланды", ping = 64),
)

private val fakeConfig = AppConfig(
    tariffs = listOf(
        Tariff(code = "base", title = "Базовый", periodDays = 30, priceKopeks = 15900, periods = listOf(
            TariffPeriod("p30", 30, 15900), TariffPeriod("p90", 90, 39900), TariffPeriod("p365", 365, 119900),
        ), deviceLimit = 5),
        Tariff(code = "bypass", title = "Обход глушилок", periodDays = 30, priceKopeks = 49900, periods = listOf(
            TariffPeriod("b30", 30, 49900),
        )),
    ),
    trialDays = 3,
)

private fun fakeStats(name: String): StatsState {
    val now = System.currentTimeMillis()
    val days = mutableMapOf<String, LongArray>()
    val gb = 1_000_000_000L
    listOf(1.2, 0.9, 1.6, 1.1, 2.0, 1.0, 1.8).forEachIndexed { i, v ->
        val key = TrafficLedger.dayKey(now - (6 - i) * 86_400_000L)
        val total = (v * gb).toLong()
        days[key] = longArrayOf(total * 8 / 9, total / 9)
    }
    val on = name == "on" || name == "bypass" || name == "guest" || name == "daily"
    return StatsState(
        connectedAt = if (on) now - (45 * 60 + 28) * 1000L else 0L,
        downBps = if (on) 23_300_000 else 0, upBps = if (on) 3_100_000 else 0,
        speedHistory = if (on) listOf(9, 12, 10, 15, 13, 17, 14, 19, 16, 21, 18, 23).map { it * 1_000_000L } else emptyList(),
        pingMs = if (on) 42 else null,
        pingHistory = if (on) listOf(41, 43, 40, 42, 44, 45, 43, 41, 42, 44, 42).map { it.toLong() } else emptyList(),
        trafficDays = days,
        ip = if (on) IpInfo(ip = "203.0.113.42", country = "Германия", countryCode = "DE", city = "Франкфурт")
             else IpInfo(ip = "198.51.100.77", country = "Россия", countryCode = "RU", city = "Москва"),
        ipViaVpn = on,
        ipv6Available = true,
    )
}

private fun fakeNews(): NewsState = NewsState(
    items = listOf(
        NewsItem("n1", "Смена тарифа прямо в приложении", "Сменить тариф или срок можно без поддержки: остаток подписки засчитывается, а на доплату — скидка 10%.", null, isoIn(-1)),
        NewsItem("n2", "Временный «Обход глушилок»", "Подключите его на 30 дней поверх годовой или квартальной подписки — потом вернётся прежний тариф.", null, isoIn(-6)),
        NewsItem("n3", "Вход по коду с сайта", "Без пароля: в кабинете на сайте откройте «Подключение» → «Получить код» и введите цифры в приложении.", null, isoIn(-14)),
        NewsItem("n4", "Если VPN не подключается", "Нажмите «Обновить ссылку» в профиле или выберите «Авто-баланс» — он сам найдёт рабочий сервер.", null, isoIn(-26)),
    ),
    readIds = setOf("n3", "n4"),
    status = ServerStatusList(
        servers = listOf(
            ServerStatus("Авто-баланс", null, "up"), ServerStatus("Быстрый сервер", "DE", "up"),
            ServerStatus("Лучшая скорость", "DE", "up"), ServerStatus("Германия", "DE", "up"), ServerStatus("Нидерланды", "NL", "up"),
        ),
    ),
    statusAt = System.currentTimeMillis() - 60_000L,
    loadedAt = System.currentTimeMillis(),
)

private fun fakeState(name: String, theme: AppTheme): UiState {
    val base = UiState(
        loading = false,
        isAuthed = true,
        themeMode = theme,
        subscriptions = listOf(Subscription(id = "sub", url = "https://example.invalid/sub", servers = fakeServers)),
        activeServerId = "s4",
        accountStatus = "active",
        accountPeriodEnd = isoIn(24),
        accountTariffCode = "base",
        appConfig = fakeConfig,
        subscriptionUrl = "https://example.invalid/sub/4f9c2a7d8f3a",
        linkVersion = 2,
        linkUpdatedAt = isoIn(-7),
        hwid = "hw-this",
        lastPingAt = System.currentTimeMillis(),
        autoConnectOnLaunch = true,
        bypassDomains = listOf("example.org", "example.net", "corp.example.com"),
        bypassDomainDates = mapOf("example.org" to System.currentTimeMillis() - 2 * 86_400_000L, "example.net" to System.currentTimeMillis() - 2 * 86_400_000L),
        notifPrefs = NotificationPrefs(paymentReminders = true, news = true, telegramMessages = false),
        splitTunnelMode = SplitMode.BYPASS_SELECTED,
        splitTunnelApps = listOf("com.android.chrome"),
        logLines = listOf(
            "[14:01:58] >> Lipton VPN 1.3.0 · Android 16 · VPN: выключен · обход РФ: вкл",
            "[14:02:11] >> Обход РУ трафика: включён",
            "[14:02:15] >> Подключение к: Авто-баланс",
            "[14:02:16] >> VPN подключён",
            "[14:02:17] Пинг Германия: 41 мс",
            "[14:02:17] [Warning] пинг Нидерланды: 142 мс",
            "[14:02:18] Пинг Авто-баланс: 38 мс",
            "[14:03:40] Сессия: 13,5 МБ",
        ),
        me = MeProfile(
            email = "user@example.com", telegramLinked = true, tgUsername = "user",
            hasCard = true, cardLast4 = "4242", createdAt = isoIn(-210),
            nextChargeKopeks = 15900, nextChargePeriodDays = 30, nextChargeAt = isoIn(24),
            cardBrand = "visa", cardExp = "08/28", nextChargeTariffTitle = "Базовый",
        ),
        devices = listOf(
            DeviceItem(hwid = "hw-this", platform = "Android", model = "Pixel 8", updatedAt = isoIn(0)),
            DeviceItem(hwid = "hw-2", platform = "Windows", model = "DESKTOP-7K2", updatedAt = isoIn(-1)),
            DeviceItem(hwid = "hw-3", platform = "iOS", model = "iPhone 15", app = "Happ", updatedAt = isoIn(-5)),
        ),
        deviceLimit = 5,
    )
    val now = System.currentTimeMillis()
    val guestBase = base.copy(
        isAuthed = false, accountStatus = null, accountPeriodEnd = null, accountTariffCode = null,
        me = null, devices = null, subscriptionUrl = null, notifPrefs = null,
    )
    return when (name) {
        "guest" -> guestBase.copy(
            status = VpnStatus.CONNECTED,
            guest = TrialSession(now + (12 * 60 + 34) * 1000L, 15, "Авто-баланс", "https://example.invalid/guest"),
        )
        "guest-ended" -> guestBase.copy(
            status = VpnStatus.DISCONNECTED, subscriptions = emptyList(),
            guest = TrialSession(now - 1000L, 15, "Авто-баланс", null, ended = true),
        )
        "daily" -> base.copy(
            status = VpnStatus.CONNECTED, accountStatus = "none", accountPeriodEnd = null, accountNoSub = true,
            dailyTrial = TrialSession(now + (9 * 60 + 12) * 1000L, 15, null, "https://example.invalid/daily"),
        )
        "nocard" -> base.copy(status = VpnStatus.DISCONNECTED, me = base.me?.copy(hasCard = false, cardLast4 = null))
        "cooldown" -> base.copy(status = VpnStatus.CONNECTED, cardUnlinkAvailableAt = isoIn(1))
        "banner" -> base.copy(status = VpnStatus.CONNECTED, banners = fakeBanners.filter { it.kind != "screen" && it.dismissible })
        "screen" -> base.copy(status = VpnStatus.CONNECTED, banners = fakeBanners.filter { it.kind == "screen" })
        "off" -> base.copy(status = VpnStatus.DISCONNECTED)
        "nosub" -> base.copy(status = VpnStatus.DISCONNECTED, accountStatus = "expired", accountNoSub = true)
        "bypass" -> base.copy(
            status = VpnStatus.CONNECTED,
            accountTariffCode = "bypass",
            subscriptions = listOf(Subscription(id = "sub", url = "https://example.invalid/sub", servers = fakeBypassServers + fakeServers)),
            activeServerId = "b1",
        )
        else -> base.copy(status = VpnStatus.CONNECTED)
    }
}

/** Баннеры из админки для витрины: в слоте, полноэкранный и обязательное обновление. */
private val fakeBanners = listOf(
    AppBanner("b1", "banner", "promo", "«Обход глушилок» на 30 дней", "Подключите поверх годовой подписки — потом вернётся прежний тариф.", "Подробнее", "/app/billing", true, 5),
    AppBanner("b2", "update", "info", "Доступна версия 1.4.0", "Новый дизайн, гостевой доступ и раздельное туннелирование.", "Обновить", null, true, 3),
    AppBanner("s1", "screen", "promo", "Новый тариф «Обход глушилок»", "Работает, когда обычный VPN глушат. Попробуйте 30 дней — потом вернётся прежний тариф.", "Подробнее", "/app/billing", true, 9),
    AppBanner("u1", "update", "warning", "Обновите приложение", "Эта версия больше не поддерживается. Установите новую — подписка и настройки сохранятся.", "Обновить", null, false, 10),
)

/** Тестовые данные экранов, которые сами ходят в API. */
private fun fakeFixtures(route: String?): ScreenFixtures = ScreenFixtures(
    transactions = listOf(
        TxItem("t1", "renewal", 15900, "succeeded", null, isoIn(0), "Базовый", 30),
        TxItem("t2", "change", 31100, "succeeded", null, isoIn(-6), "Обход", null),
        TxItem("t3", "renewal", 15900, "succeeded", null, isoIn(-25), "Базовый", 30),
        TxItem("t4", "renewal", 15900, "failed", "Недостаточно средств", isoIn(-25), "Базовый", 30),
        TxItem("t5", "initial", 119900, "succeeded", null, isoIn(-79), "Базовый", 365),
        TxItem("t6", "refund", 39900, "succeeded", null, isoIn(-210), "Базовый", 90),
    ),
    articles = listOf(
        ArticleSummary("vpn-ne-podklyuchaetsya", "VPN не подключается — что делать", "Подключение", 3),
        ArticleSummary("iphone-happ", "Как подключить iPhone через Happ", "Устройства", 2),
        ArticleSummary("novyj-telefon", "Как перенести подписку на новый телефон", "Устройства", 2),
        ArticleSummary("drugaya-strana", "Почему сайты видят другую страну", "Обход блокировок", 1),
        ArticleSummary("oplata-kartoj", "Как оплатить и отвязать карту", "Оплата", 2),
    ),
    article = ArticleDetail(
        "vpn-ne-podklyuchaetsya", "VPN не подключается — что делать", "Подключение", 3, null,
        "<p>Чаще всего помогает одно из трёх действий.</p><h3>1. Обновите ссылку</h3><p>В профиле нажмите «Обновить ссылку» — приложение подтянет её само.</p><h3>2. Выберите Авто-баланс</h3><p>Он сам найдёт рабочий сервер.</p><h3>3. Напишите нам</h3><p>Чат поддержки отвечает сразу.</p>",
    ),
    dialog = listOf(
        AiMessage("user", "Не подключается на iPhone в Happ, пишет timeout. Вчера всё работало.", isoIn(0), "m1"),
        AiMessage("assistant", "Давайте проверим по шагам:\n1. В Happ нажмите «Обновить подписку».\n2. Выберите «Авто-баланс».\n3. Переподключитесь.", isoIn(0), "m2"),
    ),
    showUnlinkSheet = route == "unlink",
    emailStep2 = if (route == "email2") "new@example.com" else null,
)

@Composable
private fun ComponentsGallery(stateName: String) {
    var tone by remember {
        mutableStateOf(
            when (stateName) {
                "off" -> AuroraTone.OFF
                "nosub" -> AuroraTone.NO_SUB
                "bypass" -> AuroraTone.BYPASS
                else -> AuroraTone.ON
            }
        )
    }
    var sw1 by remember { mutableStateOf(true) }
    var sw2 by remember { mutableStateOf(false) }
    val changeTheme = LocalThemeChange.current
    val currentTheme = if (LiptonTheme.colors.isDark) AppTheme.DARK else AppTheme.LIGHT

    AuroraBackground(tone = tone, layout = AuroraLayout.PROFILE) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding),
        ) {
            TabTopBar { Capsule("24 дня", tone = CapsuleTone.ACCENT, icon = LiptonIcons.Calendar) }
            ScreenTitle("Компоненты")

            SectionHeader("Свечение", Modifier.padding(top = 24.dp))
            Spacer(Modifier.height(8.dp))
            Segmented(
                options = listOf(
                    SegmentOption(AuroraTone.ON, "Вкл"),
                    SegmentOption(AuroraTone.OFF, "Выкл"),
                    SegmentOption(AuroraTone.NO_SUB, "Нет"),
                    SegmentOption(AuroraTone.BYPASS, "Обход"),
                ),
                selected = tone,
                onSelect = { t, _ -> tone = t },
            )

            SectionHeader("Тема", Modifier.padding(top = 24.dp))
            Spacer(Modifier.height(8.dp))
            Segmented(
                options = listOf(
                    SegmentOption(AppTheme.DARK, AppTheme.DARK.title, LiptonIcons.Moon),
                    SegmentOption(AppTheme.LIGHT, AppTheme.LIGHT.title, LiptonIcons.Sun),
                ),
                selected = currentTheme,
                onSelect = { t, c -> changeTheme(t, c) },
            )

            SectionHeader("Капсулы и логотип", Modifier.padding(top = 24.dp))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                LiptonLogo(size = 40.dp)
                Capsule("Базовый", secondary = "24 дня", tone = CapsuleTone.ACCENT)
                Capsule("15 мин", tone = CapsuleTone.WARN)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Capsule("Обход · 24 дня", tone = CapsuleTone.BYPASS)
                Capsule("Нет подписки", tone = CapsuleTone.WARN)
                Capsule("Авто-баланс", tone = CapsuleTone.NEUTRAL)
            }

            SectionHeader("Кнопки", Modifier.padding(top = 24.dp))
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                PrimaryButton("Подключить", onClick = {}, icon = LiptonIcons.Power, tone = ButtonTone.WARN, modifier = Modifier.fillMaxWidth(0.6f))
                GlassPillButton("Отключить", onClick = {}, icon = LiptonIcons.Power, modifier = Modifier.fillMaxWidth(0.6f))
                PrimaryButton("Оформить подписку", onClick = {}, tone = ButtonTone.BYPASS, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassButton("Продлить", onClick = {})
                    GhostButton("Ввести промокод", onClick = {})
                }
            }

            SectionHeader("Стекло и строки", Modifier.padding(top = 24.dp))
            Spacer(Modifier.height(8.dp))
            GlassGroup {
                ListRow("Автоподключение", subtitle = "При запуске приложения", icon = LiptonIcons.Bolt,
                    onClick = { sw1 = !sw1 }, trailing = { LiptonSwitch(checked = sw1) })
                GroupDivider()
                ListRow("Обход российских сайтов", subtitle = "Банки и госсервисы — напрямую", icon = LiptonIcons.ShieldCheck,
                    onClick = { sw2 = !sw2 }, trailing = { LiptonSwitch(checked = sw2) })
                GroupDivider()
                ListRow("База знаний", icon = LiptonIcons.Book, onClick = {})
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(Modifier.weight(1f)) {
                    Text("СКОРОСТЬ", style = MaterialTheme.typography.labelSmall, color = LiptonTheme.colors.text3)
                    Text("186,4", style = MaterialTheme.typography.displayMedium, color = LiptonTheme.colors.text)
                    Text("Мбит/с", style = MaterialTheme.typography.bodySmall, color = LiptonTheme.colors.text2)
                }
                GlassCard(Modifier.weight(1f)) {
                    Text("ТАРИФ", style = MaterialTheme.typography.labelSmall, color = LiptonTheme.colors.text3)
                    Text("24", style = MaterialTheme.typography.displayMedium, color = LiptonTheme.colors.text)
                    Text("из 30 дней", style = MaterialTheme.typography.bodySmall, color = LiptonTheme.colors.text2)
                }
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                Text("00:45:28", style = MaterialTheme.typography.displayLarge, color = LiptonTheme.colors.text)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
