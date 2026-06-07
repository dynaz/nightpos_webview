package com.nightpos.app.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Builds a [WebView] pre-configured with everything an Odoo 19 POS UI needs:
 * JavaScript, DOM storage, first/third-party cookies, mixed-content handling for
 * legacy assets, file access for uploads, and a desktop-leaning viewport so the
 * responsive Odoo UI renders in its tablet/desktop layout rather than its mobile one.
 *
 * Centralizing this avoids subtly different configs between the main POS WebView
 * and any popup windows created via `window.open()`.
 */
@SuppressLint("SetJavaScriptEnabled")
object WebViewFactory {

    fun create(context: Context): WebView {
        // CookieManager must be enabled before any WebView loads content.
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        return WebView(context).apply {
            configure(this, cookieManager)
        }
    }

    fun configure(webView: WebView, cookieManager: CookieManager = CookieManager.getInstance()) {
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            // Odoo's POS UI is a responsive web app designed primarily for desktop/tablet —
            // disabling the "mobile viewport" heuristics keeps the layout matching the
            // tablet-optimized design called for in the spec.
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false

            // File uploads & local resource access (needed for some Odoo asset bundling/blobs).
            allowFileAccess = true
            allowContentAccess = true

            // Mixed content can occur with some self-hosted Odoo asset CDNs during migrations;
            // COMPATIBILITY_MODE allows HTTPS pages to load HTTP sub-resources without
            // breaking the page, while shouldOverrideUrlLoading still blocks navigation
            // to non-allowed domains.
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true

            cacheMode = WebSettings.LOAD_DEFAULT

            userAgentString = "$userAgentString NightPOS/1.0"
        }

        webView.apply {
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = false
            settings.setGeolocationEnabled(true)
        }
    }
}
