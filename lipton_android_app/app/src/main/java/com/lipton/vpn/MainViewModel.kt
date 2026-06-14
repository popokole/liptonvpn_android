package com.lipton.vpn

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lipton.vpn.BuildConfig
import com.lipton.vpn.R
import com.lipton.vpn.data.SettingsManager
import com.lipton.vpn.data.SubscriptionManager
import com.lipton.vpn.data.UpdateChecker
import com.lipton.vpn.data.UpdateInfo
import com.lipton.vpn.data.model.Subscription
import com.lipton.vpn.data.model.displayName
import com.lipton.vpn.service.LiptonVpnService
import com.lipton.vpn.ui.theme.AppTheme
import com.lipton.vpn.util.HapticManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentLinkedQueue

sealed class ConnectionError {
    object Timeout         : ConnectionError()
    object NoInternet      : ConnectionError()
    object DnsFail         : ConnectionError()
    object ServerUnreachable : ConnectionError()
    object XrayCrash       : ConnectionError()
    object Tun2socksFail   : ConnectionError()
    data class Unknown(val rawMessage: String = "") : ConnectionError()
}

data class UiState(
    val status:              LiptonVpnService.VpnStatus = LiptonVpnService.VpnStatus.DISCONNECTED,
    val subscriptions:       List<Subscription> = emptyList(),
    val activeServerId:      String?            = null,
    val bypassRu:            Boolean            = true,
    val bypassDomains:       List<String>       = emptyList(),
    val autostart:           Boolean            = false,
    val pinging:             Boolean            = false,
    val loading:             Boolean            = true,
    val themeMode:           AppTheme           = AppTheme.SYSTEM,
    val autoConnectOnLaunch: Boolean            = false,
    val logLines:            List<String>       = emptyList(),
    val trialUsed:           Boolean            = false,
    val isFirstLaunch:       Boolean            = false,
    val updateInfo:          UpdateInfo?        = null,
    val downloadProgress:    Int?               = null,
    val downloadedApkPath:   String?            = null,
    val errorMessage:        String?            = null,
    val connectionError:     ConnectionError?   = null,
    val showWhatsNew:        Boolean            = false,
    val clipboardUrl:        String?            = null,
    val hapticEnabled:       Boolean            = true,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val settings   = SettingsManager(app)
    val subManager = SubscriptionManager(settings)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var vpnService:            LiptonVpnService?              = null
    private var vpnPermissionLauncher: ActivityResultLauncher<Intent>? = null

    private val recentConnectionLogs = mutableListOf<String>()
    private var connectionTimeoutJob: Job? = null

    // Log debouncing — batch xray log lines to avoid per-line recomposition
    private val pendingLogLines  = ConcurrentLinkedQueue<String>()
    private val logFlushPending  = AtomicBoolean(false)

    private fun logAction(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _state.update { it.copy(logLines = (it.logLines + "[$time] >> $message").takeLast(500)) }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? LiptonVpnService.LocalBinder ?: return
            vpnService = binder.getService().also { svc ->
                svc.statusListener = { newStatus ->
                    val statusMsg = when (newStatus) {
                        LiptonVpnService.VpnStatus.CONNECTING    -> "Подключение к VPN..."
                        LiptonVpnService.VpnStatus.CONNECTED     -> "VPN подключён"
                        LiptonVpnService.VpnStatus.DISCONNECTING -> "Отключение VPN..."
                        LiptonVpnService.VpnStatus.DISCONNECTED  -> "VPN отключён"
                        LiptonVpnService.VpnStatus.ERROR         -> "Ошибка подключения"
                    }
                    logAction(statusMsg)
                    when (newStatus) {
                        LiptonVpnService.VpnStatus.CONNECTING -> {
                            synchronized(recentConnectionLogs) { recentConnectionLogs.clear() }
                            startConnectionTimeout()
                            if (state.value.hapticEnabled) HapticManager.connect(getApplication())
                        }
                        LiptonVpnService.VpnStatus.CONNECTED -> {
                            connectionTimeoutJob?.cancel()
                            if (state.value.hapticEnabled) HapticManager.success(getApplication())
                        }
                        LiptonVpnService.VpnStatus.ERROR -> {
                            connectionTimeoutJob?.cancel()
                            if (state.value.hapticEnabled) HapticManager.error(getApplication())
                        }
                        else -> connectionTimeoutJob?.cancel()
                    }
                    _state.update {
                        it.copy(
                            status = newStatus,
                            connectionError = when (newStatus) {
                                LiptonVpnService.VpnStatus.ERROR      -> classifyConnectionError()
                                LiptonVpnService.VpnStatus.CONNECTED  -> null
                                else                                   -> it.connectionError
                            },
                        )
                    }
                }
                svc.logListener = { line ->
                    if (_state.value.status == LiptonVpnService.VpnStatus.CONNECTING) {
                        synchronized(recentConnectionLogs) { recentConnectionLogs.add(line) }
                    }
                    pendingLogLines.add(line)
                    // Start a flush job only if none is pending — at most one per 150ms
                    if (logFlushPending.compareAndSet(false, true)) {
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(150)
                            logFlushPending.set(false)
                            val batch = buildList<String> {
                                while (true) add(pendingLogLines.poll() ?: break)
                            }
                            if (batch.isNotEmpty()) {
                                _state.update { st ->
                                    st.copy(logLines = (st.logLines + batch).takeLast(500))
                                }
                            }
                        }
                    }
                }
                _state.update { it.copy(status = svc.status) }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            vpnService = null
            _state.update { it.copy(status = LiptonVpnService.VpnStatus.DISCONNECTED) }
        }
    }

    init {
        loadInitialData()
        observeSettings()
        checkForUpdate()
        watchTrialExpiry()
    }

    private fun checkForUpdate() {
        viewModelScope.launch { doCheckUpdate() }
    }

    private fun watchTrialExpiry() {
        viewModelScope.launch {
            // Ждём пока загрузятся данные (loadInitialData асинхронный)
            state.first { !it.loading }
            while (true) {
                val now = System.currentTimeMillis() / 1000L
                val expired = state.value.subscriptions.filter { sub ->
                    sub.isTrial && sub.userInfo.expire > 0L && sub.userInfo.expire < now
                }
                if (expired.isNotEmpty()) {
                    expired.forEach { sub ->
                        subManager.remove(sub.id)
                        logAction("Пробный доступ истёк — подписка удалена")
                    }
                    if (state.value.status == LiptonVpnService.VpnStatus.CONNECTED ||
                        state.value.status == LiptonVpnService.VpnStatus.CONNECTING) {
                        disconnect(getApplication())
                        logAction("VPN отключён: срок пробного доступа истёк")
                    }
                    _state.update { it.copy(
                        errorMessage = "Пробный доступ истёк. Оформите подписку для продолжения."
                    ) }
                }
                delay(30_000)
            }
        }
    }

    suspend fun manualCheckUpdate(): Boolean {
        val info = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
        _state.update { it.copy(updateInfo = info) }
        return info != null
    }

    private suspend fun doCheckUpdate() {
        val info = runCatching { UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME) }.getOrNull()
        if (info != null) {
            _state.update { it.copy(updateInfo = info) }
            downloadUpdate()
        }
    }

    fun dismissFirstLaunch() = _state.update { it.copy(isFirstLaunch = false) }
    fun clearError() = _state.update { it.copy(errorMessage = null) }
    fun clearConnectionError() = _state.update { it.copy(connectionError = null) }
    fun dismissWhatsNew()      = _state.update { it.copy(showWhatsNew = false) }
    fun dismissClipboard()     = _state.update { it.copy(clipboardUrl = null) }

    fun checkClipboard(context: Context, text: String?) {
        if (text.isNullOrBlank()) return
        val url = text.trim()
        if (!url.startsWith("https://sub.popokole.online/") && !url.startsWith("liptonvpn://")) return
        viewModelScope.launch {
            val last = settings.getClipboardLastImported()
            if (last == url) return@launch
            val already = settings.getSubscriptions().any { it.url == url || it.url == url.replaceFirst("liptonvpn://", "https://") }
            if (already) return@launch
            _state.update { it.copy(clipboardUrl = url) }
        }
    }

    fun importClipboardUrl(context: Context) {
        val url = state.value.clipboardUrl ?: return
        _state.update { it.copy(clipboardUrl = null) }
        viewModelScope.launch {
            settings.setClipboardLastImported(url)
            try { addSubscription(url) } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setHapticEnabled(enabled) }
        _state.update { it.copy(hapticEnabled = enabled) }
    }

    fun checkWhatsNew(currentVersion: String) {
        viewModelScope.launch {
            val last = settings.getLastSeenVersion()
            if (last != currentVersion) {
                _state.update { it.copy(showWhatsNew = true) }
                settings.setLastSeenVersion(currentVersion)
            }
        }
    }

    fun switchToNextServer(context: Context) {
        clearConnectionError()
        val allServers = state.value.subscriptions.flatMap { it.servers }
        if (allServers.isEmpty()) return
        val currentIdx = allServers.indexOfFirst { it.id == state.value.activeServerId }
        val nextServer = allServers[(currentIdx + 1) % allServers.size]
        connect(context, nextServer.id)
    }

    private fun startConnectionTimeout() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = viewModelScope.launch {
            delay(20_000)
            if (state.value.status == LiptonVpnService.VpnStatus.CONNECTING) {
                disconnect(getApplication())
                _state.update { it.copy(connectionError = ConnectionError.Timeout) }
            }
        }
    }

    private fun classifyConnectionError(): ConnectionError {
        val logs = synchronized(recentConnectionLogs) {
            recentConnectionLogs.joinToString("\n")
        }.lowercase()
        return when {
            logs.contains("i/o timeout") ||
            logs.contains("connection timed out") ||
            logs.contains("context deadline exceeded") -> ConnectionError.Timeout

            logs.contains("network is unreachable") ||
            logs.contains("no route to host") ||
            logs.contains("network unreachable") -> ConnectionError.NoInternet

            logs.contains("no such host") ||
            (logs.contains("dns") && (logs.contains("fail") || logs.contains("error"))) -> ConnectionError.DnsFail

            logs.contains("connection refused") -> ConnectionError.ServerUnreachable

            logs.contains("[tun2socks]") &&
            (logs.contains("error") || logs.contains("fail")) -> ConnectionError.Tun2socksFail

            else -> ConnectionError.Unknown()
        }
    }

    fun dismissUpdate() = _state.update { it.copy(updateInfo = null, downloadProgress = null, downloadedApkPath = null) }

    fun downloadUpdate() {
        if (state.value.downloadProgress != null) return  // already downloading
        val url = state.value.updateInfo?.downloadUrl ?: return
        viewModelScope.launch {
            val app = getApplication<Application>()
            val destFile = File(app.getExternalFilesDir("downloads"), "liptonvpn-update.apk")
            _state.update { it.copy(downloadProgress = 0) }
            val success = UpdateChecker.downloadApk(url, destFile) { progress ->
                _state.update { it.copy(downloadProgress = progress) }
            }
            if (success) {
                _state.update { it.copy(downloadProgress = 100, downloadedApkPath = destFile.absolutePath) }
                showInstallNotification(app, destFile)
            } else {
                _state.update { it.copy(downloadProgress = null) }
            }
        }
    }

    private fun showInstallNotification(app: Application, apkFile: File) {
        val channelId = "lipton_updates"
        val manager = app.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Обновления LiptonVPN", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(app, 0, installIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val version = state.value.updateInfo?.versionName ?: ""
        val notif = NotificationCompat.Builder(app, channelId)
            .setContentTitle("Обновление LiptonVPN v$version готово")
            .setContentText("Нажмите чтобы установить")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(42, notif)
    }

    fun installUpdate(context: Context) {
        val path = state.value.downloadedApkPath ?: return
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val subs            = settings.getSubscriptions()
            val bypassRu        = settings.getBypassRu()
            val bypassDomains   = settings.getBypassDomains()
            val autostart       = settings.getAutostart()
            val themeMode       = settings.getThemeMode()
            val autoConnect     = settings.getAutoConnectOnLaunch()
            val trialUsed       = settings.getTrialAdded()
            val firstLaunchDone = settings.getFirstLaunchDone()
            val hapticEnabled   = settings.getHapticEnabled()

            if (!firstLaunchDone) settings.setFirstLaunchDone(true)

            // Auto-select first server if none is saved
            var activeId = settings.getActiveServerId()
            val allServers = subs.flatMap { it.servers }
            if (activeId == null || allServers.none { it.id == activeId }) {
                activeId = allServers.firstOrNull()?.id
                if (activeId != null) settings.setActiveServerId(activeId)
            }

            val crashLines = CrashLogger.readAndClear(getApplication()) ?: emptyList()

            _state.update {
                it.copy(
                    subscriptions       = subs,
                    activeServerId      = activeId,
                    bypassRu            = bypassRu,
                    bypassDomains       = bypassDomains,
                    autostart           = autostart,
                    themeMode           = themeMode,
                    autoConnectOnLaunch = autoConnect,
                    trialUsed           = trialUsed,
                    isFirstLaunch       = !firstLaunchDone,
                    logLines            = crashLines,
                    hapticEnabled       = hapticEnabled,
                    loading             = false,
                )
            }

            // Background refresh + ping on startup
            if (subs.isNotEmpty()) {
                launch {
                    val now = System.currentTimeMillis()
                    val stale = subs.filter { sub ->
                        !sub.isTrial && (now - sub.lastUpdated) > 6 * 3_600_000L
                    }
                    if (stale.isNotEmpty()) {
                        logAction("Обновление конфигов серверов...")
                        var refreshed = 0
                        stale.forEach { sub ->
                            try {
                                subManager.refresh(sub.id)
                                refreshed++
                            } catch (_: Exception) {
                                // silent — no internet or server error, keep existing data
                            }
                        }
                        if (refreshed > 0) logAction("Конфиги обновлены")
                    }

                    // Ping after refresh so we get latencies for fresh server list
                    val freshSubs = settings.getSubscriptions()
                    _state.update { it.copy(pinging = true) }
                    freshSubs.forEach { sub -> subManager.pingAll(sub.id) }
                    _state.update { it.copy(pinging = false) }
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settings.subscriptionsFlow.collect { subs ->
                _state.update { it.copy(subscriptions = subs) }
            }
        }
        viewModelScope.launch {
            settings.bypassRuFlow.collect { v ->
                _state.update { it.copy(bypassRu = v) }
            }
        }
        viewModelScope.launch {
            settings.bypassDomainsFlow.collect { domains ->
                _state.update { it.copy(bypassDomains = domains) }
            }
        }
    }

    // ─── Service binding ─────────────────────────────────────────────────────

    fun bindService(context: Context) {
        val intent = Intent(context, LiptonVpnService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        try { context.unbindService(serviceConnection) } catch (_: Exception) {}
    }

    fun setPermissionLauncher(launcher: ActivityResultLauncher<Intent>) {
        vpnPermissionLauncher = launcher
    }

    // ─── VPN control ─────────────────────────────────────────────────────────

    fun handleConnectToggle(context: Context) {
        val st = state.value.status
        if (st == LiptonVpnService.VpnStatus.CONNECTING ||
            st == LiptonVpnService.VpnStatus.DISCONNECTING) return

        if (st == LiptonVpnService.VpnStatus.CONNECTED) {
            disconnect(context)
            return
        }

        val serverId = state.value.activeServerId
            ?: state.value.subscriptions.flatMap { it.servers }.firstOrNull()?.id
            ?: return

        val permIntent = VpnService.prepare(context)
        if (permIntent != null) {
            vpnPermissionLauncher?.launch(permIntent)
        } else {
            connect(context, serverId)
        }
    }

    fun connect(context: Context, serverId: String) {
        val name = state.value.subscriptions.flatMap { it.servers }
            .find { it.id == serverId }?.displayName() ?: serverId
        logAction("Подключение к: $name")
        viewModelScope.launch { settings.setActiveServerId(serverId) }
        _state.update { it.copy(activeServerId = serverId) }

        Intent(context, LiptonVpnService::class.java).apply {
            action = LiptonVpnService.ACTION_START
            putExtra(LiptonVpnService.EXTRA_SERVER_ID, serverId)
            context.startForegroundService(this)
        }
    }

    fun disconnect(context: Context) {
        logAction("Отключение VPN")
        Intent(context, LiptonVpnService::class.java).apply {
            action = LiptonVpnService.ACTION_STOP
            context.startService(this)
        }
    }

    fun selectServer(context: Context, serverId: String) {
        val name = state.value.subscriptions.flatMap { it.servers }
            .find { it.id == serverId }?.displayName() ?: serverId
        logAction("Выбран сервер: $name")
        viewModelScope.launch { settings.setActiveServerId(serverId) }
        _state.update { it.copy(activeServerId = serverId) }
        if (state.value.status == LiptonVpnService.VpnStatus.CONNECTED) {
            connect(context, serverId)
        }
    }

    // ─── Subscriptions ────────────────────────────────────────────────────────

    fun showError(message: String?) {
        _state.update { it.copy(errorMessage = message) }
    }

    suspend fun addSubscription(url: String) {
        val existing = settings.getSubscriptions()
        val hasNonTrial = existing.any { !it.isTrial }
        if (hasNonTrial) {
            throw Exception("Можно добавить только одну платную подписку. Сначала удалите текущую подписку в настройках.")
        }
        subManager.add(url)
    }
    suspend fun removeSubscription(subId: String)       = subManager.remove(subId)
    suspend fun refreshSubscription(subId: String)      = subManager.refresh(subId)

    fun pingAll(subId: String) {
        viewModelScope.launch {
            _state.update { it.copy(pinging = true) }
            subManager.pingAll(subId)
            _state.update { it.copy(pinging = false) }
        }
    }

    suspend fun getTrialSubscription(durationMinutes: Int) {
        val hwid = settings.getHwid()
        subManager.getTrialSubscription(hwid, durationMinutes)
        settings.setTrialAdded(true)
        _state.update { it.copy(trialUsed = true) }
    }

    // ─── Settings ─────────────────────────────────────────────────────────────

    fun setBypassRu(enabled: Boolean) {
        logAction("Обход РУ трафика: ${if (enabled) "включён" else "выключен"}")
        viewModelScope.launch {
            settings.setBypassRu(enabled)
            if (state.value.status == LiptonVpnService.VpnStatus.CONNECTED) {
                val serverId = state.value.activeServerId
                    ?: state.value.subscriptions.flatMap { it.servers }.firstOrNull()?.id
                if (serverId != null) try { connect(getApplication(), serverId) } catch (_: Exception) {}
            }
        }
    }

    fun setAutostart(enabled: Boolean) {
        viewModelScope.launch { settings.setAutostart(enabled) }
        _state.update { it.copy(autostart = enabled) }
    }

    fun setThemeMode(theme: AppTheme) {
        viewModelScope.launch { settings.setThemeMode(theme) }
        _state.update { it.copy(themeMode = theme) }
    }

    fun setAutoConnectOnLaunch(enabled: Boolean) {
        logAction("Авто-подключение при запуске: ${if (enabled) "включено" else "выключено"}")
        viewModelScope.launch { settings.setAutoConnectOnLaunch(enabled) }
        _state.update { it.copy(autoConnectOnLaunch = enabled) }
    }

    fun addBypassDomain(domain: String) {
        viewModelScope.launch {
            val current = settings.getBypassDomains().toMutableList()
            if (!current.contains(domain)) {
                current.add(domain)
                settings.saveBypassDomains(current)
            }
        }
    }

    fun removeBypassDomain(domain: String) {
        viewModelScope.launch {
            val current = settings.getBypassDomains().toMutableList()
            current.remove(domain)
            settings.saveBypassDomains(current)
        }
    }

    fun clearLogs() {
        pendingLogLines.clear()
        _state.update { it.copy(logLines = emptyList()) }
    }

    fun resetProfile(context: Context) {
        viewModelScope.launch {
            if (state.value.status == LiptonVpnService.VpnStatus.CONNECTED ||
                state.value.status == LiptonVpnService.VpnStatus.CONNECTING) {
                disconnect(context)
            }
            settings.resetNetworkSettings()
            _state.update {
                it.copy(
                    bypassRu            = true,
                    bypassDomains       = emptyList(),
                    autoConnectOnLaunch = false,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectionTimeoutJob?.cancel()
        vpnService?.statusListener = null
        vpnService?.logListener    = null
        pendingLogLines.clear()
    }
}
