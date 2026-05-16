package com.lipton.vpn.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.data.model.Subscription
import com.lipton.vpn.data.model.toReadableBytes
import com.lipton.vpn.data.model.usedBytes
import com.lipton.vpn.data.model.usedPercent
import com.lipton.vpn.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PremiumCard(
    subscription: Subscription,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lc = LocalLiptonColors.current
    val now = System.currentTimeMillis() / 1000L
    val expireSecs = subscription.userInfo.expire
    val secsLeft = if (expireSecs > 0) expireSecs - now else 0L
    val daysLeft  = secsLeft / 86400L
    val hoursLeft = (secsLeft % 86400L) / 3600L

    val state = when {
        expireSecs <= 0 || secsLeft > 7 * 86400L -> CardState.ACTIVE
        secsLeft > 0                              -> CardState.EXPIRING
        else                                      -> CardState.EXPIRED
    }

    val gradientColors = when (state) {
        CardState.ACTIVE    -> listOf(Green.copy(alpha = 0.15f), Green.copy(alpha = 0.05f))
        CardState.EXPIRING  -> listOf(Amber.copy(alpha = 0.18f), Amber.copy(alpha = 0.06f))
        CardState.EXPIRED   -> listOf(Red.copy(alpha = 0.18f), Red.copy(alpha = 0.06f))
    }
    val borderColor = when (state) {
        CardState.ACTIVE   -> Green.copy(alpha = 0.35f)
        CardState.EXPIRING -> Amber.copy(alpha = 0.4f)
        CardState.EXPIRED  -> Red.copy(alpha = 0.4f)
    }
    val accentColor = when (state) {
        CardState.ACTIVE   -> Green
        CardState.EXPIRING -> Amber
        CardState.EXPIRED  -> Red
    }

    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(when (state) { CardState.ACTIVE -> "✓"; CardState.EXPIRING -> "⏱"; CardState.EXPIRED -> "✕" }, fontSize = 14.sp, color = accentColor)
                }
                Column {
                    Text(subscription.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
                    Text("${subscription.servers.size} серверов", fontSize = 11.sp, color = lc.textTertiary)
                }
            }

            // Status badge
            val (badgeText, badgeBg) = when (state) {
                CardState.ACTIVE   -> "Активна" to Green.copy(alpha = 0.15f)
                CardState.EXPIRING -> "Истекает" to Amber.copy(alpha = 0.15f)
                CardState.EXPIRED  -> "Истекла" to Red.copy(alpha = 0.15f)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(badgeText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
            }
        }

        // Expiry info
        if (expireSecs > 0) {
            val expDate = SimpleDateFormat("dd MMM yyyy", Locale("ru")).format(Date(expireSecs * 1000L))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📅", fontSize = 12.sp)
                Text(
                    text = when (state) {
                        CardState.ACTIVE   -> "До $expDate · $daysLeft дн. осталось"
                        CardState.EXPIRING -> if (daysLeft > 0) "Истекает через $daysLeft дн. $hoursLeft ч." else "Истекает через $hoursLeft ч.!"
                        CardState.EXPIRED  -> "Истекла $expDate"
                    },
                    fontSize = 12.sp,
                    color = accentColor,
                    fontWeight = if (state == CardState.EXPIRING) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }

        // Traffic bar
        if (subscription.userInfo.total > 0) {
            val used    = subscription.userInfo.usedBytes()
            val total   = subscription.userInfo.total
            val percent = subscription.userInfo.usedPercent()
            val barColor = when {
                percent > 0.85f -> Red
                percent > 0.6f  -> Amber
                else            -> Green
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Трафик", fontSize = 11.sp, color = lc.textSecondary)
                    Text("${used.toReadableBytes()} / ${total.toReadableBytes()}", fontSize = 11.sp, color = lc.textSecondary, fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(5.dp)
                        .clip(RoundedCornerShape(3.dp)).background(barColor.copy(alpha = 0.12f))
                ) {
                    val animPct by animateFloatAsState(percent, tween(600, easing = FastOutSlowInEasing), label = "traffic")
                    Box(modifier = Modifier.fillMaxWidth(animPct).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(barColor))
                }
            }
        }

        // Expired CTA
        if (state == CardState.EXPIRED || state == CardState.EXPIRING) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)).border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBuyClick).padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Продлить подписку", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
            }
        }
    }
}

private enum class CardState { ACTIVE, EXPIRING, EXPIRED }
