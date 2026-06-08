package com.nightpos.app

import android.app.Application
import com.nightpos.app.print.PrintServiceEnabler
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class NightPOSApplication : Application() {

    companion object {
        lateinit var geckoRuntime: GeckoRuntime
            private set
    }

    override fun onCreate() {
        super.onCreate()
        PrintServiceEnabler.ensureEnabled(this)

        geckoRuntime = GeckoRuntime.create(
            this,
            GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .remoteDebuggingEnabled(false)
                .consoleOutput(false)
                .build(),
        )
    }
}
