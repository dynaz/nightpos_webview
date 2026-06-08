package com.nightpos.app.print

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * On Sunmi T1 (Android 6.0.1) the standard Settings → Printing screen is absent
 * from the firmware, so the user can never enable [SunmiPrintService] through
 * the normal UI. As a workaround, WRITE_SECURE_SETTINGS is declared in the
 * manifest and granted once via ADB:
 *
 *   adb shell pm grant <packageName> android.permission.WRITE_SECURE_SETTINGS
 *
 * After that single ADB step, this helper enables (and keeps enabled) the print
 * service automatically every time the app runs.
 *
 * Returns true if the service is now enabled, false if the permission hasn't
 * been granted yet (caller can surface a prompt to run the ADB command).
 */
object PrintServiceEnabler {

    fun ensureEnabled(context: Context): Boolean {
        if (!hasWriteSecureSettings(context)) return false

        val component = ComponentName(context.packageName, SunmiPrintService::class.java.name)
        val key = "enabled_print_services"
        val current = Settings.Secure.getString(context.contentResolver, key) ?: ""

        if (component.flattenToString() !in current) {
            val updated = if (current.isBlank()) component.flattenToString()
            else "$current:${component.flattenToString()}"
            Settings.Secure.putString(context.contentResolver, key, updated)
        }
        return true
    }

    fun hasWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED

    /** Flat ADB command the user or admin needs to run exactly once per install. */
    fun adbGrantCommand(context: Context) =
        "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
}
