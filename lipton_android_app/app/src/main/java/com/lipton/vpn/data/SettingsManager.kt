package com.lipton.vpn.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lipton.vpn.data.model.Subscription
import com.lipton.vpn.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lipton_settings")

class SettingsManager(private val context: Context) {

    private val gson = Gson()

    companion object {
        private val KEY_HWID              = stringPreferencesKey("hwid")
        private val KEY_SUBS              = stringPreferencesKey("subscriptions")
        private val KEY_ACTIVE_SERVER     = stringPreferencesKey("active_server_id")
        private val KEY_BYPASS_RU         = booleanPreferencesKey("bypass_ru")
        private val KEY_BYPASS_DOMAINS    = stringPreferencesKey("bypass_domains")
        private val KEY_TRIAL_ADDED       = booleanPreferencesKey("trial_added")
        private val KEY_TRIAL_DATE        = stringPreferencesKey("trial_date")
        private val KEY_SOCKS_PORT        = intPreferencesKey("socks_port")
        private val KEY_HTTP_PORT         = intPreferencesKey("http_port")
        private val KEY_AUTOSTART         = booleanPreferencesKey("autostart")
        private val KEY_THEME             = stringPreferencesKey("app_theme")
        private val KEY_AUTO_CONNECT      = booleanPreferencesKey("auto_connect_on_launch")
        private val KEY_FIRST_LAUNCH_DONE = booleanPreferencesKey("first_launch_done")
        private val KEY_LAST_SEEN_VERSION = stringPreferencesKey("last_seen_version")
        private val KEY_NOTIF_SENT_FLAGS  = stringPreferencesKey("notif_sent_flags")
        private val KEY_HAPTIC_ENABLED    = booleanPreferencesKey("haptic_enabled")
        private val KEY_CLIPBOARD_LAST    = stringPreferencesKey("clipboard_last_imported")

        // ─── Аккаунт (вход в Lipton, liptonone.online) ──────────────────────────
        private val KEY_AUTH_ACCESS       = stringPreferencesKey("auth_access")
        private val KEY_AUTH_REFRESH      = stringPreferencesKey("auth_refresh")
        private val KEY_AUTH_EXPIRES      = longPreferencesKey("auth_expires_at")

        // ─── Редизайн: новости, трафик по дням, уведомления, раздельное туннелирование ──
        private val KEY_NEWS_READ         = stringPreferencesKey("news_read_ids")
        private val KEY_TRAFFIC_DAYS      = stringPreferencesKey("traffic_days")
        private val KEY_NOTIFICATIONS     = booleanPreferencesKey("notifications_enabled")
        private val KEY_SPLIT_APPS        = stringPreferencesKey("split_tunnel_apps")

        // ─── Редизайн, волна 2 ──────────────────────────────────────────────────
        private val KEY_SPLIT_MODE        = stringPreferencesKey("split_tunnel_mode")      // all | bypass
        private val KEY_GUEST             = stringPreferencesKey("guest_session")          // JSON GuestStored
        private val KEY_DAILY_TRIAL       = stringPreferencesKey("daily_trial_session")    // JSON TrialStored
        private val KEY_BANNERS_DISMISSED = stringPreferencesKey("banners_dismissed")
        private val KEY_DOMAIN_DATES      = stringPreferencesKey("bypass_domain_dates")    // домен → когда добавлен
        private val KEY_VERBOSE_LOGS      = booleanPreferencesKey("verbose_logs")
        private val MAP_LONG_TYPE  = object : TypeToken<Map<String, Long>>() {}.type

        private val SUB_TYPE       = object : TypeToken<List<Subscription>>() {}.type
        private val STR_LIST_TYPE  = object : TypeToken<List<String>>() {}.type
        private val MAP_BOOL_TYPE  = object : TypeToken<MutableMap<String, Boolean>>() {}.type
    }

    // ─── HWID ────────────────────────────────────────────────────────────────

    suspend fun getHwid(): String {
        val prefs = context.dataStore.data.first()
        var hwid = prefs[KEY_HWID]
        if (hwid.isNullOrBlank()) {
            hwid = UUID.randomUUID().toString()
            context.dataStore.edit { it[KEY_HWID] = hwid }
        }
        return hwid
    }

    // ─── Subscriptions ────────────────────────────────────────────────────────

    val subscriptionsFlow: Flow<List<Subscription>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_SUBS] ?: return@map emptyList()
        try { gson.fromJson(json, SUB_TYPE) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    suspend fun getSubscriptions(): List<Subscription> = subscriptionsFlow.first()

    suspend fun saveSubscriptions(subs: List<Subscription>) {
        context.dataStore.edit { it[KEY_SUBS] = gson.toJson(subs) }
    }

    // ─── Active server ────────────────────────────────────────────────────────

    val activeServerIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_ACTIVE_SERVER] }

    suspend fun getActiveServerId(): String? = context.dataStore.data.first()[KEY_ACTIVE_SERVER]

    suspend fun setActiveServerId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id != null) prefs[KEY_ACTIVE_SERVER] = id
            else prefs.remove(KEY_ACTIVE_SERVER)
        }
    }

    // ─── Bypass RU ───────────────────────────────────────────────────────────

    val bypassRuFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_BYPASS_RU] != false }

    suspend fun getBypassRu(): Boolean = context.dataStore.data.first()[KEY_BYPASS_RU] != false

    suspend fun setBypassRu(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BYPASS_RU] = enabled }
    }

    // ─── Bypass domains ──────────────────────────────────────────────────────

    val bypassDomainsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_BYPASS_DOMAINS] ?: return@map emptyList()
        try { gson.fromJson(json, STR_LIST_TYPE) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    suspend fun getBypassDomains(): List<String> = bypassDomainsFlow.first()

    suspend fun saveBypassDomains(domains: List<String>) {
        context.dataStore.edit { it[KEY_BYPASS_DOMAINS] = gson.toJson(domains) }
    }

    // ─── Trial ───────────────────────────────────────────────────────────────

    suspend fun getTrialAdded(): Boolean = context.dataStore.data.first()[KEY_TRIAL_ADDED] == true

    suspend fun setTrialAdded(v: Boolean) {
        context.dataStore.edit { it[KEY_TRIAL_ADDED] = v }
    }

    suspend fun getTrialDate(): String? = context.dataStore.data.first()[KEY_TRIAL_DATE]

    suspend fun setTrialDate(date: String) {
        context.dataStore.edit { it[KEY_TRIAL_DATE] = date }
    }

    // ─── Ports ───────────────────────────────────────────────────────────────

    suspend fun getSocksPort(): Int = context.dataStore.data.first()[KEY_SOCKS_PORT] ?: 10808

    suspend fun getHttpPort(): Int = context.dataStore.data.first()[KEY_HTTP_PORT] ?: 10809

    // ─── Autostart (device boot) ──────────────────────────────────────────────

    val autostartFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTOSTART] == true }

    suspend fun getAutostart(): Boolean = context.dataStore.data.first()[KEY_AUTOSTART] == true

    suspend fun setAutostart(v: Boolean) {
        context.dataStore.edit { it[KEY_AUTOSTART] = v }
    }

    // ─── App theme ────────────────────────────────────────────────────────────

    // Тёмная / Светлая / Системная (по умолчанию). Старое значение HACKER читается как DARK.
    suspend fun getThemeMode(): AppTheme =
        AppTheme.fromStored(context.dataStore.data.first()[KEY_THEME])

    suspend fun setThemeMode(theme: AppTheme) {
        context.dataStore.edit { it[KEY_THEME] = theme.name }
    }

    // ─── Auto-connect on launch ───────────────────────────────────────────────

    suspend fun getAutoConnectOnLaunch(): Boolean =
        context.dataStore.data.first()[KEY_AUTO_CONNECT] == true

    suspend fun setAutoConnectOnLaunch(v: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CONNECT] = v }
    }

    // ─── First launch ─────────────────────────────────────────────────────────

    suspend fun getFirstLaunchDone(): Boolean =
        context.dataStore.data.first()[KEY_FIRST_LAUNCH_DONE] == true

    suspend fun setFirstLaunchDone(v: Boolean) {
        context.dataStore.edit { it[KEY_FIRST_LAUNCH_DONE] = v }
    }

    // ─── Last seen version (What's New) ──────────────────────────────────────

    suspend fun getLastSeenVersion(): String? = context.dataStore.data.first()[KEY_LAST_SEEN_VERSION]

    suspend fun setLastSeenVersion(v: String) {
        context.dataStore.edit { it[KEY_LAST_SEEN_VERSION] = v }
    }

    // ─── Notification sent flags ──────────────────────────────────────────────

    suspend fun getNotifSentFlag(key: String): Boolean {
        val json = context.dataStore.data.first()[KEY_NOTIF_SENT_FLAGS] ?: return false
        return try {
            val map: Map<String, Boolean> = gson.fromJson(json, MAP_BOOL_TYPE) ?: return false
            map[key] == true
        } catch (_: Exception) { false }
    }

    suspend fun setNotifSentFlag(key: String, value: Boolean) {
        context.dataStore.edit { prefs ->
            val current: MutableMap<String, Boolean> = try {
                val json = prefs[KEY_NOTIF_SENT_FLAGS]
                if (json != null) gson.fromJson(json, MAP_BOOL_TYPE) else mutableMapOf()
            } catch (_: Exception) { mutableMapOf() }
            current[key] = value
            prefs[KEY_NOTIF_SENT_FLAGS] = gson.toJson(current)
        }
    }

    // ─── Haptic ───────────────────────────────────────────────────────────────

    suspend fun getHapticEnabled(): Boolean = context.dataStore.data.first()[KEY_HAPTIC_ENABLED] != false

    suspend fun setHapticEnabled(v: Boolean) {
        context.dataStore.edit { it[KEY_HAPTIC_ENABLED] = v }
    }

    // ─── Clipboard import ─────────────────────────────────────────────────────

    suspend fun getClipboardLastImported(): String? = context.dataStore.data.first()[KEY_CLIPBOARD_LAST]

    suspend fun setClipboardLastImported(v: String) {
        context.dataStore.edit { it[KEY_CLIPBOARD_LAST] = v }
    }

    // ─── Аккаунт: токены доступа ───────────────────────────────────────────────

    data class AuthTokens(val access: String, val refresh: String, val expiresAt: Long)

    suspend fun getAuthTokens(): AuthTokens? {
        val prefs = context.dataStore.data.first()
        val access = prefs[KEY_AUTH_ACCESS] ?: return null
        val refresh = prefs[KEY_AUTH_REFRESH] ?: return null
        if (refresh.isBlank()) return null
        return AuthTokens(access, refresh, prefs[KEY_AUTH_EXPIRES] ?: 0L)
    }

    val authedFlow: Flow<Boolean> = context.dataStore.data.map {
        !(it[KEY_AUTH_REFRESH].isNullOrBlank())
    }

    suspend fun setAuthTokens(access: String, refresh: String, expiresAt: Long) {
        context.dataStore.edit {
            it[KEY_AUTH_ACCESS] = access
            it[KEY_AUTH_REFRESH] = refresh
            it[KEY_AUTH_EXPIRES] = expiresAt
        }
    }

    suspend fun clearAuthTokens() {
        context.dataStore.edit {
            it.remove(KEY_AUTH_ACCESS)
            it.remove(KEY_AUTH_REFRESH)
            it.remove(KEY_AUTH_EXPIRES)
        }
    }

    // ─── Новости: прочитанные (локально, по id) ──────────────────────────────

    val newsReadFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_NEWS_READ] ?: return@map emptySet()
        try { (gson.fromJson<List<String>>(json, STR_LIST_TYPE) ?: emptyList()).toSet() } catch (_: Exception) { emptySet() }
    }

    suspend fun markNewsRead(ids: Collection<String>) {
        if (ids.isEmpty()) return
        context.dataStore.edit { prefs ->
            val cur: List<String> = try {
                prefs[KEY_NEWS_READ]?.let { gson.fromJson<List<String>>(it, STR_LIST_TYPE) } ?: emptyList()
            } catch (_: Exception) { emptyList() }
            // Храним последние 300 id — лента всё равно отдаёт ~30 записей.
            prefs[KEY_NEWS_READ] = gson.toJson((cur + ids).distinct().takeLast(300))
        }
    }

    // ─── Трафик по дням «на этом телефоне» (пишет VPN-сервис) ────────────────

    /** Дни «yyyy-MM-dd» → [приём, отдача] в байтах; хранится 14 последних дней. */
    val trafficDaysFlow: Flow<Map<String, LongArray>> = context.dataStore.data.map { prefs ->
        TrafficLedger.decode(prefs[KEY_TRAFFIC_DAYS])
    }

    suspend fun addTraffic(day: String, rx: Long, tx: Long) {
        if (rx <= 0 && tx <= 0) return
        context.dataStore.edit { prefs ->
            val map = TrafficLedger.decode(prefs[KEY_TRAFFIC_DAYS]).toMutableMap()
            prefs[KEY_TRAFFIC_DAYS] = TrafficLedger.encode(TrafficLedger.add(map, day, rx, tx))
        }
    }

    // ─── Уведомления (локальные: срок подписки, трафик) ─────────────────────
    // TODO(redesign): B4 — синхронизировать с GET/PUT /me/notifications, когда появится.

    val notificationsFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFICATIONS] != false }

    suspend fun getNotificationsEnabled(): Boolean = context.dataStore.data.first()[KEY_NOTIFICATIONS] != false

    suspend fun setNotificationsEnabled(v: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = v }
    }

    // ─── Раздельное туннелирование: приложения мимо VPN ──────────────────────

    val splitTunnelAppsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_SPLIT_APPS] ?: return@map emptyList()
        try { gson.fromJson<List<String>>(json, STR_LIST_TYPE) ?: emptyList() } catch (_: Exception) { emptyList() }
    }

    suspend fun getSplitTunnelApps(): List<String> = splitTunnelAppsFlow.first()

    suspend fun setSplitTunnelApps(packages: List<String>) {
        context.dataStore.edit { it[KEY_SPLIT_APPS] = gson.toJson(packages.distinct().sorted()) }
    }

    /** Режим раздельного туннелирования (старые версии режим не хранили — выводим из списка). */
    val splitTunnelModeFlow: Flow<SplitMode> = context.dataStore.data.map { prefs ->
        val hasApps = !prefs[KEY_SPLIT_APPS].isNullOrBlank() && prefs[KEY_SPLIT_APPS] != "[]"
        SplitMode.fromStored(prefs[KEY_SPLIT_MODE], hasApps)
    }

    suspend fun getSplitTunnelMode(): SplitMode = splitTunnelModeFlow.first()

    suspend fun setSplitTunnelMode(mode: SplitMode) {
        context.dataStore.edit { it[KEY_SPLIT_MODE] = mode.stored }
    }

    // ─── Гостевой доступ и «15 минут в день» ─────────────────────────────────

    /** Пробная сессия: ссылка, до какого момента действует, сервер и когда можно снова. */
    data class TrialStored(
        val subscriptionUrl: String? = null,
        val expiresAt: Long = 0L,
        val minutes: Int = 15,
        val serverName: String? = null,
        val retryAt: Long = 0L,
    )

    suspend fun getGuestSession(): TrialStored? = readTrial(KEY_GUEST)
    suspend fun setGuestSession(v: TrialStored?) = writeTrial(KEY_GUEST, v)
    suspend fun getDailyTrial(): TrialStored? = readTrial(KEY_DAILY_TRIAL)
    suspend fun setDailyTrial(v: TrialStored?) = writeTrial(KEY_DAILY_TRIAL, v)

    private suspend fun readTrial(key: Preferences.Key<String>): TrialStored? {
        val json = context.dataStore.data.first()[key] ?: return null
        return try { gson.fromJson(json, TrialStored::class.java) } catch (_: Exception) { null }
    }

    private suspend fun writeTrial(key: Preferences.Key<String>, v: TrialStored?) {
        context.dataStore.edit { if (v == null) it.remove(key) else it[key] = gson.toJson(v) }
    }

    // ─── Баннеры: закрытые пользователем (локально) ──────────────────────────

    suspend fun getDismissedBanners(): Set<String> {
        val json = context.dataStore.data.first()[KEY_BANNERS_DISMISSED] ?: return emptySet()
        return try { (gson.fromJson<List<String>>(json, STR_LIST_TYPE) ?: emptyList()).toSet() } catch (_: Exception) { emptySet() }
    }

    suspend fun dismissBanner(id: String) {
        context.dataStore.edit { prefs ->
            val cur: List<String> = try {
                prefs[KEY_BANNERS_DISMISSED]?.let { gson.fromJson<List<String>>(it, STR_LIST_TYPE) } ?: emptyList()
            } catch (_: Exception) { emptyList() }
            prefs[KEY_BANNERS_DISMISSED] = gson.toJson((cur + id).distinct().takeLast(200))
        }
    }

    // ─── Свои домены: когда добавлен («Добавлен 6 октября») ──────────────────

    val bypassDomainDatesFlow: Flow<Map<String, Long>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_DOMAIN_DATES] ?: return@map emptyMap()
        try { gson.fromJson<Map<String, Long>>(json, MAP_LONG_TYPE) ?: emptyMap() } catch (_: Exception) { emptyMap() }
    }

    suspend fun setBypassDomainDate(domain: String, at: Long?) {
        context.dataStore.edit { prefs ->
            val cur: MutableMap<String, Long> = try {
                prefs[KEY_DOMAIN_DATES]?.let { gson.fromJson<Map<String, Long>>(it, MAP_LONG_TYPE) }?.toMutableMap() ?: mutableMapOf()
            } catch (_: Exception) { mutableMapOf() }
            if (at == null) cur.remove(domain) else cur[domain] = at
            prefs[KEY_DOMAIN_DATES] = gson.toJson(cur)
        }
    }

    // ─── Подробные логи ядра (по просьбе поддержки) ──────────────────────────

    val verboseLogsFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_VERBOSE_LOGS] == true }

    suspend fun getVerboseLogs(): Boolean = context.dataStore.data.first()[KEY_VERBOSE_LOGS] == true

    suspend fun setVerboseLogs(v: Boolean) {
        context.dataStore.edit { it[KEY_VERBOSE_LOGS] = v }
    }

    // ─── Reset ───────────────────────────────────────────────────────────────

    suspend fun reset() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SUBS)
            prefs.remove(KEY_ACTIVE_SERVER)
            prefs.remove(KEY_BYPASS_DOMAINS)
            prefs.remove(KEY_TRIAL_ADDED)
            prefs.remove(KEY_TRIAL_DATE)
        }
    }

    suspend fun resetNetworkSettings() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_BYPASS_RU)
            prefs.remove(KEY_BYPASS_DOMAINS)
            prefs.remove(KEY_SOCKS_PORT)
            prefs.remove(KEY_HTTP_PORT)
            prefs.remove(KEY_AUTO_CONNECT)
            prefs.remove(KEY_SPLIT_APPS)
            prefs.remove(KEY_SPLIT_MODE)
            prefs.remove(KEY_DOMAIN_DATES)
            prefs.remove(KEY_VERBOSE_LOGS)
        }
    }
}
