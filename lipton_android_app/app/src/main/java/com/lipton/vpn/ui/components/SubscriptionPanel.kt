package com.lipton.vpn.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.data.model.Subscription
import com.lipton.vpn.data.model.UserInfo
import com.lipton.vpn.data.model.toReadableBytes
import com.lipton.vpn.data.model.usedBytes
import com.lipton.vpn.data.model.usedPercent
import com.lipton.vpn.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SubscriptionPanel(
    subscriptions: List<Subscription>,
    trialUsed:     Boolean,
    onAdd:         suspend (String) -> Unit,
    onRemove:      suspend (String) -> Unit,
    onRefresh:     suspend (String) -> Unit,
    onGetTrial:    suspend (Int) -> Unit,
    onBuyClick:    () -> Unit,
    modifier:      Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    var showAddForm by remember { mutableStateOf(false) }
    var addUrl      by remember { mutableStateOf("") }
    var addError    by remember { mutableStateOf<String?>(null) }
    var adding      by remember { mutableStateOf(false) }

    val hasMainSub = subscriptions.any { !it.isTrial }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ПОДПИСКИ",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = Green.copy(alpha = 0.7f),
            )
        }

        // Welcome card — показывается пока нет подписок и пробный не активирован
        if (subscriptions.isEmpty() && !trialUsed) {
            FirstLaunchCard(onGetTrial = onGetTrial, onBuyClick = onBuyClick, scope = scope)
        }

        // Subscription cards
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            subscriptions.forEach { sub ->
                SubscriptionCard(
                    sub       = sub,
                    scope     = scope,
                    onRemove  = { scope.launch { onRemove(sub.id) } },
                    onRefresh = { onRefresh(sub.id) },
                )
            }
        }

        // Add subscription row + buy buttons (hidden when welcome card is visible or main sub exists)
        val showFirstLaunchCard = subscriptions.isEmpty() && !trialUsed
        AnimatedContent(
            targetState = showAddForm,
            transitionSpec = {
                fadeIn(tween(250, easing = FastOutSlowInEasing))
                    .togetherWith(fadeOut(tween(180)))
            },
            label = "add_form",
        ) { isShowing ->
            if (isShowing) {
                AddSubForm(
                    url       = addUrl,
                    onUrlChange = { addUrl = it; addError = null },
                    error     = addError,
                    loading   = adding,
                    onSubmit  = {
                        scope.launch {
                            adding = true; addError = null
                            try {
                                onAdd(addUrl)
                                addUrl = ""; showAddForm = false
                            } catch (e: Exception) {
                                addError = e.message
                            }
                            adding = false
                        }
                    },
                    onDismiss = { showAddForm = false; addUrl = ""; addError = null },
                )
            } else {
                if (!hasMainSub && !showFirstLaunchCard) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlineActionButton(
                            text     = "+ Добавить подписку",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick  = { showAddForm = true },
                        )
                        OutlineActionButton(
                            text     = "Купить",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            accent   = true,
                            onClick  = onBuyClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstLaunchCard(
    onGetTrial: suspend (Int) -> Unit,
    onBuyClick: () -> Unit,
    scope:      kotlinx.coroutines.CoroutineScope,
) {
    val lc = LocalLiptonColors.current
    var loading by remember { mutableStateOf(false) }
    var error   by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Green.copy(alpha = 0.12f), Green.copy(alpha = 0.06f))
                )
            )
            .border(1.dp, Green.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🎁", fontSize = 22.sp)
            Column {
                Text(
                    "Добро пожаловать!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = lc.textPrimary,
                )
                Text(
                    "Получите 1 час бесплатного VPN",
                    fontSize = 12.sp,
                    color = lc.textSecondary,
                    lineHeight = 17.sp,
                )
            }
        }

        if (error != null) {
            Text(error!!, fontSize = 11.sp, color = Red)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Green, Green3)))
                    .clickable(enabled = !loading) {
                        scope.launch {
                            loading = true; error = null
                            try { onGetTrial(60) }
                            catch (e: Exception) { error = e.message }
                            loading = false
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Активировать 1 час", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(lc.cardBg)
                    .border(1.dp, lc.cardBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onBuyClick)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Купить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Green)
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    sub:      Subscription,
    scope:    kotlinx.coroutines.CoroutineScope,
    onRemove: () -> Unit,
    onRefresh: suspend () -> Unit,
) {
    val lc = LocalLiptonColors.current
    var confirmDelete  by remember { mutableStateOf(false) }
    var refreshing     by remember { mutableStateOf(false) }
    var refreshError   by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(lc.cardBg)
            .border(1.dp, lc.cardBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(GreenSoft)
                        .border(1.dp, lc.greenBorder, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", fontSize = 12.sp, color = Green, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(
                        text = sub.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = lc.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("${sub.servers.size} серверов", fontSize = 11.sp, color = lc.textTertiary)
                }
            }

            AnimatedContent(
                targetState = confirmDelete,
                transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(150))) },
                label = "confirm_delete",
            ) { confirming ->
                if (confirming) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(RedSoft)
                                .border(1.dp, Red.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                .clickable(onClick = onRemove)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("Удалить", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Red)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(lc.cardBg)
                                .border(1.dp, lc.cardBorder, RoundedCornerShape(20.dp))
                                .clickable { confirmDelete = false }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("Отмена", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = lc.textSecondary)
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniIconButton(
                            text    = if (refreshing) "…" else "↻",
                            enabled = !refreshing,
                            onClick = {
                                scope.launch {
                                    refreshing = true; refreshError = null
                                    try { onRefresh() }
                                    catch (e: Exception) { refreshError = e.message ?: "Ошибка обновления" }
                                    refreshing = false
                                }
                            },
                        )
                        MiniIconButton(text = "✕", danger = true, onClick = { confirmDelete = true })
                    }
                }
            }
        }

        if (refreshError != null) {
            Text(
                text     = refreshError!!,
                fontSize = 11.sp,
                color    = Red,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            )
        }

        if (sub.isTrial) {
            val expireMs = if (sub.userInfo.expire > 0) sub.userInfo.expire * 1000L
                          else sub.addedAt + 3_600_000L
            TrialCountdown(expireMs = expireMs)
        }
        TrafficBar(userInfo = sub.userInfo)
    }
}

@Composable
private fun TrialCountdown(expireMs: Long) {
    var remainingMs by remember { mutableLongStateOf(expireMs - System.currentTimeMillis()) }

    LaunchedEffect(expireMs) {
        while (remainingMs > 0) {
            kotlinx.coroutines.delay(500)
            remainingMs = expireMs - System.currentTimeMillis()
        }
    }

    val totalSec = if (remainingMs > 0) (remainingMs / 1000).toInt() else 0
    val minutes  = totalSec / 60
    val seconds  = totalSec % 60
    val expired  = remainingMs <= 0

    val timerColor = when {
        expired        -> Red
        totalSec < 300 -> Red
        else           -> Amber
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("⏱", fontSize = 12.sp)
        if (expired) {
            Text(
                "Пробный период истёк",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Red,
            )
        } else {
            Text(
                "Осталось %d:%02d".format(minutes, seconds),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = timerColor,
            )
        }
    }
}

@Composable
private fun TrafficBar(userInfo: UserInfo) {
    if (userInfo.total <= 0) return

    val lc      = LocalLiptonColors.current
    val used    = userInfo.usedBytes()
    val percent = userInfo.usedPercent()
    val barColor = when {
        percent > 0.85f -> Red
        percent > 0.6f  -> Amber
        else            -> Green
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Green.copy(alpha = 0.08f))
        ) {
            val animPercent by animateFloatAsState(
                targetValue = percent,
                animationSpec = tween(600, easing = FastOutSlowInEasing),
                label = "traffic_bar",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(animPercent)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(used.toReadableBytes(), fontSize = 11.sp, color = barColor, fontWeight = FontWeight.SemiBold)
            Text(userInfo.total.toReadableBytes(), fontSize = 11.sp, color = lc.textSecondary)
        }

        if (userInfo.expire > 0) {
            val expDate  = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(userInfo.expire * 1000L))
            val daysLeft = ((userInfo.expire * 1000L - System.currentTimeMillis()) / 86400000L).toInt()
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⏱", fontSize = 11.sp)
                Text("До $expDate", fontSize = 11.sp, color = lc.textSecondary)
                if (daysLeft <= 7) {
                    Text(
                        "($daysLeft дн.)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (daysLeft <= 2) Red else Amber,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSubForm(
    url:        String,
    onUrlChange: (String) -> Unit,
    error:      String?,
    loading:    Boolean,
    onSubmit:   () -> Unit,
    onDismiss:  () -> Unit,
) {
    val lc               = LocalLiptonColors.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester   = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(lc.cardBg)
            .border(1.dp, if (error != null) Red.copy(alpha = 0.3f) else lc.cardBorder, RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value         = url,
                onValueChange = onUrlChange,
                placeholder   = { Text("https://sub.popokole.online/...", fontSize = 12.sp, color = lc.textTertiary) },
                modifier      = Modifier.weight(1f).focusRequester(focusRequester),
                singleLine    = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Green.copy(alpha = 0.5f),
                    unfocusedBorderColor    = Green.copy(alpha = 0.1f),
                    focusedTextColor        = lc.textPrimary,
                    unfocusedTextColor      = lc.textPrimary,
                    cursorColor             = Green,
                    focusedContainerColor   = Green.copy(alpha = 0.04f),
                    unfocusedContainerColor = Green.copy(alpha = 0.04f),
                ),
                shape           = RoundedCornerShape(10.dp),
                textStyle       = LocalTextStyle.current.copy(fontSize = 12.5.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); onSubmit() }),
                isError         = error != null,
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Green, Green3)))
                    .clickable(enabled = !loading) { keyboardController?.hide(); onSubmit() },
                contentAlignment = Alignment.Center,
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("→", fontSize = 20.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (error != null) Text(error, fontSize = 11.5.sp, color = Red)

        TextButton(onClick = onDismiss, contentPadding = PaddingValues(0.dp)) {
            Text("Отмена", fontSize = 12.sp, color = lc.textTertiary)
        }
    }
}

@Composable
private fun MiniIconButton(text: String, danger: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val lc = LocalLiptonColors.current
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (danger) RedSoft else lc.cardBg)
            .border(1.dp, if (danger) Red.copy(alpha = 0.18f) else lc.cardBorder, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 13.sp,
            color = when {
                !enabled -> lc.textTertiary
                danger   -> Red
                else     -> lc.textSecondary
            },
        )
    }
}

@Composable
private fun OutlineActionButton(
    text:     String,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
    accent:   Boolean  = false,
) {
    val lc = LocalLiptonColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(lc.bgCard)
            .border(1.dp, lc.cardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text     = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color    = if (accent) Green else lc.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
