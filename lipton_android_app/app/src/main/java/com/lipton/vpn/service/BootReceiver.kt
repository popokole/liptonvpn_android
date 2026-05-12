package com.lipton.vpn.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lipton.vpn.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        CoroutineScope(Dispatchers.IO).launch {
            val settings = SettingsManager(context)
            if (!settings.getAutostart()) return@launch

            val activeId = settings.getActiveServerId() ?: return@launch
            val intent = Intent(context, LiptonVpnService::class.java).apply {
                action = LiptonVpnService.ACTION_START
                putExtra(LiptonVpnService.EXTRA_SERVER_ID, activeId)
            }
            context.startForegroundService(intent)
        }
    }
}
