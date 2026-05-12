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

        private val SUB_TYPE       = object : TypeToken<List<Subscription>>() {}.type
        private val STR_LIST_TYPE  = object : TypeToken<List<String>>() {}.type
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

    suspend fun getThemeMode(): AppTheme {
        val raw = context.dataStore.data.first()[KEY_THEME] ?: return AppTheme.SYSTEM
        return runCatching { AppTheme.valueOf(raw) }.getOrDefault(AppTheme.SYSTEM)
    }

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
}
