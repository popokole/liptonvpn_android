package com.lipton.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.UiState
import com.lipton.vpn.data.model.AiMessage
import com.lipton.vpn.data.model.ArticleDetail
import com.lipton.vpn.data.model.ArticleSummary
import com.lipton.vpn.data.model.FaqEntry
import com.lipton.vpn.data.model.TxItem
import com.lipton.vpn.ui.components.AuroraTone
import com.lipton.vpn.ui.components.LiptonIcons
import com.lipton.vpn.ui.components.LiptonSwitch
import com.lipton.vpn.ui.components.ListRow
import com.lipton.vpn.ui.components.ScreenHorizontalPadding
import com.lipton.vpn.ui.components.TabTopBar
import com.lipton.vpn.ui.components.glass
import com.lipton.vpn.ui.components.stateTone
import com.lipton.vpn.ui.components.tabBarBottomPadding
import com.lipton.vpn.ui.tabs.SubscriptionCapsule
import com.lipton.vpn.ui.theme.LiptonTheme

// ─────────────────────────────────────────────────────────────────────────────
//  Каркас подэкранов профиля по макетам new-scr-*: шапка «Lipton VPN» с
//  капсулой срока, «‹ Заголовок», прокручиваемое содержимое; снизу остаётся
//  капсула навигации (вкладка «Профиль»).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Тестовые данные экранов, которые сами ходят в API (история платежей, база
 * знаний, чат). Задаёт только debug-витрина; в приложении — null.
 */
data class ScreenFixtures(
    val transactions: List<TxItem>? = null,
    val faq: List<FaqEntry>? = null,
    val articles: List<ArticleSummary>? = null,
    val article: ArticleDetail? = null,
    val dialog: List<AiMessage>? = null,
    val showUnlinkSheet: Boolean = false,
    val emailStep2: String? = null,
)

val LocalScreenFixtures = compositionLocalOf<ScreenFixtures?> { null }

/**
 * Подэкран профиля. [scroll] = false — содержимое само управляет высотой
 * (чат поддержки со строкой ввода, список приложений).
 */
@Composable
fun ProfileSubPage(
    state: UiState,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scroll: Boolean = true,
    bottomPadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        TabTopBar(Modifier.padding(horizontal = ScreenHorizontalPadding)) { SubscriptionCapsule(state) }
        SubTitleRow(title, onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = ScreenHorizontalPadding)
                .padding(top = 16.dp),
        ) {
            content()
            if (scroll && bottomPadding) Spacer(Modifier.height(tabBarBottomPadding() + 8.dp))
        }
    }
}

/** «‹ Заголовок»: стеклянная кнопка 40 и заголовок 24/700 (до двух строк). */
@Composable
fun SubTitleRow(title: String, onBack: () -> Unit) {
    val c = LiptonTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = ScreenHorizontalPadding).padding(top = 20.dp).heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .glass(CircleShape, highlightHeight = 20.dp)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = "Назад" },
            contentAlignment = Alignment.Center,
        ) { Icon(LiptonIcons.ChevronLeft, null, tint = c.text, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp, lineHeight = 30.sp),
            color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Вводный текст подэкрана (15/22, .78). */
@Composable
fun SubIntro(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = LiptonTheme.colors.text.copy(alpha = 0.78f))
}

/** Строка-переключатель в стеклянной группе. */
@Composable
fun SwitchRow(title: String, subtitle: String?, icon: ImageVector, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    ListRow(
        title, subtitle = subtitle, icon = icon,
        onClick = if (enabled) ({ onChange(!checked) }) else null,
        trailing = { LiptonSwitch(checked = checked, enabled = enabled) },
    )
}

/** Подпись-примечание с иконкой (мелкий текст .6). */
@Composable
fun NoteLine(icon: ImageVector, text: String, color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    val c = LiptonTheme.colors
    val tint = if (color == Color.Unspecified) c.text.copy(alpha = 0.6f) else color
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp).padding(top = 2.dp))
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, lineHeight = 17.sp), color = tint)
    }
}

/** Поле ввода в стекле 48–52dp с иконкой слева. */
@Composable
fun GlassInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    error: Boolean = false,
    onDone: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .height(52.dp)
            .clip(shape)
            .background(c.text.copy(alpha = if (c.isDark) 0.05f else 0.04f))
            .border(1.dp, if (error) c.warn.copy(alpha = 0.6f) else c.text.copy(alpha = 0.12f), shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = c.stateTone(AuroraTone.ON).a.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), color = c.text.copy(alpha = 0.4f), maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.text),
                cursorBrush = SolidColor(c.stateTone(AuroraTone.ON).a),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailing?.invoke()
    }
}

/** Маркированный пункт «• **Почта или Telegram-ID** — для входа». */
@Composable
fun BulletLine(text: AnnotatedString) {
    val c = LiptonTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.padding(top = 8.dp).size(5.dp).clip(CircleShape).background(c.stateTone(AuroraTone.ON).a))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.text.copy(alpha = 0.78f))
    }
}
