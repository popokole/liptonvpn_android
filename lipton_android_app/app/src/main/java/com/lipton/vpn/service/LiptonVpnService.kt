package com.lipton.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lipton.vpn.MainActivity
import com.lipton.vpn.R
import com.lipton.vpn.data.SettingsManager
import com.lipton.vpn.data.XrayConfigGenerator
import com.lipton.vpn.data.model.Server
import com.lipton.vpn.widget.LiptonWidget
import com.lipton.vpn.service.LiptonNotificationHelper
import kotlinx.coroutines.*
import java.io.File

private const val UNEXPECTED_DISCONNECT_NOTIF_ID = 4000

class LiptonVpnService : VpnService() {

    companion object {
        const val ACTION_START  = "com.lipton.vpn.START"
        const val ACTION_STOP   = "com.lipton.vpn.STOP"
        const val EXTRA_SERVER_ID = "server_id"
        private const val NOTIF_CHANNEL = "lipton_vpn"
        private const val NOTIF_ID = 1
        private const val TAG = "LiptonVPN"

        @Volatile var isConnected: Boolean = false
            private set

        @Volatile var currentServerRemark: String = ""
            private set

        init { System.loadLibrary("lipton_native") }
    }

    // fork()+execv() without closing extra fds; returns [pid, readPipeFd] or null
    private external fun nativeSpawn(args: Array<String>): IntArray?
    private external fun nativeKill(pid: Int)

    inner class LocalBinder : Binder() {
        fun getService(): LiptonVpnService = this@LiptonVpnService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var vpnInterface:     ParcelFileDescriptor? = null
    private var xrayProcess:      Process?              = null
    private var tun2socksPid:     Int                   = 0
    private var tun2socksPipe:    ParcelFileDescriptor? = null
    private var currentServer:    Server?               = null

    var statusListener: ((VpnStatus) -> Unit)? = null
    var logListener:    ((String)    -> Unit)? = null

    private var xrayLogReader: java.io.BufferedReader? = null

    enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING, ERROR }

    private var _status = VpnStatus.DISCONNECTED
    var status: VpnStatus
        get() = _status
        private set(v) {
            _status = v
            statusListener?.invoke(v)
        }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val serverId = intent.getStringExtra(EXTRA_SERVER_ID) ?: return START_NOT_STICKY
                scope.launch { startVpnForServer(serverId) }
            }
            ACTION_STOP  -> scope.launch { stopVpn() }
        }
        return START_STICKY
    }

    // ─── Start VPN ────────────────────────────────────────────────────────────

    private suspend fun startVpnForServer(serverId: String) {
        val settings = SettingsManager(applicationContext)
        val subs = settings.getSubscriptions()
        val server = subs.flatMap { it.servers }.find { it.id == serverId } ?: run {
            Log.e(TAG, "Сервер не найден: $serverId")
            status = VpnStatus.ERROR
            return
        }
        startVpn(server, settings)
    }

    suspend fun startVpn(server: Server, settings: SettingsManager) = withContext(Dispatchers.Main) {
        if (status == VpnStatus.CONNECTED || status == VpnStatus.CONNECTING) {
            // Clean up without calling stopSelf() — service stays alive for reconnect
            status = VpnStatus.DISCONNECTING
            withContext(Dispatchers.IO) { cleanupVpn() }
            stopForeground(STOP_FOREGROUND_REMOVE)
            isConnected = false
        }

        status = VpnStatus.CONNECTING
        currentServer = server

        withContext(Dispatchers.IO) {
            try {
                // Диагностическая информация в логах
                logListener?.invoke("=== LiptonVPN ${android.os.Build.VERSION.SDK_INT} / ${android.os.Build.SUPPORTED_ABIS.firstOrNull()} ===")
                logListener?.invoke("Сервер: ${server.address}:${server.port} [${server.protocol}/${server.network}/${server.security}]")

                val bypassRu = settings.getBypassRu()
                val bypassDomains = settings.getBypassDomains()
                val socksPort = settings.getSocksPort()
                val httpPort = settings.getHttpPort()

                val config = XrayConfigGenerator.generate(
                    server = server,
                    socksPort = socksPort,
                    httpPort = httpPort,
                    bypassRu = bypassRu,
                    bypassDomains = bypassDomains,
                )

                val xrayBin = findXrayBinary()
                val configFile = File(filesDir, "xray_config.json").also { it.writeText(config) }
                val geoDir = ensureGeoFiles()

                stopXrayProcess()

                val pb = ProcessBuilder(xrayBin, "-config", configFile.absolutePath)
                    .redirectErrorStream(true)
                pb.environment()["XRAY_LOCATION_ASSET"] = geoDir
                xrayProcess = pb.start()

                val connected = waitForXrayReady(xrayProcess!!)
                if (!connected) {
                    withContext(Dispatchers.Main) { status = VpnStatus.ERROR }
                    stopXrayProcess()
                    return@withContext
                }

                establishTunnel()
                val tunPfd = vpnInterface ?: run {
                    withContext(Dispatchers.Main) { status = VpnStatus.ERROR }
                    return@withContext
                }
                // dup() creates a new fd without O_CLOEXEC so it survives exec() into tun2socks.
                // detachFd() transfers ownership: the PFD won't close the fd on GC.
                // NEVER fallback to tunPfd.fd — that raw int is owned by the PFD and
                // will be closed by GC, giving tun2socks a dangling descriptor.
                val tunFd = try {
                    tunPfd.dup().detachFd()
                } catch (e: Exception) {
                    Log.e(TAG, "Не удалось дублировать TUN fd", e)
                    withContext(Dispatchers.Main) { status = VpnStatus.ERROR }
                    return@withContext
                }
                startTun2Socks(tunFd, socksPort)
                startForeground(NOTIF_ID, buildNotification(server.remark))
                isConnected = true
                currentServerRemark = server.remark
                notifyWidgets()
                withContext(Dispatchers.Main) { status = VpnStatus.CONNECTED }
                Log.i(TAG, "Подключено: ${server.remark}")

                startLogCapture()
                monitorXrayProcess()

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка подключения", e)
                withContext(Dispatchers.Main) { status = VpnStatus.ERROR }
                cleanupVpn()
            }
        }
    }

    // ─── TUN interface ────────────────────────────────────────────────────────

    private fun establishTunnel() {
        vpnInterface?.close()
        vpnInterface = Builder()
            .setSession("LiptonVPN")
            .addAddress("10.10.10.1", 24)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .addDnsServer("8.8.8.8")
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.4.4")
            .setMtu(1500)
            .also { builder ->
                // Не маршрутизируем адрес сервера через VPN
                currentServer?.let { s ->
                    try { builder.addDisallowedApplication(packageName) } catch (_: Exception) {}
                }
            }
            .establish()
    }

    // ─── Stop VPN ─────────────────────────────────────────────────────────────

    suspend fun stopVpn() = withContext(Dispatchers.Main) {
        status = VpnStatus.DISCONNECTING
        withContext(Dispatchers.IO) { cleanupVpn() }
        isConnected = false
        currentServerRemark = ""
        notifyWidgets()
        status = VpnStatus.DISCONNECTED
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanupVpn() {
        // Kill tun2socks first and wait for it to die — it holds a dup'd TUN fd.
        // Only after the process exits are its fds closed, allowing vpnInterface.close()
        // to release the last reference to the TUN device and remove the system key icon.
        stopTun2SocksProcess()
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
        stopXrayProcess()
        try { xrayLogReader?.close() } catch (_: Exception) {}
        xrayLogReader = null
    }

    private fun stopXrayProcess() {
        xrayProcess?.let { proc ->
            try { proc.destroy(); proc.waitFor() } catch (_: Exception) {}
        }
        xrayProcess = null
    }

    private fun stopTun2SocksProcess() {
        val pid = tun2socksPid
        if (pid > 0) {
            try { nativeKill(pid) } catch (_: Exception) {}
            tun2socksPid = 0
            // Wait for the process to fully exit so the kernel closes its copy of the TUN fd
            try { Thread.sleep(200) } catch (_: InterruptedException) {}
        }
        try { tun2socksPipe?.close() } catch (_: Exception) {}
        tun2socksPipe = null
    }

    // ─── Xray helpers ─────────────────────────────────────────────────────────

    private fun findXrayBinary(): String {
        val nativeDir = applicationInfo.nativeLibraryDir
        val f = File(nativeDir, "libxray.so")
        if (f.exists()) {
            f.setExecutable(true, true)
            return f.absolutePath
        }
        throw Exception("libxray.so не найден в $nativeDir")
    }

    private fun startTun2Socks(tunFd: Int, socksPort: Int) {
        val nativeDir = applicationInfo.nativeLibraryDir
        val bin = File(nativeDir, "libtun2socks.so")
        if (!bin.exists()) {
            Log.w(TAG, "libtun2socks.so не найден")
            logListener?.invoke("[tun2socks] ОШИБКА: libtun2socks.so не найден в $nativeDir")
            return
        }
        bin.setExecutable(true, true)
        stopTun2SocksProcess()

        Log.i(TAG, "tun2socks: запуск с fd=$tunFd, proxy=socks5://127.0.0.1:$socksPort")

        // ProcessBuilder закрывает все fd > 2 перед exec() — нативный спаунер этого не делает,
        // поэтому TUN fd корректно наследуется tun2socks.
        val result = nativeSpawn(arrayOf(
            bin.absolutePath,
            "-device", "fd://$tunFd",
            "-proxy",  "socks5://127.0.0.1:$socksPort",
            "-loglevel", "info",
        ))

        if (result == null || result[0] <= 0) {
            Log.e(TAG, "tun2socks: nativeSpawn вернул null или pid<=0")
            logListener?.invoke("[tun2socks] ОШИБКА: не удалось запустить процесс")
            return
        }

        tun2socksPid  = result[0]
        tun2socksPipe = ParcelFileDescriptor.adoptFd(result[1])

        logListener?.invoke("[tun2socks] запущен: pid=$tun2socksPid, fd=$tunFd -> socks5:127.0.0.1:$socksPort")

        // Читаем вывод и мониторим завершение через pipe (EOF = процесс умер)
        scope.launch(Dispatchers.IO) {
            try {
                val pipe = tun2socksPipe ?: return@launch
                java.io.FileInputStream(pipe.fileDescriptor).bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line!!
                        Log.d(TAG, "[tun2socks] $l")
                        logListener?.invoke("[tun2socks] $l")
                    }
                }
                // EOF — процесс завершился
                if (status != VpnStatus.DISCONNECTING && status != VpnStatus.DISCONNECTED) {
                    Log.e(TAG, "tun2socks завершился неожиданно")
                    logListener?.invoke("[tun2socks] процесс завершился, VPN остановлен")
                    LiptonNotificationHelper.ensureChannels(this@LiptonVpnService)
                    LiptonNotificationHelper.showDisconnectNotification(this@LiptonVpnService)
                    isConnected = false
                    currentServerRemark = ""
                    notifyWidgets()
                    withContext(Dispatchers.Main) { status = VpnStatus.DISCONNECTED }
                    cleanupVpn()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            } catch (_: Exception) {}
        }
    }

    private fun ensureGeoFiles(): String {
        val geoDir = File(filesDir, "xray").also { it.mkdirs() }
        listOf("geoip.dat", "geosite.dat").forEach { name ->
            val dest = File(geoDir, name)
            if (!dest.exists()) {
                try {
                    assets.open("xray/$name").use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    Log.i(TAG, "Скопирован $name -> ${dest.absolutePath}")
                } catch (e: Exception) {
                    Log.w(TAG, "Не найден assets/xray/$name: ${e.message}")
                }
            }
        }
        return geoDir.absolutePath
    }

    private fun waitForXrayReady(proc: Process): Boolean {
        val reader = proc.inputStream.bufferedReader()
        xrayLogReader = reader
        val deadline = System.currentTimeMillis() + 8000
        try {
            while (System.currentTimeMillis() < deadline) {
                if (!proc.isAlive) return false
                val line = reader.readLine() ?: break
                Log.d(TAG, "[xray] $line")
                logListener?.invoke(line)
                if (line.contains("started") || line.contains("Running") || line.contains("[Warning]")) {
                    return true
                }
            }
        } catch (_: Exception) {}
        return proc.isAlive
    }

    private fun startLogCapture() {
        scope.launch(Dispatchers.IO) {
            val reader = xrayLogReader ?: return@launch
            try {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line!!
                    Log.d(TAG, "[xray] $l")
                    logListener?.invoke(l)
                }
            } catch (_: Exception) {}
        }
    }

    private fun monitorXrayProcess() {
        scope.launch(Dispatchers.IO) {
            try {
                xrayProcess?.waitFor()
                if (status == VpnStatus.CONNECTED) {
                    Log.e(TAG, "xray завершился неожиданно")
                    LiptonNotificationHelper.ensureChannels(this@LiptonVpnService)
                    LiptonNotificationHelper.showDisconnectNotification(this@LiptonVpnService)
                    isConnected = false
                    currentServerRemark = ""
                    notifyWidgets()
                    withContext(Dispatchers.Main) { status = VpnStatus.DISCONNECTED }
                    cleanupVpn()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            } catch (_: Exception) {}
        }
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun buildNotification(serverName: String): Notification {
        createNotifChannel()
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, LiptonVpnService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, NOTIF_CHANNEL)
            .setContentTitle("LiptonVPN подключён")
            .setContentText(serverName.replace(Regex("[\uD83C][\uDDE6-\uDDFF][\uD83C][\uDDE6-\uDDFF]\\s*"), "").trim())
            .setSmallIcon(R.drawable.ic_notif)
            .setContentIntent(pendingIntent)
            .addAction(0, "Отключить", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL,
                "LiptonVPN",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Статус VPN соединения" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notifyWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        manager.getAppWidgetIds(ComponentName(this, LiptonWidget::class.java))
            .forEach { LiptonWidget.updateWidget(this, manager, it) }
        com.lipton.vpn.widget.LiptonStatusWidget.refreshAll(this)
        com.lipton.vpn.widget.LiptonServerWidget.refreshAll(this)
    }

    override fun onDestroy() {
        isConnected = false
        currentServerRemark = ""
        notifyWidgets()
        scope.cancel()
        val pid = tun2socksPid
        if (pid > 0) try { nativeKill(pid) } catch (_: Exception) {}
        tun2socksPid = 0
        try { tun2socksPipe?.close() } catch (_: Exception) {}
        tun2socksPipe = null
        try { xrayProcess?.destroy() } catch (_: Exception) {}
        xrayProcess = null
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
        super.onDestroy()
    }
}
