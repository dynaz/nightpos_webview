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
import com.nightpos.app.webview.GeckoRuntimeHolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NightPOSApplication : Application() {

    companion object {
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
        GeckoRuntimeHolder.init(this, configFile.absolutePath)

        // Install built-in extension that polyfills Promise.withResolvers (Firefox 121)
        // and structuredClone (Firefox 94) — required by Odoo 19 on GeckoView 99.
        // Block until the extension is confirmed installed so the content script fires
        // at document_start on the very first page load (no timing race).
        val polyfillLatch = CountDownLatch(1)
        GeckoRuntimeHolder.runtime.webExtensionController
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

    private fun clearGeckoStartupCache() {
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
              # Sandbox — required on Sunmi kernel 3.10 (SELinux blocks IPC ioctl)
              security.sandbox.content.level: 0
              security.sandbox.content.syscall_whitelist: ""

              # Mixed content — allow HTTPS pages to fetch http://localhost
              # (Chrome has a localhost exemption; Firefox does not by default)
              # Required for the Sunmi printer HTTP bridge on port 8585.
              security.mixed_content.block_active_content: false
              security.mixed_content.block_display_content: false

              # Safe Browsing — disable Google URL lookups (internal POS, no need)
              browser.safebrowsing.enabled: false
              browser.safebrowsing.downloads.enabled: false
              browser.safebrowsing.malware.enabled: false
              browser.safebrowsing.phishing.enabled: false
              browser.safebrowsing.blockedURIs.enabled: false

              # Tracking Protection — off so Odoo's own XHR/analytics are not blocked
              privacy.trackingprotection.enabled: false
              privacy.trackingprotection.pbmode.enabled: false
              privacy.trackingprotection.socialtracking.enabled: false
              privacy.trackingprotection.cryptomining.enabled: false
              privacy.trackingprotection.fingerprinting.enabled: false

              # Cookie policy — accept all (Odoo session depends on first-party cookies)
              network.cookie.cookieBehavior: 0

              # Performance — disable speculative/prefetch network activity
              network.dns.disablePrefetch: true
              network.prefetch-next: false
              network.http.speculative-parallel-limit: 0

              # Telemetry — disable all data collection
              toolkit.telemetry.enabled: false
              toolkit.telemetry.unified: false
              datareporting.healthreport.uploadEnabled: false
              datareporting.policy.dataSubmissionEnabled: false

              # Service Workers — must be on for Odoo PWA offline mode
              dom.serviceWorkers.enabled: true
              dom.serviceWorkers.interception.enabled: true
              dom.serviceWorkers.openWindow.enabled: true

              # IndexedDB, Cache Storage, Storage Manager — POS caches product/order
              # data locally to avoid full reload on every session
              dom.indexedDB.enabled: true
              dom.caches.enabled: true
              dom.storageManager.enabled: true

              # Origin Private File System — used by Odoo 17+ for large binary blobs
              dom.fs.enabled: true

              # Raise IndexedDB warning threshold to 512 MB (default is 50 MB).
              # Odoo POS product catalogue + images can exceed the default easily.
              dom.indexedDB.warningQuota: 512
            """.trimIndent()
        )
        return config
    }
}
