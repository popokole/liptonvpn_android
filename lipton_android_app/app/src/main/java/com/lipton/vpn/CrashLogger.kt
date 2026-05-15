package com.lipton.vpn

import android.content.Context
import java.io.File
import java.util.Date

object CrashLogger {
    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val appCtx = context.applicationContext
        val default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val text = "=== CRASH ${Date()} ===\nThread: ${thread.name}\n${throwable.stackTraceToString()}"
                File(appCtx.filesDir, FILE).writeText(text)
            } catch (_: Exception) {}
            default?.uncaughtException(thread, throwable)
        }
    }

    fun readAndClear(context: Context): List<String>? {
        val file = File(context.applicationContext.filesDir, FILE)
        if (!file.exists()) return null
        val lines = try { file.readLines() } catch (_: Exception) { return null }
        file.delete()
        return lines
    }
}
