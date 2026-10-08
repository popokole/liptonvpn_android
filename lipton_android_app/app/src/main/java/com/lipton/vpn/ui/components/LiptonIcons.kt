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
}
