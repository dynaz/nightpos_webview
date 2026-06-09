package com.nightpos.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nightpos.app.util.Constants
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
        val ODOO_DB = stringPreferencesKey("odoo_db")
        val ODOO_API_LOGIN = stringPreferencesKey("odoo_api_login")
        val ODOO_API_PASSWORD = stringPreferencesKey("odoo_api_password")
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

    val odooDb: Flow<String> = context.dataStore.data.map { prefs -> prefs[Keys.ODOO_DB] ?: "" }
    val odooApiLogin: Flow<String> = context.dataStore.data.map { prefs -> prefs[Keys.ODOO_API_LOGIN] ?: "admin" }
    val odooApiPassword: Flow<String> = context.dataStore.data.map { prefs -> prefs[Keys.ODOO_API_PASSWORD] ?: "" }

    suspend fun setOdooDb(db: String) { context.dataStore.edit { it[Keys.ODOO_DB] = db } }
    suspend fun setOdooApiLogin(login: String) { context.dataStore.edit { it[Keys.ODOO_API_LOGIN] = login } }
    suspend fun setOdooApiPassword(pw: String) { context.dataStore.edit { it[Keys.ODOO_API_PASSWORD] = pw } }
}
