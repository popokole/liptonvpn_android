package com.lipton.vpn.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.lipton.vpn.MainActivity
import com.lipton.vpn.R
import com.lipton.vpn.service.LiptonVpnService

class LiptonStatusWidget : AppWidgetProvider() {

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val connected = LiptonVpnService.isConnected
            val views = RemoteViews(context.packageName, R.layout.widget_status)

            views.setTextViewText(
                R.id.ws_status,
                if (connected) "Подключено" else "Отключено",
            )
            views.setTextColor(
                R.id.ws_status,
                if (connected) 0xFF34D058.toInt() else 0xFFEDFFF2.toInt(),
            )

            val remark = LiptonVpnService.currentServerRemark
                .replace(Regex("[\uD83C][\uDDE6-\uDDFF][\uD83C][\uDDE6-\uDDFF]\\s*"), "").trim()
            if (connected && remark.isNotEmpty()) {
                views.setTextViewText(R.id.ws_server, remark)
                views.setViewVisibility(R.id.ws_server, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.ws_server, View.GONE)
            }

            val openIntent = PendingIntent.getActivity(
                context,
                widgetId + 20000,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.ws_root, openIntent)

            manager.updateAppWidget(widgetId, views)
        }

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LiptonStatusWidget::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }
}
