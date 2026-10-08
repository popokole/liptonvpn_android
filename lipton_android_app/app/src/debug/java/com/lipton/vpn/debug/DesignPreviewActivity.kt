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
import com.lipton.vpn.UiState
import com.lipton.vpn.data.model.Server
import com.lipton.vpn.data.model.SubOverlay
import com.lipton.vpn.data.model.Subscription
import com.lipton.vpn.service.LiptonVpnService.VpnStatus
import com.lipton.vpn.ui.MainScreen
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

        setContent {
            var theme by remember { mutableStateOf(initialTheme) }
            LiptonTheme(appTheme = theme) {
                ThemeRevealHost(currentTheme = theme, onApply = { theme = it }) {
                    if (screen == "components") {
                        ComponentsGallery(stateName)
                    } else {
                        val tab = LiptonTab.fromRoute(screen) ?: LiptonTab.HOME
                        MainScreen(
                            state = fakeState(stateName, theme),
                            viewModel = viewModel,
                            activity = this,
                            startTab = tab,
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
    Server(id = "s1", protocol = "vless", address = "203.0.113.10", port = 443, remark = "🇩🇪 Германия · Франкфурт", ping = 42),
    Server(id = "s2", protocol = "vless", address = "203.0.113.11", port = 443, remark = "🇳🇱 Нидерланды · Амстердам", ping = 58),
    Server(id = "s3", protocol = "vless", address = "203.0.113.12", port = 443, remark = "🇫🇮 Финляндия · Хельсинки", ping = 71),
    Server(id = "s4", protocol = "vless", address = "203.0.113.13", port = 443, remark = "⚖️ Авто-баланс", ping = 39),
)

private fun fakeState(name: String, theme: AppTheme): UiState {
    val base = UiState(
        loading = false,
        isAuthed = true,
        themeMode = theme,
        subscriptions = listOf(Subscription(id = "sub", url = "https://example.invalid/sub", servers = fakeServers)),
        activeServerId = "s1",
        accountStatus = "active",
        accountPeriodEnd = isoIn(24),
        accountTariffCode = "base",
    )
    return when (name) {
        "off" -> base.copy(status = VpnStatus.DISCONNECTED)
        "nosub" -> base.copy(status = VpnStatus.DISCONNECTED, accountStatus = "expired", accountNoSub = true)
        "bypass" -> base.copy(
            status = VpnStatus.CONNECTED,
            accountOverlay = SubOverlay(tariffTitle = "Обход глушилок", until = isoIn(24), revertTariffTitle = "Базовый"),
        )
        else -> base.copy(status = VpnStatus.CONNECTED)
    }
}

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
