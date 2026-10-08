package com.lipton.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lipton.vpn.ui.theme.LiptonText
import com.lipton.vpn.ui.theme.LiptonTheme

/**
 * Диалог подтверждения в стиле редизайна: непрозрачная «стеклянная» карточка 28,
 * иконка тона, заголовок, текст, предупреждение и две кнопки.
 * [danger] — подтверждение необратимого действия (рыжая кнопка).
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector? = null,
    warning: String? = null,
    danger: Boolean = false,
    dismissText: String = "Отмена",
    busy: Boolean = false,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val c = LiptonTheme.colors
    val accent = if (danger) c.warn else c.accentDeepOrAccent()
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.surfaceSheet)
                .border(1.dp, c.glassBorder, shape)
                .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                ToneCircleIcon(icon, accent, size = 48.dp, iconSize = 22.dp, glow = true)
                Spacer(Modifier.height(14.dp))
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = c.text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = c.text.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
            if (warning != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.warn.copy(alpha = 0.08f))
                        .border(1.dp, c.warn.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(LiptonIcons.Alert, null, tint = c.warn, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                    Text(warning, style = MaterialTheme.typography.bodySmall, color = if (c.isDark) Color(0xFFFFC08A) else c.warnSoft)
                }
            }
            extra?.invoke(this)
            Spacer(Modifier.height(20.dp))
            DialogButton(confirmText, onConfirm, filled = true, color = accent, busy = busy)
            Spacer(Modifier.height(8.dp))
            DialogButton(dismissText, { if (!busy) onDismiss() }, filled = false, color = c.text)
        }
    }
}

@Composable
private fun DialogButton(text: String, onClick: () -> Unit, filled: Boolean, color: Color, busy: Boolean = false) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(if (filled) color.copy(alpha = if (c.isDark) 0.18f else 0.12f) else Color.Transparent)
            .border(1.dp, if (filled) color.copy(alpha = 0.5f) else Color.Transparent, shape)
            .clickable(enabled = !busy, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(color = color, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Text(
                text,
                style = LiptonText.buttonLarge.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                color = if (filled) (if (c.isDark) Color.White else color) else c.text.copy(alpha = 0.7f),
                maxLines = 1,
            )
        }
    }
}
