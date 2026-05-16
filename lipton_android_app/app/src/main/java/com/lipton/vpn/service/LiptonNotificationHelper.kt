package com.lipton.vpn.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.lipton.vpn.MainActivity
import com.lipton.vpn.R

object LiptonNotificationHelper {

    const val CHANNEL_TRAFFIC = "lipton_traffic"
    const val CHANNEL_EXPIRY  = "lipton_expiry"
    const val CHANNEL_SYSTEM  = "lipton_system"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(listOf(
            NotificationChannel(CHANNEL_TRAFFIC, "Трафик", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Уведомления о расходе трафика"
            },
            NotificationChannel(CHANNEL_EXPIRY, "Подписка", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Уведомления об истечении подписки"
            },
            NotificationChannel(CHANNEL_SYSTEM, "Система", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Системные уведомления LiptonVPN"
            },
        ))
    }

    private fun mainIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun showTrafficNotification(context: Context, percent: Int, subName: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val (title, text) = when {
            percent >= 100 -> "Трафик исчерпан" to "Весь трафик подписки «$subName» использован"
            percent >= 80  -> "Трафик почти закончился" to "Использовано $percent% трафика · «$subName»"
            else           -> "Трафик 50%" to "Использовано $percent% трафика · «$subName»"
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_TRAFFIC)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentIntent(mainIntent(context))
            .setAutoCancel(true)
            .build()
        nm.notify(1000 + percent, notif)
    }

    fun showExpiryNotification(context: Context, subName: String, hoursLeft: Long) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val (title, text) = when {
            hoursLeft <= 0  -> "Подписка истекла" to "Подписка «$subName» истекла. Продлите для продолжения."
            hoursLeft <= 1  -> "Подписка истекает" to "Подписка «$subName» истечёт менее чем через час!"
            hoursLeft <= 24 -> "Подписка истекает" to "Подписка «$subName» истечёт через $hoursLeft ч."
            else            -> "Подписка истекает" to "Подписка «$subName» истечёт через ${hoursLeft / 24} дн."
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_EXPIRY)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentIntent(mainIntent(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(2000, notif)
    }

    fun showDisconnectNotification(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val reconnectPi = PendingIntent.getActivity(
            context, 99,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("action", "reconnect")
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setContentTitle("VPN отключён")
            .setContentText("Соединение прервалось неожиданно. Нажмите для переподключения.")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentIntent(reconnectPi)
            .addAction(0, "Подключить", reconnectPi)
            .setAutoCancel(true)
            .build()
        nm.notify(3000, notif)
    }
}
