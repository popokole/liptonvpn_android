package com.lipton.vpn.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Линейные иконки 24×24 из макетов (stroke 1.8, round). Цвет задаётся tint'ом
 * у Icon(), поэтому контуры здесь чёрные.
 */
object LiptonIcons {

    private fun stroke(name: String, width: Float = 1.8f, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(
            name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        )
        paths.forEach { d ->
            b.addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    // Нижняя навигация
    val Home: ImageVector by lazy {
        stroke("home", 1.8f, "M3 10.5 12 3l9 7.5", "M5 9.5V20h14V9.5", "M10 20v-5h4v5")
    }
    val Globe: ImageVector by lazy {
        stroke("globe", 1.8f,
            "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M3 12h18",
            "M12 3a14 14 0 0 1 0 18a14 14 0 0 1 0-18")
    }
    val Bell: ImageVector by lazy {
        stroke("bell", 1.8f, "M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z", "M10 20a2 2 0 0 0 4 0")
    }
    val User: ImageVector by lazy {
        stroke("user", 1.8f, "M12 4a4 4 0 1 0 0 8a4 4 0 1 0 0-8z", "M4 21a8 8 0 0 1 16 0")
    }

    // Тема
    val Moon: ImageVector by lazy {
        stroke("moon", 1.8f, "M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z")
    }
    val Sun: ImageVector by lazy {
        stroke("sun", 1.8f,
            "M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8z",
            "M12 2.5v2", "M12 19.5v2", "m5.3 5.3 1.4 1.4", "m17.3 17.3 1.4 1.4",
            "M2.5 12h2", "M19.5 12h2", "m5.3 18.7 1.4-1.4", "m17.3 6.7 1.4-1.4")
    }
    val System: ImageVector by lazy {
        stroke("system", 1.8f,
            "M12 3.5a8.5 8.5 0 1 0 0 17a8.5 8.5 0 1 0 0-17z",
            "M12 3.5v17", "M12 8h5.5", "M12 12h7", "M12 16h5.5")
    }

    // Общие
    val Power: ImageVector by lazy { stroke("power", 1.8f, "M12 3v8", "M6.3 6.3a8 8 0 1 0 11.4 0") }
    val ShieldCheck: ImageVector by lazy {
        stroke("shield_check", 1.8f, "M12 3 4.5 6v6c0 4.5 3.2 7.8 7.5 9 4.3-1.2 7.5-4.5 7.5-9V6z", "m8.5 12 2.5 2.5 4.5-5")
    }
    val ChevronRight: ImageVector by lazy { stroke("chevron_right", 2f, "m9 6 6 6-6 6") }
    val ChevronLeft: ImageVector by lazy { stroke("chevron_left", 2f, "m15 6-6 6 6 6") }
    val Calendar: ImageVector by lazy {
        stroke("calendar", 2f, "M6.5 5h11a3 3 0 0 1 3 3v9.5a3 3 0 0 1-3 3h-11a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z",
            "M3.5 10h17", "M8 3v4", "M16 3v4")
    }
    val Check: ImageVector by lazy { stroke("check", 2.8f, "m5 12.5 4.5 4.5L19 7.5") }
    val Bolt: ImageVector by lazy { stroke("bolt", 1.8f, "M13 2.5 4.5 14H11l-1 7.5L18.5 10H12z") }
    val Palette: ImageVector by lazy {
        stroke("palette", 1.8f,
            "M12 3a9 9 0 0 0 0 18c1.1 0 1.8-.9 1.5-1.9-.3-1 .4-2.1 1.5-2.1H17a4 4 0 0 0 4-4c0-5.5-4-10-9-10z",
            "M7.5 11h.01", "M10.5 7h.01", "M15 7.5h.01")
    }

    // Профиль: помощь, оплата, о приложении (new-combo-profile-full)
    val Chat: ImageVector by lazy {
        stroke("chat", 1.8f,
            "M21 12a8 8 0 0 1-11.8 7L4 20.5l1.5-4.8A8 8 0 1 1 21 12z", "M8.5 12h.01", "M12 12h.01", "M15.5 12h.01")
    }
    val Book: ImageVector by lazy {
        stroke("book", 1.8f,
            "M4 4.5A1.5 1.5 0 0 1 5.5 3H20v15H5.5A1.5 1.5 0 0 0 4 19.5z", "M4 19.5A1.5 1.5 0 0 0 5.5 21H20", "M8.5 7.5h7")
    }
    val Terminal: ImageVector by lazy {
        stroke("terminal", 1.8f,
            "M6 4h12a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3z", "m7 9 3 3-3 3", "M12.5 15H17")
    }
    val Refresh: ImageVector by lazy {
        stroke("refresh", 1.8f,
            "M3 12a9 9 0 0 1 15.5-6.2L21 8", "M21 3v5h-5", "M21 12a9 9 0 0 1-15.5 6.2L3 16", "M3 21v-5h5")
    }
    val Info: ImageVector by lazy {
        stroke("info", 1.8f, "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M12 11v5", "M12 7.5h.01")
    }
    val Card: ImageVector by lazy {
        stroke("card", 1.8f,
            "M5 5.5h14a2.5 2.5 0 0 1 2.5 2.5v8a2.5 2.5 0 0 1-2.5 2.5H5a2.5 2.5 0 0 1-2.5-2.5V8A2.5 2.5 0 0 1 5 5.5z",
            "M2.5 10h19", "M6.5 15h3")
    }
    val Send: ImageVector by lazy { stroke("send", 1.8f, "m21.5 2.5-7 19-4-8.5-8.5-4z", "M21.5 2.5 10.5 13") }

    // Главная: бенто-статистика (new-combo-stats-full)
    val Eye: ImageVector by lazy {
        stroke("eye", 1.8f, "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z", "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z")
    }
    val Timer: ImageVector by lazy {
        stroke("timer", 1.8f, "M10 2.5h4", "M12 14l3-3", "M12 6a8 8 0 1 0 0 16a8 8 0 1 0 0-16z")
    }
    val Gauge: ImageVector by lazy { stroke("gauge", 1.8f, "M12 14l4-4", "M3.3 19a10 10 0 1 1 17.4 0") }
    val Tag: ImageVector by lazy { stroke("tag", 1.8f, "M3 12V4h8l10 10-8 8z", "M7.5 7.5h.01") }
    val ArrowsUpDown: ImageVector by lazy {
        stroke("arrows_up_down", 1.8f, "M7 4v16", "m3 8 4-4 4 4", "M17 20V4", "m13 16 4 4 4-4")
    }
    val ArrowDown: ImageVector by lazy { stroke("arrow_down", 2.2f, "M12 5v14", "m6 13 6 6 6-6") }
    val ArrowUp: ImageVector by lazy { stroke("arrow_up", 2.2f, "M12 19V5", "m6 11 6-6 6 6") }
    val ArrowRight: ImageVector by lazy { stroke("arrow_right", 2f, "M5 12h14", "m13 6 6 6-6 6") }
    val Shield: ImageVector by lazy { stroke("shield", 1.8f, "M12 3 4.5 6v6c0 4.5 3.2 7.8 7.5 9 4.3-1.2 7.5-4.5 7.5-9V6z") }
    val ShieldOff: ImageVector by lazy {
        stroke("shield_off", 1.8f, "M19.5 12.5c-.5 4.1-3.5 7.3-7.5 8.5-4.3-1.2-7.5-4.5-7.5-9V6.5", "M7.5 4.5 12 3l7.5 3v4", "M3 3l18 18")
    }
    val BarChart: ImageVector by lazy { stroke("bar_chart", 2f, "M4 20h16", "M7 16v-4", "M12 16V8", "M17 16v-6") }
    val Alert: ImageVector by lazy { stroke("alert", 1.8f, "M12 3.5 2.5 20h19z", "M12 10v4.5", "M12 17.5h.01") }
    val Exclaim: ImageVector by lazy { stroke("exclaim", 2.4f, "M12 6v8", "M12 18h.01") }

    // Серверы
    val Shuffle: ImageVector by lazy {
        stroke("shuffle", 1.8f, "M16 3h5v5", "M4 20 21 3", "M21 16v5h-5", "m15 15 6 6", "M4 4l5 5")
    }
    val Star: ImageVector by lazy {
        stroke("star", 1.8f, "m12 3 2.8 5.7 6.2.9-4.5 4.4 1 6.2L12 17.3 6.5 20.2l1-6.2L3 9.6l6.2-.9z")
    }
    val Lock: ImageVector by lazy {
        stroke("lock", 1.8f, "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z", "M8 11V8a4 4 0 0 1 8 0v3")
    }
    val Unlock: ImageVector by lazy {
        stroke("unlock", 1.8f, "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z", "M8 11V8a4 4 0 0 1 7.5-2")
    }
    val Pulse: ImageVector by lazy { stroke("pulse", 1.8f, "M3 12h4l3-8 4 16 3-8h4") }

    // Новости
    val CheckDouble: ImageVector by lazy {
        stroke("check_double", 2f, "m1.5 12.5 4.5 4.5", "m7 12.5 4.5 4.5L21 7.5", "M11.5 12 16 7.5")
    }

    // Профиль
    val Copy: ImageVector by lazy {
        stroke("copy", 1.8f,
            "M10 8h9a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-9a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2z",
            "M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3")
    }
    val Link: ImageVector by lazy {
        stroke("link", 1.8f,
            "M10 13a5 5 0 0 0 7.5.5l3-3a5 5 0 0 0-7-7l-1.7 1.7",
            "M14 11a5 5 0 0 0-7.5-.5l-3 3a5 5 0 0 0 7 7l1.7-1.7")
    }
    val Unlink: ImageVector by lazy {
        stroke("unlink", 1.8f,
            "M18.8 13.4l1.4-1.4a4 4 0 0 0-5.7-5.7l-1.4 1.4", "M5.2 10.6l-1.4 1.4a4 4 0 0 0 5.7 5.7l1.4-1.4",
            "M8 2.5v3", "M2.5 8h3", "M16 21.5v-3", "M21.5 16h-3")
    }
    val Phone: ImageVector by lazy {
        stroke("phone", 1.8f, "M8 2.5h8a2 2 0 0 1 2 2v15a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2v-15a2 2 0 0 1 2-2z", "M11 18.5h2")
    }
    val Monitor: ImageVector by lazy {
        stroke("monitor", 1.8f,
            "M4 4.5h16a1.5 1.5 0 0 1 1.5 1.5v9a1.5 1.5 0 0 1-1.5 1.5H4A1.5 1.5 0 0 1 2.5 15V6A1.5 1.5 0 0 1 4 4.5z",
            "M8 20.5h8", "M12 16.5v4")
    }
    val Branch: ImageVector by lazy {
        stroke("branch", 1.8f, "M6 3v12", "M18 9a3 3 0 1 0 0-6a3 3 0 1 0 0 6z", "M6 21a3 3 0 1 0 0-6a3 3 0 1 0 0 6z", "M18 9a9 9 0 0 1-9 9")
    }
    val ListIcon: ImageVector by lazy {
        stroke("list", 1.8f, "M8 6h13", "M8 12h13", "M8 18h13", "M3 6h.01", "M3 12h.01", "M3 18h.01")
    }
    val Apps: ImageVector by lazy {
        stroke("apps", 1.8f, "M4 4h6v6H4z", "M14 4h6v6h-6z", "M4 14h6v6H4z", "M17 14v6", "M14 17h6")
    }
    val Translate: ImageVector by lazy {
        stroke("translate", 1.8f, "M4 5h7", "M7.5 3v2", "M5 9c1.5 3 4 5 6 6", "M10 5c-.5 3-2.5 7-6 9", "M12.5 20l4-9 4 9", "M14 17h5")
    }
    val Doc: ImageVector by lazy { stroke("doc", 1.8f, "M14 3H6v18h12V7z", "M14 3v4h4", "M9 13h6", "M9 17h4") }
    val Receipt: ImageVector by lazy {
        stroke("receipt", 1.8f, "M5 3h14v18l-3-2-2 2-2-2-2 2-2-2-3 2z", "M9 8h6", "M9 12h6")
    }
    val LogIn: ImageVector by lazy {
        stroke("log_in", 1.8f, "M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3", "M10 17l5-5-5-5", "M15 12H3")
    }
    val XCircle: ImageVector by lazy {
        stroke("x_circle", 1.8f, "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "m15 9-6 6", "m9 9 6 6")
    }
    val Crown: ImageVector by lazy { stroke("crown", 1.8f, "M3 8l4 4 5-7 5 7 4-4-2 11H5z") }
    val Mail: ImageVector by lazy {
        stroke("mail", 1.8f,
            "M4 5.5h16a1.5 1.5 0 0 1 1.5 1.5v10a1.5 1.5 0 0 1-1.5 1.5H4A1.5 1.5 0 0 1 2.5 17V7A1.5 1.5 0 0 1 4 5.5z",
            "m3 7 9 6 9-6")
    }
    val Gift: ImageVector by lazy {
        stroke("gift", 1.8f, "M3 9h18v4H3z", "M5 13v8h14v-8", "M12 9v12",
            "M12 9c-2-4-6-4-6-1.5S9 9 12 9z", "M12 9c2-4 6-4 6-1.5S15 9 12 9z")
    }
    val Bell2: ImageVector get() = Bell
    val Vibrate: ImageVector by lazy {
        stroke("vibrate", 1.8f, "M9 4h6a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z", "M4 9v6", "M20 9v6")
    }
    val Infinity: ImageVector by lazy {
        stroke("infinity", 1.8f,
            "M18.2 8.4c2.4 0 4.3 1.6 4.3 3.6s-1.9 3.6-4.3 3.6c-3.9 0-8.5-7.2-12.4-7.2C3.4 8.4 1.5 10 1.5 12s1.9 3.6 4.3 3.6c3.9 0 8.5-7.2 12.4-7.2z")
    }
    val Close: ImageVector by lazy { stroke("close", 2f, "m6 6 12 12", "M18 6 6 18") }
    val Search: ImageVector by lazy { stroke("search", 1.8f, "M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14z", "m20 20-4-4") }

    // Вход, онбординг, подэкраны (волна 2: new-onb-*, new-scr-*)
    val Browser: ImageVector by lazy { stroke("browser", 1.8f, "M3.5 5h17v14h-17z", "M3.5 9h17", "M7 7h.01", "M10 7h.01") }
    val UserPlus: ImageVector by lazy {
        stroke("user_plus", 1.8f, "M9 4a4 4 0 1 0 0 8a4 4 0 1 0 0-8z", "M2 21a7 7 0 0 1 14 0", "M19 8v6", "M16 11h6")
    }
    val External: ImageVector by lazy { stroke("external", 1.8f, "M14 4h6v6", "M20 4l-9 9", "M18 14v6H4V6h6") }
    val Paperclip: ImageVector by lazy {
        stroke("paperclip", 1.8f, "M20.5 11.5l-8.2 8.2a5 5 0 0 1-7.1-7.1l8.5-8.5a3.3 3.3 0 0 1 4.7 4.7l-8.5 8.5a1.7 1.7 0 0 1-2.4-2.4l7.8-7.8")
    }
    val Headset: ImageVector by lazy {
        stroke("headset", 1.8f, "M4 14v-2a8 8 0 0 1 16 0v2", "M4 14h3v6H5a1 1 0 0 1-1-1z", "M20 14h-3v6h2a1 1 0 0 0 1-1z")
    }
    val Sparkle: ImageVector by lazy {
        stroke("sparkle", 1.8f, "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z", "M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z")
    }
    val Plus: ImageVector by lazy { stroke("plus", 2f, "M12 5v14", "M5 12h14") }
    val Percent: ImageVector by lazy {
        stroke("percent", 1.8f, "M19 5 5 19", "M7 5a2 2 0 1 0 0 4a2 2 0 1 0 0-4z", "M17 15a2 2 0 1 0 0 4a2 2 0 1 0 0-4z")
    }
    val Megaphone: ImageVector by lazy {
        stroke("megaphone", 1.8f, "M3 10v4h4l7 4V6l-7 4z", "M17 9a4 4 0 0 1 0 6", "M7 14l1.5 5h2.5l-1-5")
    }
    val Download: ImageVector by lazy { stroke("download", 1.8f, "M12 4v11", "m7 10 5 5 5-5", "M5 20h14") }
    val Wifi: ImageVector by lazy {
        stroke("contactless", 1.8f, "M8.5 8.5a5 5 0 0 1 0 7", "M12 6a8.5 8.5 0 0 1 0 12", "M15.5 3.5a12 12 0 0 1 0 17")
    }
}
