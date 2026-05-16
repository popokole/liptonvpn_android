package com.lipton.vpn.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.ui.theme.*

data class WhatsNewFeature(val emoji: String, val title: String, val description: String)

private val FEATURES_1_1 = listOf(
    WhatsNewFeature("🔔", "Уведомления о трафике", "Получайте уведомления когда расходуете 50%, 80% и 100% трафика"),
    WhatsNewFeature("⏰", "Уведомления о подписке", "Напомним за 3 дня, 24 часа и 1 час до истечения"),
    WhatsNewFeature("📡", "Push при отключении", "Мгновенное уведомление с кнопкой переподключения"),
    WhatsNewFeature("🆕", "Метка NEW у серверов", "Новые серверы помечаются зелёным значком на 24 часа"),
    WhatsNewFeature("❓", "FAQ внутри приложения", "Ответы на частые вопросы без выхода в браузер"),
    WhatsNewFeature("📋", "Авто-импорт подписки", "Скопируйте ссылку — приложение предложит добавить её"),
    WhatsNewFeature("🎮", "Тема Hacker", "Новая тёмная тема с зелёным свечением в стиле терминала"),
    WhatsNewFeature("📳", "Haptic feedback", "Тактильный отклик при подключении и ошибках"),
)

@Composable
fun WhatsNewScreen(version: String, onDismiss: () -> Unit) {
    val lc = LocalLiptonColors.current
    var page by remember { mutableIntStateOf(0) }
    val features = FEATURES_1_1

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(lc.bgDeep, lc.bgCard))),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Green, Green3))),
                contentAlignment = Alignment.Center,
            ) {
                Text("🎉", fontSize = 32.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text("Что нового", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
            Text("Версия $version", fontSize = 14.sp, color = Green, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(32.dp))

            // Feature card
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    else
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                },
                label = "feature_page",
                modifier = Modifier.weight(1f),
            ) { idx ->
                val f = features[idx]
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(lc.bgCard).border(1.dp, lc.greenBorder, RoundedCornerShape(24.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(f.emoji, fontSize = 52.sp)
                    Spacer(Modifier.height(20.dp))
                    Text(f.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(f.description, fontSize = 14.sp, color = lc.textSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                features.indices.forEach { i ->
                    val active = i == page
                    val width by animateDpAsState(if (active) 24.dp else 6.dp, tween(200), label = "dot_w")
                    Box(
                        modifier = Modifier.height(6.dp).width(width).clip(RoundedCornerShape(3.dp))
                            .background(if (active) Green else Green.copy(alpha = 0.25f))
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Navigation buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (page > 0) {
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(lc.cardBg).border(1.dp, lc.cardBorder, RoundedCornerShape(14.dp))
                            .clickable { page-- }.padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("← Назад", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = lc.textSecondary)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Green, Green3)))
                        .clickable { if (page < features.lastIndex) page++ else onDismiss() }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (page < features.lastIndex) "Далее →" else "Начать",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
