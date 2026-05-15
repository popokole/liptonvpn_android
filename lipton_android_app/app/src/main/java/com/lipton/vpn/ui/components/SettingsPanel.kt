package com.lipton.vpn.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
    bypassRu:            Boolean,
    bypassDomains:       List<String>,
    autoConnectOnLaunch: Boolean,
    logLines:            List<String>,
    trialUsed:           Boolean,
    onBypassRuChange:    (Boolean) -> Unit,
    onAddDomain:         (String) -> Unit,
    onRemoveDomain:      (String) -> Unit,
    onAutoConnectChange: (Boolean) -> Unit,
    onClearLogs:         () -> Unit,
    onGetTrial:          suspend (Int) -> Unit,
    onBuyClick:          () -> Unit,
    onReset:             () -> Unit,
    onClose:             () -> Unit,
    onCheckUpdate:       suspend () -> Boolean,
) {
    val lc    = LocalLiptonColors.current
    val scope = rememberCoroutineScope()

    var showBypassDomains by remember { mutableStateOf(false) }
    var showLogs          by remember { mutableStateOf(false) }
    var confirmReset      by remember { mutableStateOf(false) }

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val handleClose: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onClose() }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState     = sheetState,
        containerColor = lc.bgSheet,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Green.copy(alpha = 0.3f))
            )
        },
    ) {
        if (showLogs) {
            LogsScreen(
                logLines = logLines,
                onClear  = onClearLogs,
                onBack   = { showLogs = false },
            )
        } else if (showBypassDomains) {
            BypassDomainsScreen(
                domains  = bypassDomains,
                onAdd    = onAddDomain,
                onRemove = onRemoveDomain,
                onBack   = { showBypassDomains = false },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // ── Header ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Настройки", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(lc.cardBg)
                            .border(1.dp, lc.cardBorder, RoundedCornerShape(50))
                            .clickable(onClick = handleClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", fontSize = 11.sp, color = lc.textSecondary)
                    }
                }

                // ── ПОДКЛЮЧЕНИЕ ──────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("ПОДКЛЮЧЕНИЕ")

                    SettingsToggleRow(
                        label    = "Подключаться при открытии",
                        subtitle = "Автоматически включать VPN при запуске приложения",
                        checked  = autoConnectOnLaunch,
                        onChange = onAutoConnectChange,
                    )
                }

                // ── ТРАФИК ───────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("ТРАФИК")

                    SettingsToggleRow(
                        label    = "Обход РУ трафика",
                        subtitle = "Банки, Госуслуги, ВКонтакте — без VPN",
                        checked  = bypassRu,
                        onChange = onBypassRuChange,
                    )

                    SettingsNavRow(
                        label    = "Исключения VPN",
                        subtitle = "Сайты, которые открываются напрямую",
                        badge    = if (bypassDomains.isNotEmpty()) "${bypassDomains.size}" else null,
                        onClick  = { showBypassDomains = true },
                    )
                }

                // ── ДОПОЛНИТЕЛЬНО ─────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("ДОПОЛНИТЕЛЬНО")

                    SettingsNavRow(
                        label    = "Логи подключения",
                        subtitle = "Вывод xray-core для диагностики",
                        badge    = if (logLines.isNotEmpty()) "${logLines.size}" else null,
                        onClick  = { showLogs = true },
                    )

                    UpdateCheckRow(scope = scope, onCheckUpdate = onCheckUpdate)
                }

                // ── ПРОБНЫЙ ДОСТУП ────────────────────────────────────────────
                if (!trialUsed) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel("ПРОБНЫЙ ДОСТУП")

                        TrialCard(
                            scope      = scope,
                            onGetTrial = onGetTrial,
                            onBuyClick = onBuyClick,
                        )
                    }
                }

                // ── СБРОС ─────────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("СБРОС")

                    AnimatedContent(
                        targetState = confirmReset,
                        transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(150))) },
                        label = "reset",
                    ) { confirming ->
                        if (confirming) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(RedSoft)
                                    .border(1.dp, Red.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    "Сбросит настройки VPN: обход РУ, исключения, автоподключение. Подписки сохранятся. Уверен?",
                                    fontSize = 13.sp,
                                    color = lc.textSecondary,
                                    lineHeight = 19.sp,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Red.copy(alpha = 0.18f))
                                            .border(1.dp, Red.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                            .clickable { onReset(); onClose() }
                                            .padding(vertical = 11.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("Сбросить", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Red)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(lc.cardBg)
                                            .border(1.dp, lc.cardBorder, RoundedCornerShape(10.dp))
                                            .clickable { confirmReset = false }
                                            .padding(vertical = 11.dp, horizontal = 20.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("Отмена", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = lc.textSecondary)
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(RedSoft)
                                    .border(1.dp, Red.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                    .clickable { confirmReset = true }
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("↺", fontSize = 15.sp, color = Red)
                                    Text("Сбросить профиль", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Red)
                                }
                            }
                        }
                    }
                }

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("разработка ", fontSize = 11.sp, color = lc.textTertiary)
                    Text("by popokole", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Green.copy(alpha = 0.4f))
                }
            }
        }
    }
}

// ─── Trial card (15 min) ──────────────────────────────────────────────────────

@Composable
private fun TrialCard(
    scope:      kotlinx.coroutines.CoroutineScope,
    onGetTrial: suspend (Int) -> Unit,
    onBuyClick: () -> Unit,
) {
    val lc      = LocalLiptonColors.current
    var loading by remember { mutableStateOf(false) }
    var error   by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Green.copy(alpha = 0.07f))
            .border(1.dp, Green.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⚡", fontSize = 20.sp)
            Column {
                Text("Попробовать бесплатно", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = lc.textPrimary)
                Text("15 минут без ограничений", fontSize = 11.5.sp, color = lc.textTertiary, lineHeight = 17.sp)
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
                            try { onGetTrial(15) }
                            catch (e: Exception) { error = e.message }
                            loading = false
                        }
                    }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Получить 15 минут", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(lc.cardBg)
                    .border(1.dp, lc.cardBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onBuyClick)
                    .padding(vertical = 11.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Купить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Green)
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.3.sp,
        color = Green.copy(alpha = 0.7f),
    )
}

@Composable
private fun SettingsToggleRow(
    label:    String,
    subtitle: String,
    checked:  Boolean,
    onChange: (Boolean) -> Unit,
) {
    val lc = LocalLiptonColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(lc.cardBg)
            .border(1.dp, lc.cardBorder, RoundedCornerShape(14.dp))
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = lc.textPrimary)
            Text(subtitle, fontSize = 11.5.sp, color = lc.textTertiary, lineHeight = 16.sp)
        }
        LiptonSwitch(checked = checked)
    }
}

@Composable
private fun SettingsNavRow(
    label:   String,
    subtitle: String,
    badge:   String? = null,
    onClick: () -> Unit,
) {
    val lc              = LocalLiptonColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed       by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh,
        ),
        label = "nav_scale",
    )
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) lc.cardHover else lc.cardBg,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "nav_bg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, lc.cardBorder, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = lc.textSecondary)
            Text(subtitle, fontSize = 11.5.sp, color = lc.textTertiary, lineHeight = 16.sp)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Green.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(badge, fontSize = 10.sp, color = Green, fontWeight = FontWeight.Bold)
                }
            }
            Text("›", fontSize = 20.sp, color = lc.textTertiary)
        }
    }
}

@Composable
fun LiptonSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)? = null) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Green else Color.White.copy(alpha = 0.1f),
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "track",
    )
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 18f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "thumb",
    )

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .let { m -> if (onCheckedChange != null) m.clickable { onCheckedChange(!checked) } else m }
    ) {
        Box(
            modifier = Modifier
                .padding(start = (2f + thumbOffset.coerceIn(0f, 18f)).dp, top = 2.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(50))
                .background(if (checked) Color.White else Color.White.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun BypassDomainsScreen(
    domains:  List<String>,
    onAdd:    (String) -> Unit,
    onRemove: (String) -> Unit,
    onBack:   () -> Unit,
) {
    val lc       = LocalLiptonColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(lc.cardBg)
                    .border(1.dp, lc.cardBorder, RoundedCornerShape(50))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", fontSize = 20.sp, color = lc.textSecondary)
            }
            Text("Исключения VPN", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
        }

        Text(
            "Домены в этом списке будут открываться напрямую, без VPN.",
            fontSize = 12.sp,
            color = lc.textTertiary,
            lineHeight = 18.sp,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value         = input,
                onValueChange = { input = it; error = null },
                placeholder   = { Text("example.com", fontSize = 13.sp, color = lc.textTertiary) },
                modifier      = Modifier.weight(1f),
                singleLine    = true,
                isError       = error != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Green.copy(alpha = 0.45f),
                    unfocusedBorderColor    = Green.copy(alpha = 0.12f),
                    focusedTextColor        = lc.textPrimary,
                    unfocusedTextColor      = lc.textPrimary,
                    cursorColor             = Green,
                    focusedContainerColor   = Green.copy(alpha = 0.04f),
                    unfocusedContainerColor = Green.copy(alpha = 0.04f),
                ),
                shape           = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    val d = input.trim().lowercase()
                    if (d.isBlank()) { error = "Введите домен"; return@KeyboardActions }
                    if (domains.contains(d)) { error = "Уже добавлен"; return@KeyboardActions }
                    onAdd(d); input = ""
                }),
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(listOf(Green, Green3))
                    )
                    .clickable {
                        keyboard?.hide()
                        val d = input.trim().lowercase()
                        if (d.isBlank()) { error = "Введите домен"; return@clickable }
                        if (domains.contains(d)) { error = "Уже добавлен"; return@clickable }
                        onAdd(d); input = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("+", fontSize = 22.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (error != null) Text(error!!, fontSize = 11.5.sp, color = Red)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp, max = 280.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(lc.cardBg)
                .border(1.dp, lc.cardBorder, RoundedCornerShape(14.dp))
                .verticalScroll(rememberScrollState()),
        ) {
            if (domains.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Список пуст", fontSize = 12.sp, color = lc.textTertiary)
                }
            } else {
                domains.forEachIndexed { i, domain ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = domain,
                            fontSize = 13.sp,
                            color = lc.textPrimary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { onRemove(domain) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕", fontSize = 12.sp, color = lc.textTertiary)
                        }
                    }
                    if (i < domains.lastIndex) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Green.copy(alpha = 0.06f)))
                    }
                }
            }
        }
    }
}

private enum class UpdateState { IDLE, CHECKING, UP_TO_DATE, FOUND, ERROR }

@Composable
private fun UpdateCheckRow(
    scope:         kotlinx.coroutines.CoroutineScope,
    onCheckUpdate: suspend () -> Boolean,
) {
    val lc = LocalLiptonColors.current
    var state by remember { mutableStateOf(UpdateState.IDLE) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(lc.cardBg)
            .border(1.dp, lc.cardBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = state != UpdateState.CHECKING) {
                scope.launch {
                    state = UpdateState.CHECKING
                    state = try {
                        if (onCheckUpdate()) UpdateState.FOUND else UpdateState.UP_TO_DATE
                    } catch (_: Exception) { UpdateState.ERROR }
                }
            }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Проверить обновление", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = lc.textPrimary)
            Text(
                text = when (state) {
                    UpdateState.IDLE       -> "v${com.lipton.vpn.BuildConfig.VERSION_NAME} — нажмите для проверки"
                    UpdateState.CHECKING   -> "Проверяем..."
                    UpdateState.UP_TO_DATE -> "Уже актуальная версия"
                    UpdateState.FOUND      -> "Доступно обновление!"
                    UpdateState.ERROR      -> "Ошибка проверки"
                },
                fontSize = 11.5.sp,
                color = when (state) {
                    UpdateState.FOUND  -> Green
                    UpdateState.ERROR  -> Red
                    else               -> lc.textTertiary
                },
            )
        }
        if (state == UpdateState.CHECKING) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Green, strokeWidth = 2.dp)
        } else {
            Text(
                text = when (state) {
                    UpdateState.FOUND      -> "↓"
                    UpdateState.UP_TO_DATE -> "✓"
                    else                   -> "↻"
                },
                fontSize = 16.sp,
                color = when (state) {
                    UpdateState.FOUND      -> Green
                    UpdateState.UP_TO_DATE -> Green
                    else                   -> lc.textTertiary
                },
            )
        }
    }
}
