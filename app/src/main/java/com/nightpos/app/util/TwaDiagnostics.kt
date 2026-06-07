package com.nightpos.app.util

import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabsClient
import com.nightpos.app.BuildConfig

/**
 * Gathers on-device info relevant to diagnosing "TWA opens in Chrome with the
 * address bar instead of full-screen" issues — surfaced via a hidden tap
 * gesture in Settings (About row) since the device can't be plugged into
 * Android Studio for logcat in the field.
 *
 * The two package/version pairs that matter most:
 *  - the Custom Tabs provider: this is the browser the TWA actually renders
 *    with (should be Chrome on a healthy device)
 *  - the System WebView provider: the (possibly outdated) component the
 *    in-app WebView screens use — comparing the two explains why TWA exists
 */
object TwaDiagnostics {

    fun collect(context: Context, configuredServerUrl: String): String {
        val pm = context.packageManager

        fun describe(packageName: String?): String {
            if (packageName.isNullOrBlank()) return "(none)"
            val versionName = runCatching { pm.getPackageInfo(packageName, 0).versionName }.getOrNull()
            return if (versionName != null) "$packageName — $versionName" else packageName
        }

        val customTabsPackage = CustomTabsClient.getPackageName(context, null)
        val webViewPackage = WebView.getCurrentWebViewPackage()?.packageName
        val baseUrl = configuredServerUrl.ifBlank { Constants.DEFAULT_BASE_URL }

        return buildString {
            appendLine("NightPOS Soho ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Application ID: ${BuildConfig.APPLICATION_ID}")
            appendLine()
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine()
            appendLine("Custom Tabs / TWA provider (renders POS/Reports/Customers):")
            appendLine("  ${describe(customTabsPackage)}")
            appendLine()
            appendLine("System WebView provider (renders nothing now, kept for reference):")
            appendLine("  ${describe(webViewPackage)}")
            appendLine()
            appendLine("Configured server URL: $baseUrl")
            appendLine("POS launch URL: ${Constants.openPosUrl(baseUrl)}")
            appendLine("Reports/Customers launch URL: ${Constants.reportsUrl(baseUrl)}")
            appendLine()
            appendLine("Asset Links check:")
            appendLine("  https://digitalassetlinks.googleapis.com/v1/statements:list")
            appendLine("    ?source.web.site=$baseUrl")
            appendLine("    &relation=delegate_permission/common.handle_all_urls")
        }
    }
}
