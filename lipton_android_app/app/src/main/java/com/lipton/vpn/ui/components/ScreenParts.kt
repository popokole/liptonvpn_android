package com.lipton.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.Tokens

// ─────────────────────────────────────────────────────────────────────────────
//  Общие части экранов-вкладок: шапка (логотип + капсула), заголовок экрана,
//  отступ под плавающую капсулу навигации, строки списков в стекле.
//  Размеры — из макетов new-combo-* (390×844): шапка top 56 / высота 32,
//  заголовок 28/36 через 20dp, поля экрана 20.
// ─────────────────────────────────────────────────────────────────────────────

/** Шапка вкладки: «Lipton VPN» слева, [trailing] (капсула срока и т. п.) справа. */
@Composable
fun TabTopBar(
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LogoTitle()
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

/** Заголовок экрана («Серверы», «Профиль»): 28sp / 700, отступ 20dp от шапки. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 20.dp).heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.headlineMedium,
            color = LiptonTheme.colors.text,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/**
 * Сколько места оставить снизу прокручиваемого содержимого вкладки, чтобы
 * последний элемент не прятался под плавающей капсулой навигации.
 */
@Composable
fun tabBarBottomPadding(): Dp =
    TabBarReservedHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * Слот баннера над содержимым главной. Сейчас сюда кладётся баннер обновления
 * приложения. TODO(redesign): баннеры и экраны из админки с таргетингом по
 * версии («обновите приложение», новости) — следующей волной, место под них здесь.
 */
@Composable
fun BannerSlot(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

// ─── Строки списков в стекле (профиль, настройки) ────────────────────────────

/** Группа строк в одной стеклянной карточке; между строками — разделители с отступом 60. */
@Composable
fun GlassGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp), content = content)
}

/** Разделитель строк внутри [GlassGroup]. */
@Composable
fun GroupDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 60.dp)
            .height(1.dp)
            .background(LiptonTheme.colors.divider),
    )
}

/**
 * Строка 56/64dp: иконка в плашке 32, заголовок 15/600, подпись 12/500 (.6),
 * справа [trailing] (по умолчанию шеврон, если строка кликабельна).
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    iconTint: Color = Color.Unspecified,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = LiptonTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle != null) 64.dp else 56.dp)
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            IconTile(icon, tint = if (iconTint == Color.Unspecified) c.accentDeepOrAccent() else iconTint)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (titleColor == Color.Unspecified) c.text else titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = c.text.copy(alpha = if (c.isDark) 0.6f else 0.58f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when {
            trailing != null -> Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
            onClick != null -> Icon(
                LiptonIcons.ChevronRight,
                contentDescription = null,
                tint = c.text.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Поля экрана по макету (20dp слева и справа). */
val ScreenHorizontalPadding: Dp = Tokens.Space.screen
