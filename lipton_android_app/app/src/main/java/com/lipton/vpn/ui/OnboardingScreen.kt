package com.lipton.vpn.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.foundation.clickable

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val lc = LocalLiptonColors.current

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.55f,
        targetValue  = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lc.bgDeep),
        contentAlignment = Alignment.Center,
    ) {
        // Glow
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Green.copy(alpha = pulse * 0.18f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Green, Green3))),
                contentAlignment = Alignment.Center,
            ) {
                Text("L", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Добро пожаловать!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = lc.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "LiptonVPN установлен. Вот как пользоваться виджетами для быстрого доступа.",
                    fontSize = 13.sp,
                    color = lc.textTertiary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }

            // Steps
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OnboardingStep(
                    number = "1",
                    title  = "Плашка в шторке уведомлений",
                    body   = "Смахните вниз → нажмите карандаш ✏ → найдите «LiptonVPN» → перетащите в активные плитки.",
                )
                OnboardingStep(
                    number = "2",
                    title  = "Виджет на рабочем столе",
                    body   = "Зажмите пустое место на экране → «Виджеты» → найдите «LiptonVPN» → перетащите на экран.",
                )
                OnboardingStep(
                    number = "3",
                    title  = "Добавьте подписку",
                    body   = "Нажмите «+ Добавить подписку» на главном экране и вставьте ссылку. Или купите — кнопка рядом.",
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Green, Green3)))
                    .clickable(onClick = onFinish)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Начать",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
            }
        }
    }
}

@Composable
private fun OnboardingStep(number: String, title: String, body: String) {
    val lc = LocalLiptonColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(lc.cardBg)
            .border(1.dp, lc.cardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Green.copy(alpha = 0.18f))
                .border(1.dp, Green.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Green)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = lc.textPrimary)
            Text(body, fontSize = 12.sp, color = lc.textTertiary, lineHeight = 17.sp)
        }
    }
}
