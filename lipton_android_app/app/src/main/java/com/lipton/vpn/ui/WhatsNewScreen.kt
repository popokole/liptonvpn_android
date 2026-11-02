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
import com.lipton.vpn.ui.theme.LiptonSize
import androidx.compose.ui.graphics.vector.ImageVector
import com.lipton.vpn.ui.components.LiptonIcons

data class WhatsNewFeature(val icon: ImageVector, val title: String, val description: String)

private val FEATURES_1_1 = listOf(
    WhatsNewFeature(LiptonIcons.Bell, "Уведомления о трафике", "Получайте уведомления когда расходуете 50%, 80% и 100% трафика"),
    WhatsNewFeature(LiptonIcons.Timer, "Уведомления о подписке", "Напомним за 3 дня, 24 часа и 1 час до истечения"),
    WhatsNewFeature(LiptonIcons.Wifi, "Push при отключении", "Мгновенное уведомление с кнопкой переподключения"),
    WhatsNewFeature(LiptonIcons.Star, "Метка NEW у серверов", "Новые серверы помечаются зелёным значком на 24 часа"),
    WhatsNewFeature(LiptonIcons.Info, "FAQ внутри приложения", "Ответы на частые вопросы без выхода в браузер"),
    WhatsNewFeature(LiptonIcons.Copy, "Авто-импорт подписки", "Скопируйте ссылку — приложение предложит добавить её"),
    WhatsNewFeature(LiptonIcons.Terminal, "Тема Hacker", "Новая тёмная тема с зелёным свечением в стиле терминала"),
    WhatsNewFeature(LiptonIcons.Vibrate, "Haptic feedback", "Тактильный отклик при подключении и ошибках"),
)

private val FEATURES_1_1_6 = listOf(
    WhatsNewFeature(LiptonIcons.Shield, "Обход глушилок", "Добавлена поддержка протоколов XHTTP и SplitHTTP — стабильная работа даже при активных блокировках и ТСПУ"),
    WhatsNewFeature(LiptonIcons.Globe, "Новые транспорты", "Клиент теперь поддерживает все современные транспорты: xhttp, splithttp, TCP с HTTP-обфускацией"),
    WhatsNewFeature(LiptonIcons.Monitor, "YouTube и стриминг", "Оптимизирован MTU туннеля — видео и стриминг работают без буферизации"),
)

private val FEATURES_1_3 = listOf(
    WhatsNewFeature(LiptonIcons.User, "Вход в аккаунт", "Войдите по коду с сайта, почте или через Telegram — подписка подтянется автоматически, без ручных ссылок"),
    WhatsNewFeature(LiptonIcons.Card, "Оплата внутри приложения", "Выбирайте тариф и срок и оплачивайте прямо здесь — статус платежа обновится сам"),
    WhatsNewFeature(LiptonIcons.Megaphone, "Новости", "Лента новостей Lipton VPN теперь доступна прямо в приложении"),
    WhatsNewFeature(LiptonIcons.Chat, "Поддержка и ИИ-помощник", "Задайте вопрос — ответит ИИ, при необходимости подключится оператор"),
)

private fun featuresForVersion(version: String) = when {
    version.startsWith("1.3") || version >= "1.3" -> FEATURES_1_3
    version.startsWith("1.1.6") || version > "1.1.5" -> FEATURES_1_1_6
    else -> FEATURES_1_1
}

@Composable
fun WhatsNewScreen(version: String, onDismiss: () -> Unit) {
    val lc = LocalLiptonColors.current
    var page by remember { mutableIntStateOf(0) }
    val features = featuresForVersion(version)

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
                Icon(LiptonIcons.Gift, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Что нового", fontSize = LiptonSize.title1, fontWeight = FontWeight.Bold, color = lc.textPrimary)
            Text("Версия $version", fontSize = LiptonSize.subhead, color = Green, fontWeight = FontWeight.Medium)
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
                    Icon(f.icon, contentDescription = null, tint = Green, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(20.dp))
                    Text(f.title, fontSize = LiptonSize.title3, fontWeight = FontWeight.Bold, color = lc.textPrimary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(f.description, fontSize = LiptonSize.subhead, color = lc.textSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Dots
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                features.indices.forEach { i ->
                    val active = i == page
                    val width by animateDpAsState(if (active) 24.dp else 6.dp, tween(200), label = "dot_w")
                    Box(
                        modifier = Modifier.height(6.dp).width(width).clip(RoundedCornerShape(4.dp))
                            .background(if (active) Green else Green.copy(alpha = 0.25f))
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Navigation buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (page > 0) {
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                            .background(lc.cardBg).border(1.dp, lc.cardBorder, RoundedCornerShape(16.dp))
                            .clickable { page-- }.padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("← Назад", fontSize = LiptonSize.subhead, fontWeight = FontWeight.Medium, color = lc.textSecondary)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Green, Green3)))
                        .clickable { if (page < features.lastIndex) page++ else onDismiss() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (page < features.lastIndex) "Далее →" else "Начать",
                        fontSize = LiptonSize.subhead, fontWeight = FontWeight.Bold, color = Color.Black,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
