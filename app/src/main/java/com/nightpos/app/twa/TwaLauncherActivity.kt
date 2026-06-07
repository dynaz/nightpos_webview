package com.nightpos.app.twa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsClient
import com.google.androidbrowserhelper.trusted.LauncherActivity
import com.nightpos.app.util.TwaLaunchLog

/**
 * Launches the Odoo backend in a Trusted Web Activity rendered by Chrome.
 *
 * Falls back to a plain browser Intent when no Custom Tabs provider is available
 * (e.g. Chrome not installed / disabled) so buttons always do something useful.
 *
 * [LauncherActivity] normally reads its URL from the `DEFAULT_URL` manifest
 * meta-data, but the Dashboard passes the URL via [EXTRA_URL] instead so it can
 * point to different destinations (POS / Reports / etc.) without separate activities.
 */
class TwaLauncherActivity : LauncherActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val url = resolveUrl()
        val provider = CustomTabsClient.getPackageName(this, null)
        logLaunch(url, provider)

        if (provider == null) {
            // No Custom Tabs / TWA provider — open in whatever browser the device has.
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            finish()
            return
        }

        super.onCreate(savedInstanceState)
    }

    override fun getLaunchingUrl(): Uri = Uri.parse(resolveUrl())

    private fun resolveUrl(): String =
        intent?.getStringExtra(EXTRA_URL)?.takeIf { it.isNotBlank() }
            ?: super.getLaunchingUrl().toString()

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

        fun createIntent(context: Context, url: String): Intent =
            Intent(context, TwaLauncherActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
    }
}
