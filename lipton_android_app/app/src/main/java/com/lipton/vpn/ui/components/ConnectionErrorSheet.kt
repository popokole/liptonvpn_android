package com.lipton.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.ConnectionError
import com.lipton.vpn.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionErrorSheet(
    error:          ConnectionError,
    onDismiss:      () -> Unit,
    onRetry:        () -> Unit,
    onSwitchServer: () -> Unit,
    onHelp:         () -> Unit,
) {
    val lc = LocalLiptonColors.current

    data class ErrorInfo(val icon: String, val title: String, val description: String)
    val info = when (error) {
        ConnectionError.Timeout -> ErrorInfo(
            "⏱",
            "Время подключения истекло",
            "Сервер не ответил за 20 секунд. Попробуйте другой сервер или повторите позже.",
        )
        ConnectionError.NoInternet -> ErrorInfo(
            "📡",
            "Нет соединения с интернетом",
            "Проверьте подключение к Wi-Fi или мобильным данным.",
        )
        ConnectionError.DnsFail -> ErrorInfo(
            "🔍",
            "Ошибка DNS",
            "Не удалось разрешить адрес сервера. Проверьте интернет или смените сервер.",
        )
        ConnectionError.ServerUnreachable -> ErrorInfo(
            "🚫",
            "Сервер недоступен",
            "Не удалось установить соединение. Сервер может быть временно недоступен.",
        )
        ConnectionError.XrayCrash -> ErrorInfo(
            "💥",
            "Ошибка ядра VPN",
            "Ядро xray завершилось с ошибкой. Попробуйте другой сервер.",
        )
        ConnectionError.Tun2socksFail -> ErrorInfo(
            "⚙",
            "Ошибка VPN-интерфейса",
            "Не удалось настроить VPN. Перезапустите приложение.",
        )
        is ConnectionError.Unknown -> ErrorInfo(
            "⚠",
            "Ошибка подключения",
            "Не удалось установить VPN соединение. Проверьте сервер и интернет.",
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = lc.bgSheet,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(info.icon, fontSize = 42.sp)

            Text(
                text       = info.title,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = lc.textPrimary,
                textAlign  = TextAlign.Center,
            )

            Text(
                text       = info.description,
                fontSize   = 13.sp,
                color      = lc.textSecondary,
                textAlign  = TextAlign.Center,
                lineHeight = 19.sp,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Green, Green3)))
                    .clickable(onClick = onRetry)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "↻  Повторить",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.Black,
                )
            }

            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(lc.greenCard)
                        .border(1.dp, lc.greenBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onSwitchServer)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Сменить сервер",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = lc.textPrimary,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(lc.greenCard)
                        .border(1.dp, lc.greenBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onHelp)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "✈  Помощь",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = lc.textPrimary,
                    )
                }
            }
        }
    }
}
