package com.nightpos.app.twa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsClient
import com.google.androidbrowserhelper.trusted.LauncherActivity
import com.nightpos.app.util.TwaLaunchLog

/**
 * Launches the Odoo backend in a Trusted Web Activity, rendered by the device's
 * Chrome browser engine instead of the (often outdated) Android System WebView
 * component — some devices ship a WebView build too old for Odoo 19's UI while
 * still keeping Chrome itself up to date.
 *
 * [LauncherActivity] normally reads its URL from the `DEFAULT_URL` manifest
 * meta-data, but the Dashboard needs to point this same launcher at three
 * different destinations (POS / Reports / Customers), so the URL is instead
 * passed via [EXTRA_URL] and falls back to the manifest default only if absent.
 */
class TwaLauncherActivity : LauncherActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        logLaunch()
        super.onCreate(savedInstanceState)
    }

    override fun getLaunchingUrl(): Uri {
        val url = intent?.getStringExtra(EXTRA_URL)
        return if (!url.isNullOrBlank()) Uri.parse(url) else super.getLaunchingUrl()
    }

    private fun logLaunch() {
        val url = intent?.getStringExtra(EXTRA_URL) ?: "(default)"
        val provider = CustomTabsClient.getPackageName(this, null)
        val entry = if (provider == null) {
            "WARN url=$url provider=NONE — no Custom Tabs provider; Chrome may not be installed or is disabled"
        } else {
            val version = runCatching { packageManager.getPackageInfo(provider, 0).versionName }.getOrNull()
            "INFO url=$url provider=$provider${version?.let { "@$it" } ?: ""}"
        }
        TwaLaunchLog.append(this, entry)
    }

    companion object {
        private const val EXTRA_URL = "com.nightpos.app.twa.EXTRA_URL"

        fun createIntent(context: Context, url: String): Intent =
            Intent(context, TwaLauncherActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
    }
}
