package com.lipton.vpn.ui.account

import android.content.Intent
import android.net.Uri
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
import com.lipton.vpn.UiState
import com.lipton.vpn.data.ApiClient
import com.lipton.vpn.data.model.*
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

// ─── Панель аккаунта (заменяет ручное добавление ссылки) ─────────────────────

@Composable
fun AccountPanel(
    state: UiState,
    onPay: () -> Unit,
    onNews: () -> Unit,
    onSupport: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
) {
    val lc = LocalLiptonColors.current
    val active = state.accountStatus == "active" || state.accountStatus == "trial" || state.accountStatus == "grace"

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

@Composable
private fun SheetScaffold(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val lc = LocalLiptonColors.current
    Column(Modifier.fillMaxSize().background(lc.bgDeep)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("‹", fontSize = 30.sp, color = lc.textSecondary,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClose).padding(horizontal = 8.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
        }
        content()
    }
}

// ─── Экран оплаты ────────────────────────────────────────────────────────────

@Composable
fun PaymentScreen(vm: MainViewModel, onClose: () -> Unit) {
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
                                        } catch (e: ApiClient.ApiException) { err = e.message }
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

// ─── Экран новостей ──────────────────────────────────────────────────────────

@Composable
fun NewsScreen(vm: MainViewModel, onClose: () -> Unit) {
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
                Modifier.fillMaxSize().padding(16.dp),
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
