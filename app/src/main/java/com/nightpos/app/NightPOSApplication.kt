package com.nightpos.app

import android.app.Application
import com.nightpos.app.print.PrintServiceEnabler

/**
 * Application entry point. Currently only needed so the manifest can reference
 * a named Application class (useful for future DI / global init hooks such as
 * crash reporting or WorkManager configuration) without changing the manifest again.
 */
class NightPOSApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // On Sunmi T1 (Android 6.0.1) Settings → Printing doesn't exist, so
        // we self-enable our PrintService if WRITE_SECURE_SETTINGS was granted via ADB.
        PrintServiceEnabler.ensureEnabled(this)
    }
}
