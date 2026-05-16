package com.lipton.vpn.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.data.FaqCategory
import com.lipton.vpn.data.FaqItem
import com.lipton.vpn.data.FAQ_DATA
import com.lipton.vpn.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(onClose: () -> Unit, onOpenTelegram: () -> Unit) {
    val lc = LocalLiptonColors.current
    var query by remember { mutableStateOf("") }

    val filtered: List<FaqCategory> = remember(query) {
        if (query.isBlank()) FAQ_DATA
        else FAQ_DATA.mapNotNull { cat ->
            val items = cat.items.filter {
                it.question.contains(query, ignoreCase = true) ||
                it.answer.contains(query, ignoreCase = true)
            }
            if (items.isEmpty()) null else cat.copy(items = items)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState       = sheetState,
        containerColor   = lc.bgSheet,
        dragHandle = {
            Box(
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Green.copy(alpha = 0.3f))
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("FAQ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(lc.cardBg).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", fontSize = 14.sp, color = lc.textSecondary)
                }
            }

            // Search bar
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("Поиск по вопросам…", fontSize = 13.sp, color = lc.textTertiary) },
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Green.copy(alpha = 0.5f),
                    unfocusedBorderColor    = Green.copy(alpha = 0.15f),
                    focusedTextColor        = lc.textPrimary,
                    unfocusedTextColor      = lc.textPrimary,
                    cursorColor             = Green,
                    focusedContainerColor   = Green.copy(alpha = 0.04f),
                    unfocusedContainerColor = Green.copy(alpha = 0.04f),
                ),
                textStyle       = LocalTextStyle.current.copy(fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )

            Spacer(Modifier.height(8.dp))

            // FAQ list
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Text("Ничего не найдено", fontSize = 14.sp, color = lc.textTertiary)
                    }
                } else {
                    filtered.forEach { category -> FaqCategorySection(category = category, lc = lc) }
                }

                // Support CTA
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(lc.greenCard).border(1.dp, lc.greenBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenTelegram).padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("✈", fontSize = 22.sp)
                        Column {
                            Text("Не нашли ответ?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = lc.textPrimary)
                            Text("Напишите нам в Telegram", fontSize = 12.sp, color = lc.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FaqCategorySection(category: FaqCategory, lc: LiptonColors) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(category.emoji, fontSize = 14.sp)
            Text(
                category.title.uppercase(),
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp, color = Green.copy(alpha = 0.7f),
            )
        }
        category.items.forEach { item -> FaqItemRow(item = item, lc = lc) }
    }
}

@Composable
private fun FaqItemRow(item: FaqItem, lc: LiptonColors) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label         = "arrow",
    )
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(lc.cardBg).border(1.dp, lc.cardBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.question, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = lc.textPrimary, modifier = Modifier.weight(1f),
            )
            Text("▼", fontSize = 11.sp, color = lc.textTertiary, modifier = Modifier.rotate(arrowRotation))
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(tween(200)), exit = shrinkVertically(tween(150))) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 14.dp)
            ) {
                Text(text = item.answer, fontSize = 12.5.sp, color = lc.textSecondary, lineHeight = 18.sp)
            }
        }
    }
}
