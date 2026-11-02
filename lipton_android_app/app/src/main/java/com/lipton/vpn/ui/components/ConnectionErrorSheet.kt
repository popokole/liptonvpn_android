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
import com.lipton.vpn.ui.theme.LiptonSize
import androidx.compose.ui.graphics.vector.ImageVector

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

    data class ErrorInfo(val icon: ImageVector, val title: String, val description: String)
    val info = when (error) {
        ConnectionError.Timeout -> ErrorInfo(
            LiptonIcons.Timer,
            "Время подключения истекло",
            "Сервер не ответил за 20 секунд. Попробуйте другой сервер или повторите позже.",
        )
        ConnectionError.NoInternet -> ErrorInfo(
            LiptonIcons.Wifi,
            "Нет соединения с интернетом",
            "Проверьте подключение к Wi-Fi или мобильным данным.",
        )
        ConnectionError.DnsFail -> ErrorInfo(
            LiptonIcons.Search,
            "Ошибка DNS",
            "Не удалось разрешить адрес сервера. Проверьте интернет или смените сервер.",
        )
        ConnectionError.ServerUnreachable -> ErrorInfo(
            LiptonIcons.XCircle,
            "Сервер недоступен",
            "Не удалось установить соединение. Сервер может быть временно недоступен.",
        )
        ConnectionError.XrayCrash -> ErrorInfo(
            LiptonIcons.Alert,
            "Ошибка ядра VPN",
            "Ядро xray завершилось с ошибкой. Попробуйте другой сервер.",
        )
        ConnectionError.Tun2socksFail -> ErrorInfo(
            LiptonIcons.Terminal,
            "Ошибка VPN-интерфейса",
            "Не удалось настроить VPN. Перезапустите приложение.",
        )
        is ConnectionError.Unknown -> ErrorInfo(
            LiptonIcons.Alert,
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
            Icon(info.icon, contentDescription = null, tint = Green, modifier = Modifier.size(40.dp))

            Text(
                text       = info.title,
                fontSize = LiptonSize.body,
                fontWeight = FontWeight.Bold,
                color      = lc.textPrimary,
                textAlign  = TextAlign.Center,
            )

            Text(
                text       = info.description,
                fontSize = LiptonSize.footnote,
                color      = lc.textSecondary,
                textAlign  = TextAlign.Center,
                lineHeight = 19.sp,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Green, Green3)))
                    .clickable(onClick = onRetry)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "↻  Повторить",
                    fontSize = LiptonSize.subhead,
                    fontWeight = FontWeight.Bold,
                    color      = Color.Black,
                )
            }

            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(lc.greenCard)
                        .border(1.dp, lc.greenBorder, RoundedCornerShape(16.dp))
                        .clickable(onClick = onSwitchServer)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Сменить сервер",
                        fontSize = LiptonSize.footnote,
                        fontWeight = FontWeight.SemiBold,
                        color      = lc.textPrimary,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(lc.greenCard)
                        .border(1.dp, lc.greenBorder, RoundedCornerShape(16.dp))
                        .clickable(onClick = onHelp)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "✈  Помощь",
                        fontSize = LiptonSize.footnote,
                        fontWeight = FontWeight.SemiBold,
                        color      = lc.textPrimary,
                    )
                }
            }
        }
    }
}
