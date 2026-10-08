package com.lipton.vpn.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lipton.vpn.R
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.Tokens

/** Логотип «три наклонные полоски» (res/drawable/ic_logo.xml). */
@Composable
fun LiptonLogo(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Image(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = "Lipton VPN",
        modifier = modifier.size(size),
    )
}

/** Логотип в стеклянной плашке 28dp (шапка экранов). */
@Composable
fun LogoBadge(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(Tokens.Radius.badge)
    // Тёмная: заливка .06, рамка .10; светлая: белая .72, рамка .95 (как в макетах)
    val fill = if (c.isDark) c.glassFill else Color(0xB8FFFFFF)
    val border = if (c.isDark) c.glassBorder else Color(0xF2FFFFFF)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .border(1.dp, Brush.verticalGradient(0f to c.glassInset, 0.2f to Color.Transparent), shape),
        contentAlignment = Alignment.Center,
    ) {
        LiptonLogo(size = size * (18f / 28f))
    }
}

/** Плашка-логотип и подпись «Lipton VPN» (левая часть шапки). */
@Composable
fun LogoTitle(modifier: Modifier = Modifier) {
    val c = LiptonTheme.colors
    Row(
        modifier = modifier.semantics { contentDescription = "Lipton VPN" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LogoBadge()
        Text(
            "Lipton VPN",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em,
            ),
            color = c.text,
            maxLines = 1,
        )
    }
}
