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
import com.lipton.vpn.data.ApiClient
import com.lipton.vpn.data.BannerLogic
import com.lipton.vpn.data.SettingsManager
import com.lipton.vpn.data.SplitMode
import com.lipton.vpn.data.maskIps
import com.lipton.vpn.data.parseIsoMillis
import com.lipton.vpn.data.SubscriptionManager
import com.lipton.vpn.data.UpdateChecker
import com.lipton.vpn.data.UpdateInfo
import com.lipton.vpn.data.model.AppBanner
import com.lipton.vpn.data.model.AppConfig
import com.lipton.vpn.data.model.NotificationPrefs
import com.lipton.vpn.data.model.ChangeCurrent
import com.lipton.vpn.data.model.ChangeOption
import com.lipton.vpn.data.model.DeviceItem
import com.lipton.vpn.data.model.MeProfile
import com.lipton.vpn.data.model.MeSubscription
import com.lipton.vpn.data.model.PromoResult
import com.lipton.vpn.data.model.Server
import com.lipton.vpn.data.model.SubOverlay
import com.lipton.vpn.data.model.Subscription
import com.lipton.vpn.data.model.displayName
import com.lipton.vpn.service.LiptonVpnService
import com.lipton.vpn.ui.theme.AppTheme
import com.lipton.vpn.util.HapticManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
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
    val isFirstLaunch:       Boolean            = false,
    val updateInfo:          UpdateInfo?        = null,
    val downloadProgress:    Int?               = null,
    val downloadedApkPath:   String?            = null,
    val errorMessage:        String?            = null,
    val connectionError:     ConnectionError?   = null,
    val showWhatsNew:        Boolean            = false,
    val clipboardUrl:        String?            = null,
    val hapticEnabled:       Boolean            = true,
    // ─── Аккаунт (liptonone.online) ───────────────────────────────────────
    val isAuthed:            Boolean            = false,
    val accountSyncing:      Boolean            = false,   // тянем подписку после входа
    val accountStatus:       String?            = null,    // active | trial | grace | none | ...
    val accountPeriodEnd:    String?            = null,
    val accountNoSub:        Boolean            = false,   // вошёл, но подписки нет → предложить оплату
    val accountOverlay:      SubOverlay?        = null,    // действующий временный тариф («Обход» поверх «Базового»)
    val accountTariffCode:   String?            = null,    // tariff_code из /me/subscription (цвет свечения «Обход»)
    val accountCanceled:     Boolean            = false,
    // ─── Профиль (редизайн A4) ────────────────────────────────────────────
    val me:                  MeProfile?         = null,    // GET /me
    val devices:             List<DeviceItem>?  = null,    // GET /me/devices; null — ещё не загружены
    val deviceLimit:         Int?               = null,
    val devicesUsed:         Int?               = null,
    val appConfig:           AppConfig?         = null,    // GET /config: тарифы и сроки
    val subscriptionUrl:     String?            = null,
    val linkVersion:         Int                = 0,
    val linkUpdatedAt:       String?            = null,
    val hwid:                String?            = null,    // HWID этого телефона («это устройство»)
    val profileBusy:         String?            = null,    // relink | revoke:<hwid> | revoke_all | cancel | card
    val lastPingAt:          Long?              = null,    // когда последний раз пинговали серверы
    val notificationsEnabled: Boolean           = true,
    val splitTunnelApps:     List<String>       = emptyList(),
    // ─── Редизайн, волна 2 ────────────────────────────────────────────────
    val splitTunnelMode:     SplitMode          = SplitMode.ALL,
    val guest:               TrialSession?      = null,    // гостевой режим без аккаунта (ended — 15 минут прошли)
    val guestRetryAt:        Long?              = null,    // когда снова можно гостевой доступ (из 429)
    val dailyTrial:          TrialSession?      = null,    // «15 минут бесплатно» для вошедших без подписки
    val dailyTrialRetryAt:   Long?              = null,
    val authEntry:           String?            = null,    // гость открыл вход: start | login | email | telegram
    val loginSuccess:        Boolean            = false,   // экран «Вы вошли. Всё готово.»
    val banners:             List<AppBanner>    = emptyList(),  // активные баннеры и экраны из админки
    val notifPrefs:          NotificationPrefs? = null,    // GET /me/notifications; null — ручки нет
    val bypassDomainDates:   Map<String, Long>  = emptyMap(),
    val verboseLogs:         Boolean            = false,
    val cardUnlinkAvailableAt: String?          = null,    // из ответа 409 card_unlink_cooldown
)

/**
 * Пробная сессия (гостевая или «15 минут в день»): до какого момента действует
 * (мс), сколько минут всего, сервер и ссылка подписки. ended — время вышло.
 */
data class TrialSession(
    val expiresAt:       Long,
    val minutes:         Int,
    val serverName:      String? = null,
    val subscriptionUrl: String? = null,
    val ended:           Boolean = false,
)

/** Итог попытки включить пробный доступ. */
sealed class TrialStart {
    object Ok : TrialStart()
    data class Used(val retryAt: Long?) : TrialStart()
    object Disabled : TrialStart()
    object HasSubscription : TrialStart()
    data class Failed(val message: String) : TrialStart()
}

// Состояние экрана «Сменить тариф».
// phase: choose (список вариантов) | confirm (предпросмотр + подтверждение) |
//        wait (ждём оплату / списание) | ok | fail
data class TariffChangeState(
    val loading:         Boolean             = false,
    val available:       Boolean             = false,
    val reason:          String?             = null,   // почему смена недоступна (текст сервера)
    val discountPercent: Int                 = 0,
    val current:         ChangeCurrent?      = null,
    val options:         List<ChangeOption>  = emptyList(),
    val preview:         ChangeOption?       = null,   // вариант, пересчитанный сервером
    val previewing:      Boolean             = false,
    val submitting:      Boolean             = false,
    val phase:           String              = "choose",
    val txId:            String?             = null,
    val resultText:      String?             = null,   // пояснение на экране ok / fail
    val error:           String?             = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val settings   = SettingsManager(app)
    val subManager = SubscriptionManager(settings)
    val api        = ApiClient(settings)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _stats = MutableStateFlow(StatsState())
    val stats: StateFlow<StatsState> = _stats.asStateFlow()

    private val _news = MutableStateFlow(NewsState())
    val news: StateFlow<NewsState> = _news.asStateFlow()

    /** Срок, выбранный на главной («Купить»), — экран оплаты откроется сразу на нём. */
    var paymentPreselectPeriod: String? = null
    /** Проверенный промокод — уйдёт в ближайший checkout. */
    var pendingPromo: String? = null
        private set

    private var ipJob: Job? = null

    private val _change = MutableStateFlow(TariffChangeState())
    val changeState: StateFlow<TariffChangeState> = _change.asStateFlow()
    private var changePollJob: Job? = null
    private var changeKey: String? = null   // idempotency_key текущей попытки
    private var changeGen = 0               // растёт при сбросе — устаревшие ответы игнорируются

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
        observeExtras()
        watchConnectionStats()
        checkForUpdate()
        watchTrialExpiry()
        watchTrialDeadlines()
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
                val own = setOfNotNull(state.value.guest?.subscriptionUrl, state.value.dailyTrial?.subscriptionUrl)
                val expired = state.value.subscriptions.filter { sub ->
                    sub.isTrial && sub.userInfo.expire > 0L && sub.userInfo.expire < now && sub.url !in own
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
            val now             = System.currentTimeMillis()
            val guestStored     = if (settings.getAuthTokens() == null) settings.getGuestSession() else null
            val dailyStored     = settings.getDailyTrial()
            val firstLaunchDone = settings.getFirstLaunchDone()
            val hapticEnabled   = settings.getHapticEnabled()
            val authed          = settings.getAuthTokens() != null

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
                    guest               = guestStored?.takeIf { it.expiresAt > now && !it.subscriptionUrl.isNullOrBlank() }?.toSession(),
                    guestRetryAt        = guestStored?.retryAt?.takeIf { it > now },
                    dailyTrial          = dailyStored?.takeIf { authed && it.expiresAt > now }?.toSession(),
                    dailyTrialRetryAt   = dailyStored?.retryAt?.takeIf { authed && it > now },
                    isFirstLaunch       = !firstLaunchDone,
                    logLines            = crashLines,
                    hapticEnabled       = hapticEnabled,
                    isAuthed            = authed,
                    loading             = false,
                )
            }

            // Вошёл в аккаунт → тихо тянем актуальную подписку с сервера.
            if (authed) launch { syncAccountSubscription() }
            // Конфиг нужен и до входа (кнопка «15 минут без регистрации»), баннеры — всем.
            loadConfigIfNeeded()
            loadBanners()
            // Истёкшая гостевая сессия: подписку убираем, «когда снова» помним.
            if (guestStored != null && guestStored.expiresAt <= now) {
                launch { finishGuestStorage(guestStored) }
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
                    _state.update { it.copy(pinging = false, lastPingAt = System.currentTimeMillis()) }
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
            _state.update { it.copy(pinging = false, lastPingAt = System.currentTimeMillis()) }
        }
    }

    /** «Пинг» на вкладке «Серверы»: все подписки разом. */
    fun pingAllServers() {
        if (state.value.pinging) return
        viewModelScope.launch {
            _state.update { it.copy(pinging = true) }
            settings.getSubscriptions().forEach { sub -> try { subManager.pingAll(sub.id) } catch (_: Exception) {} }
            _state.update { it.copy(pinging = false, lastPingAt = System.currentTimeMillis()) }
        }
    }

    // ─── Аккаунт (вход + авто-подписка) ────────────────────────────────────────

    // Обёртки входа для LoginScreen. Кидают ApiException при ошибке.
    suspend fun authDeviceExchange(code: String) = api.deviceExchange(code)
    suspend fun authEmailRequest(email: String)  = api.emailRequest(email)
    suspend fun authEmailVerify(email: String, code: String) = api.emailVerify(email, code)
    suspend fun authTgInit()                     = api.tgInit()
    suspend fun authTgPoll(linkToken: String)    = api.tgPoll(linkToken)

    // Вызывать после успешного входа — переключает экран («Вы вошли») и тянет подписку.
    // Гостевая сессия при входе заканчивается: её подписка заменяется подпиской аккаунта.
    fun completeLogin() {
        val guest = state.value.guest
        _state.update { it.copy(isAuthed = true, loginSuccess = true, authEntry = null, guest = null) }
        viewModelScope.launch {
            if (guest != null) {
                if (isVpnActive()) disconnect(getApplication())
                removeSubscriptionsByUrl(guest.subscriptionUrl)
                settings.setGuestSession(null)
            }
            syncAccountSubscription()
            loadBanners()
        }
    }

    fun dismissLoginSuccess() = _state.update { it.copy(loginSuccess = false) }

    /** Гость открыл вход или регистрацию (start | login | email | telegram); null — вернуться. */
    fun openAuth(entry: String?) = _state.update { it.copy(authEntry = entry) }

    // Тянет /me/subscription. Если есть subscription_url — заводит/обновляет
    // единственную подписку. Если подписки нет — ставим флаг accountNoSub
    // (экран предложит оплатить).
    suspend fun syncAccountSubscription() {
        _state.update { it.copy(accountSyncing = true) }
        try {
            val me = api.getSubscription()
            val url = me.subscriptionUrl
            val hasSub = !url.isNullOrBlank() &&
                me.status != "none" && me.status != "expired" && me.status != "canceled"
            if (hasSub) {
                try { subManager.syncFromAccount(url!!) } catch (_: Exception) { /* оффлайн — оставляем кэш */ }
            }
            _state.update { applySubscriptionView(it, me).copy(accountNoSub = !hasSub, accountSyncing = false) }
            loadProfile()
        } catch (e: ApiClient.ApiException) {
            if (e.code == "unauthorized") {
                // сессия умерла — на экран входа
                _state.update { it.copy(isAuthed = false, accountSyncing = false) }
            } else {
                _state.update { it.copy(accountSyncing = false, errorMessage = e.message) }
            }
        }
    }

    fun refreshAccount() { viewModelScope.launch { syncAccountSubscription() } }

    private fun applySubscriptionView(st: UiState, me: MeSubscription): UiState = st.copy(
        accountStatus     = me.status,
        accountPeriodEnd  = me.currentPeriodEnd,
        accountOverlay    = me.overlay,
        accountTariffCode = me.tariffCode,
        accountCanceled   = me.canceled,
        subscriptionUrl   = me.subscriptionUrl,
        linkVersion       = me.linkVersion,
        linkUpdatedAt     = me.linkUpdatedAt,
        devicesUsed       = me.devicesUsed ?: st.devicesUsed,
        deviceLimit       = me.deviceLimit ?: st.deviceLimit,
    )

    fun logoutAccount() {
        viewModelScope.launch {
            if (state.value.status == LiptonVpnService.VpnStatus.CONNECTED ||
                state.value.status == LiptonVpnService.VpnStatus.CONNECTING) {
                disconnect(getApplication())
            }
            api.logout()
            settings.saveSubscriptions(emptyList())
            _state.update {
                it.copy(
                    isAuthed = false, subscriptions = emptyList(), activeServerId = null,
                    accountStatus = null, accountPeriodEnd = null, accountNoSub = false,
                    accountOverlay = null, accountTariffCode = null, accountCanceled = false,
                    me = null, devices = null, deviceLimit = null, devicesUsed = null,
                    subscriptionUrl = null, linkVersion = 0, linkUpdatedAt = null, profileBusy = null,
                    dailyTrial = null, dailyTrialRetryAt = null, notifPrefs = null, cardUnlinkAvailableAt = null,
                    loginSuccess = false,
                )
            }
            settings.setDailyTrial(null)
            resetTariffChange()
            loadBanners()
        }
    }

    // ─── Профиль: /me, устройства, ссылка, отмена, карта (редизайн A4) ────────

    /** Профиль, устройства и тарифы. Ошибки тихие — блоки просто остаются пустыми. */
    fun loadProfile() {
        if (!state.value.isAuthed) return
        viewModelScope.launch {
            try {
                val me = api.getMe()
                _state.update { it.copy(me = me) }
            } catch (e: ApiClient.ApiException) {
                if (e.code == "unauthorized") _state.update { it.copy(isAuthed = false) }
            } catch (_: Exception) {}
        }
        refreshDevices()
        loadConfigIfNeeded()
        loadNotificationPrefs()
    }

    fun refreshDevices() {
        if (!state.value.isAuthed) return
        viewModelScope.launch {
            try {
                val d = api.getDevices()
                _state.update { it.copy(devices = d.devices ?: emptyList(), deviceLimit = d.deviceLimit ?: it.deviceLimit) }
            } catch (_: Exception) {}
        }
    }

    fun loadConfigIfNeeded() {
        if (state.value.appConfig != null) return
        viewModelScope.launch {
            try {
                val cfg = api.getConfig()
                _state.update { it.copy(appConfig = cfg) }
            } catch (_: Exception) {}
        }
    }

    private fun profileAction(tag: String, block: suspend () -> Unit) {
        if (state.value.profileBusy != null) return
        _state.update { it.copy(profileBusy = tag) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: ApiClient.ApiException) {
                if (e.code == "unauthorized") _state.update { it.copy(isAuthed = false) }
                _state.update { it.copy(errorMessage = e.message) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "Не получилось, попробуйте ещё раз") }
            } finally {
                _state.update { it.copy(profileBusy = null) }
            }
        }
    }

    fun revokeDevice(hwid: String) = profileAction("revoke:$hwid") {
        api.revokeDevice(hwid)
        _state.update { st -> st.copy(devices = st.devices?.filterNot { it.hwid == hwid }, errorMessage = "Устройство отвязано") }
        refreshDevices()
    }

    fun revokeAllDevices() = profileAction("revoke_all") {
        api.revokeAllDevices()
        _state.update { it.copy(devices = emptyList(), errorMessage = "Все устройства отвязаны — подключите их заново") }
        refreshDevices()
    }

    /**
     * «Обновить ссылку»: старая ссылка и все устройства на ней отключатся.
     * Новую ссылку сразу подтягиваем на этот телефон и, если VPN был включён,
     * переподключаемся к тому же серверу.
     */
    fun relinkSubscription(context: Context) = profileAction("relink") {
        val oldActive = activeServer()?.displayName()
        val res = api.relinkSubscription(state.value.linkVersion.takeIf { it > 0 })
        val url = res.subscriptionUrl
        if (!url.isNullOrBlank()) {
            try { subManager.syncFromAccount(url) } catch (_: Exception) {}
        }
        val servers = settings.getSubscriptions().flatMap { it.servers }
        val next = servers.find { it.displayName() == oldActive } ?: servers.firstOrNull()
        if (next != null) settings.setActiveServerId(next.id)
        _state.update {
            applySubscriptionView(it, res).copy(
                activeServerId = next?.id ?: it.activeServerId,
                errorMessage = "Ссылка обновлена — подключите другие устройства заново",
            )
        }
        if (next != null && state.value.status == LiptonVpnService.VpnStatus.CONNECTED) connect(context, next.id)
        refreshDevices()
    }

    /** Отмена подписки: заканчивается сразу, остаток не возвращается, карта удаляется. */
    fun cancelSubscription(context: Context) = profileAction("cancel") {
        api.cancelSubscription()
        if (state.value.status == LiptonVpnService.VpnStatus.CONNECTED ||
            state.value.status == LiptonVpnService.VpnStatus.CONNECTING) {
            disconnect(context)
        }
        syncAccountSubscription()
        _state.update { it.copy(errorMessage = "Подписка отменена") }
    }

    /**
     * «Отвязать карту»: автопродление выключится, подписка работает до конца срока.
     * 409 card_unlink_cooldown — карту привязали меньше 24 ч назад: запоминаем
     * available_at, экран покажет, когда отвязка станет доступна.
     */
    fun unlinkCard(onDone: (Boolean) -> Unit = {}) = profileAction("card") {
        try {
            val res = api.deleteCard()
            val w = res.warning
            _state.update { it.copy(errorMessage = if (!w.isNullOrBlank()) w else "Карта отвязана, автопродление выключено", cardUnlinkAvailableAt = null) }
            onDone(true)
        } catch (e: ApiClient.ApiException) {
            if (e.status == 409 && e.serverCode == "card_unlink_cooldown") {
                _state.update { it.copy(cardUnlinkAvailableAt = e.availableAt ?: it.cardUnlinkAvailableAt, errorMessage = e.message) }
                onDone(false)
                return@profileAction
            }
            onDone(false)
            throw e
        }
        try {
            val me = api.getMe()
            _state.update { it.copy(me = me) }
        } catch (_: Exception) {}
    }

    /** Проверка промокода; валидный запоминается до ближайшей оплаты. */
    suspend fun validatePromo(code: String): PromoResult {
        val res = api.validatePromo(code.trim())
        pendingPromo = if (res.valid) code.trim() else null
        return res
    }

    fun clearPendingPromo() { pendingPromo = null }

    fun setNotificationsEnabled(enabled: Boolean) {
        _state.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch { settings.setNotificationsEnabled(enabled) }
    }

    /** Приложения мимо VPN. Применяются при следующем подключении (или «Переподключить»). */
    fun setSplitTunnelApps(packages: List<String>) {
        _state.update { it.copy(splitTunnelApps = packages) }
        viewModelScope.launch { settings.setSplitTunnelApps(packages) }
    }

    /** Режим: «Все через VPN» / «Выбранные мимо VPN». Применяется при следующем подключении. */
    fun setSplitTunnelMode(mode: SplitMode) {
        _state.update { it.copy(splitTunnelMode = mode) }
        viewModelScope.launch { settings.setSplitTunnelMode(mode) }
    }

    /** Переподключиться к текущему серверу, чтобы применить настройки туннеля. */
    fun reconnectIfConnected(context: Context) {
        val st = state.value
        if (st.status != LiptonVpnService.VpnStatus.CONNECTED) return
        val id = st.activeServerId ?: st.subscriptions.flatMap { it.servers }.firstOrNull()?.id ?: return
        connect(context, id)
    }

    fun setVerboseLogs(enabled: Boolean) {
        _state.update { it.copy(verboseLogs = enabled) }
        logAction("Подробные логи: ${if (enabled) "включены" else "выключены"} — применятся при следующем подключении")
        viewModelScope.launch { settings.setVerboseLogs(enabled) }
    }

    /** Логи для поддержки: адреса замаскированы. */
    fun logsForSupport(): String = maskIps(state.value.logLines.joinToString("\n"))

    /** Отправить логи в чат поддержки (POST /support/ai/logs). */
    suspend fun sendLogsToSupport(note: String = "Логи из приложения Android ${BuildConfig.VERSION_NAME}") {
        api.aiLogs(logsForSupport().takeLast(60_000), note)
    }

    private fun activeServer(): Server? {
        val all = state.value.subscriptions.flatMap { it.servers }
        return all.find { it.id == state.value.activeServerId } ?: all.firstOrNull()
    }

    // ─── Новости и статус серверов ───────────────────────────────────────────

    fun loadNews(force: Boolean = false) {
        val n = _news.value
        if (n.loading) return
        val now = System.currentTimeMillis()
        if (!force && n.loadedAt > 0 && now - n.loadedAt < 5 * 60_000L) return
        _news.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            var err: String? = null
            val items = try { api.getNews().items } catch (e: Exception) { err = e.message; null }
            val status = try { api.getServerStatus() } catch (_: Exception) { null }
            val at = System.currentTimeMillis()
            _news.update {
                it.copy(
                    items = items ?: it.items,
                    status = status ?: it.status,
                    statusAt = if (status != null) at else it.statusAt,
                    loading = false,
                    loadedAt = at,
                    error = if (items == null) (err ?: "Не удалось загрузить новости") else null,
                )
            }
        }
    }

    fun markNewsRead(id: String) {
        if (id.isBlank() || id in _news.value.readIds) return
        viewModelScope.launch { settings.markNewsRead(listOf(id)) }
    }

    fun markAllNewsRead() {
        val ids = _news.value.items.map { it.id }.filter { it.isNotBlank() }
        viewModelScope.launch { settings.markNewsRead(ids) }
    }

    // ─── Живая статистика главной ────────────────────────────────────────────

    private fun observeExtras() {
        viewModelScope.launch { settings.trafficDaysFlow.collect { d -> _stats.update { it.copy(trafficDays = d) } } }
        viewModelScope.launch { settings.newsReadFlow.collect { ids -> _news.update { it.copy(readIds = ids) } } }
        viewModelScope.launch { settings.notificationsFlow.collect { v -> _state.update { it.copy(notificationsEnabled = v) } } }
        viewModelScope.launch { settings.splitTunnelAppsFlow.collect { v -> _state.update { it.copy(splitTunnelApps = v) } } }
        viewModelScope.launch { settings.splitTunnelModeFlow.collect { v -> _state.update { it.copy(splitTunnelMode = v) } } }
        viewModelScope.launch { settings.bypassDomainDatesFlow.collect { v -> _state.update { it.copy(bypassDomainDates = v) } } }
        viewModelScope.launch { settings.verboseLogsFlow.collect { v -> _state.update { it.copy(verboseLogs = v) } } }
        viewModelScope.launch {
            val hwid = settings.getHwid()
            _state.update { it.copy(hwid = hwid) }
        }
        viewModelScope.launch {
            state.first { !it.loading }
            loadNews()
        }
    }

    /** Подключено → таймер, скорость (раз в секунду) и пинг (раз в 30 с); смена состояния → «как видят сайты». */
    private fun watchConnectionStats() {
        viewModelScope.launch {
            state.map { it.status }.distinctUntilChanged().collectLatest { st ->
                when (st) {
                    LiptonVpnService.VpnStatus.CONNECTED -> {
                        val since = LiptonVpnService.connectedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
                        _stats.update { it.copy(connectedAt = since, speedHistory = emptyList(), pingHistory = emptyList(), pingMs = null) }
                        refreshIpInfo(delayMs = 1500)
                        coroutineScope {
                            launch { speedLoop() }
                            launch { pingLoop() }
                        }
                    }
                    else -> {
                        _stats.update { it.copy(connectedAt = 0L, downBps = 0L, upBps = 0L, speedHistory = emptyList(), pingMs = null) }
                        if (st == LiptonVpnService.VpnStatus.DISCONNECTED || st == LiptonVpnService.VpnStatus.ERROR) {
                            refreshIpInfo(delayMs = 600)
                        }
                    }
                }
            }
        }
    }

    private suspend fun speedLoop() {
        val uid = android.os.Process.myUid()
        var lastRx = android.net.TrafficStats.getUidRxBytes(uid)
        var lastTx = android.net.TrafficStats.getUidTxBytes(uid)
        if (lastRx < 0 || lastTx < 0) return
        var lastT = android.os.SystemClock.elapsedRealtime()
        while (true) {
            delay(1000)
            val rx = android.net.TrafficStats.getUidRxBytes(uid)
            val tx = android.net.TrafficStats.getUidTxBytes(uid)
            val t = android.os.SystemClock.elapsedRealtime()
            val dt = ((t - lastT).coerceAtLeast(1)) / 1000.0
            val down = ((rx - lastRx).coerceAtLeast(0) / dt).toLong()
            val up = ((tx - lastTx).coerceAtLeast(0) / dt).toLong()
            lastRx = rx; lastTx = tx; lastT = t
            _stats.update { s -> s.copy(downBps = down, upBps = up, speedHistory = (s.speedHistory + down).takeLast(12)) }
        }
    }

    private suspend fun pingLoop() {
        while (true) {
            val srv = activeServer()
            val ms = if (srv != null) tcpPing(srv.address, srv.port) else null
            if (ms != null) {
                _stats.update { s -> s.copy(pingMs = ms, pingHistory = (s.pingHistory + ms).takeLast(120)) }
            }
            delay(30_000)
        }
    }

    /** Время TCP-рукопожатия с сервером (приложение исключено из туннеля — это реальный путь до сервера). */
    private suspend fun tcpPing(host: String, port: Int): Long? = withContext(Dispatchers.IO) {
        try {
            val start = System.nanoTime()
            java.net.Socket().use { it.connect(java.net.InetSocketAddress(host, port), 3000) }
            ((System.nanoTime() - start) / 1_000_000L).coerceAtLeast(1)
        } catch (_: Exception) {
            null
        }
    }

    /** «Сайты видят вас»: при VPN — запрос через локальный SOCKS ядра, без VPN — напрямую. */
    fun refreshIpInfo(delayMs: Long = 0) {
        ipJob?.cancel()
        ipJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            val viaVpn = state.value.status == LiptonVpnService.VpnStatus.CONNECTED
            _stats.update { it.copy(ipLoading = true) }
            val info = try {
                api.ipCheck(if (viaVpn) settings.getSocksPort() else null)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) { null }
            val v6 = hasPublicIpv6()
            _stats.update { s ->
                s.copy(
                    ip = info ?: (if (s.ipViaVpn == viaVpn) s.ip else null),
                    ipViaVpn = viaVpn,
                    ipLoading = false,
                    ipv6Available = v6,
                )
            }
        }
    }

    /** Результат «Проверки соединения» (профиль → VPN). */
    data class ConnCheckResult(
        val connected: Boolean,
        val ip: com.lipton.vpn.data.model.IpInfo?,
        val trace: Map<String, String>,
        val ipv6Public: Boolean,
        val pingMs: Long?,
        val serverName: String?,
        val error: String? = null,
    )

    /** Проверка: адрес глазами сайтов, ответ Cloudflare, IPv6, пинг до сервера. Через VPN — если подключено. */
    suspend fun runConnectionCheck(): ConnCheckResult = coroutineScope {
        val connected = state.value.status == LiptonVpnService.VpnStatus.CONNECTED
        val port = if (connected) settings.getSocksPort() else null
        val srv = activeServer()
        val ipD = async { try { api.ipCheck(port) } catch (_: Exception) { null } }
        val traceD = async { try { api.cloudflareTrace(port) } catch (_: Exception) { emptyMap() } }
        val pingD = async { srv?.let { tcpPing(it.address, it.port) } }
        val ip = ipD.await()
        val trace = traceD.await()
        if (ip != null) _stats.update { it.copy(ip = ip, ipViaVpn = connected) }
        ConnCheckResult(
            connected = connected,
            ip = ip,
            trace = trace,
            ipv6Public = hasPublicIpv6(),
            pingMs = pingD.await(),
            serverName = srv?.displayName(),
            error = if (ip == null && trace.isEmpty()) "Нет ответа — проверьте интернет" else null,
        )
    }

    @Suppress("DEPRECATION")
    private fun hasPublicIpv6(): Boolean = try {
        val cm = getApplication<Application>().getSystemService(android.net.ConnectivityManager::class.java)
        cm.allNetworks.any { net ->
            val caps = cm.getNetworkCapabilities(net)
            if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true) return@any false
            cm.getLinkProperties(net)?.linkAddresses?.any { la ->
                val a = la.address
                a is java.net.Inet6Address && !a.isLinkLocalAddress && !a.isLoopbackAddress &&
                    !a.isSiteLocalAddress && (a.address[0].toInt() and 0xFE) != 0xFC
            } == true
        }
    } catch (_: Exception) { false }


    // ─── Смена тарифа ─────────────────────────────────────────────────────────

    // Загрузить варианты смены (экран «Сменить тариф» открылся).
    fun loadChangeOptions() {
        changePollJob?.cancel()
        changeKey = null
        val gen = ++changeGen
        _change.value = TariffChangeState(loading = true)
        viewModelScope.launch {
            try {
                val res = api.changeOptions()
                if (gen != changeGen) return@launch
                _change.update {
                    it.copy(
                        loading         = false,
                        available       = res.available,
                        reason          = res.reason,
                        discountPercent = res.discountPercent,
                        current         = res.current,
                        options         = res.options ?: emptyList(),
                    )
                }
            } catch (e: ApiClient.ApiException) {
                if (e.code == "unauthorized") _state.update { it.copy(isAuthed = false) }
                _change.update { it.copy(loading = false, error = e.message ?: "Не удалось загрузить варианты") }
            } catch (e: Exception) {
                _change.update { it.copy(loading = false, error = e.message ?: "Не удалось загрузить варианты") }
            }
        }
    }

    // Выбор варианта → предпросмотр на сервере (суммы на текущий момент).
    fun previewChange(option: ChangeOption) {
        val cur = _change.value
        if (cur.previewing || cur.submitting) return
        _change.update { it.copy(previewing = true, error = null) }
        val gen = changeGen
        viewModelScope.launch {
            try {
                val p = api.changePreview(option.tariffId, option.periodDays)
                if (gen != changeGen) return@launch
                changeKey = UUID.randomUUID().toString()   // новая попытка — новый ключ
                _change.update { it.copy(previewing = false, preview = p, phase = "confirm") }
            } catch (e: Exception) {
                _change.update { it.copy(previewing = false, error = e.message ?: "Не удалось рассчитать смену") }
            }
        }
    }

    // Назад к списку вариантов.
    fun backToChangeOptions() {
        if (_change.value.submitting) return   // запрос смены уже ушёл — ждём ответа
        changePollJob?.cancel()
        changeKey = null
        changeGen++
        _change.update {
            it.copy(
                phase = "choose", preview = null, txId = null, resultText = null,
                error = null, submitting = false, previewing = false,
            )
        }
    }

    // Подтверждение смены. openUrl — открыть страницу оплаты (СБП / 3DS);
    // вызывается из viewModelScope, т.е. на главном потоке.
    fun confirmChange(openUrl: (String) -> Unit) {
        val st = _change.value
        val p = st.preview ?: return
        if (st.submitting) return
        val key = changeKey ?: UUID.randomUUID().toString().also { changeKey = it }
        _change.update { it.copy(submitting = true, error = null) }
        val gen = changeGen
        viewModelScope.launch {
            try {
                val res = api.changeTariff(p.tariffId, p.periodDays, key, p.surchargeKopeks)
                if (gen != changeGen) {
                    // Экран закрыт, пока шёл запрос — просто подтянем подписку.
                    syncAccountSubscription()
                    return@launch
                }
                val tx = res.transactionId
                when (res.status) {
                    "changed" -> finishChangeOk("Тариф изменён без доплаты.")
                    "charged" -> {
                        if (tx != null && tx.isNotBlank()) startChangePolling(tx)
                        else finishChangeOk("Оплата прошла, тариф изменён.")
                    }
                    "pending" -> {
                        if (tx != null && tx.isNotBlank()) startChangePolling(tx)
                        else finishChangeOk("Платёж в обработке. Подписка обновится автоматически.")
                    }
                    "payment_required" -> {
                        val url = res.paymentUrl
                        if (url != null && url.isNotBlank() && tx != null && tx.isNotBlank()) {
                            try { openUrl(url) } catch (_: Exception) {}
                            startChangePolling(tx)
                        } else {
                            _change.update { it.copy(submitting = false, error = "Не удалось создать платёж") }
                        }
                    }
                    "failed" -> {
                        _change.update {
                            it.copy(
                                submitting = false, phase = "fail",
                                resultText = "Банк отклонил платёж. Попробуйте ещё раз или выберите другой способ.",
                            )
                        }
                    }
                    else -> {
                        _change.update { it.copy(submitting = false, error = "Неизвестный ответ сервера: ${res.status}") }
                    }
                }
            } catch (e: ApiClient.ApiException) {
                _change.update { it.copy(submitting = false, error = e.message) }
                if (e.code == "unauthorized") {
                    _state.update { it.copy(isAuthed = false) }
                } else if (e.status == 409) {
                    // 409 бывает и «уже выполняется» (тот же ключ ещё обрабатывается) —
                    // тогда ключ НЕ меняем, чтобы повтор вернул результат первой попытки.
                    // Новый ключ — только если условия реально изменились.
                    try {
                        val fresh = api.changePreview(p.tariffId, p.periodDays)
                        if (gen == changeGen) {
                            val changed = fresh.surchargeKopeks != p.surchargeKopeks ||
                                fresh.mode != p.mode ||
                                fresh.willChargeCard != p.willChargeCard
                            if (changed) {
                                changeKey = UUID.randomUUID().toString()
                                _change.update { it.copy(preview = fresh) }
                            }
                        }
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                _change.update { it.copy(submitting = false, error = e.message ?: "Ошибка смены тарифа") }
            }
        }
    }

    private suspend fun finishChangeOk(text: String) {
        _change.update { it.copy(submitting = false, phase = "ok", resultText = text) }
        syncAccountSubscription()
    }

    // Опрос /payments/status до успеха / отказа (до 5 минут).
    private fun startChangePolling(txId: String) {
        changePollJob?.cancel()
        _change.update { it.copy(submitting = false, phase = "wait", txId = txId, error = null) }
        changePollJob = viewModelScope.launch {
            val deadline = System.currentTimeMillis() + 5 * 60_000L
            var done = false
            while (!done && System.currentTimeMillis() < deadline) {
                try {
                    val ps = api.paymentStatus(txId)
                    when (ps.status) {
                        "succeeded" -> {
                            done = true
                            finishChangeOk("Оплата прошла, тариф изменён.")
                        }
                        "failed", "canceled" -> {
                            done = true
                            val reason = ps.failureReason
                            _change.update {
                                it.copy(
                                    phase = "fail",
                                    resultText = if (reason != null && reason.isNotBlank()) reason
                                        else "Платёж отклонён или отменён. Попробуйте снова.",
                                )
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {}
                if (!done) delay(2500)
            }
            if (!done) {
                _change.update {
                    it.copy(
                        phase = "fail",
                        resultText = "Не дождались подтверждения оплаты. Если деньги списались, подписка обновится сама — нажмите ⟳ в карточке подписки.",
                    )
                }
            }
        }
    }

    // Экран закрыт — останавливаем опрос и сбрасываем состояние.
    fun resetTariffChange() {
        changePollJob?.cancel()
        changePollJob = null
        changeKey = null
        changeGen++
        _change.value = TariffChangeState()
    }

    // ─── Гостевой доступ «15 минут без регистрации» ──────────────────────────

    private fun SettingsManager.TrialStored.toSession() =
        TrialSession(expiresAt = expiresAt, minutes = minutes, serverName = serverName, subscriptionUrl = subscriptionUrl)

    private fun TrialSession.toStored(retryAt: Long? = null) = SettingsManager.TrialStored(
        subscriptionUrl = subscriptionUrl, expiresAt = expiresAt, minutes = minutes,
        serverName = serverName, retryAt = retryAt ?: 0L,
    )

    private fun isVpnActive(): Boolean = state.value.status == LiptonVpnService.VpnStatus.CONNECTED ||
        state.value.status == LiptonVpnService.VpnStatus.CONNECTING

    private suspend fun removeSubscriptionsByUrl(url: String?) {
        val subs = settings.getSubscriptions()
        val left = subs.filterNot { (url != null && it.url == url) || it.isTrial }
        if (left.size != subs.size) settings.saveSubscriptions(left)
    }

    /**
     * POST /guest/trial: ссылка на гостевой сервер на 15 минут, раз в день, без аккаунта.
     * device_id — постоянный идентификатор приложения (тот же HWID, что уходит в подписку).
     * Успех — подписка заводится как пробная, выбирается первый сервер, включается гостевой режим.
     */
    suspend fun startGuestTrial(): TrialStart {
        val cfgMinutes = state.value.appConfig?.guestMinutes ?: 15
        return try {
            val res = api.guestTrial(settings.getHwid(), BuildConfig.VERSION_NAME)
            val url = res.subscriptionUrl?.takeIf { it.isNotBlank() }
                ?: return TrialStart.Failed("Сервер не выдал ссылку — попробуйте позже")
            val expires = parseIsoMillis(res.expiresAt) ?: (System.currentTimeMillis() + cfgMinutes * 60_000L)
            val sub = subManager.addTrialFromApi(url, expires, "Пробный доступ")
            val first = sub.servers.firstOrNull()?.id
            if (first != null) settings.setActiveServerId(first)
            val session = TrialSession(expires, cfgMinutes, res.serverName, url)
            settings.setGuestSession(session.toStored())
            _state.update {
                it.copy(
                    guest = session, guestRetryAt = null, authEntry = null,
                    subscriptions = settings.getSubscriptions(),
                    activeServerId = first ?: it.activeServerId,
                )
            }
            logAction("Пробный доступ на $cfgMinutes мин включён")
            TrialStart.Ok
        } catch (e: ApiClient.ApiException) {
            when {
                e.status == 429 || e.serverCode == "guest_trial_used" -> {
                    val at = parseIsoMillis(e.retryAt)
                    _state.update { it.copy(guestRetryAt = at) }
                    settings.setGuestSession(SettingsManager.TrialStored(retryAt = at ?: 0L))
                    TrialStart.Used(at)
                }
                e.status == 403 || e.serverCode == "guest_trial_disabled" || e.endpointMissing -> {
                    // В админке выключили (или ручки ещё нет) — прячем гостевые кнопки.
                    _state.update { st -> st.copy(appConfig = st.appConfig?.copy(guestTrial = com.lipton.vpn.data.model.GuestTrialConfig(false, cfgMinutes), trialGuestEnabled = false)) }
                    TrialStart.Disabled
                }
                else -> TrialStart.Failed(e.message ?: "Не удалось включить пробный доступ")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            TrialStart.Failed(e.message ?: "Не удалось включить пробный доступ")
        }
    }

    /** 15 минут прошли: VPN отключается, гостевая подписка удаляется, главная показывает «15 минут прошли». */
    private fun endGuest() {
        val g = state.value.guest ?: return
        if (g.ended) return
        _state.update { it.copy(guest = g.copy(ended = true)) }
        viewModelScope.launch {
            if (isVpnActive()) disconnect(getApplication())
            finishGuestStorage(g.toStored(state.value.guestRetryAt))
            logAction("Пробный доступ закончился")
        }
    }

    private suspend fun finishGuestStorage(stored: SettingsManager.TrialStored) {
        removeSubscriptionsByUrl(stored.subscriptionUrl)
        // Помним только «когда снова» (если известно из 429), ссылку и срок — забываем.
        settings.setGuestSession(stored.retryAt.takeIf { it > System.currentTimeMillis() }?.let { SettingsManager.TrialStored(retryAt = it) })
    }

    /** Выйти из экрана «15 минут прошли» на приветствие (без входа). */
    fun leaveGuest() {
        _state.update { it.copy(guest = null, authEntry = null) }
    }

    // ─── «15 минут бесплатно» для вошедших без подписки ─────────────────────

    /** POST /me/daily-trial → подписка на 15 минут; подключаемся сразу. */
    fun startDailyTrial(context: Context) {
        if (state.value.profileBusy != null) return
        _state.update { it.copy(profileBusy = "daily") }
        viewModelScope.launch {
            val minutes = state.value.appConfig?.guestMinutes ?: 15
            try {
                val res = api.dailyTrial()
                val url = res.subscriptionUrl?.takeIf { it.isNotBlank() } ?: throw ApiClient.ApiException("Сервер не выдал ссылку — попробуйте позже")
                val expires = parseIsoMillis(res.expiresAt) ?: (System.currentTimeMillis() + minutes * 60_000L)
                val sub = subManager.addTrialFromApi(url, expires, "15 минут бесплатно")
                val first = sub.servers.firstOrNull()?.id
                if (first != null) settings.setActiveServerId(first)
                val session = TrialSession(expires, minutes, null, url)
                settings.setDailyTrial(session.toStored())
                _state.update {
                    it.copy(
                        dailyTrial = session, dailyTrialRetryAt = null, profileBusy = null,
                        subscriptions = settings.getSubscriptions(), activeServerId = first ?: it.activeServerId,
                    )
                }
                logAction("15 минут бесплатно: доступ включён")
                if (first != null) handleConnectToggle(context)
            } catch (e: ApiClient.ApiException) {
                _state.update { it.copy(profileBusy = null) }
                when {
                    e.status == 409 || e.serverCode == "has_subscription" -> {
                        _state.update { it.copy(errorMessage = "Подписка уже есть — обновляем данные") }
                        syncAccountSubscription()
                    }
                    e.status == 429 || e.serverCode == "daily_trial_used" -> {
                        val at = parseIsoMillis(e.retryAt)
                        _state.update { it.copy(dailyTrialRetryAt = at, errorMessage = "Сегодня 15 минут уже были" + (at?.let { t -> " — снова ${com.lipton.vpn.data.ruWhen(t)}" } ?: "")) }
                        settings.setDailyTrial(SettingsManager.TrialStored(retryAt = at ?: 0L))
                    }
                    e.code == "unauthorized" -> _state.update { it.copy(isAuthed = false) }
                    else -> _state.update { it.copy(errorMessage = e.message ?: "Не удалось включить 15 минут") }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(profileBusy = null, errorMessage = e.message ?: "Не удалось включить 15 минут") }
            }
        }
    }

    private fun endDailyTrial() {
        val d = state.value.dailyTrial ?: return
        _state.update { it.copy(dailyTrial = null) }
        viewModelScope.launch {
            if (isVpnActive()) disconnect(getApplication())
            removeSubscriptionsByUrl(d.subscriptionUrl)
            settings.setDailyTrial(null)
            _state.update { it.copy(errorMessage = "15 минут закончились — оформите подписку, чтобы продолжить") }
            logAction("15 минут бесплатно закончились")
            if (state.value.isAuthed) syncAccountSubscription()
        }
    }

    /** Срок гостевой или дневной сессии: ждём ближайший и завершаем её. */
    private fun watchTrialDeadlines() {
        viewModelScope.launch {
            state.map { st -> listOfNotNull(st.guest?.takeIf { !it.ended }?.expiresAt, st.dailyTrial?.expiresAt).minOrNull() }
                .distinctUntilChanged()
                .collectLatest { deadline ->
                    if (deadline == null) return@collectLatest
                    val wait = deadline - System.currentTimeMillis()
                    if (wait > 0) delay(wait)
                    checkTrialDeadlines()
                }
        }
    }

    /** Проверка сроков (и при возврате в приложение: delay не идёт, пока телефон спит). */
    fun checkTrialDeadlines() {
        val now = System.currentTimeMillis()
        val st = state.value
        st.guest?.let { if (!it.ended && now >= it.expiresAt) endGuest() }
        st.dailyTrial?.let { if (now >= it.expiresAt) endDailyTrial() }
    }

    // ─── Баннеры и экраны из админки ─────────────────────────────────────────

    /** GET /app/banners — с авторизацией, если вошли (таргетинг по аудитории). Ошибки тихие. */
    fun loadBanners() {
        viewModelScope.launch {
            val list = try { api.getBanners(BuildConfig.VERSION_NAME) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { return@launch }
            val dismissed = settings.getDismissedBanners()
            _state.update { it.copy(banners = BannerLogic.active(list, dismissed, System.currentTimeMillis())) }
        }
    }

    /** Закрыть баннер или экран (запоминается локально по id). Обязательное обновление не закрывается. */
    fun dismissBanner(id: String) {
        val b = state.value.banners.find { it.id == id } ?: return
        _state.update { it.copy(banners = it.banners.filterNot { x -> x.id == id }) }
        if (b.dismissible) viewModelScope.launch { settings.dismissBanner(id) }
    }

    // ─── Уведомления: три переключателя (GET/PUT /me/notifications) ──────────

    fun loadNotificationPrefs() {
        if (!state.value.isAuthed) return
        viewModelScope.launch {
            try {
                val p = api.getNotificationPrefs()
                _state.update { it.copy(notifPrefs = p, notificationsEnabled = p.paymentReminders) }
                settings.setNotificationsEnabled(p.paymentReminders)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // ручки нет или сеть — остаётся локальный тумблер
            }
        }
    }

    /**
     * Переключатель уведомлений: сразу в интерфейсе, затем PUT. Не вышло — откатываем.
     * «Напоминания об оплате» заодно управляют локальными напоминаниями телефона.
     */
    fun updateNotificationPrefs(change: (NotificationPrefs) -> NotificationPrefs) {
        val before = state.value.notifPrefs ?: return
        val after = change(before)
        _state.update { it.copy(notifPrefs = after, notificationsEnabled = after.paymentReminders) }
        viewModelScope.launch {
            settings.setNotificationsEnabled(after.paymentReminders)
            try {
                val saved = api.putNotificationPrefs(after)
                _state.update { it.copy(notifPrefs = saved) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(notifPrefs = before, notificationsEnabled = before.paymentReminders, errorMessage = "Не удалось сохранить — попробуйте ещё раз") }
                settings.setNotificationsEnabled(before.paymentReminders)
            }
        }
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
                settings.setBypassDomainDate(domain, System.currentTimeMillis())
            }
        }
    }

    fun removeBypassDomain(domain: String) {
        viewModelScope.launch {
            val current = settings.getBypassDomains().toMutableList()
            current.remove(domain)
            settings.saveBypassDomains(current)
            settings.setBypassDomainDate(domain, null)
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
                    splitTunnelApps     = emptyList(),
                    splitTunnelMode     = SplitMode.ALL,
                    verboseLogs         = false,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectionTimeoutJob?.cancel()
        changePollJob?.cancel()
        vpnService?.statusListener = null
        vpnService?.logListener    = null
        pendingLogLines.clear()
    }
}
