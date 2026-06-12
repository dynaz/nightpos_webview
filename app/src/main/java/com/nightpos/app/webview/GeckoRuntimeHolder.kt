package com.nightpos.app.webview

import android.content.Context
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

/**
 * Process-wide GeckoRuntime singleton.
 *
 * GeckoRuntime.create() must be called at most once per process — calling it
 * a second time throws. This object enforces that guarantee with double-checked
 * locking and exposes the instance through a safe property that errors clearly
 * if accessed before init().
 */
object GeckoRuntimeHolder {

    @Volatile
    private var _runtime: GeckoRuntime? = null

    /** The shared runtime. Throws if accessed before [init] completes. */
    val runtime: GeckoRuntime
        get() = _runtime
            ?: error("GeckoRuntime not initialised — call GeckoRuntimeHolder.init() from Application.onCreate() first")

    /** Returns true once [init] has been called. */
    val isInitialised: Boolean get() = _runtime != null

    /**
     * Creates the runtime. Safe to call multiple times — subsequent calls are
     * no-ops so the same instance is always returned.
     */
    fun init(context: Context, configFilePath: String) {
        if (_runtime != null) return
        synchronized(this) {
            if (_runtime != null) return
            _runtime = GeckoRuntime.create(
                context.applicationContext,
                GeckoRuntimeSettings.Builder()
                    .javaScriptEnabled(true)
                    .remoteDebuggingEnabled(false)
                    .consoleOutput(true)
                    .configFilePath(configFilePath)
                    // Use dark color scheme to match NightPOS theme
                    .preferredColorScheme(GeckoRuntimeSettings.COLOR_SCHEME_DARK)
                    .build(),
            ).also { rt ->
                // This is an internal POS app on a private network — disable all
                // privacy/security scanning features that add latency or block
                // legitimate Odoo resources.
                rt.settings.contentBlocking.apply {
                    // No anti-tracking (blocks Odoo's own XHR/fetch calls)
                    antiTracking = ContentBlocking.AntiTracking.NONE
                    // Accept all cookies — Odoo session requires first-party cookies
                    cookieBehavior = ContentBlocking.CookieBehavior.ACCEPT_ALL
                    // Disable Enhanced Tracking Protection entirely
                    enhancedTrackingProtectionLevel = ContentBlocking.EtpLevel.NONE
                    // No Safe Browsing — avoids Google lookups on every URL
                    safeBrowsing = ContentBlocking.SafeBrowsing.NONE
                    // No social-media tracker blocking
                    strictSocialTrackingProtection = false
                }
            }
        }
    }
}
