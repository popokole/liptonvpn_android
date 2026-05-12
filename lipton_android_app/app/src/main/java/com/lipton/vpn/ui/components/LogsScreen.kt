package com.lipton.vpn.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.ui.theme.*

@Composable
fun LogsScreen(
    logLines: List<String>,
    onClear:  () -> Unit,
    onBack:   () -> Unit,
) {
    val lc          = LocalLiptonColors.current
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom when new lines arrive
    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
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
                Text("Логи подключения", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = lc.textPrimary)
            }

            if (logLines.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(RedSoft)
                        .border(1.dp, Red.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("Очистить", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Red)
                }
            }
        }

        Text(
            text = "Вывод xray-core в реальном времени",
            fontSize = 11.5.sp,
            color = lc.textTertiary,
            lineHeight = 17.sp,
        )

        // Log content box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 480.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(lc.cardBg)
                .border(1.dp, lc.cardBorder, RoundedCornerShape(14.dp))
                .verticalScroll(scrollState)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (logLines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Логи появятся после подключения",
                        fontSize = 12.sp,
                        color = lc.textTertiary,
                    )
                }
            } else {
                logLines.forEach { line ->
                    val color = when {
                        line.contains("error",   ignoreCase = true) ||
                        line.contains("failed",  ignoreCase = true) ||
                        line.contains("refused",  ignoreCase = true) -> Red
                        line.contains("warn",    ignoreCase = true) ||
                        line.contains("warning", ignoreCase = true) -> Amber
                        line.contains("started") ||
                        line.contains("Running") ||
                        line.contains("connected", ignoreCase = true) -> Green
                        else -> lc.textSecondary
                    }
                    Text(
                        text = line,
                        fontSize = 10.sp,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp,
                    )
                }
            }
        }

        // Line count
        if (logLines.isNotEmpty()) {
            Text(
                "${logLines.size} строк",
                fontSize = 11.sp,
                color = lc.textTertiary,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}
