package com.lipton.vpn.ui.tabs

import android.graphics.BitmapFactory
import android.util.Base64
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.BuildConfig
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.UiState
import com.lipton.vpn.data.model.DeviceItem
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.ConfirmDialog
import com.lipton.vpn.ui.components.GlassButton
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.GlassGroup
import com.lipton.vpn.ui.components.GradientProgress
import com.lipton.vpn.ui.components.GroupDivider
import com.lipton.vpn.ui.components.IconTile
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.LiptonSwitch
import com.lipton.vpn.ui.components.ListRow
import com.lipton.vpn.ui.components.ScreenHorizontalPadding
import com.lipton.vpn.ui.components.ScreenTitle
import com.lipton.vpn.ui.components.SectionHeader
import com.lipton.vpn.ui.components.SegmentOption
import com.lipton.vpn.ui.components.Segmented
import com.lipton.vpn.ui.components.StateTone
import com.lipton.vpn.ui.components.TabTopBar
import com.lipton.vpn.ui.components.TileHeader
import com.lipton.vpn.ui.components.ToneChip
import com.lipton.vpn.ui.components.ToneCircleIcon
import com.lipton.vpn.ui.components.accentDeepOrAccent
import com.lipton.vpn.ui.components.glass
import com.lipton.vpn.ui.components.softGlow
import com.lipton.vpn.ui.components.stateText
import com.lipton.vpn.ui.components.stateTone
import com.lipton.vpn.ui.components.tabBarBottomPadding
import com.lipton.vpn.ui.components.topAccentLine
import com.lipton.vpn.ui.theme.AppTheme
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.LocalThemeChange
import com.lipton.vpn.ui.theme.TABULAR_NUMS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

/** Переходы из профиля в подэкраны и наружу. */
class ProfileActions(
    val onPay: () -> Unit,
    val onChangeTariff: () -> Unit,
    val onSupport: () -> Unit,
    val onFaq: () -> Unit,
    val onLogs: () -> Unit,
    val onDomains: () -> Unit,
    val onSplitTunnel: () -> Unit,
    val onConnectionCheck: () -> Unit,
    val onPaymentsHistory: () -> Unit,
    val onPaymentMethod: () -> Unit,
    val onOpenUrl: (String) -> Unit,
)

const val SITE_URL = "https://liptonone.online"

/**
 * Вкладка «Профиль» по макету new-combo-profile-full: шапка (аватар Telegram,
 * почта, @username, тариф), подписка («Продлить» / «Сменить тариф»), ссылка
 * подписки и «Обновить ссылку», устройства N/5, VPN, приложение, аккаунт,
 * оплата, помощь, о приложении, «Отменить подписку» и «Выйти».
 * Нет по решению владельца: автопродления (только «Отвязать карту»), Kill Switch,
 * автозапуска, «Удалить аккаунт».
 */
@Composable
fun ProfileTab(
    state: UiState,
    viewModel: MainViewModel,
    activity: ComponentActivity,
    actions: ProfileActions,
) {
    val c = LiptonTheme.colors
    val scope = rememberCoroutineScope()
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }   // relink | revoke:<hwid> | revoke_all | cancel | reset
    var updateText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.loadProfile() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = ScreenHorizontalPadding),
    ) {
        TabTopBar { SubscriptionCapsule(state) }
        ScreenTitle("Профиль")
        Spacer(Modifier.height(24.dp))

        ProfileHeader(state)
        Spacer(Modifier.height(24.dp))

        SubscriptionCard(state, actions)
        if (!state.subscriptionUrl.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            LinkCard(state, busy = state.profileBusy == "relink", onCopied = { viewModel.showError("Ссылка скопирована") }) { confirm = "relink" }
        }

        // ── Устройства ──
        if (hasActiveSubscription(state) || !state.devices.isNullOrEmpty()) {
            Section("Устройства") {
                DevicesGroup(state, onRevoke = { confirm = "revoke:$it" }, onRevokeAll = { confirm = "revoke_all" })
            }
        }

        // ── VPN ──
        Section("VPN") {
            GlassGroup {
                ToggleRow("Подключаться при запуске", "Как только откроете приложение", LiptonIcons.Bolt, state.autoConnectOnLaunch) {
                    viewModel.setAutoConnectOnLaunch(it)
                }
                GroupDivider()
                ToggleRow("Обход российских сайтов", "Российские сайты — напрямую, остальное через VPN", LiptonIcons.Branch, state.bypassRu) {
                    viewModel.setBypassRu(it)
                }
                GroupDivider()
                val n = state.bypassDomains.size
                ListRow(
                    "Свои домены для обхода",
                    subtitle = if (n == 0) "Открываются без VPN" else "$n ${pluralRu(n, "домен", "домена", "доменов")} · открываются без VPN",
                    icon = LiptonIcons.ListIcon,
                    onClick = actions.onDomains,
                )
                GroupDivider()
                val apps = state.splitTunnelApps.size
                ListRow(
                    "Раздельное туннелирование",
                    subtitle = if (apps == 0) "Все приложения через VPN" else "$apps ${pluralRu(apps, "приложение", "приложения", "приложений")} мимо VPN",
                    icon = LiptonIcons.Apps,
                    onClick = actions.onSplitTunnel,
                )
                GroupDivider()
                ListRow("Проверка соединения", subtitle = "Что видят сайты, утечки IPv6 и DNS", icon = LiptonIcons.Pulse, onClick = actions.onConnectionCheck)
            }
        }

        // ── Приложение ──
        Section("Приложение") {
            GlassGroup {
                ThemeBlock(state.themeMode)
                GroupDivider()
                ToggleRow("Уведомления", "О подписке и новостях", LiptonIcons.Bell, state.notificationsEnabled) {
                    viewModel.setNotificationsEnabled(it)
                }
                GroupDivider()
                ToggleRow("Тактильный отклик", "Вибрация при подключении", LiptonIcons.Vibrate, state.hapticEnabled) {
                    viewModel.setHapticEnabled(it)
                }
                GroupDivider()
                // TODO(redesign): других языков пока нет — строка без перехода.
                ListRow("Язык", icon = LiptonIcons.Translate, trailing = { TrailingText("Русский") })
            }
        }

        // ── Аккаунт ──
        val me = state.me
        Section("Аккаунт") {
            GlassGroup {
                ListRow(
                    "Почта",
                    subtitle = me?.email?.takeIf { it.isNotBlank() } ?: "не привязана",
                    icon = LiptonIcons.Mail,
                    // TODO(redesign): нативная смена почты (new-scr-email, /auth/link/request-code + /auth/email/change) — пакет A4 ч.2; пока — кабинет на сайте.
                    trailing = { GlassButton(if (me?.email.isNullOrBlank()) "Привязать" else "Изменить", onClick = { actions.onOpenUrl("$SITE_URL/app/settings") }) },
                )
                GroupDivider()
                val tg = me?.tgUsername?.takeIf { it.isNotBlank() }
                ListRow(
                    "Telegram",
                    subtitle = when {
                        tg != null -> "@$tg · вход через бота"
                        me?.telegramLinked == true -> "привязан · вход через бота"
                        else -> "не привязан"
                    },
                    icon = LiptonIcons.Send,
                    // TODO(redesign): «Отвязать» (GET/DELETE /auth/identities) — пакет A4 ч.2.
                )
                ruDayMonthYear(me?.createdAt)?.let { since ->
                    GroupDivider()
                    ListRow("С нами", icon = LiptonIcons.Calendar, trailing = { TrailingText("с $since") })
                }
                GroupDivider()
                ListRow(
                    "Пригласить друга",
                    subtitle = "Бонусные дни за приглашения — на сайте",
                    icon = LiptonIcons.Gift,
                    onClick = { actions.onOpenUrl("$SITE_URL/app/referral") },
                )
            }
        }

        // ── Оплата ──
        Section("Оплата") {
            GlassGroup {
                ListRow("История платежей", subtitle = "Оплаты и продления", icon = LiptonIcons.Receipt, onClick = actions.onPaymentsHistory)
                GroupDivider()
                val last4 = me?.cardLast4?.takeIf { me.hasCard && it.isNotBlank() }
                ListRow(
                    "Способ оплаты",
                    icon = LiptonIcons.Card,
                    onClick = actions.onPaymentMethod,
                    trailing = {
                        TrailingText(if (last4 != null) "•••• $last4" else "не привязан")
                        Spacer(Modifier.width(6.dp))
                        Icon(LiptonIcons.ChevronRight, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    },
                )
            }
        }

        // ── Помощь ──
        Section("Помощь") {
            GlassGroup {
                ListRow("Чат поддержки", subtitle = "ИИ-помощник отвечает сразу", icon = LiptonIcons.Chat, onClick = actions.onSupport)
                GroupDivider()
                ListRow("База знаний", subtitle = "Инструкции и частые вопросы", icon = LiptonIcons.Book, onClick = actions.onFaq)
                GroupDivider()
                ListRow("Логи приложения", subtitle = "Пригодятся поддержке", icon = LiptonIcons.Terminal, onClick = actions.onLogs)
                GroupDivider()
                ListRow(
                    "Сбросить подключение VPN",
                    subtitle = "Удалит профиль VPN и настройки подключения на этом устройстве",
                    icon = LiptonIcons.Refresh,
                    onClick = { confirm = "reset" },
                )
            }
        }

        // ── О приложении ──
        Section("О приложении") {
            GlassGroup {
                ListRow(
                    "Версия",
                    icon = LiptonIcons.Info,
                    onClick = {
                        updateText = "проверяем…"
                        scope.launch {
                            val has = try { viewModel.manualCheckUpdate() } catch (_: Exception) { null }
                            updateText = when (has) {
                                true -> "есть обновление"
                                false -> "обновлений нет"
                                null -> "не удалось проверить"
                            }
                        }
                    },
                    trailing = {
                        TrailingText(BuildConfig.VERSION_NAME + (state.updateInfo?.let { " · доступна ${it.versionName}" } ?: updateText?.let { " · $it" } ?: ""))
                        Spacer(Modifier.width(6.dp))
                        Icon(LiptonIcons.ChevronRight, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    },
                )
                GroupDivider()
                ListRow("Политика конфиденциальности", icon = LiptonIcons.Lock, onClick = { actions.onOpenUrl("$SITE_URL/legal?doc=privacy") })
                GroupDivider()
                ListRow("Публичная оферта", icon = LiptonIcons.Doc, onClick = { actions.onOpenUrl("$SITE_URL/legal?doc=offer") })
            }
        }

        Spacer(Modifier.height(28.dp))
        val paid = state.accountStatus == "active" || state.accountStatus == "grace"
        if (paid && !state.accountCanceled) {
            OutlineWarnButton("Отменить подписку", LiptonIcons.XCircle, busy = state.profileBusy == "cancel") { confirm = "cancel" }
            Spacer(Modifier.height(12.dp))
        }
        LogoutButton { viewModel.logoutAccount() }
        Text(
            "Lipton VPN · разработка popokole",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = c.text.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )
        Spacer(Modifier.height(tabBarBottomPadding()))
    }

    // ── Подтверждения ──
    when (val cf = confirm) {
        "relink" -> ConfirmDialog(
            title = "Обновить ссылку?",
            text = "Выдадим новую ссылку подписки. Этот телефон переключится на неё сам.",
            warning = "Старая ссылка и все устройства на ней отключатся, подключите их заново.",
            confirmText = "Обновить ссылку",
            icon = LiptonIcons.Refresh,
            danger = true,
            busy = state.profileBusy == "relink",
            onConfirm = { viewModel.relinkSubscription(activity); confirm = null },
            onDismiss = { confirm = null },
        )
        "revoke_all" -> ConfirmDialog(
            title = "Отвязать все устройства?",
            text = "Все устройства отключатся от подписки. Чтобы пользоваться VPN, подключите их заново — ссылка не изменится.",
            confirmText = "Отвязать все",
            icon = LiptonIcons.Unlink,
            danger = true,
            onConfirm = { viewModel.revokeAllDevices(); confirm = null },
            onDismiss = { confirm = null },
        )
        "cancel" -> ConfirmDialog(
            title = "Отменить подписку?",
            text = "Подписка закончится сразу, оставшиеся дни не возвращаются, карта удалится.",
            confirmText = "Отменить подписку",
            dismissText = "Оставить подписку",
            icon = LiptonIcons.XCircle,
            danger = true,
            onConfirm = { viewModel.cancelSubscription(activity); confirm = null },
            onDismiss = { confirm = null },
        )
        "reset" -> ConfirmDialog(
            title = "Сбросить подключение?",
            text = "VPN отключится, настройки подключения на этом устройстве (обход российских сайтов, свои домены, приложения мимо VPN, автоподключение) вернутся к исходным. Подписка и аккаунт не пострадают.",
            confirmText = "Сбросить",
            icon = LiptonIcons.Refresh,
            danger = true,
            onConfirm = { viewModel.resetProfile(activity); confirm = null },
            onDismiss = { confirm = null },
        )
        else -> if (cf != null && cf.startsWith("revoke:")) {
            val hwid = cf.removePrefix("revoke:")
            val dev = state.devices?.find { it.hwid == hwid }
            ConfirmDialog(
                title = "Отвязать устройство?",
                text = "«${dev?.let { deviceTitle(it) } ?: "Устройство"}» отключится от подписки. Подключить его снова можно по той же ссылке.",
                confirmText = "Отвязать",
                icon = LiptonIcons.Unlink,
                danger = true,
                onConfirm = { viewModel.revokeDevice(hwid); confirm = null },
                onDismiss = { confirm = null },
            )
        }
    }
}

// ─── Шапка: аватар, почта, чипы ─────────────────────────────────────────────

@Composable
private fun ProfileHeader(state: UiState) {
    val c = LiptonTheme.colors
    val me = state.me
    val tg = me?.tgUsername?.takeIf { it.isNotBlank() }
    val name = me?.email?.takeIf { it.isNotBlank() } ?: tg?.let { "@$it" } ?: "Мой аккаунт"
    val tone = auroraToneFor(state).let { if (it == AuroraTone.OFF) AuroraTone.ON else it }
    val title = state.accountOverlay?.tariffTitle?.takeIf { it.isNotBlank() }
        ?: tariffTitleFor(state.accountTariffCode, state.appConfig)
        ?: if (state.accountStatus == "trial") "Пробный период" else null
    Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Avatar(
            photoUrl = me?.tgPhotoUrl,
            dataUrl = me?.avatar,
            letter = name.trimStart('@').firstOrNull()?.uppercaseChar() ?: 'L',
            ring = c.stateTone(tone),
            telegram = me?.telegramLinked == true,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
                color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tg != null && me?.email?.isNotBlank() == true) {
                    ToneChip("@$tg · Telegram", Color(0xFF2AABEE), icon = LiptonIcons.Send, textColor = c.text.copy(alpha = 0.9f))
                }
                if (title != null) {
                    ToneChip(title, c.stateTone(tone).a, icon = LiptonIcons.Crown, textColor = c.text.copy(alpha = 0.9f))
                }
            }
        }
    }
}

/** Кэш загруженных аватаров (ключ — ссылка). */
private object AvatarCache {
    val map = ConcurrentHashMap<String, ImageBitmap>()
}

/** Аватар 64: фото из Telegram (tg_photo_url или data:-URL из /me), иначе буква на градиенте. */
@Composable
private fun Avatar(photoUrl: String?, dataUrl: String?, letter: Char, ring: StateTone, telegram: Boolean) {
    val c = LiptonTheme.colors
    val key = photoUrl?.takeIf { it.startsWith("http") } ?: dataUrl?.takeIf { it.startsWith("data:image") }
    val bitmap by produceState<ImageBitmap?>(initialValue = key?.let { AvatarCache.map[it] }, key) {
        if (key == null || value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            try {
                val bytes = if (key.startsWith("data:")) {
                    Base64.decode(key.substringAfter("base64,"), Base64.DEFAULT)
                } else {
                    okhttp3.OkHttpClient().newCall(okhttp3.Request.Builder().url(key).build()).execute().use { it.body?.bytes() }
                }
                bytes?.let { b -> BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() }?.also { AvatarCache.map[key] = it }
            } catch (_: Exception) {
                null
            }
        }
    }
    Box(Modifier.size(64.dp).semantics { contentDescription = "Фото профиля" }) {
        Box(
            Modifier
                .size(64.dp)
                .drawBehind {
                    softGlow(ring.a.copy(alpha = 0.35f), size.minDimension / 2, 10.dp.toPx())
                }
                .clip(CircleShape)
                .border(2.dp, Brush.linearGradient(listOf(ring.a, ring.b)), CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(ring.a.copy(alpha = 0.5f), ring.b.copy(alpha = 0.35f)))),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(bmp, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(letter.toString(), style = MaterialTheme.typography.headlineSmall, color = Color.White)
            }
        }
        if (telegram) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(3.dp, 3.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(c.bg)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2AABEE)),
                contentAlignment = Alignment.Center,
            ) { Icon(LiptonIcons.Send, "Telegram", tint = Color.White, modifier = Modifier.size(10.dp)) }
        }
    }
}

// ─── Подписка ────────────────────────────────────────────────────────────────

@Composable
private fun SubscriptionCard(state: UiState, actions: ProfileActions) {
    val c = LiptonTheme.colors
    val active = hasActiveSubscription(state)
    val tone = if (active) (if (isBypassTariff(state)) AuroraTone.BYPASS else AuroraTone.ON) else AuroraTone.NO_SUB
    val t = c.stateTone(tone)
    val left = daysLeft(state.accountPeriodEnd)
    val total = left?.let { periodTotalDays(it, state.accountTariffCode, state.appConfig) }
    val overlay = state.accountOverlay
    val title = when {
        !active -> "Нет подписки"
        state.accountStatus == "trial" -> "Пробный период"
        overlay != null && !overlay.tariffTitle.isNullOrBlank() -> overlay.tariffTitle
        else -> tariffTitleFor(state.accountTariffCode, state.appConfig) ?: "Подписка"
    }
    val statusText = when (state.accountStatus) {
        "active" -> if (state.accountCanceled) "отменена" else "активна"
        "trial" -> "пробный период"
        "grace" -> "льготный период"
        "expired" -> "истекла"
        null -> "загружаем…"
        else -> "не активна"
    }
    val until = ruDayMonth(state.accountPeriodEnd)
    val subtitle = when {
        !active -> "Оформите подписку, чтобы подключиться"
        else -> buildString {
            append(statusText)
            if (until != null) append(" до $until")
            if (left != null) append(" · осталось ${pluralDays(left)}")
        }
    }
    val paid = state.accountStatus == "active" || state.accountStatus == "grace"
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .glass(shape)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(t.a.copy(alpha = if (c.isDark) 0.20f else 0.12f), t.b.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(size.width, 0f), radius = size.width * 0.8f,
                    ),
                )
            }
            .topAccentLine(t)
            .padding(20.dp),
    ) {
        TileHeader(LiptonIcons.Crown, "Подписка") {
            ToneChip(statusText.replaceFirstChar { it.uppercase() }, if (active) t.a else c.warn, dot = true, textColor = if (active) c.stateText(tone) else c.stateText(AuroraTone.OFF))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS), color = c.text.copy(alpha = 0.72f), maxLines = 2)
        if (overlay != null && !overlay.revertTariffTitle.isNullOrBlank()) {
            Text(
                "потом снова «${overlay.revertTariffTitle}»",
                style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.6f),
            )
        }
        if (active && left != null && total != null) {
            Spacer(Modifier.height(16.dp))
            GradientProgress(left.toFloat() / total, t)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                val price = state.me?.nextChargeKopeks?.let { k ->
                    val d = state.me.nextChargePeriodDays
                    if (d != null) "${rubles(k)} за ${pluralDays(d)}" else rubles(k)
                } ?: state.appConfig?.tariffs?.firstOrNull { it.code == state.accountTariffCode }?.monthlyPeriod()?.let {
                    "${rubles(it.priceKopeks)} за ${pluralDays(it.days)}"
                }
                Text(price ?: "", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR_NUMS), color = c.text.copy(alpha = 0.55f), modifier = Modifier.weight(1f))
                Text("$left из ${pluralDays(total)}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR_NUMS), color = c.text.copy(alpha = 0.55f))
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WhitePill(if (active) "Продлить" else "Оформить подписку", t, Modifier.weight(1f), onClick = actions.onPay)
            if (paid) {
                GlassButton("Сменить тариф", onClick = actions.onChangeTariff, height = 44.dp, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Белая пилюля 44dp «Продлить» (тёмная тема) / графитовая (светлая). */
@Composable
private fun WhitePill(text: String, t: StateTone, modifier: Modifier, onClick: () -> Unit) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .height(44.dp)
            .drawBehind { softGlow(t.a.copy(alpha = if (c.isDark) 0.28f else 0.18f), size.height / 2, 12.dp.toPx(), offsetY = 8.dp.toPx()) }
            .clip(shape)
            .background(if (c.isDark) Brush.verticalGradient(listOf(Color.White, Color(0xFFE4FFF1))) else Brush.verticalGradient(listOf(c.text, c.text)))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = if (c.isDark) Color(0xFF050807) else Color.White, maxLines = 1)
    }
}

// ─── Ссылка подписки ─────────────────────────────────────────────────────────

@Composable
private fun LinkCard(state: UiState, busy: Boolean, onCopied: () -> Unit, onRelink: () -> Unit) {
    val c = LiptonTheme.colors
    val clipboard = LocalClipboardManager.current
    val url = state.subscriptionUrl.orEmpty()
    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        TileHeader(LiptonIcons.Link, "Ссылка подписки") {
            ruDayMonth(state.linkUpdatedAt)?.let { Text("обновлена $it", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.55f)) }
        }
        Spacer(Modifier.height(12.dp))
        val fieldShape = RoundedCornerShape(14.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(fieldShape)
                .background(c.text.copy(alpha = if (c.isDark) 0.04f else 0.04f))
                .border(1.dp, c.text.copy(alpha = 0.08f), fieldShape)
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(LiptonIcons.Link, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
            Text(
                maskSubscriptionUrl(url),
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS),
                color = c.text.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(c.text.copy(alpha = 0.05f))
                    .border(1.dp, c.text.copy(alpha = 0.10f), CircleShape)
                    .clickable(role = Role.Button) { clipboard.setText(AnnotatedString(url)); onCopied() }
                    .semantics { contentDescription = "Скопировать ссылку подписки" },
                contentAlignment = Alignment.Center,
            ) { Icon(LiptonIcons.Copy, null, tint = c.text.copy(alpha = 0.7f), modifier = Modifier.size(14.dp)) }
        }
        Spacer(Modifier.height(12.dp))
        Text("Поможет, если VPN перестал подключаться", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.72f))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth()) {
            GlassButton(
                if (busy) "Обновляем…" else "Обновить ссылку",
                onClick = onRelink,
                icon = LiptonIcons.Refresh,
                height = 44.dp,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(LiptonIcons.Alert, null, tint = c.warn, modifier = Modifier.size(14.dp).padding(top = 1.dp))
            Text(
                "Старая ссылка и все устройства на ней отключатся — подключите их заново",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = c.stateText(AuroraTone.OFF),
            )
        }
    }
}

// ─── Устройства ──────────────────────────────────────────────────────────────

/** Название устройства: «Pixel 8», «iPhone 15 · Happ». */
fun deviceTitle(d: DeviceItem): String {
    val base = d.model?.takeIf { it.isNotBlank() } ?: d.platform?.takeIf { it.isNotBlank() } ?: "Устройство"
    val app = d.app?.takeIf { it.isNotBlank() && !it.contains("lipton", ignoreCase = true) }
    return if (app != null) "$base · $app" else base
}

/** «последний вход сейчас / сегодня в 14:05 / вчера в 22:14 / 3 октября». */
fun lastSeenRu(iso: String?, nowMs: Long = System.currentTimeMillis()): String? {
    val t = iso?.let { parseRfc3339(it) } ?: return null
    val diff = nowMs - t
    if (diff < 5 * 60_000L) return "сейчас"
    val a = Calendar.getInstance().apply { timeInMillis = t }
    val n = Calendar.getInstance().apply { timeInMillis = nowMs }
    val hm = String.format(java.util.Locale.US, "%02d:%02d", a.get(Calendar.HOUR_OF_DAY), a.get(Calendar.MINUTE))
    val sameDay = a.get(Calendar.YEAR) == n.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == n.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return "сегодня в $hm"
    n.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = a.get(Calendar.YEAR) == n.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == n.get(Calendar.DAY_OF_YEAR)
    if (yesterday) return "вчера в $hm"
    return ruDayMonth(t)
}

private fun deviceIcon(d: DeviceItem): ImageVector {
    val p = (d.platform ?: d.model ?: "").lowercase()
    return if ("windows" in p || "mac" in p || "linux" in p || "desktop" in p) LiptonIcons.Monitor else LiptonIcons.Phone
}

@Composable
private fun DevicesGroup(state: UiState, onRevoke: (String) -> Unit, onRevokeAll: () -> Unit) {
    val c = LiptonTheme.colors
    val devices = state.devices
    val limit = state.deviceLimit ?: 5
    val used = devices?.size ?: state.devicesUsed ?: 0
    val t = c.stateTone(AuroraTone.ON)
    GlassGroup {
        Row(
            Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$used", style = MaterialTheme.typography.displayMedium, color = c.text)
                    Text(" / $limit", style = MaterialTheme.typography.displayMedium, color = c.text.copy(alpha = 0.5f), modifier = Modifier.padding(start = 4.dp))
                }
                val free = (limit - used).coerceAtLeast(0)
                Text(
                    buildAnnotatedString {
                        if (free > 0) {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = c.stateText(AuroraTone.ON))) { append("$free ${pluralRu(free, "место", "места", "мест")}") }
                            append(" ${pluralRu(free, "свободно", "свободно", "свободно")}")
                        } else {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = c.stateText(AuroraTone.OFF))) { append("Мест нет") }
                            append(" — отвяжите лишнее")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = c.text.copy(alpha = 0.7f),
                )
            }
            // Слоты: занятые — иконкой, свободные — пунктиром
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val shown = devices.orEmpty().take(limit)
                for (i in 0 until limit.coerceAtMost(6)) {
                    val d = shown.getOrNull(i)
                    if (d != null || i < used) {
                        ToneCircleIcon(d?.let { deviceIcon(it) } ?: LiptonIcons.Phone, if (i % 3 == 2) t.b else t.a, size = 24.dp, iconSize = 11.dp)
                    } else {
                        Box(
                            Modifier.size(24.dp).drawBehind {
                                drawCircle(
                                    c.text.copy(alpha = 0.24f), radius = size.minDimension / 2 - 0.5.dp.toPx(),
                                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx()))),
                                )
                            },
                        )
                    }
                }
            }
        }
        when {
            devices == null -> {
                GroupDivider()
                Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = t.a, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                }
            }
            devices.isEmpty() -> {
                GroupDivider()
                Text(
                    "Устройств пока нет — они появятся после первого подключения",
                    style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.6f),
                    modifier = Modifier.padding(16.dp),
                )
            }
            else -> {
                devices.forEach { d ->
                    GroupDivider()
                    DeviceRow(d, isThis = d.hwid == state.hwid, busy = state.profileBusy == "revoke:${d.hwid}", onRevoke = { onRevoke(d.hwid) })
                }
                GroupDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable(enabled = state.profileBusy == null, role = Role.Button, onClick = onRevokeAll),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.profileBusy == "revoke_all") {
                        CircularProgressIndicator(color = c.warn, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(LiptonIcons.Unlink, null, tint = c.stateText(AuroraTone.OFF), modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Отвязать все устройства", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = c.stateText(AuroraTone.OFF))
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(d: DeviceItem, isThis: Boolean, busy: Boolean, onRevoke: () -> Unit) {
    val c = LiptonTheme.colors
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(deviceIcon(d), tint = c.text.copy(alpha = 0.8f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(deviceTitle(d), style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (isThis) ToneChip("это устройство", c.stateTone(AuroraTone.ON).a, textColor = c.stateText(AuroraTone.ON), height = 20.dp)
            }
            val platform = d.platform?.takeIf { it.isNotBlank() && d.model?.isNotBlank() == true && !d.model.contains(it, true) }
            val seen = lastSeenRu(d.updatedAt)
            Text(
                listOfNotNull(platform, seen?.let { "последний вход $it" }).joinToString(" · ").ifBlank { "подключено" },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = c.text.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(c.text.copy(alpha = 0.05f))
                .border(1.dp, c.text.copy(alpha = 0.10f), CircleShape)
                .clickable(enabled = !busy, role = Role.Button, onClick = onRevoke)
                .semantics { contentDescription = "Отвязать ${deviceTitle(d)}" },
            contentAlignment = Alignment.Center,
        ) {
            if (busy) CircularProgressIndicator(color = c.text, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            else Icon(LiptonIcons.Unlink, null, tint = c.text.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
        }
    }
}

// ─── Общие строки и кнопки ──────────────────────────────────────────────────

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    SectionHeader(title, Modifier.padding(top = 28.dp))
    Spacer(Modifier.height(8.dp))
    Column(content = content)
}

@Composable
private fun ToggleRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListRow(title, subtitle = subtitle, icon = icon, onClick = { onChange(!checked) }, trailing = { LiptonSwitch(checked = checked) })
}

@Composable
private fun TrailingText(text: String) {
    val c = LiptonTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR_NUMS),
        color = c.text.copy(alpha = 0.6f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Блок «Тема» в группе «Приложение»: Тёмная / Светлая / Системная, смена — кругом из варианта. */
@Composable
private fun ThemeBlock(current: AppTheme) {
    val c = LiptonTheme.colors
    val changeTheme = LocalThemeChange.current
    val options = listOf(
        SegmentOption(AppTheme.DARK, AppTheme.DARK.title, LiptonIcons.Moon),
        SegmentOption(AppTheme.LIGHT, AppTheme.LIGHT.title, LiptonIcons.Sun),
        SegmentOption(AppTheme.SYSTEM, AppTheme.SYSTEM.title, LiptonIcons.System),
    )
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(LiptonIcons.Palette)
            Text("Тема", style = MaterialTheme.typography.titleSmall, color = c.text)
        }
        Spacer(Modifier.height(12.dp))
        Segmented(options = options, selected = current, onSelect = { theme, center -> changeTheme(theme, center) })
    }
}

@Composable
private fun OutlineWarnButton(text: String, icon: ImageVector, busy: Boolean, onClick: () -> Unit) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(c.warn.copy(alpha = 0.04f))
            .border(1.dp, c.warn.copy(alpha = 0.3f), shape)
            .clickable(enabled = !busy, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) CircularProgressIndicator(color = c.warn, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        else Icon(icon, null, tint = c.stateText(AuroraTone.OFF), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = c.stateText(AuroraTone.OFF))
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .glass(shape, highlightHeight = 26.dp)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LiptonIcons.LogIn, null, tint = c.text, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text("Выйти из аккаунта", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = c.text)
    }
}
