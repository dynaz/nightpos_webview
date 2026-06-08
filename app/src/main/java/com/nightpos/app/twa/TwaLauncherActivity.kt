package com.nightpos.app.twa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsClient
import com.google.androidbrowserhelper.trusted.LauncherActivity
import com.nightpos.app.util.TwaLaunchLog

/**
 * Launches the Odoo backend in an external browser.
 *
 * Sunmi T1 terminals (Android 6.0.1) ship with a Chrome/system-WebView build too
 * old to render Odoo 19, so this prefers handing off to **Firefox** (explicit
 * package match) whose modern Gecko engine renders it correctly. If Firefox isn't
 * installed, it falls back to the original Trusted Web Activity (rendered by
 * whatever Custom Tabs provider — normally Chrome — is available), and finally to
 * a plain browser Intent so the buttons always do something useful.
 *
 * [LauncherActivity] normally reads its URL from the `DEFAULT_URL` manifest
 * meta-data, but the Dashboard passes the URL via [EXTRA_URL] instead so it can
 * point to different destinations (POS / Reports / etc.) without separate activities.
 */
class TwaLauncherActivity : LauncherActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val url = resolveUrl()
        val uri = Uri.parse(url)

        if (launchInFirefox(uri)) {
            finish()
            return
        }

        val provider = CustomTabsClient.getPackageName(this, null)
        logLaunch(url, provider)

        if (provider == null) {
            // No Custom Tabs / TWA provider — open in whatever browser the device has.
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            finish()
            return
        }

        super.onCreate(savedInstanceState)
    }

    override fun getLaunchingUrl(): Uri = Uri.parse(resolveUrl())

    private fun resolveUrl(): String =
        intent?.getStringExtra(EXTRA_URL)?.takeIf { it.isNotBlank() }
            ?: super.getLaunchingUrl().toString()

    /**
     * Explicitly targets the Firefox package so the URL opens there instead of
     * through Custom Tabs/TWA (which always renders with Chrome). Returns false
     * — without starting anything — if Firefox isn't installed, so callers can
     * fall through to the TWA/system-browser paths below.
     */
    private fun launchInFirefox(uri: Uri): Boolean {
        val firefoxIntent = Intent(Intent.ACTION_VIEW, uri).setPackage(FIREFOX_PACKAGE)
        if (packageManager.resolveActivity(firefoxIntent, 0) == null) return false

        return runCatching { startActivity(firefoxIntent) }
            .onSuccess { TwaLaunchLog.append(this, "INFO url=$uri provider=$FIREFOX_PACKAGE (explicit Firefox handoff)") }
            .isSuccess
    }

    private fun logLaunch(url: String, provider: String?) {
        val entry = if (provider == null) {
            "WARN url=$url provider=NONE — no Custom Tabs provider; falling back to system browser"
        } else {
            val version = runCatching { packageManager.getPackageInfo(provider, 0).versionName }.getOrNull()
            "INFO url=$url provider=$provider${version?.let { "@$it" } ?: ""}"
        }
        TwaLaunchLog.append(this, entry)
    }

    companion object {
        private const val EXTRA_URL = "com.nightpos.app.twa.EXTRA_URL"
        private const val FIREFOX_PACKAGE = "org.mozilla.firefox"

        fun createIntent(context: Context, url: String): Intent =
            Intent(context, TwaLauncherActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
    }
}
