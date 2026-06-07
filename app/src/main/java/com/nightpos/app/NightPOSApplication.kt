package com.nightpos.app

import android.app.Application

/**
 * Application entry point. Currently only needed so the manifest can reference
 * a named Application class (useful for future DI / global init hooks such as
 * crash reporting or WorkManager configuration) without changing the manifest again.
 */
class NightPOSApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
