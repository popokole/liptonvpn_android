package com.lipton.vpn.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.MainViewModel
import com.lipton.vpn.UiState
import com.lipton.vpn.data.groupPaymentsByMonth
import com.lipton.vpn.data.isFailed
import com.lipton.vpn.data.isRefund
import com.lipton.vpn.data.model.TxItem
import com.lipton.vpn.data.parseIsoMillis
import com.lipton.vpn.data.ruWhen
import com.lipton.vpn.data.summarizePayments
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.ButtonTone
import com.lipton.vpn.ui.components.GhostButton
import com.lipton.vpn.ui.components.GlassCard
import com.lipton.vpn.ui.components.GlassGroup
import com.lipton.vpn.ui.components.GroupDivider
import com.lipton.vpn.ui.components.IconTile
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.PrimaryButton
import com.lipton.vpn.ui.components.SectionHeader
import com.lipton.vpn.ui.components.ToneChip
import com.lipton.vpn.ui.components.glass
import com.lipton.vpn.ui.components.softGlow
import com.lipton.vpn.ui.components.stateText
import com.lipton.vpn.ui.components.stateTone
import com.lipton.vpn.ui.tabs.daysLeft
import com.lipton.vpn.ui.tabs.pluralDays
import com.lipton.vpn.ui.tabs.pluralRu
import com.lipton.vpn.ui.tabs.ruDayMonth
import com.lipton.vpn.ui.tabs.rubles
import com.lipton.vpn.ui.tabs.tariffTitleFor
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.TABULAR_NUMS
import com.lipton.vpn.ui.theme.UnboundedFamily

// ─────────────────────────────────────────────────────────────────────────────
//  История платежей (new-scr-payments) и способ оплаты с отвязкой карты
//  (new-combo-payment-method / -unlink). Кулдаун отвязки 24 ч — для всех:
//  заранее по card_unlink_available_at и по ответу 409 card_unlink_cooldown.
// ─────────────────────────────────────────────────────────────────────────────

private fun txTitle(t: TxItem, tariff: String?): String {
    val title = t.tariffTitle?.takeIf { it.isNotBlank() } ?: tariff
    val days = t.periodDays?.takeIf { it > 0 }
    return when {
        t.isRefund() -> listOfNotNull("Возврат", title).joinToString(": ")
        t.kind == "change" -> if (title != null) "Смена тарифа: $title" else "Смена тарифа"
        t.kind == "overlay" -> if (title != null) "Временный тариф: $title" else "Временный тариф"
        title != null && days != null -> "$title · ${pluralDays(days)}"
        title != null -> title
        t.kind == "initial" -> "Оплата подписки"
        t.kind in setOf("subscription", "renewal", "renew") -> "Продление подписки"
        t.kind == "manual" -> "Списание"
        else -> "Платёж"
    }
}

@Composable
fun PaymentsHistoryScreen(state: UiState, viewModel: MainViewModel, onBack: () -> Unit) {
    val c = LiptonTheme.colors
    val fx = LocalScreenFixtures.current
    var items by remember { mutableStateOf(fx?.transactions) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        if (fx?.transactions != null) return@LaunchedEffect
        try { items = viewModel.api.getTransactions().transactions } catch (e: Exception) { error = e.message ?: "Не удалось загрузить" }
    }
    val fallbackTariff = tariffTitleFor(state.accountTariffCode, state.appConfig)
    ProfileSubPage(state, "История платежей", onBack) {
        val list = items
        when {
            list == null && error == null -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.stateTone(AuroraTone.ON).a, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }
            list == null -> GlassCard(Modifier.fillMaxWidth()) { Text(error ?: "", style = MaterialTheme.typography.bodyMedium, color = c.text.copy(alpha = 0.72f)) }
            list.isEmpty() -> GlassCard(Modifier.fillMaxWidth()) {
                Text("Платежей пока нет", style = MaterialTheme.typography.titleSmall, color = c.text)
                Text("Чек об оплате ЮKassa присылает на почту.", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.7f))
            }
            else -> {
                SummaryCard(list)
                groupPaymentsByMonth(list).forEach { (month, txs) ->
                    SectionHeader(month, Modifier.padding(top = 26.dp, bottom = 8.dp))
                    GlassGroup {
                        txs.forEachIndexed { i, t ->
                            if (i > 0) GroupDivider()
                            TxRow(t, fallbackTariff)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                val email = state.me?.email?.takeIf { it.isNotBlank() }
                NoteLine(LiptonIcons.Mail, if (email != null) "Копии чеков приходят на $email" else "Чеки ЮKassa присылает на почту")
            }
        }
    }
}

@Composable
private fun SummaryCard(list: List<TxItem>) {
    val c = LiptonTheme.colors
    val s = summarizePayments(list)
    val t = c.stateTone(AuroraTone.ON)
    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(LiptonIcons.Receipt, null, tint = c.stateText(AuroraTone.ON), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text("Всего оплачено", style = MaterialTheme.typography.labelMedium, color = c.text.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
            s.sinceMs?.let { ms ->
                Text("с ${ruDayMonth(ms)} ${java.util.Calendar.getInstance().apply { timeInMillis = ms }.get(java.util.Calendar.YEAR)}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            buildAnnotatedString {
                append(rubles(s.totalKopeks).replace("₽", "").trim())
                withStyle(SpanStyle(fontSize = 24.sp, color = c.text.copy(alpha = 0.6f))) { append(" ₽") }
            },
            style = TextStyle(fontFamily = UnboundedFamily, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, fontFeatureSettings = TABULAR_NUMS),
            color = c.text,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (s.paid > 0) ToneChip("${s.paid} ${pluralRu(s.paid, "оплата", "оплаты", "оплат")}", t.a, dot = true, textColor = c.stateText(AuroraTone.ON))
            if (s.refunds > 0) ToneChip("${s.refunds} ${pluralRu(s.refunds, "возврат", "возврата", "возвратов")}", c.blue, dot = true, textColor = c.text.copy(alpha = 0.85f))
            if (s.failed > 0) ToneChip("${s.failed} не ${pluralRu(s.failed, "прошёл", "прошли", "прошли")}", c.warn, dot = true, textColor = c.stateText(AuroraTone.OFF))
        }
    }
}

@Composable
private fun TxRow(t: TxItem, fallbackTariff: String?) {
    val c = LiptonTheme.colors
    val failed = t.isFailed()
    val refund = t.isRefund()
    val pending = !failed && !refund && t.status != "succeeded"
    val icon: ImageVector = when {
        failed -> LiptonIcons.Alert
        refund -> LiptonIcons.Refresh
        t.kind == "change" || t.kind == "overlay" -> LiptonIcons.Shuffle
        else -> LiptonIcons.Crown
    }
    val (label, color) = when {
        failed -> "Не прошёл" to c.warn
        refund -> "Возврат" to c.blue
        pending -> "В обработке" to c.text.copy(alpha = 0.5f)
        else -> "Оплачено" to c.stateTone(AuroraTone.ON).a
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, tint = if (failed) c.warn else c.accentForTile())
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(txTitle(t, fallbackTariff), style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToneChip(label, color, textColor = if (failed) c.stateText(AuroraTone.OFF) else if (refund) c.text.copy(alpha = 0.85f) else c.stateText(AuroraTone.ON), height = 20.dp)
                val date = ruDayMonth(t.createdAt)
                Text(
                    listOfNotNull(date, if (t.kind == "change") "доплата" else null, t.cardLast4?.let { "•••• $it" }).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.58f), maxLines = 1,
                )
            }
        }
        Text(
            rubles(t.amountKopeks),
            style = MaterialTheme.typography.titleSmall.copy(
                fontFeatureSettings = TABULAR_NUMS,
                textDecoration = if (failed) TextDecoration.LineThrough else null,
            ),
            color = if (failed) c.text.copy(alpha = 0.5f) else c.text,
        )
    }
}

private fun com.lipton.vpn.ui.theme.LiptonColors.accentForTile(): Color = if (isDark) accent else accentDeep

// ─── Способ оплаты и отвязка карты ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(state: UiState, viewModel: MainViewModel, onBack: () -> Unit, onPay: () -> Unit) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    val fx = LocalScreenFixtures.current
    val me = state.me
    var sheet by remember { mutableStateOf(fx?.showUnlinkSheet == true) }
    LaunchedEffect(Unit) { if (fx == null) viewModel.loadProfile() }
    val hasCard = me?.hasCard == true
    val unlinkAt = parseIsoMillis(state.cardUnlinkAvailableAt) ?: parseIsoMillis(me?.cardUnlinkAvailableAt)
    val now = System.currentTimeMillis()
    val cooldown = unlinkAt != null && unlinkAt > now
    val periodEnd = ruDayMonth(state.accountPeriodEnd)
    val nextAtMs = parseIsoMillis(me?.nextChargeAt) ?: parseIsoMillis(state.accountPeriodEnd)
    val nextAt = nextAtMs?.let { ruDayMonth(it) }
    val nextSum = me?.nextChargeKopeks?.let { rubles(it) }
    val nextDays = me?.nextChargePeriodDays
    val tariff = me?.nextChargeTariffTitle?.takeIf { it.isNotBlank() } ?: tariffTitleFor(state.accountTariffCode, state.appConfig)

    ProfileSubPage(state, "Способ оплаты", onBack) {
        if (hasCard) {
            BankCard(last4 = me?.cardLast4.orEmpty(), exp = me?.cardExp, brand = me?.cardBrand)
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(LiptonIcons.Refresh, null, tint = c.stateText(AuroraTone.ON), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Автопродление", style = MaterialTheme.typography.labelMedium, color = c.text.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
                    ToneChip("Включено", t.a, dot = true, textColor = c.stateText(AuroraTone.ON))
                }
                if (nextAt != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Следующее списание", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            nextAt + (nextSum?.let { " — $it" } ?: ""),
                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TABULAR_NUMS), color = c.text,
                            modifier = Modifier.weight(1f),
                        )
                        daysLeft(me?.nextChargeAt ?: state.accountPeriodEnd)?.let { d ->
                            Text("через ${pluralDays(d)}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f))
                        }
                    }
                    val sub = listOfNotNull(tariff, nextDays?.let { pluralDays(it) }).joinToString(" · ")
                    if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.6f))
                }
            }
            me?.email?.takeIf { it.isNotBlank() }?.let { email ->
                Spacer(Modifier.height(12.dp))
                GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconTile(LiptonIcons.Receipt)
                        Column {
                            Text("Чек приходит на почту", style = MaterialTheme.typography.titleSmall, color = c.text)
                            Text(email, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.6f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            NoteLine(LiptonIcons.ShieldCheck, "Платежи через ЮKassa, данные карты у нас не хранятся", color = c.text.copy(alpha = 0.6f))
            Spacer(Modifier.height(18.dp))
            if (cooldown) {
                NoteLine(LiptonIcons.Timer, "Отвязать карту можно будет ${ruWhen(unlinkAt ?: now)} — через 24 часа после новой привязки", color = c.stateText(AuroraTone.OFF))
                Spacer(Modifier.height(6.dp))
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GhostButton(
                    "Отвязать карту", onClick = { sheet = true }, icon = LiptonIcons.Unlink,
                    color = c.stateText(AuroraTone.OFF).copy(alpha = if (cooldown) 0.45f else 1f),
                    enabled = !cooldown && state.profileBusy == null,
                )
            }
        } else {
            NoCard()
            Spacer(Modifier.height(16.dp))
            Text(
                "Карта привяжется при оплате картой — тогда подписка будет продлеваться сама. Отвязать её можно здесь в любой момент.",
                style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton(if (state.accountStatus == "active" || state.accountStatus == "grace") "Продлить" else "Оформить подписку", onClick = onPay, trailingIcon = LiptonIcons.ArrowRight, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            NoteLine(LiptonIcons.ShieldCheck, "Платежи через ЮKassa, данные карты у нас не хранятся")
        }
    }

    if (sheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (state.profileBusy != "card") sheet = false },
            sheetState = sheetState,
            containerColor = c.surfaceSheet,
            scrimColor = Color.Black.copy(alpha = 0.55f),
            dragHandle = { Box(Modifier.padding(top = 10.dp).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(c.text.copy(alpha = 0.25f))) },
        ) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 20.dp)) {
                val warn = c.stateTone(AuroraTone.OFF).a
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(warn.copy(alpha = 0.14f)).border(1.dp, warn.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(LiptonIcons.Alert, null, tint = c.stateText(AuroraTone.OFF), modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.height(16.dp))
                Text("Отвязать карту?", style = MaterialTheme.typography.headlineSmall, color = c.text)
                Spacer(Modifier.height(10.dp))
                if (nextAt != null) {
                    Text(
                        buildAnnotatedString {
                            append("Если не отвязать, ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(nextAt) }
                            append(" подписка продлится")
                            nextDays?.let { d -> append(if (d in 28..31) " на месяц" else " на ${pluralDays(d)}") }
                            if (nextSum != null) { append(" — "); withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(nextSum) } }
                            append(".")
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = c.text,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Text(
                    "После отвязки автопродление выключится, а карта исчезнет из способов оплаты." + (periodEnd?.let { " Подписка будет работать до $it." } ?: ""),
                    style = MaterialTheme.typography.bodyMedium, color = c.text.copy(alpha = 0.72f),
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.text.copy(alpha = 0.04f)).border(1.dp, c.text.copy(alpha = 0.10f), RoundedCornerShape(14.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(LiptonIcons.Timer, null, tint = c.text.copy(alpha = 0.6f), modifier = Modifier.size(14.dp).padding(top = 1.dp))
                    Text("Отвязать карту снова получится только через 24 часа после новой привязки.", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = c.text.copy(alpha = 0.7f))
                }
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Оставить карту", onClick = { sheet = false }, tone = ButtonTone.ACCENT, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                val busy = state.profileBusy == "card"
                val sh = RoundedCornerShape(999.dp)
                Row(
                    Modifier.fillMaxWidth().height(52.dp).clip(sh).background(warn.copy(alpha = 0.06f)).border(1.dp, warn.copy(alpha = 0.45f), sh)
                        .clickable(enabled = !busy, role = Role.Button) { viewModel.unlinkCard { sheet = false } },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (busy) CircularProgressIndicator(color = warn, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else Text("Отвязать", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = c.stateText(AuroraTone.OFF))
                }
            }
        }
    }
}

/** Карта по макету: чип, бесконтактный значок, «•••• 4242», срок, бренд. */
@Composable
private fun BankCard(last4: String, exp: String?, brand: String?) {
    val c = LiptonTheme.colors
    val t = c.stateTone(AuroraTone.ON)
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.75f)
            .drawBehind { softGlow(t.a.copy(alpha = if (c.isDark) 0.22f else 0.16f), 24.dp.toPx(), 18.dp.toPx(), offsetY = 6.dp.toPx()) }
            .clip(shape)
            .background(
                if (c.isDark) Brush.linearGradient(listOf(Color(0xFF0E1A16), Color(0xFF0B2A22), Color(0xFF07130F)))
                else Brush.linearGradient(listOf(Color(0xFFF7FFFB), Color(0xFFDDF7EC), Color(0xFFE4F1FF)))
            )
            .border(1.dp, Brush.linearGradient(listOf(t.a.copy(alpha = 0.7f), t.b.copy(alpha = 0.3f))), shape)
            .drawBehind {
                // декоративные дуги
                listOf(0.55f, 0.8f, 1.05f).forEach { k ->
                    drawCircle(c.text.copy(alpha = 0.06f), radius = size.width * k * 0.5f, center = Offset(size.width * 1.0f, size.height * 1.05f), style = Stroke(1.dp.toPx()))
                }
            }
            .padding(20.dp),
    ) {
        // чип
        Canvas(Modifier.size(width = 40.dp, height = 30.dp).align(Alignment.TopStart)) {
            drawRoundRect(Brush.linearGradient(listOf(Color(0xFFE9E4D8), Color(0xFFB9B3A6))), cornerRadius = CornerRadius(6.dp.toPx()))
            val line = Color(0x55000000)
            drawLine(line, Offset(size.width * 0.33f, 0f), Offset(size.width * 0.33f, size.height), 1f)
            drawLine(line, Offset(size.width * 0.66f, 0f), Offset(size.width * 0.66f, size.height), 1f)
            drawLine(line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1f)
            drawRoundRect(line, topLeft = Offset(size.width * 0.33f, size.height * 0.25f), size = Size(size.width * 0.33f, size.height * 0.5f), cornerRadius = CornerRadius(3f), style = Stroke(1f))
        }
        Icon(LiptonIcons.Wifi, null, tint = c.text.copy(alpha = 0.7f), modifier = Modifier.size(22.dp).align(Alignment.TopEnd))
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(
                "•••• $last4",
                style = TextStyle(fontFamily = UnboundedFamily, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = 1.sp, fontFeatureSettings = TABULAR_NUMS),
                color = c.text,
            )
            Spacer(Modifier.height(8.dp))
            if (!exp.isNullOrBlank()) Text("действует до $exp", style = MaterialTheme.typography.bodySmall, color = c.text.copy(alpha = 0.65f))
        }
        if (!brand.isNullOrBlank()) {
            Text(
                brand.uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic),
                color = c.text, modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

/** Пунктирная карточка «Карта не привязана». */
@Composable
private fun NoCard() {
    val c = LiptonTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(2.4f)
            .drawBehind {
                drawRoundRect(
                    c.text.copy(alpha = 0.28f), cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(LiptonIcons.Card, null, tint = c.text.copy(alpha = 0.6f), modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text("Карта не привязана", style = MaterialTheme.typography.titleMedium, color = c.text)
        }
    }
}
