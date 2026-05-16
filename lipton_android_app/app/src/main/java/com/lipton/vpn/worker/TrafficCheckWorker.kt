package com.lipton.vpn.worker

import android.content.Context
import androidx.work.*
import com.lipton.vpn.data.SettingsManager
import com.lipton.vpn.data.model.usedPercent
import com.lipton.vpn.service.LiptonNotificationHelper
import java.util.concurrent.TimeUnit

class TrafficCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsManager(applicationContext)
        LiptonNotificationHelper.ensureChannels(applicationContext)
        settings.getSubscriptions().filter { !it.isTrial && it.userInfo.total > 0 }.forEach { sub ->
            val percent = (sub.userInfo.usedPercent() * 100).toInt()
            listOf(50, 80, 100).forEach { threshold ->
                if (percent >= threshold) {
                    val key = "${sub.id}_traffic_$threshold"
                    if (!settings.getNotifSentFlag(key)) {
                        LiptonNotificationHelper.showTrafficNotification(applicationContext, threshold, sub.name)
                        settings.setNotifSentFlag(key, true)
                    }
                }
            }
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "traffic_check",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<TrafficCheckWorker>(6, TimeUnit.HOURS).build(),
            )
        }
    }
}
