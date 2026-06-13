package com.nightpos.geckoview.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nightpos.geckoview.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nightpos_settings")

/**
 * Persists user-configurable app settings (server URL, kiosk mode, etc.) using
 * Jetpack DataStore. All reads are exposed as [Flow]s so the Settings screen and
 * the rest of the app stay in sync automatically.
 */
class PreferencesManager(private val context: Context) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val KIOSK_MODE = booleanPreferencesKey("kiosk_mode")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val AUTO_REOPEN_POS = booleanPreferencesKey("auto_reopen_pos")
        val PRINTER_PAPER_WIDTH_MM = intPreferencesKey("printer_paper_width_mm")
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val LAST_LOGIN = stringPreferencesKey("last_login")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SERVER_URL] ?: Constants.DEFAULT_BASE_URL
    }

    val kioskModeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.KIOSK_MODE] ?: false
    }

    val keepScreenOnEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.KEEP_SCREEN_ON] ?: true
    }

    val autoReopenPosEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_REOPEN_POS] ?: false
    }

    val printerPaperWidthMm: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.PRINTER_PAPER_WIDTH_MM] ?: 58
    }

    /** True once the user has successfully signed in via the native Login screen. */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOGGED_IN] ?: false
    }

    /** Last login/username used — prefills the PIN login tab. */
    val lastLogin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_LOGIN] ?: ""
    }

    /** Odoo display name of the signed-in user (e.g. "Administrator", "John Doe"). */
    val displayName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISPLAY_NAME] ?: ""
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[Keys.SERVER_URL] = url.trim().trimEnd('/') }
    }

    suspend fun setKioskModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KIOSK_MODE] = enabled }
    }

    suspend fun setKeepScreenOnEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setAutoReopenPosEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_REOPEN_POS] = enabled }
    }

    suspend fun setPrinterPaperWidthMm(widthMm: Int) {
        context.dataStore.edit { it[Keys.PRINTER_PAPER_WIDTH_MM] = widthMm }
    }

    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { it[Keys.LOGGED_IN] = loggedIn }
    }

    suspend fun setLastLogin(login: String) {
        context.dataStore.edit { it[Keys.LAST_LOGIN] = login }
    }

    suspend fun setDisplayName(name: String) {
        context.dataStore.edit { it[Keys.DISPLAY_NAME] = name }
    }
}
