package com.nightpos.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import android.util.Log
import com.nightpos.app.print.PrintHttpServer
import com.nightpos.app.print.PrintServiceEnabler
import com.nightpos.app.print.SunmiJsBridge
import com.nightpos.app.print.SunmiPrinterConnection
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.StorageController
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NightPOSApplication : Application() {

    companion object {
        lateinit var geckoRuntime: GeckoRuntime
            private set
        lateinit var printerConnection: SunmiPrinterConnection
            private set
        lateinit var jsBridge: SunmiJsBridge
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // GeckoView spawns content processes that each run their own Application
        // instance. Restrict heavy setup to the main process only.
        if (!isMainProcess()) return

        PrintServiceEnabler.ensureEnabled(this)

        printerConnection = SunmiPrinterConnection(this).also { it.bind() }
        jsBridge = SunmiJsBridge(this).also { it.bindPrinter() }

        runCatching {
            PrintHttpServer(printerConnection).start()
            Log.i("NightPOS", "PrintHttpServer started on port ${PrintHttpServer.PORT}")
        }.onFailure { Log.e("NightPOS", "PrintHttpServer failed to start: ${it.message}") }

        // Wipe the GeckoView extension startup cache so updated content.js /
        // pos-configs.js are always loaded fresh from assets rather than a
        // stale lz4-compressed cache that survives APK upgrades.
        clearGeckoStartupCache()

        val configFile = writeGeckoConfig()
        geckoRuntime = GeckoRuntime.create(
            this,
            GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .remoteDebuggingEnabled(false)
                .consoleOutput(true)
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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        // Sunmi POS terminals run with ~2GB RAM and the GeckoView session stays
        // open for an entire shift. When Android signals memory pressure, drop
        // GeckoView's network/image caches (cookies, localStorage, IndexedDB and
        // the active session are preserved) so the app doesn't get OOM-killed or
        // throttled by the OS.
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            runCatching {
                geckoRuntime.storageController.clearData(StorageController.ClearFlags.ALL_CACHES)
                Log.i("NightPOS", "onTrimMemory($level): cleared GeckoView network/image caches")
            }.onFailure { Log.w("NightPOS", "onTrimMemory clearData failed: ${it.message}") }
        }
    }

    private fun isMainProcess(): Boolean {
        val pid = Process.myPid()
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName == packageName
    }

    private fun clearGeckoStartupCache() {
        // GeckoView caches web-extension bytecode in startupCache/webext.sc.lz4.
        // If we update content.js but this cache is stale, the old script runs.
        // Only wipe the cache when the extension manifest version actually changed
        // so we don't slow down every startup by forcing Gecko to rebuild it.
        runCatching {
            val prefs = getSharedPreferences("nightpos_ext", MODE_PRIVATE)
            val storedVersion = prefs.getString("ext_version", null)
            val currentVersion = assets.open("extensions/polyfill/manifest.json")
                .bufferedReader().readText()
                .let { Regex(""""version"\s*:\s*"([^"]+)"""").find(it)?.groupValues?.get(1) }

            if (storedVersion != currentVersion) {
                File(filesDir, "mozilla").walkTopDown()
                    .filter { it.isDirectory && it.name == "startupCache" }
                    .forEach { it.deleteRecursively() }
                Log.i("NightPOS", "GeckoView startupCache cleared ($storedVersion → $currentVersion)")
                if (currentVersion != null) prefs.edit().putString("ext_version", currentVersion).apply()
            }
        }.onFailure { Log.w("NightPOS", "clearGeckoStartupCache: ${it.message}") }
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
              # Odoo 19's PWA shell relies on a Service Worker + IndexedDB for
              # offline order queueing. Both are enabled by default in Gecko,
              # but set explicitly so offline support keeps working regardless
              # of upstream default changes.
              dom.serviceWorkers.enabled: true
              dom.indexedDB.enabled: true
            """.trimIndent()
        )
        return config
    }
}
