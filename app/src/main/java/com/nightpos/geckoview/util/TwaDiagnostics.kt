package com.nightpos.geckoview.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabsClient
import com.nightpos.geckoview.BuildConfig

/**
 * Gathers on-device info relevant to diagnosing TWA / browser issues.
 *
 * Key fields:
 *  - Custom Tabs provider: the browser TWA renders with (should be Chrome)
 *  - Browser inventory: every installed browser + whether it supports Custom Tabs
 *  - System WebView: the in-app WebView component version
 */
object TwaDiagnostics {

    /** Well-known browser packages to probe even when not chosen as Custom Tabs provider. */
    private val KNOWN_BROWSERS = listOf(
        "com.android.chrome"            to "Chrome",
        "com.chrome.beta"               to "Chrome Beta",
        "com.chrome.dev"                to "Chrome Dev",
        "com.chrome.canary"             to "Chrome Canary",
        "com.sec.android.app.sbrowser"  to "Samsung Internet",
        "com.microsoft.emmx"            to "Edge",
        "org.mozilla.firefox"           to "Firefox",
        "com.brave.browser"             to "Brave",
        "com.opera.browser"             to "Opera",
    )

    fun collect(context: Context, configuredServerUrl: String): String {
        val pm = context.packageManager

        fun pkgVersion(packageName: String): String? =
            runCatching { pm.getPackageInfo(packageName, 0).versionName }.getOrNull()

        fun isInstalled(packageName: String): Boolean =
            runCatching { pm.getPackageInfo(packageName, 0); true }.getOrDefault(false)

        /** Returns true if the package responds to the CustomTabsService intent. */
        fun supportsCustomTabs(packageName: String): Boolean {
            val intent = Intent("android.support.customtabs.action.CustomTabsService")
                .setPackage(packageName)
            return pm.resolveService(intent, 0) != null
        }

        /** All installed browsers (handles ACTION_VIEW http/https). */
        fun installedBrowserPackages(): Set<String> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
            val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PackageManager.MATCH_ALL else 0
            return pm.queryIntentActivities(intent, flag)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
        }

        val customTabsPackage = CustomTabsClient.getPackageName(context, null)
        val webViewPackage = WebView.getCurrentWebViewPackage()?.packageName
        val baseUrl = configuredServerUrl.ifBlank { Constants.DEFAULT_BASE_URL }

        val allBrowsers = installedBrowserPackages()

        // Merge known-browser list with any discovered packages
        val browserRows = (KNOWN_BROWSERS.map { it.first } + allBrowsers)
            .distinct()
            .filter { isInstalled(it) }
            .map { pkg ->
                val label = KNOWN_BROWSERS.firstOrNull { it.first == pkg }?.second ?: pkg
                val version = pkgVersion(pkg) ?: "?"
                val cts = if (supportsCustomTabs(pkg)) "CustomTabs=YES" else "CustomTabs=NO"
                val chosen = if (pkg == customTabsPackage) " ← selected" else ""
                "  $label ($pkg) v$version  $cts$chosen"
            }

        return buildString {
            appendLine("NightPOS Soho ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Application ID: ${BuildConfig.APPLICATION_ID}")
            appendLine()
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine()
            appendLine("Custom Tabs / TWA provider:")
            appendLine("  ${if (customTabsPackage != null) "$customTabsPackage (${pkgVersion(customTabsPackage) ?: "?"})" else "(none) — install/enable Chrome for TWA full-screen"}")
            appendLine()
            appendLine("Browser inventory:")
            if (browserRows.isEmpty()) appendLine("  (no browsers found)")
            else browserRows.forEach { appendLine(it) }
            appendLine()
            appendLine("System WebView: ${if (webViewPackage != null) "$webViewPackage — ${pkgVersion(webViewPackage) ?: "?"}" else "(none)"}")
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
