package com.nightpos.app.webview

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.nightpos.app.print.SunmiJsBridge

/**
 * Modern flavor — returns a system WebView for D2s family (Android 11+).
 *
 * Android 11 ships with a Chrome-based WebView that fully supports Odoo 19.
 * Uses @JavascriptInterface bridge instead of GeckoView PromptDelegate.
 * Chrome allows fetch("http://localhost:8585") from HTTPS as a mixed-content
 * exception for localhost — no special prefs needed.
 */
object WebViewFactory {

    @SuppressLint("SetJavaScriptEnabled")
    fun create(context: Context, onPageStarted: (String) -> Unit): View {
        val webView = WebView(context)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        val jsBridge = SunmiJsBridge(context)
        jsBridge.bindPrinter()
        webView.addJavascriptInterface(jsBridge, "NightPOSBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                onPageStarted(url)
                // Inject device identity and bridge shim synchronously before page JS runs
                view.evaluateJavascript(SunmiJsBridge.buildInjectionScript(), null)
            }
        }

        return webView
    }
}
