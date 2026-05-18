package com.lipton.vpn.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.widget.RemoteViews
import com.lipton.vpn.MainActivity
import com.lipton.vpn.R
import com.lipton.vpn.data.SettingsManager
import com.lipton.vpn.service.LiptonVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LiptonWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.lipton.vpn.WIDGET_TOGGLE"

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val connected = LiptonVpnService.isConnected
            val views = RemoteViews(context.packageName, R.layout.widget_lipton)

            views.setTextViewText(R.id.widget_status,
                if (connected) "Подключено" else "Отключено")
            views.setTextViewText(R.id.widget_btn_label,
                if (connected) "Отключить" else "Подключить")
            views.setInt(R.id.widget_btn, "setBackgroundColor",
                if (connected) 0xFF34D058.toInt() else 0xFFE53935.toInt())
            views.setTextColor(R.id.widget_status,
                if (connected) 0xFF34D058.toInt() else 0xFFEDFFF2.toInt())

            val remark = LiptonVpnService.currentServerRemark
                .replace(Regex("[\uD83C][\uDDE6-\uDDFF][\uD83C][\uDDE6-\uDDFF]\\s*"), "").trim()
            if (connected && remark.isNotEmpty()) {
                views.setTextViewText(R.id.widget_server, remark)
                views.setViewVisibility(R.id.widget_server, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_server, android.view.View.GONE)
            }

            // Кнопка — toggle VPN
            val toggleIntent = PendingIntent.getBroadcast(
                context,
                widgetId,
                Intent(context, LiptonWidget::class.java).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_btn, toggleIntent)

            // Заголовок / статус — открыть приложение
            val openIntent = PendingIntent.getActivity(
                context,
                widgetId + 10000,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openIntent)

            manager.updateAppWidget(widgetId, views)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return

        if (LiptonVpnService.isConnected) {
            context.startService(
                Intent(context, LiptonVpnService::class.java).apply {
                    action = LiptonVpnService.ACTION_STOP
                }
            )
            refreshAllWidgets(context)
            return
        }

        val permIntent = VpnService.prepare(context)
        if (permIntent != null) {
            // Нужно разрешение — открываем приложение
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
            return
        }

        scope.launch {
            val settings = SettingsManager(context)
            val serverId = settings.getActiveServerId()
                ?: settings.getSubscriptions().flatMap { it.servers }.firstOrNull()?.id
                ?: return@launch
            context.startForegroundService(
                Intent(context, LiptonVpnService::class.java).apply {
                    action = LiptonVpnService.ACTION_START
                    putExtra(LiptonVpnService.EXTRA_SERVER_ID, serverId)
                }
            )
            refreshAllWidgets(context)
        }
    }

    private fun refreshAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, LiptonWidget::class.java))
        ids.forEach { updateWidget(context, manager, it) }
    }
}
