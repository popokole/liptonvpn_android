package com.lipton.vpn.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.Tokens

/** Вкладки нижней навигации. */
enum class LiptonTab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Главная", LiptonIcons.Home),
    SERVERS("servers", "Серверы", LiptonIcons.Globe),
    NEWS("news", "Новости", LiptonIcons.Bell),
    PROFILE("profile", "Профиль", LiptonIcons.User);

    companion object {
        fun fromRoute(route: String?): LiptonTab? = entries.firstOrNull { it.route == route }
    }
}

/** Сколько места снизу оставить под плавающую капсулу (без системной навигации). */
val TabBarReservedHeight = 26.dp + 56.dp + 16.dp

/**
 * Плавающая капсула навигации по макету: поле 6, зазор 4, кнопки 44dp;
 * активная вкладка — подложка и подпись, остальные — только иконка.
 * [badges] — точка «есть непрочитанное» (например, у «Новостей»).
 */
@Composable
fun LiptonTabBar(
    selected: LiptonTab,
    onSelect: (LiptonTab) -> Unit,
    modifier: Modifier = Modifier,
    badges: Set<LiptonTab> = emptySet(),
) {
    val c = LiptonTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .drawBehind {
                // тень 0 12 32 (тёмная — чёрная .5, светлая — зелёно-серая .12)
                softGlow(
                    if (c.isDark) Color.Black.copy(alpha = 0.45f) else Color(0x1F14281E),
                    size.height / 2f, 22.dp.toPx(), offsetY = 10.dp.toPx(),
                )
            }
            .clip(shape)
            .background(c.navFill)
            .border(1.dp, c.navBorder, shape)
            .padding(6.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiptonTab.entries.forEach { tab ->
            TabItem(
                tab = tab,
                selected = tab == selected,
                badge = tab in badges,
                onClick = { onSelect(tab) },
            )
        }
    }
}

@Composable
private fun TabItem(tab: LiptonTab, selected: Boolean, badge: Boolean, onClick: () -> Unit) {
    val c = LiptonTheme.colors
    val anim = tween<Color>(Tokens.Motion.TAB_FADE_MS, easing = Tokens.Motion.standard)
    val bg by animateColorAsState(if (selected) c.navActiveFill else c.navActiveFill.copy(alpha = 0f), anim, label = "tab_bg")
    val fg by animateColorAsState(if (selected) c.navActiveText else c.navInactive, anim, label = "tab_fg")
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .height(44.dp)
            .defaultMinSize(minWidth = 44.dp)
            .clip(shape)
            .background(bg)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { contentDescription = tab.label }
            .padding(horizontal = if (selected) 16.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box {
            Icon(tab.icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            if (badge) {
                Box(
                    Modifier
                        .offset(x = 13.dp, y = (-1).dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(c.navFill)
                        .padding(1.5.dp)
                        .clip(CircleShape)
                        .background(if (c.isDark) c.accent else c.accentDeep),
                )
            }
        }
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(Tokens.Motion.TAB_FADE_MS)) + expandHorizontally(tween(Tokens.Motion.TAB_FADE_MS, easing = Tokens.Motion.standard)),
            exit = fadeOut(tween(120)) + shrinkHorizontally(tween(Tokens.Motion.TAB_FADE_MS, easing = Tokens.Motion.standard)),
        ) {
            Row {
                Box(Modifier.width(8.dp))
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, letterSpacing = 0.sp),
                    color = fg,
                    maxLines = 1,
                )
            }
        }
    }
}
