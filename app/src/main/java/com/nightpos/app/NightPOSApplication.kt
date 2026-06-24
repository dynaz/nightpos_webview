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
        // Null on d2splus flavor (USE_GECKO = false); non-null on arm32/arm64.
        var geckoRuntime: GeckoRuntime? = null
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
            PrintHttpServer(printerConnection, this).start()
            Log.i("NightPOS", "PrintHttpServer started on port ${PrintHttpServer.PORT}")
        }.onFailure { Log.e("NightPOS", "PrintHttpServer failed to start: ${it.message}") }

        if (BuildConfig.USE_GECKO) {
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
                    // Forwarding every JS console message to logcat costs a JNI round trip
                    // per call; Odoo's POS UI logs frequently (barcode scans, sync status).
                    // Keep it for debug builds only.
                    .consoleOutput(BuildConfig.DEBUG)
                    .configFilePath(configFile.absolutePath)
                    .build(),
            )

            // Install built-in extension that polyfills Promise.withResolvers (Firefox 121)
            // and structuredClone (Firefox 94) — required by Odoo 19 on GeckoView 99.
            // Block until the extension is confirmed installed so the content script fires
            // at document_start on the very first page load (no timing race).
            val polyfillLatch = CountDownLatch(1)
            geckoRuntime!!.webExtensionController
                .ensureBuiltIn("resource://android/assets/extensions/polyfill/", "polyfill@nightpos")
                .accept(
                    { Log.i("NightPOS", "Polyfill extension installed"); polyfillLatch.countDown() },
                    { e -> Log.w("NightPOS", "Polyfill extension error: ${e?.message}"); polyfillLatch.countDown() },
                )
            polyfillLatch.await(3, TimeUnit.SECONDS)
        }
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
                geckoRuntime?.storageController?.clearData(StorageController.ClearFlags.ALL_CACHES)
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
              # Cache Storage API (caches.open/match/put) — what the Odoo 19
              # service worker uses to serve its JS/CSS app shell instantly on
              # repeat loads instead of re-fetching from the server.
              dom.caches.enabled: true
              # Disable Gecko's "smart" disk-cache sizing (which shrinks the
              # cache based on free disk space) and set an explicit cap so the
              # POS's JS/CSS/image bundles stay cached across app restarts
              # instead of being re-downloaded every time the app is reopened.
              browser.cache.disk.enable: true
              browser.cache.disk.smart_size.enabled: false
              browser.cache.disk.capacity: 262144
              browser.cache.memory.enable: true
              browser.cache.memory.capacity: 16384
              # Telemetry/experiments/health-report uploads are pure overhead for an
              # embedded kiosk POS — disable so the Sunmi D2s isn't spending CPU,
              # battery, and limited bandwidth on background reporting traffic.
              datareporting.healthreport.uploadEnabled: false
              toolkit.telemetry.enabled: false
              toolkit.telemetry.unified: false
              app.normandy.enabled: false
              # Speculative network prefetching/DNS prefetch fights for bandwidth
              # with real POS requests and is wasted on a fixed-destination app
              # (often on a constrained WireGuard/VPN link to the Odoo server).
              network.predictor.enabled: false
              network.dns.disablePrefetch: true
              network.prefetch-next: false
            """.trimIndent()
        )
        return config
    }
}
