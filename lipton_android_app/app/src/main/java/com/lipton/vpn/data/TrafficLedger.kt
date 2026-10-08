package com.lipton.vpn.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Трафик за день «на этом телефоне». */
data class DayTraffic(val day: String, val rx: Long, val tx: Long) {
    val total: Long get() = rx + tx
}

/**
 * Учёт трафика по дням без сервера: VPN-сервис раз в несколько секунд добавляет
 * приращение счётчиков TrafficStats своего uid (через него идёт весь туннель —
 * ядро xray работает в процессе приложения). Хранится 14 последних дней.
 * TODO(redesign): B4 — когда появится GET /me/traffic, показывать серверные цифры.
 */
object TrafficLedger {

    const val KEEP_DAYS = 14

    private val gson = Gson()

    fun dayKey(ms: Long, tz: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }.format(Date(ms))

    fun decode(json: String?): Map<String, LongArray> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = gson.fromJson(json, JsonObject::class.java) ?: return emptyMap()
            obj.entrySet().mapNotNull { (k, v) ->
                val arr = v.asJsonArray
                if (arr.size() < 2) null else k to longArrayOf(arr[0].asLong, arr[1].asLong)
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun encode(map: Map<String, LongArray>): String {
        val obj = JsonObject()
        map.toSortedMap().forEach { (k, v) ->
            val arr = com.google.gson.JsonArray()
            arr.add(v[0]); arr.add(v[1])
            obj.add(k, arr)
        }
        return gson.toJson(obj)
    }

    /** Прибавить к дню [day] и оставить [KEEP_DAYS] последних дней. */
    fun add(map: Map<String, LongArray>, day: String, rx: Long, tx: Long): Map<String, LongArray> {
        val out = map.toMutableMap()
        val cur = out[day] ?: longArrayOf(0, 0)
        out[day] = longArrayOf(cur[0] + rx.coerceAtLeast(0), cur[1] + tx.coerceAtLeast(0))
        return out.toSortedMap().entries.toList().takeLast(KEEP_DAYS).associate { it.key to it.value }
    }

    /** 7 дней, от самого старого до сегодняшнего; дней без данных — нули. */
    fun week(map: Map<String, LongArray>, nowMs: Long, tz: TimeZone = TimeZone.getDefault()): List<DayTraffic> {
        val cal = Calendar.getInstance(tz).apply { timeInMillis = nowMs }
        cal.add(Calendar.DAY_OF_YEAR, -6)
        return (0 until 7).map {
            val key = dayKey(cal.timeInMillis, tz)
            val v = map[key]
            cal.add(Calendar.DAY_OF_YEAR, 1)
            DayTraffic(key, v?.get(0) ?: 0L, v?.get(1) ?: 0L)
        }
    }
}
