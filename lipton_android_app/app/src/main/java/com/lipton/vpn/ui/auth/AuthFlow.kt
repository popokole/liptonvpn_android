package com.lipton.vpn.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.NewsState
import com.lipton.vpn.TrialStart
import com.lipton.vpn.UiState
import com.lipton.vpn.data.ApiClient
import com.lipton.vpn.data.ruWhen
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.ButtonTone
import com.lipton.vpn.ui.components.FlagCircle
import com.lipton.vpn.ui.components.GhostButton
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.PrimaryButton
import com.lipton.vpn.ui.components.StatusDot
import com.lipton.vpn.ui.components.ToneCircleIcon
import com.lipton.vpn.ui.components.glass
import com.lipton.vpn.ui.components.stateText
import com.lipton.vpn.ui.components.stateTone
import com.lipton.vpn.ui.tabs.baseTariff
import com.lipton.vpn.ui.tabs.daysLeft
import com.lipton.vpn.ui.tabs.hasActiveSubscription
import com.lipton.vpn.ui.tabs.monthlyPeriod
import com.lipton.vpn.ui.tabs.pluralDays
import com.lipton.vpn.ui.tabs.ruDayMonth
import com.lipton.vpn.ui.tabs.rubles
import com.lipton.vpn.ui.tabs.tariffTitleFor
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.TABULAR_NUMS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val SITE = "https://liptonone.online"
private const val DEFAULT_BOT = "@liptonvpn_bot"

/** Шаги онбординга и входа (макеты new-onb-*). */
enum class AuthStep { WELCOME, HOWTO, START, LOGIN, SITE_CODE, EMAIL_CODE, TELEGRAM, TRIAL }

/** Точка входа по строке из UiState.authEntry (гость нажал «Создать аккаунт» / «Войти»). */
fun authStepFor(entry: String?): AuthStep = when (entry) {
    "start" -> AuthStep.START
    "login" -> AuthStep.LOGIN
    "email" -> AuthStep.LOGIN
    "telegram" -> AuthStep.TELEGRAM
    "trial" -> AuthStep.TRIAL
    else -> AuthStep.WELCOME
}

/**
 * Онбординг и вход: приветствие → как пользоваться → «Начнём знакомство»
 * (Telegram-бот или почта); вход по почте, коду с сайта или через Telegram;
 * «15 минут без регистрации». [onClose] — вернуться в гостевой режим.
 */
@Composable
fun AuthFlow(
    state: UiState,
    vm: MainViewModel,
    activity: ComponentActivity,
    start: AuthStep = AuthStep.WELCOME,
    onClose: (() -> Unit)? = null,
    signupStart: Boolean = start == AuthStep.START || start == AuthStep.TELEGRAM,
    newsFlow: StateFlow<NewsState> = vm.news,
    previewEmail: String? = null,
    offline: Boolean = false,   // debug-витрина: без запросов и без открытия бота
) {
    val stack = remember { mutableStateListOf(start) }
    var signup by rememberSaveable { mutableStateOf(signupStart) }
    var email by rememberSaveable { mutableStateOf(previewEmail.orEmpty()) }
    val step = stack.last()
    val ctx = LocalContext.current
    val openUrl: (String) -> Unit = { url -> try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {} }

    fun go(s: AuthStep) { stack.add(s) }
    fun replace(s: AuthStep) { stack[stack.lastIndex] = s }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) else onClose?.invoke() }

    BackHandler(enabled = stack.size > 1 || onClose != null) { back() }

    val cfg = state.appConfig
    val guestOn = cfg?.guestEnabled == true
    val bot = cfg?.supportBot?.takeIf { it.isNotBlank() }?.let { if (it.startsWith("@")) it else "@$it" } ?: DEFAULT_BOT
    val botUrl = cfg?.supportBotUrl?.takeIf { it.startsWith("https://") } ?: "https://t.me/${bot.removePrefix("@")}"

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            (fadeIn(tween(260)) + slideInHorizontally(tween(320)) { it / 10 }) togetherWith fadeOut(tween(180))
        },
        label = "auth_step",
    ) { s ->
        when (s) {
            AuthStep.WELCOME -> WelcomeStep(
                state = state, newsFlow = newsFlow, guestOn = guestOn,
                onTrial = { go(AuthStep.TRIAL) },
                onCreate = { signup = true; go(AuthStep.HOWTO) },
                onLogin = { signup = false; go(AuthStep.LOGIN) },
                onBack = onClose,
            )
            AuthStep.HOWTO -> HowtoStep(onBack = { back() }, onNext = { go(AuthStep.START) })
            AuthStep.START -> StartStep(
                bot = bot,
                onBack = if (stack.size > 1 || onClose != null) ({ back() }) else null,
                onLogin = { signup = false; go(AuthStep.LOGIN) },
                onTelegram = { signup = true; go(AuthStep.TELEGRAM) },
                onEmail = { signup = true; go(AuthStep.LOGIN) },
                onTerms = { openUrl("$SITE/legal?doc=offer") },
                onPrivacy = { openUrl("$SITE/legal?doc=privacy") },
            )
            AuthStep.LOGIN -> EmailStep(
                vm = vm, signup = signup, email = email, onEmail = { email = it },
                onBack = if (stack.size > 1 || onClose != null) ({ back() }) else null,
                onSent = { go(AuthStep.EMAIL_CODE) },
                onTab = { tab -> if (tab == LoginTab.CODE) replace(AuthStep.SITE_CODE) else if (tab == LoginTab.TELEGRAM) go(AuthStep.TELEGRAM) },
                onSwitchMode = { signup = !signup },
                onBot = { signup = true; go(AuthStep.TELEGRAM) },
            )
            AuthStep.SITE_CODE -> SiteCodeStep(
                vm = vm,
                onBack = if (stack.size > 1 || onClose != null) ({ back() }) else null,
                onTab = { tab -> if (tab == LoginTab.EMAIL) replace(AuthStep.LOGIN) else if (tab == LoginTab.TELEGRAM) go(AuthStep.TELEGRAM) },
                onOpenSite = { openUrl("$SITE/app/connect") },
            )
            AuthStep.EMAIL_CODE -> EmailCodeStep(vm = vm, email = email, onBack = { back() })
            AuthStep.TELEGRAM -> TelegramStep(
                vm = vm, bot = bot, botUrl = botUrl, signup = signup, offline = offline,
                onBack = if (stack.size > 1 || onClose != null) ({ back() }) else null,
                onOther = { if (stack.size > 1) back() else replace(AuthStep.LOGIN) },
                openUrl = openUrl,
            )
            AuthStep.TRIAL -> TrialStep(
                state = state, vm = vm, activity = activity,
                onBack = if (stack.size > 1 || onClose != null) ({ back() }) else null,
                onCreate = { signup = true; go(AuthStep.START) },
            )
        }
    }
}

// ─── Приветствие ─────────────────────────────────────────────────────────────

@Composable
private fun WelcomeStep(
    state: UiState,
    newsFlow: StateFlow<NewsState>,
    guestOn: Boolean,
    onTrial: () -> Unit,
    onCreate: () -> Unit,
    onLogin: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val news by newsFlow.collectAsState()
    val countries = news.status?.servers.orEmpty().mapNotNull { it.country?.uppercase()?.takeIf { c -> c.length == 2 } }.distinct()
    val tone = AuroraTone.ON
    OnbPage(
        tone = tone,
        topBar = { if (onBack != null) RoundIconButton(LiptonIcons.ChevronLeft, "Назад", onBack) },
        art = { RadarArt(countries) },
    ) {
        LogoRow(tone, step = 0)
        OnbTitle("Интернет", "без границ.", tone)
        OnbSubtitle("Подключение в одно касание. Защита для всех ваших устройств — до ${deviceLimitOf(state)} штук.")
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FactTile(Modifier.weight(1f), "безлимит") { Icon(LiptonIcons.Infinity, null, tint = LiptonTheme.colors.stateTone(tone).a, modifier = Modifier.size(24.dp)) }
            FactTile(Modifier.weight(1f), "устройств") { FactValue("${deviceLimitOf(state)}") }
            if (countries.isNotEmpty()) {
                FactTile(Modifier.weight(1f), countriesWord(countries.size)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FactValue("${countries.size}")
                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) { countries.take(2).forEach { FlagCircle(it, size = 18.dp) } }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        val retry = state.guestRetryAt?.takeIf { it > System.currentTimeMillis() }
        AnimatedVisibility(visible = guestOn) {
            Column {
                TrialCtaButton(
                    title = "Попробовать ${state.appConfig?.guestMinutes ?: 15} минут",
                    subtitle = if (retry != null) "снова ${ruWhen(retry)}" else "без регистрации",
                    onClick = onTrial,
                    enabled = retry == null,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        if (guestOn) {
            SecondaryPill("Создать аккаунт", onClick = onCreate, icon = LiptonIcons.UserPlus)
        } else {
            PrimaryButton("Создать аккаунт", onClick = onCreate, icon = LiptonIcons.UserPlus, modifier = Modifier.fillMaxWidth())
        }
        FooterLink("Уже есть аккаунт?", "Войти", tone, onLogin)
    }
}

@Composable
private fun FactTile(modifier: Modifier, label: String, value: @Composable () -> Unit) {
    val c = LiptonTheme.colors
    Column(
        modifier.height(72.dp).glass(RoundedCornerShape(20.dp), highlightHeight = 30.dp).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.height(26.dp), contentAlignment = Alignment.CenterStart) { value() }
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.66f), maxLines = 1)
    }
}

@Composable
private fun FactValue(text: String) {
    Text(text, style = MaterialTheme.typography.displaySmall.copy(fontSize = 22.sp), color = LiptonTheme.colors.text, maxLines = 1)
}

private fun countriesWord(n: Int): String = com.lipton.vpn.ui.tabs.pluralRu(n, "страна", "страны", "стран")

/** Лимит устройств: из тарифа в /config (нет — 5, как в боте). */
private fun deviceLimitOf(state: UiState): Int =
    state.appConfig?.baseTariff()?.deviceLimit?.takeIf { it > 0 } ?: state.deviceLimit ?: 5

// ─── Как пользоваться ────────────────────────────────────────────────────────

@Composable
private fun HowtoStep(onBack: () -> Unit, onNext: () -> Unit) {
    val c = LiptonTheme.colors
    val tone = AuroraTone.OFF
    OnbPage(tone = tone, topBar = { RoundIconButton(LiptonIcons.ChevronLeft, "Назад", onBack) }) {
        LogoRow(tone, step = 1)
        OnbTitle("Как пользоваться", "VPN", tone)
        OnbSubtitle("Три простых шага — и вы под защитой.")
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HowtoRow(1, "Выберите сервер", "или Авто-баланс") {
                Row(
                    Modifier.height(36.dp).clip(RoundedCornerShape(999.dp)).background(c.text.copy(alpha = 0.06f))
                        .border(1.dp, c.text.copy(alpha = 0.16f), RoundedCornerShape(999.dp)).padding(start = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlagCircle("DE", size = 22.dp)
                    Text("Германия", style = MaterialTheme.typography.labelLarge, color = c.text)
                    Icon(LiptonIcons.ChevronRight, null, tint = c.text.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                }
            }
            HowtoRow(2, "Подключитесь", "в одно касание") { MiniPill("Подключить", filled = true) }
            HowtoRow(3, "Отключитесь", "когда нужно") { MiniPill("Отключить", filled = false) }
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Продолжить", onClick = onNext, tone = ButtonTone.WARN, trailingIcon = LiptonIcons.ArrowRight, modifier = Modifier.fillMaxWidth())
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            GhostButton("Пропустить", onClick = onNext, color = c.text.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun HowtoRow(n: Int, title: String, sub: String, trailing: @Composable RowScope.() -> Unit) {
    val c = LiptonTheme.colors
    val warn = c.stateTone(AuroraTone.OFF).a
    Row(
        Modifier.fillMaxWidth().height(64.dp).glass(RoundedCornerShape(20.dp), highlightHeight = 28.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(warn.copy(alpha = 0.14f)).border(1.dp, warn.copy(alpha = 0.45f), CircleShape), contentAlignment = Alignment.Center) {
            Text("$n", style = MaterialTheme.typography.titleSmall, color = c.stateText(AuroraTone.OFF))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1)
            Text(sub, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f), maxLines = 1)
        }
        trailing()
    }
}

@Composable
private fun MiniPill(text: String, filled: Boolean) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .height(34.dp)
            .clip(shape)
            .background(if (filled) (if (c.isDark) Color.White else c.text) else c.text.copy(alpha = 0.06f))
            .border(1.dp, if (filled) Color.Transparent else c.text.copy(alpha = 0.18f), shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val fg = if (filled) (if (c.isDark) Color(0xFF050807) else Color.White) else c.text
        Icon(LiptonIcons.Power, null, tint = fg, modifier = Modifier.size(13.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = fg, maxLines = 1)
    }
}

// ─── Начнём знакомство ───────────────────────────────────────────────────────

@Composable
private fun StartStep(
    bot: String,
    onBack: (() -> Unit)?,
    onLogin: () -> Unit,
    onTelegram: () -> Unit,
    onEmail: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
) {
    val c = LiptonTheme.colors
    val tone = AuroraTone.OFF
    OnbPage(
        tone = tone,
        topBar = {
            if (onBack != null) RoundIconButton(LiptonIcons.ChevronLeft, "Назад", onBack)
            TopLabel("Войти", onLogin)
        },
        art = { ChatArt(bot) },
    ) {
        LogoRow(tone, step = 2)
        OnbTitle("Начнём", "знакомство.", tone)
        OnbSubtitle("Создайте аккаунт за минуту — через нашего Telegram-бота или по почте.")
        Spacer(Modifier.height(20.dp))
        GlassCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), onClick = onTelegram) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF2AABEE)), contentAlignment = Alignment.Center) {
                    Icon(LiptonIcons.Send, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Бот Lipton VPN", style = MaterialTheme.typography.titleMedium, color = c.text)
                    Text(bot, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF2AABEE))
                    Text("создаст аккаунт и пришлёт ссылку", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.62f))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Создать аккаунт в Telegram", onClick = onTelegram, tone = ButtonTone.WARN, icon = LiptonIcons.Send, trailingIcon = LiptonIcons.ArrowRight, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        SecondaryPill("Создать по почте", onClick = onEmail, icon = LiptonIcons.Mail)
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.Center) {
            Text("Нажимая, вы соглашаетесь с ", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.55f))
            LinkText("условиями", onTerms)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("и ", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.55f))
            LinkText("политикой конфиденциальности", onPrivacy)
        }
        FooterLink("Уже есть аккаунт?", "Войти", tone, onLogin)
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    val c = LiptonTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
        color = c.text.copy(alpha = 0.85f),
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    )
}

// ─── Вход по почте ───────────────────────────────────────────────────────────

@Composable
private fun EmailStep(
    vm: MainViewModel,
    signup: Boolean,
    email: String,
    onEmail: (String) -> Unit,
    onBack: (() -> Unit)?,
    onSent: () -> Unit,
    onTab: (LoginTab) -> Unit,
    onSwitchMode: () -> Unit,
    onBot: () -> Unit,
) {
    val c = LiptonTheme.colors
    val tone = AuroraTone.OFF
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val send: () -> Unit = {
        if (!busy && isLikelyEmail(email)) scope.launch {
            busy = true; err = null
            val e = email.trim().lowercase()
            try { vm.authEmailRequest(e); onEmail(e); onSent() }
            catch (ex: ApiClient.ApiException) { err = ex.message ?: "Не удалось отправить код" }
            catch (ex: Exception) { err = ex.message ?: "Не удалось отправить код" }
            finally { busy = false }
        }
    }
    OnbPage(
        tone = tone,
        topBar = {
            if (onBack != null) RoundIconButton(LiptonIcons.ChevronLeft, "Назад", onBack)
            TopLabel(if (signup) "Регистрация" else "Вход")
        },
    ) {
        LogoRow(tone)
        if (signup) OnbTitle("Создайте аккаунт.", "Займёт минуту.", tone)
        else OnbTitle("С возвращением.", "Всё на месте.", tone)
        OnbSubtitle(
            if (signup) "Пришлём код на почту — аккаунт создастся сам, подписка и устройства будут в нём."
            else "Войдите — подписка и устройства подтянутся сами.",
        )
        LoginTabs(LoginTab.EMAIL, onTab)
        Text(
            "Почта",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = c.text.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp, start = 2.dp),
        )
        EmailField(email, { onEmail(it); err = null }, tone, onDone = send)
        ErrorText(err)
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            if (busy) "Отправляем…" else "Получить код", onClick = send, tone = ButtonTone.WARN,
            trailingIcon = LiptonIcons.ArrowRight, loading = busy, enabled = isLikelyEmail(email) && !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        HintLine(LiptonIcons.Lock, "Пришлём 6-значный код на почту", color = c.stateText(tone))
        if (signup) FooterLink("Уже есть аккаунт?", "Войти", tone, onSwitchMode)
        else FooterLink("Нет аккаунта?", "Создать через бота", tone, onBot, underline = true)
    }
}

// ─── Код из письма ───────────────────────────────────────────────────────────

@Composable
private fun EmailCodeStep(vm: MainViewModel, email: String, onBack: () -> Unit) {
    val c = LiptonTheme.colors
    val tone = AuroraTone.OFF
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var resendAt by remember { mutableLongStateOf(System.currentTimeMillis() + 60_000L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(resendAt) { while (now < resendAt) { delay(500); now = System.currentTimeMillis() } }
    val verify: () -> Unit = {
        if (!busy && code.length == 6) scope.launch {
            busy = true; err = null
            try { vm.authEmailVerify(email, code); vm.completeLogin() }
            catch (ex: ApiClient.ApiException) { err = ex.message ?: "Неверный код"; code = "" }
            catch (ex: Exception) { err = ex.message ?: "Не удалось войти" }
            finally { busy = false }
        }
    }
    OnbPage(
        tone = tone,
        topBar = { RoundIconButton(LiptonIcons.ChevronLeft, "Назад", onBack); TopLabel("Код из письма") },
    ) {
        LogoRow(tone)
        OnbTitle("Проверьте", "вашу почту.", tone)
        OnbSubtitle(boldParts("Мы отправили код на\n**$email**", c.text))
        Spacer(Modifier.height(24.dp))
        CodeBoxes(code, 6, { code = it; err = null }, tone, onComplete = verify)
        ErrorText(err)
        Spacer(Modifier.height(22.dp))
        PrimaryButton(
            if (busy) "Входим…" else "Подтвердить и войти", onClick = verify, tone = ButtonTone.WARN,
            trailingIcon = LiptonIcons.ArrowRight, loading = busy, enabled = code.length == 6 && !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        val left = ((resendAt - now) / 1000).coerceAtLeast(0)
        if (left > 0) {
            HintLine(LiptonIcons.Refresh, "Отправить код ещё раз · ${left / 60}:${String.format(java.util.Locale.US, "%02d", left % 60)}", color = c.stateText(tone).copy(alpha = 0.75f))
        } else {
            HintLine(LiptonIcons.Refresh, "Отправить код ещё раз", color = c.stateText(tone)) {
                scope.launch {
                    try { vm.authEmailRequest(email); resendAt = System.currentTimeMillis() + 60_000L; now = System.currentTimeMillis(); err = null }
                    catch (ex: Exception) { err = ex.message ?: "Не удалось отправить код" }
                }
            }
        }
        HintLine(LiptonIcons.Alert, "Нет письма — проверьте «Спам»")
        FooterLink("", "Изменить почту", tone, onBack, underline = true)
    }
}

// ─── Код с сайта ─────────────────────────────────────────────────────────────

@Composable
private fun SiteCodeStep(vm: MainViewModel, onBack: (() -> Unit)?, onTab: (LoginTab) -> Unit, onOpenSite: () -> Unit) {
    val c = LiptonTheme.colors
    val tone = AuroraTone.OFF
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val login: () -> Unit = {
        if (!busy && code.length == 4) scope.launch {
            busy = true; err = null
            try { vm.authDeviceExchange(code); vm.completeLogin() }
            catch (e: ApiClient.ApiException) { err = e.message ?: "Неверный или истёкший код"; code = "" }
            catch (e: Exception) { err = e.message ?: "Не удалось войти" }
            finally { busy = false }
        }
    }
    OnbPage(
        tone = tone,
        topBar = {
            if (onBack != null) RoundIconButton(LiptonIcons.ChevronLeft, "Назад", onBack)
            TopLabel("Код с сайта")
        },
    ) {
        LogoRow(tone)
        OnbTitle("Один код.", "Ваш аккаунт.", tone)
        LoginTabs(LoginTab.CODE, onTab)
        OnbSubtitle(boldParts("Откройте **liptonone.online** → **Кабинет** → **Подключение** → **«Получить код»**", c.text), Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
        CodeBoxes(code, 4, { code = it; err = null }, tone, onComplete = login)
        ErrorText(err)
        Spacer(Modifier.height(22.dp))
        PrimaryButton(
            if (busy) "Входим…" else "Войти", onClick = login, tone = ButtonTone.WARN,
            trailingIcon = LiptonIcons.ArrowRight, loading = busy, enabled = code.length == 4 && !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        SecondaryPill("Открыть сайт и получить код", onClick = onOpenSite, trailingIcon = LiptonIcons.External)
        HintLine(LiptonIcons.Timer, "Код действует 5 минут", color = c.stateText(tone))
    }
}

// ─── Вход через Telegram ─────────────────────────────────────────────────────

@Composable
private fun TelegramStep(
    vm: MainViewModel,
    bot: String,
    botUrl: String,
    signup: Boolean,
    offline: Boolean,
    onBack: (() -> Unit)?,
    onOther: () -> Unit,
    openUrl: (String) -> Unit,
) {
    val c = LiptonTheme.colors
    val tone = AuroraTone.OFF
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf<String?>(null) }
    var token by remember { mutableStateOf<String?>(if (offline) "preview" else null) }
    var err by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    // Начать вход: link_token + ссылка на бота; бот подтверждает, приложение входит само (poll).
    LaunchedEffect(attempt) {
        if (offline) return@LaunchedEffect
        err = null
        try {
            val init = vm.authTgInit()
            token = init.linkToken
            link = init.link ?: botUrl
            openUrl(link ?: botUrl)
        } catch (e: ApiClient.ApiException) {
            err = e.message ?: "Не удалось начать вход"
        } catch (e: Exception) {
            err = e.message ?: "Не удалось начать вход"
        }
    }
    LaunchedEffect(token) {
        if (offline) return@LaunchedEffect
        val tok = token ?: return@LaunchedEffect
        val deadline = System.currentTimeMillis() + 5 * 60_000L
        while (System.currentTimeMillis() < deadline) {
            try {
                if (vm.authTgPoll(tok)) { vm.completeLogin(); return@LaunchedEffect }
            } catch (e: ApiClient.ApiException) {
                if (e.code != "network") { err = e.message ?: "Сессия входа истекла"; token = null; return@LaunchedEffect }
            } catch (_: Exception) { /* сеть — повторим */ }
            delay(2500)
        }
        err = "Время вышло — откройте бота ещё раз"
        token = null
    }

    OnbPage(
        tone = tone,
        topBar = {
            if (onBack != null) RoundIconButton(LiptonIcons.ChevronLeft, "Назад", onBack)
            TopLabel(if (signup) "Регистрация через Telegram" else "Вход через Telegram")
        },
    ) {
        LogoRow(tone)
        OnbTitle(if (signup) "Подтвердите" else "Подтвердите вход", "в Telegram.", tone)
        OnbSubtitle(boldParts("Мы открыли бота **$bot** — нажмите в нём «Старт» и подтвердите вход. Приложение войдёт само.", c.text))
        Spacer(Modifier.height(22.dp))
        GlassCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    if (token != null && err == null) {
                        CircularProgressIndicator(color = c.stateTone(tone).a, strokeWidth = 2.dp, modifier = Modifier.size(44.dp))
                    }
                    Icon(LiptonIcons.Send, null, tint = c.stateText(tone), modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (err == null) "Ждём подтверждения…" else "Вход не подтверждён",
                        style = MaterialTheme.typography.titleSmall, color = c.text,
                    )
                    Text(
                        err ?: "Обычно это занимает пару секунд",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (err == null) c.text.copy(alpha = 0.62f) else c.stateText(tone),
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            "Открыть бота ещё раз",
            onClick = {
                val l = link
                if (token != null && l != null) openUrl(l) else attempt++
            },
            tone = ButtonTone.WARN, trailingIcon = LiptonIcons.ArrowRight, modifier = Modifier.fillMaxWidth(),
        )
        FooterLink("", "Войти другим способом", tone, onOther, underline = true)
    }
}

// ─── 15 минут без регистрации ────────────────────────────────────────────────

@Composable
private fun TrialStep(state: UiState, vm: MainViewModel, activity: ComponentActivity, onBack: (() -> Unit)?, onCreate: () -> Unit) {
    val c = LiptonTheme.colors
    val tone = AuroraTone.ON
    val scope = rememberCoroutineScope()
    val minutes = state.appConfig?.guestMinutes ?: 15
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val retry = state.guestRetryAt?.takeIf { it > System.currentTimeMillis() }
    OnbPage(
        tone = tone,
        topBar = { if (onBack != null) RoundIconButton(LiptonIcons.ChevronLeft, "Назад", onBack) },
        art = { TimerRing(String.format(java.util.Locale.US, "%d:00", minutes), "минут", 1f, tone) },
    ) {
        LogoRow(tone)
        OnbTitle("$minutes минут", "без регистрации.", tone)
        OnbSubtitle("Успеете включить VPN, зайти в Telegram и получить код входа. Доступно раз в день.")
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CheckLine("Все серверы и Авто-баланс")
            CheckLine("Без аккаунта и карты")
            CheckLine("Раз в день — каждый день заново")
        }
        Spacer(Modifier.height(22.dp))
        PrimaryButton(
            if (busy) "Включаем…" else "Начать $minutes минут",
            onClick = {
                if (!busy) scope.launch {
                    busy = true; err = null
                    when (val r = vm.startGuestTrial()) {
                        TrialStart.Ok -> vm.handleConnectToggle(activity)
                        is TrialStart.Used -> err = "Сегодня пробный доступ уже был" + (r.retryAt?.let { " — снова ${ruWhen(it)}" } ?: ". Попробуйте завтра")
                        TrialStart.Disabled -> err = "Пробный доступ сейчас недоступен — создайте аккаунт"
                        TrialStart.HasSubscription -> Unit
                        is TrialStart.Failed -> err = r.message
                    }
                    busy = false
                }
            },
            tone = ButtonTone.ACCENT, trailingIcon = LiptonIcons.ArrowRight, loading = busy,
            enabled = !busy && retry == null && state.appConfig?.guestEnabled != false,
            modifier = Modifier.fillMaxWidth(),
        )
        ErrorText(err ?: retry?.let { "Следующая бесплатная попытка — ${ruWhen(it)}" })
        Spacer(Modifier.height(12.dp))
        SecondaryPill("Сначала создать аккаунт", onClick = onCreate, icon = LiptonIcons.UserPlus)
    }
}

@Composable
private fun CheckLine(text: String) {
    val c = LiptonTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ToneCircleIcon(LiptonIcons.Check, c.stateTone(AuroraTone.ON).a, size = 24.dp, iconSize = 12.dp)
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = c.text)
    }
}

// ─── «Вы вошли. Всё готово.» ─────────────────────────────────────────────────

/** Экран после входа: подписка и устройство уже подтянулись (new-onb-success). */
@Composable
fun LoginSuccessScreen(state: UiState, onContinue: () -> Unit) {
    val c = LiptonTheme.colors
    val tone = AuroraTone.ON
    BackHandler(onBack = onContinue)
    OnbPage(
        tone = tone,
        topBar = {
            RoundIconButton(LiptonIcons.Close, "Закрыть", onContinue)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot(c.stateTone(tone).a)
                Text("Вход выполнен", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = c.text.copy(alpha = 0.85f))
            }
        },
        art = { SuccessArt() },
    ) {
        LogoRow(tone)
        OnbTitle("Вы вошли.", "Всё готово.", tone)
        val active = hasActiveSubscription(state)
        OnbSubtitle(
            when {
                state.accountSyncing && state.accountStatus == null -> "Подтягиваем подписку и устройства…"
                active -> "Подписка и устройства уже подтянулись."
                else -> "Аккаунт готов. Осталось выбрать тариф."
            },
        )
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val left = daysLeft(state.accountPeriodEnd)
            if (active) {
                val title = state.accountOverlay?.tariffTitle?.takeIf { it.isNotBlank() }
                    ?: tariffTitleFor(state.accountTariffCode, state.appConfig)
                    ?: if (state.accountStatus == "trial") "Пробный период" else "Подписка"
                SuccessRow(
                    LiptonIcons.Calendar, "Подписка «$title»",
                    listOfNotNull(ruDayMonth(state.accountPeriodEnd)?.let { "до $it" }, left?.let { "осталось ${pluralDays(it)}" }).joinToString(" · "),
                    check = true,
                )
            } else if (state.accountStatus != null) {
                SuccessRow(LiptonIcons.Tag, "Подписки пока нет", "Тарифы — на главном экране", check = false, warn = true)
            } else {
                SuccessRow(LiptonIcons.Calendar, "Подписка", "загружаем…", check = false)
            }
            val limit = state.deviceLimit ?: 5
            val used = (state.devices?.size ?: state.devicesUsed)?.coerceAtLeast(if (active) 1 else 0)
            SuccessRow(
                LiptonIcons.Phone, if (active) "Устройство добавлено" else "Это устройство",
                listOfNotNull(Build.MODEL, used?.let { "$it из $limit" }).joinToString(" · "),
                trailing = { if (used != null) DeviceSlots(used, limit) },
            )
            SuccessRow(LiptonIcons.Monitor, "Другие устройства", "Как подключить — в Профиле", chevron = true)
        }
        Spacer(Modifier.height(22.dp))
        PrimaryButton("Перейти на главный экран", onClick = onContinue, tone = ButtonTone.ACCENT, trailingIcon = LiptonIcons.ArrowRight, modifier = Modifier.fillMaxWidth())
        if (!active && state.accountStatus != null) {
            val from = state.appConfig?.baseTariff()?.monthlyPeriod()?.priceKopeks
            HintLine(LiptonIcons.Tag, "Подписки пока нет? Выберите тариф" + (from?.let { " от ${rubles(it)}" } ?: "") + " на главном экране")
        }
    }
}

@Composable
private fun SuccessRow(
    icon: ImageVector,
    title: String,
    sub: String,
    check: Boolean = false,
    warn: Boolean = false,
    chevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LiptonTheme.colors
    val accent = c.stateTone(if (warn) AuroraTone.OFF else AuroraTone.ON).a
    Row(
        Modifier.fillMaxWidth().height(64.dp).glass(RoundedCornerShape(20.dp), highlightHeight = 28.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(accent.copy(alpha = 0.14f)).border(1.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1)
            Text(sub, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR_NUMS), color = c.text.copy(alpha = 0.62f), maxLines = 1)
        }
        when {
            trailing != null -> trailing()
            check -> Box(Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, accent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(LiptonIcons.Check, null, tint = accent, modifier = Modifier.size(12.dp))
            }
            chevron -> Icon(LiptonIcons.ChevronRight, null, tint = c.text.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DeviceSlots(used: Int, limit: Int) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 0 until limit.coerceIn(1, 6)) {
            Box(
                Modifier.width(9.dp).height(14.dp).clip(RoundedCornerShape(3.dp))
                    .background(if (i < used) t.a else Color.Transparent)
                    .border(1.dp, if (i < used) Color.Transparent else c.text.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
            )
        }
    }
}
