package com.lipton.vpn.worker

import android.content.Context
import androidx.work.*
import com.lipton.vpn.data.SettingsManager
import com.lipton.vpn.service.LiptonNotificationHelper
import java.util.concurrent.TimeUnit

class ExpiryCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsManager(applicationContext)
        LiptonNotificationHelper.ensureChannels(applicationContext)
        val now = System.currentTimeMillis() / 1000L
        settings.getSubscriptions().filter { it.userInfo.expire > 0L }.forEach { sub ->
            val secsLeft  = sub.userInfo.expire - now
            val hoursLeft = secsLeft / 3600L
            val threshold = when {
                secsLeft <= 0   -> "expired"
                hoursLeft <= 1  -> "1h"
                hoursLeft <= 24 -> "24h"
                hoursLeft <= 72 -> "3d"
                else            -> return@forEach
            }
            val key = "${sub.id}_expiry_$threshold"
            if (!settings.getNotifSentFlag(key)) {
                LiptonNotificationHelper.showExpiryNotification(applicationContext, sub.name, hoursLeft)
                settings.setNotifSentFlag(key, true)
            }
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "expiry_check",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ExpiryCheckWorker>(3, TimeUnit.HOURS).build(),
            )
        }
    }
}
