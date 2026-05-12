package com.lipton.vpn.service

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.lipton.vpn.MainActivity
import com.lipton.vpn.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class LiptonTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext

        if (LiptonVpnService.isConnected) {
            startService(Intent(context, LiptonVpnService::class.java).apply {
                action = LiptonVpnService.ACTION_STOP
            })
            qsTile?.state = Tile.STATE_INACTIVE
            qsTile?.updateTile()
            return
        }

        val permIntent = VpnService.prepare(context)
        if (permIntent != null) {
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(
                    PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(mainIntent)
            }
            return
        }

        scope.launch {
            val settings = SettingsManager(context)
            val serverId = settings.getActiveServerId()
                ?: settings.getSubscriptions().flatMap { it.servers }.firstOrNull()?.id
                ?: return@launch
            startForegroundService(
                Intent(context, LiptonVpnService::class.java).apply {
                    action = LiptonVpnService.ACTION_START
                    putExtra(LiptonVpnService.EXTRA_SERVER_ID, serverId)
                }
            )
        }
        qsTile?.state = Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        tile.label = "LiptonVPN"
        tile.state = if (LiptonVpnService.isConnected) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
