package com.lipton.vpn.ui.account

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.TariffChangeState
import com.lipton.vpn.UiState
import com.lipton.vpn.data.ApiClient
import com.lipton.vpn.data.model.*
import com.lipton.vpn.ui.components.tabBarBottomPadding
import com.lipton.vpn.ui.theme.Green
import com.lipton.vpn.ui.theme.Green3
import com.lipton.vpn.ui.theme.LocalLiptonColors
import com.lipton.vpn.ui.theme.Red
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Утилиты ─────────────────────────────────────────────────────────────────

private fun rub(kopeks: Long): String {
    val r = kopeks / 100.0
    return if (r % 1.0 == 0.0) "${r.toInt()} ₽" else String.format("%.2f ₽", r)
}

private fun statusLabel(s: String?): String = when (s) {
    "active" -> "Активна"
    "trial"  -> "Пробный период"
    "grace"  -> "Льготный период"
    "expired", "canceled", "none", null -> "Нет активной подписки"
    else     -> s
}

private fun shortDate(iso: String?): String = iso?.take(10)?.replace("-", ".") ?: ""

// RFC3339 → «ДД.ММ.ГГГГ» в часовом поясе телефона. При сбое — дата из первых 10 символов.
private fun ruDate(iso: String?): String {
    val src = iso ?: return ""
    if (src.isBlank()) return ""
    val head = src.take(10).split("-")
    val fallback = if (head.size == 3) "${head[2]}.${head[1]}.${head[0]}" else src.take(10)
    return try {
        val clean = src.replace(Regex("\\.\\d+"), "").replace("Z", "+00:00")
        val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
        val d = inFmt.parse(clean)
        if (d == null) fallback
        else java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(d)
    } catch (_: Exception) { fallback }
}

// ─── Панель аккаунта (заменяет ручное добавление ссылки) ─────────────────────

@Composable
fun AccountPanel(
    state: UiState,
    onPay: () -> Unit,
    onNews: () -> Unit,
    onSupport: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onChangeTariff: () -> Unit = {},
) {
    val lc = LocalLiptonColors.current
    val active = state.accountStatus == "active" || state.accountStatus == "trial" || state.accountStatus == "grace"
    // Смена тарифа — только для платной подписки (не триал).
    val paid = state.accountStatus == "active" || state.accountStatus == "grace"
    val overlay = state.accountOverlay

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(lc.cardBg)
            .border(1.dp, lc.cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Моя подписка", fontSize = 12.sp, color = lc.textTertiary)
                Spacer(Modifier.height(3.dp))
                Text(
                    statusLabel(state.accountStatus),
                    fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    color = if (active) Green else lc.textPrimary,
                )
                if (active && !state.accountPeriodEnd.isNullOrBlank()) {
                    Text("действует до ${shortDate(state.accountPeriodEnd)}",
                        fontSize = 12.sp, color = lc.textSecondary)
                }
                if (overlay != null && !overlay.tariffTitle.isNullOrBlank()) {
                    val revert = overlay.revertTariffTitle
                    val untilText = ruDate(overlay.until)
                    Text(
                        "Сейчас «${overlay.tariffTitle}»" +
                            (if (untilText.isNotEmpty()) " до $untilText" else "") +
                            (if (!revert.isNullOrBlank()) ", потом снова «$revert»" else ""),
                        fontSize = 12.sp, color = Green, modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (state.accountSyncing) {
                CircularProgressIndicator(color = Green, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text("⟳", fontSize = 18.sp, color = lc.textSecondary,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onRefresh).padding(6.dp))
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Green, Green3)))
                .clickable(onClick = onPay).padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (active) "Продлить подписку" else "Оформить подписку",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        if (paid) {
            SmallBtn("Сменить тариф", Modifier.fillMaxWidth(), onChangeTariff)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallBtn("Новости", Modifier.weight(1f), onNews)
            SmallBtn("Поддержка", Modifier.weight(1f), onSupport)
        }
        Text("Выйти из аккаунта", fontSize = 12.sp, color = lc.textTertiary,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onLogout).padding(top = 2.dp),
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun SmallBtn(text: String, modifier: Modifier, onClick: () -> Unit) {
    val lc = LocalLiptonColors.current
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(lc.cardBg)
            .border(1.dp, lc.cardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = lc.textSecondary) }
}

// ─── Общая шапка полноэкранных панелей ───────────────────────────────────────

// onClose = null — экран встроен во вкладку (без «назад», прозрачный фон поверх свечения;
// низ под капсулу навигации оставляет сам экран). Окно рисуется edge-to-edge, поэтому
// отступы от статус-бара, навигации и клавиатуры — здесь.
@Composable
private fun SheetScaffold(title: String, onClose: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    val lc = LocalLiptonColors.current
    val embedded = onClose == null
    Column(
        Modifier.fillMaxSize()
            .then(if (embedded) Modifier else Modifier.background(lc.bgDeep))
            .windowInsetsPadding(
                if (embedded) WindowInsets.statusBars
                else WindowInsets.safeDrawing
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onClose != null) {
                BackHandler(onBack = onClose)
                Text("‹", fontSize = 30.sp, color = lc.textSecondary,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClose).padding(horizontal = 8.dp))
                Spacer(Modifier.width(6.dp))
            } else {
                Spacer(Modifier.width(4.dp))
            }
            Text(title, fontSize = if (embedded) 28.sp else 18.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
        }
        content()
    }
}

// ─── Экран оплаты ────────────────────────────────────────────────────────────

@Composable
fun PaymentScreen(vm: MainViewModel, onClose: () -> Unit, onOpenChange: () -> Unit = {}) {
    val lc = LocalLiptonColors.current
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var loading by remember { mutableStateOf(true) }
    var tariffs by remember { mutableStateOf<List<Tariff>>(emptyList()) }
    var selectedPeriod by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("choose") } // choose | wait | ok | fail
    var txId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val cfg = vm.api.getConfig()
            tariffs = cfg.tariffs
            selectedPeriod = cfg.tariffs.firstOrNull()?.periods?.firstOrNull()?.id
        } catch (e: Exception) { err = e.message }
        loading = false
    }

    // Поллинг статуса после открытия ЮKassa.
    LaunchedEffect(phase, txId) {
        val id = txId
        if (phase != "wait" || id == null) return@LaunchedEffect
        val deadline = System.currentTimeMillis() + 5 * 60_000
        while (phase == "wait" && System.currentTimeMillis() < deadline) {
            try {
                val st = vm.api.paymentStatus(id)
                when (st.status) {
                    "succeeded" -> { phase = "ok"; vm.syncAccountSubscription() }
                    "failed", "canceled" -> { phase = "fail" }
                }
            } catch (_: Exception) {}
            if (phase == "wait") delay(2500)
        }
        if (phase == "wait") phase = "fail"
    }

    SheetScaffold("Оплата", onClose) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (phase) {
                "ok" -> ResultBlock(true) { vm.refreshAccount(); onClose() }
                "fail" -> ResultBlock(false) { phase = "choose"; err = null }
                "wait" -> Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Green)
                    Spacer(Modifier.height(16.dp))
                    Text("Подтверждаем оплату…", color = lc.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Завершите оплату в открывшемся окне и вернитесь в приложение.",
                        color = lc.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp))
                }
                else -> {
                    if (loading) {
                        Box(Modifier.fillMaxWidth().padding(top = 40.dp), Alignment.Center) {
                            CircularProgressIndicator(color = Green)
                        }
                    } else {
                        tariffs.forEach { t ->
                            Text(t.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
                            t.periods.forEach { p ->
                                val on = selectedPeriod == p.id
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(if (on) lc.greenCard else lc.cardBg)
                                        .border(1.dp, if (on) Green else lc.cardBorder, RoundedCornerShape(12.dp))
                                        .clickable { selectedPeriod = p.id }.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("${p.days} дней", fontSize = 14.sp, color = lc.textPrimary, modifier = Modifier.weight(1f))
                                    Text(rub(p.priceKopeks), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                        color = if (on) Green else lc.textPrimary)
                                }
                            }
                        }
                        err?.let { Text(it, color = Red, fontSize = 13.sp) }
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Green, Green3)))
                                .clickable(enabled = !busy && selectedPeriod != null) {
                                    scope.launch {
                                        busy = true; err = null
                                        try {
                                            val res = vm.api.checkout(null, selectedPeriod, null)
                                            txId = res.transactionId
                                            val url = res.confirmationUrl
                                            if (!url.isNullOrBlank()) {
                                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                phase = "wait"
                                            } else if (res.status == "succeeded") {
                                                phase = "ok"; vm.syncAccountSubscription()
                                            } else { err = "Не удалось создать платёж" }
                                        } catch (e: ApiClient.ApiException) {
                                            // 409 «используйте смену тарифа» — другой тариф при активной подписке.
                                            val msg = e.message ?: ""
                                            if (e.status == 409 && msg.contains("смен", ignoreCase = true)) onOpenChange()
                                            else err = e.message
                                        }
                                        catch (e: Exception) { err = e.message }
                                        finally { busy = false }
                                    }
                                }.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(if (busy) "Создаём платёж…" else "Перейти к оплате",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultBlock(ok: Boolean, onAction: () -> Unit) {
    val lc = LocalLiptonColors.current
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (ok) "🎉" else "✕", fontSize = 56.sp, color = if (ok) Green else Red)
        Spacer(Modifier.height(12.dp))
        Text(if (ok) "Оплата прошла!" else "Оплата не прошла",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
        Text(
            if (ok) "Подписка активна — можно подключаться."
            else "Платёж отклонён или отменён. Попробуйте снова.",
            fontSize = 13.sp, color = lc.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, start = 20.dp, end = 20.dp),
        )
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Green, Green3)))
                .clickable(onClick = onAction).padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text(if (ok) "Готово" else "Попробовать снова",
            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black) }
    }
}

// ─── Экран смены тарифа ──────────────────────────────────────────────────────

@Composable
fun TariffChangeScreen(vm: MainViewModel, onClose: () -> Unit) {
    val lc = LocalLiptonColors.current
    val ctx = LocalContext.current
    val cs by vm.changeState.collectAsState()

    LaunchedEffect(Unit) { vm.loadChangeOptions() }

    val close: () -> Unit = {
        vm.resetTariffChange()
        vm.refreshAccount()
        onClose()
    }

    SheetScaffold(
        "Сменить тариф",
        onClose = { if (cs.phase == "confirm") vm.backToChangeOptions() else close() },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (cs.phase) {
                "ok" -> ChangeResultBlock(true, cs.resultText) { close() }
                "fail" -> ChangeResultBlock(false, cs.resultText) { vm.loadChangeOptions() }
                "wait" -> {
                    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Green)
                        Spacer(Modifier.height(16.dp))
                        Text("Подтверждаем оплату…", color = lc.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Если открылась страница оплаты — завершите её и вернитесь в приложение.",
                            color = lc.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp))
                    }
                    SmallBtn("Закрыть", Modifier.fillMaxWidth().padding(top = 12.dp)) { close() }
                }
                "confirm" -> {
                    val p = cs.preview
                    if (p == null) {
                        SmallBtn("Назад", Modifier.fillMaxWidth()) { vm.backToChangeOptions() }
                    } else {
                        ChangeConfirm(
                            p = p,
                            discountPercent = cs.discountPercent,
                            currentTitle = cs.current?.tariffTitle,
                            busy = cs.submitting,
                            error = cs.error,
                            onConfirm = {
                                vm.confirmChange { url ->
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            },
                            onBack = { vm.backToChangeOptions() },
                        )
                    }
                }
                else -> ChangeChoose(cs, onPick = { vm.previewChange(it) }, onRetry = { vm.loadChangeOptions() })
            }
        }
    }
}

@Composable
private fun ChangeChoose(cs: TariffChangeState, onPick: (ChangeOption) -> Unit, onRetry: () -> Unit) {
    val lc = LocalLiptonColors.current
    if (cs.loading) {
        Box(Modifier.fillMaxWidth().padding(top = 40.dp), Alignment.Center) {
            CircularProgressIndicator(color = Green)
        }
        return
    }
    val err = cs.error
    if (!cs.available) {
        val reason = cs.reason
        val text = when {
            reason != null && reason.isNotBlank() -> reason
            err != null && err.isNotBlank() -> err
            else -> "Смена тарифа сейчас недоступна"
        }
        ChangeCard {
            Text(text, fontSize = 14.sp, color = lc.textPrimary, lineHeight = 20.sp)
        }
        if (err != null && (reason == null || reason.isBlank())) {
            SmallBtn("Повторить", Modifier.fillMaxWidth(), onRetry)
        }
        return
    }

    val cur = cs.current
    if (cur != null) {
        ChangeCard {
            Text("Сейчас", fontSize = 12.sp, color = lc.textTertiary)
            Text("«${cur.tariffTitle}», ${cur.periodDays} дн.", fontSize = 15.sp,
                fontWeight = FontWeight.Bold, color = lc.textPrimary, modifier = Modifier.padding(top = 2.dp))
            val end = ruDate(cur.periodEnd)
            if (end.isNotEmpty()) Text("действует до $end", fontSize = 12.sp, color = lc.textSecondary)
        }
    }
    if (cs.discountPercent > 0) {
        Text("Остаток текущей подписки засчитывается, на доплату — скидка ${cs.discountPercent}%.",
            fontSize = 12.sp, color = lc.textSecondary, lineHeight = 17.sp)
    }

    if (cs.options.isEmpty()) {
        Text("Вариантов дороже или длиннее пока нет.", fontSize = 14.sp, color = lc.textSecondary,
            modifier = Modifier.padding(top = 8.dp))
    }
    cs.options.forEach { o ->
        ChangeOptionCard(o, cs.discountPercent, cur?.tariffTitle, enabled = !cs.previewing) { onPick(o) }
    }
    if (cs.previewing) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = Green, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Считаем…", fontSize = 13.sp, color = lc.textSecondary)
        }
    }
    if (err != null) Text(err, color = Red, fontSize = 13.sp)
}

@Composable
private fun ChangeCard(content: @Composable ColumnScope.() -> Unit) {
    val lc = LocalLiptonColors.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(lc.cardBg)
            .border(1.dp, lc.cardBorder, RoundedCornerShape(12.dp)).padding(14.dp),
        content = content,
    )
}

@Composable
private fun ChangeOptionCard(
    o: ChangeOption,
    discountPercent: Int,
    currentTitle: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val lc = LocalLiptonColors.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(lc.cardBg)
            .border(1.dp, if (o.isOverlay) Green.copy(alpha = 0.5f) else lc.cardBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${o.tariffTitle} · ${o.periodDays} дн.", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = lc.textPrimary, modifier = Modifier.weight(1f))
            Text(
                if (o.isOverlay) rub(o.priceKopeks)
                else if (o.surchargeKopeks > 0L) "+${rub(o.surchargeKopeks)}" else "0 ₽",
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Green,
            )
        }
        Spacer(Modifier.height(6.dp))
        ChangeLines(o, discountPercent, currentTitle)
    }
}

// Строки расчёта варианта — общие для карточки и подтверждения.
@Composable
private fun ChangeLines(o: ChangeOption, discountPercent: Int, currentTitle: String?) {
    val lc = LocalLiptonColors.current
    if (o.isOverlay) {
        val revert = o.revertTariffTitle ?: currentTitle ?: "прежний тариф"
        Text("${rub(o.priceKopeks)}, потом снова «$revert»", fontSize = 13.sp, color = lc.textPrimary)
        val until = ruDate(o.overlayUntil)
        if (until.isNotEmpty())
            Text("«${o.tariffTitle}» до $until", fontSize = 12.sp, color = lc.textSecondary)
        val end = ruDate(o.newPeriodEnd)
        if (end.isNotEmpty())
            Text("затем «$revert» до $end", fontSize = 12.sp, color = lc.textSecondary)
    } else {
        Text(
            if (o.surchargeKopeks > 0L) "Доплата ${rub(o.surchargeKopeks)}" else "Без доплаты",
            fontSize = 13.sp, color = lc.textPrimary,
        )
        if (o.discountKopeks > 0L) {
            val label = if (discountPercent > 0) "Скидка $discountPercent%" else "Скидка"
            Text("$label: −${rub(o.discountKopeks)}", fontSize = 12.sp, color = Green)
        }
        if (o.extraDays > 0)
            Text("+${o.extraDays} дн. перенесено", fontSize = 12.sp, color = Green)
        val end = ruDate(o.newPeriodEnd)
        if (end.isNotEmpty())
            Text("Новая дата окончания: $end", fontSize = 12.sp, color = lc.textSecondary)
    }
}

private fun confirmLabel(p: ChangeOption): String {
    val sum = rub(p.surchargeKopeks)
    val last4 = p.cardLast4
    return when {
        p.surchargeKopeks <= 0L -> "Сменить без доплаты"
        p.willChargeCard && last4 != null && last4.isNotBlank() -> "Списать $sum с карты •••• $last4"
        p.willChargeCard -> "Списать $sum через привязанный СБП"
        else -> "Оплатить $sum по СБП"
    }
}

@Composable
private fun ChangeConfirm(
    p: ChangeOption,
    discountPercent: Int,
    currentTitle: String?,
    busy: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val lc = LocalLiptonColors.current
    Text("Проверьте условия", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
    ChangeCard {
        Text("${p.tariffTitle} · ${p.periodDays} дн.", fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = lc.textPrimary)
        Spacer(Modifier.height(6.dp))
        if (!p.isOverlay && p.creditKopeks > 0L)
            Text("Зачёт остатка: ${rub(p.creditKopeks)}", fontSize = 12.sp, color = lc.textSecondary)
        ChangeLines(p, discountPercent, currentTitle)
    }
    Text(
        if (p.isOverlay)
            "Оставшиеся дни текущего тарифа сохранятся и продолжатся после. Автопродление — по цене «${p.revertTariffTitle ?: currentTitle ?: "текущего тарифа"}»."
        else
            "Дальше автопродление по цене ${rub(p.priceKopeks)} за ${p.periodDays} дн.",
        fontSize = 12.sp, color = lc.textTertiary, lineHeight = 17.sp,
    )
    if (error != null) Text(error, color = Red, fontSize = 13.sp)
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Green, Green3)))
            .clickable(enabled = !busy, onClick = onConfirm).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (busy) "Подождите…" else confirmLabel(p),
            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
    SmallBtn("Назад", Modifier.fillMaxWidth(), onBack)
}

@Composable
private fun ChangeResultBlock(ok: Boolean, text: String?, onAction: () -> Unit) {
    val lc = LocalLiptonColors.current
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (ok) "🎉" else "✕", fontSize = 56.sp, color = if (ok) Green else Red)
        Spacer(Modifier.height(12.dp))
        Text(if (ok) "Тариф изменён!" else "Не получилось",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
        Text(
            text ?: (if (ok) "Можно подключаться." else "Попробуйте снова."),
            fontSize = 13.sp, color = lc.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, start = 20.dp, end = 20.dp),
        )
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Green, Green3)))
                .clickable(onClick = onAction).padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text(if (ok) "Готово" else "Попробовать снова",
            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black) }
    }
}

// ─── Экран новостей ──────────────────────────────────────────────────────────

@Composable
fun NewsScreen(vm: MainViewModel, onClose: (() -> Unit)? = null) {
    val lc = LocalLiptonColors.current
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try { items = vm.api.getNews().items } catch (e: Exception) { err = e.message }
        loading = false
    }

    SheetScaffold("Новости", onClose) {
        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Green) }
            err != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(err!!, color = Red) }
            items.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Пока новостей нет", color = lc.textSecondary)
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp,
                    bottom = if (onClose == null) tabBarBottomPadding() else 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items) { n ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(lc.cardBg)
                            .border(1.dp, lc.cardBorder, RoundedCornerShape(14.dp)).padding(14.dp),
                    ) {
                        Text(n.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
                        if (!n.publishedAt.isNullOrBlank())
                            Text(shortDate(n.publishedAt), fontSize = 11.sp, color = lc.textTertiary,
                                modifier = Modifier.padding(top = 2.dp))
                        Text(n.body, fontSize = 13.sp, color = lc.textSecondary,
                            lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

// ─── Экран поддержки / ИИ ────────────────────────────────────────────────────

@Composable
fun SupportScreen(vm: MainViewModel, onClose: () -> Unit) {
    val lc = LocalLiptonColors.current
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<AiMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try { messages = vm.api.getAiDialog().messages } catch (_: Exception) {}
    }

    SheetScaffold("Поддержка", onClose) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (messages.isEmpty()) {
                item {
                    Text("Задайте вопрос — ответит ИИ-помощник, при необходимости подключится оператор.",
                        fontSize = 13.sp, color = lc.textSecondary, modifier = Modifier.padding(vertical = 20.dp))
                }
            }
            items(messages) { m ->
                val mine = m.role == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Box(
                        Modifier.fillMaxWidth(0.85f).clip(RoundedCornerShape(14.dp))
                            .background(if (mine) lc.greenCard else lc.cardBg)
                            .border(1.dp, if (mine) Green.copy(alpha = 0.3f) else lc.cardBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                    ) { Text(m.content, fontSize = 13.5.sp, color = lc.textPrimary, lineHeight = 19.sp) }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                placeholder = { Text("Сообщение…", color = lc.textTertiary) },
                modifier = Modifier.weight(1f), maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Green, unfocusedBorderColor = lc.cardBorder,
                    focusedContainerColor = lc.cardBg, unfocusedContainerColor = lc.cardBg,
                ),
                shape = RoundedCornerShape(12.dp),
            )
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (input.isBlank() || busy) lc.cardBg else Green)
                    .clickable(enabled = input.isNotBlank() && !busy) {
                        val text = input.trim()
                        input = ""
                        messages = messages + AiMessage("user", text)
                        scope.launch {
                            busy = true
                            try {
                                val r = vm.api.aiChat(text)
                                if (r.reply.isNotBlank()) messages = messages + AiMessage("assistant", r.reply)
                            } catch (_: Exception) {}
                            finally { busy = false }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) { Text("➤", fontSize = 18.sp, color = if (input.isBlank() || busy) lc.textTertiary else Color.Black) }
        }
    }
}
