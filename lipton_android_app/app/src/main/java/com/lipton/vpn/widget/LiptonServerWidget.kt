package com.lipton.vpn.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lipton.vpn.MainActivity
import com.lipton.vpn.R
import com.lipton.vpn.data.SettingsManager
import com.lipton.vpn.data.model.displayName
import com.lipton.vpn.service.LiptonVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LiptonServerWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_NEXT_SERVER = "com.lipton.vpn.WIDGET_NEXT_SERVER"

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val connected = LiptonVpnService.isConnected
            val views = RemoteViews(context.packageName, R.layout.widget_server)

            val remark = LiptonVpnService.currentServerRemark
                .replace(Regex("[\uD83C][\uDDE6-\uDDFF][\uD83C][\uDDE6-\uDDFF]\\s*"), "").trim()

            views.setTextViewText(
                R.id.wsrv_server,
                remark.ifEmpty { "Нет сервера" },
            )
            views.setTextViewText(
                R.id.wsrv_status,
                if (connected) "● Подключено" else "○ Отключено",
            )
            views.setTextColor(
                R.id.wsrv_status,
                if (connected) 0xFF34D058.toInt() else 0x8CB4F0C3.toInt(),
            )

            val nextIntent = PendingIntent.getBroadcast(
                context,
                widgetId + 30000,
                Intent(context, LiptonServerWidget::class.java).setAction(ACTION_NEXT_SERVER),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.wsrv_btn, nextIntent)

            val openIntent = PendingIntent.getActivity(
                context,
                widgetId + 40000,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.wsrv_root, openIntent)

            manager.updateAppWidget(widgetId, views)
        }

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LiptonServerWidget::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_NEXT_SERVER) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val settings = SettingsManager(context)
                val subs = settings.getSubscriptions()
                val allServers = subs.flatMap { it.servers }
                if (allServers.isEmpty()) return@launch

                val currentId = settings.getActiveServerId()
                val currentIdx = allServers.indexOfFirst { it.id == currentId }
                val nextServer = allServers[(currentIdx + 1) % allServers.size]

                settings.setActiveServerId(nextServer.id)

                if (LiptonVpnService.isConnected) {
                    context.startForegroundService(
                        Intent(context, LiptonVpnService::class.java).apply {
                            action = LiptonVpnService.ACTION_START
                            putExtra(LiptonVpnService.EXTRA_SERVER_ID, nextServer.id)
                        }
                    )
                }

                refreshAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
