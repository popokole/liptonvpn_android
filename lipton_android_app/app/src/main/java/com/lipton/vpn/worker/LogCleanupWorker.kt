package com.lipton.vpn.worker

import android.content.Context
import androidx.work.*
import java.io.File
import java.util.concurrent.TimeUnit

class LogCleanupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val dirs = listOf(
            applicationContext.filesDir,
            applicationContext.cacheDir,
        )
        dirs.forEach { dir ->
            dir.listFiles { f -> f.isFile && f.name.endsWith(".log") || f.name.endsWith(".txt") }
                ?.filter { it.lastModified() < cutoff }
                ?.forEach { it.delete() }
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "log_cleanup",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<LogCleanupWorker>(24, TimeUnit.HOURS).build(),
            )
        }
    }
}
