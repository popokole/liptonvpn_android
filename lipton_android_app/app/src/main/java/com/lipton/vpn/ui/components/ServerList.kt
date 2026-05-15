package com.lipton.vpn.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.data.model.Server
import com.lipton.vpn.data.model.displayName
import com.lipton.vpn.data.model.flagEmoji
import com.lipton.vpn.ui.theme.*

@Composable
fun ServerList(
    servers:        List<Server>,
    activeServerId: String?,
    pinging:        Boolean,
    onSelect:       (String) -> Unit,
    onPingAll:      () -> Unit,
    isTrialOnly:    Boolean = false,
    modifier:       Modifier = Modifier,
) {
    val lc = LocalLiptonColors.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "СЕРВЕРЫ",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = Green.copy(alpha = 0.7f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${servers.size}", fontSize = 11.sp, color = lc.textTertiary)
                SmallButton(
                    text    = if (pinging) "..." else "Пинг",
                    enabled = !pinging && servers.isNotEmpty(),
                    onClick = onPingAll,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(lc.cardBg)
                .border(1.dp, lc.cardBorder, RoundedCornerShape(14.dp))
        ) {
            if (servers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Добавьте подписку", fontSize = 12.sp, color = lc.textTertiary)
                }
            } else {
                // Column + verticalScroll instead of LazyColumn to avoid nested-scroll crash
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    servers.forEachIndexed { i, server ->
                        ServerItem(
                            server      = server,
                            isActive    = server.id == activeServerId,
                            isTrialOnly = isTrialOnly,
                            onClick     = { onSelect(server.id) },
                        )
                        if (i < servers.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Green.copy(alpha = 0.06f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerItem(
    server:      Server,
    isActive:    Boolean,
    isTrialOnly: Boolean,
    onClick:     () -> Unit,
) {
    val lc = LocalLiptonColors.current

    val bgColor by animateColorAsState(
        targetValue = if (isActive) Green.copy(alpha = 0.09f) else Color.Transparent,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "server_bg",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(30.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Green, Green3)
                        ),
                        shape = RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp),
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isActive) 18.dp else 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isTrialOnly) {
                Text("🎁", fontSize = 20.sp)
            } else {
                Text(
                    text = server.flagEmoji().ifEmpty { "🌐" },
                    fontSize = 20.sp,
                )
            }
            Text(
                text = if (isTrialOnly) "Тест, купите подписку" else server.displayName(),
                fontSize = 14.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isTrialOnly) Green.copy(alpha = 0.75f) else if (isActive) Green else lc.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            PingBadge(ping = server.ping)
        }
    }
}

@Composable
private fun PingBadge(ping: Long?) {
    val (color, text) = when {
        ping == null -> Green.copy(alpha = 0.4f) to "—"
        ping < 150   -> Green to "${ping}ms"
        ping < 300   -> Amber to "${ping}ms"
        else         -> Red to "${ping}ms"
    }
    val bg = when {
        ping == null -> Green.copy(alpha = 0.05f)
        ping < 150   -> Green.copy(alpha = 0.14f)
        ping < 300   -> Amber.copy(alpha = 0.14f)
        else         -> Red.copy(alpha = 0.14f)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun SmallButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val lc = LocalLiptonColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) lc.cardBg else Color.Transparent)
            .border(1.dp, if (enabled) lc.cardBorder else Color.Transparent, RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) lc.textSecondary else lc.textTertiary,
        )
    }
}
