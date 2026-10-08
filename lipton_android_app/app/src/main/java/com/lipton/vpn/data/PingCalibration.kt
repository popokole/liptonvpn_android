package com.lipton.vpn.data

import kotlin.math.roundToLong

/**
 * Пинг для показа. Замер — время TCP-рукопожатия из приложения (с накладными
 * расходами системы и сети телефона), он стабильно выше реальной задержки до
 * сервера. Решение владельца: показываем на 35% ниже и не больше 300 мс.
 */
object PingCalibration {
    private const val FACTOR = 0.65
    const val MAX_SHOWN_MS = 300L

    fun shown(rawMs: Long?): Long? {
        if (rawMs == null || rawMs <= 0) return rawMs
        return (rawMs * FACTOR).roundToLong().coerceIn(1L, MAX_SHOWN_MS)
    }
}
