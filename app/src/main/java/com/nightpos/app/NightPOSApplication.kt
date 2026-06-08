package com.nightpos.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import android.util.Log
import com.nightpos.app.print.PrintHttpServer
import com.nightpos.app.print.PrintServiceEnabler
import com.nightpos.app.print.SunmiPrinterConnection
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NightPOSApplication : Application() {

    companion object {
        lateinit var geckoRuntime: GeckoRuntime
            private set
        lateinit var printerConnection: SunmiPrinterConnection
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // GeckoView spawns content processes that each run their own Application
        // instance. Restrict heavy setup to the main process only.
        if (!isMainProcess()) return

        PrintServiceEnabler.ensureEnabled(this)

        printerConnection = SunmiPrinterConnection(this).also { it.bind() }

        runCatching {
            PrintHttpServer(printerConnection).start()
            Log.i("NightPOS", "PrintHttpServer started on port ${PrintHttpServer.PORT}")
        }.onFailure { Log.e("NightPOS", "PrintHttpServer failed to start: ${it.message}") }

        val configFile = writeGeckoConfig()
        geckoRuntime = GeckoRuntime.create(
            this,
            GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .remoteDebuggingEnabled(false)
                .consoleOutput(false)
                .configFilePath(configFile.absolutePath)
                .build(),
        )

        // Install built-in extension that polyfills Promise.withResolvers (Firefox 121)
        // and structuredClone (Firefox 94) — required by Odoo 19 on GeckoView 99.
        // Block until the extension is confirmed installed so the content script fires
        // at document_start on the very first page load (no timing race).
        val polyfillLatch = CountDownLatch(1)
        geckoRuntime.webExtensionController
            .ensureBuiltIn("resource://android/assets/extensions/polyfill/", "polyfill@nightpos")
            .accept(
                { Log.i("NightPOS", "Polyfill extension installed"); polyfillLatch.countDown() },
                { e -> Log.w("NightPOS", "Polyfill extension error: ${e?.message}"); polyfillLatch.countDown() },
            )
        polyfillLatch.await(3, TimeUnit.SECONDS)
    }

    private fun isMainProcess(): Boolean {
        val pid = Process.myPid()
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName == packageName
    }

    private fun writeGeckoConfig(): File {
        val config = File(filesDir, "geckoview-config.yaml")
        config.writeText(
            """
            ---
            prefs:
              security.sandbox.content.level: 0
              security.sandbox.content.syscall_whitelist: ""
              # Allow HTTPS pages to fetch http://localhost — Firefox blocks this
              # as mixed content by default (Chrome has a localhost exemption).
              # Required for the Sunmi printer HTTP bridge on port 8585.
              security.mixed_content.block_active_content: false
              security.mixed_content.block_display_content: false
            """.trimIndent()
        )
        return config
    }
}
