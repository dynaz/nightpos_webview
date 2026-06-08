package com.nightpos.app.twa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import com.nightpos.app.util.TwaLaunchLog

/**
 * Launches a URL in an external browser.
 *
 * Priority order:
 *  1. Firefox (explicit package) — modern Gecko engine needed on Sunmi T1 / Android 6.0.1
 *     where the system WebView / Chrome build is too old for Odoo 19.
 *  2. Custom Tabs provider (Chrome or other) — if Firefox isn't installed.
 *  3. Plain ACTION_VIEW — last resort if no Custom Tabs provider exists.
 *
 * Does NOT extend [com.google.androidbrowserhelper.trusted.LauncherActivity] because
 * that base class calls finish() early when the activity is not the task root (first
 * launch from another task), preventing our Firefox-first logic from running.
 */
class TwaLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent?.getStringExtra(EXTRA_URL)?.takeIf { it.isNotBlank() }
            ?: run { finish(); return }
        val uri = Uri.parse(url)

        when {
            launchInFirefox(uri, url) -> Unit
            launchInCustomTabs(uri, url) -> Unit
            else -> {
                TwaLaunchLog.append(this, "WARN url=$url provider=NONE — fallback to system browser")
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            }
        }

        finish()
    }

    private fun launchInFirefox(uri: Uri, url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(FIREFOX_PACKAGE)
        if (packageManager.resolveActivity(intent, 0) == null) return false
        return runCatching { startActivity(intent) }
            .onSuccess { TwaLaunchLog.append(this, "INFO url=$url provider=$FIREFOX_PACKAGE") }
            .isSuccess
    }

    private fun launchInCustomTabs(uri: Uri, url: String): Boolean {
        val provider = CustomTabsClient.getPackageName(this, null) ?: return false
        return runCatching {
            CustomTabsIntent.Builder().build().launchUrl(this, uri)
        }.onSuccess { TwaLaunchLog.append(this, "INFO url=$url provider=$provider (CustomTabs)") }
            .isSuccess
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
