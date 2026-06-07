package com.nightpos.app.data

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Centralizes everything that needs to happen on logout: clearing cookies,
 * DOM/local storage, HTTP cache, form data and any cached login session so the
 * next user starts from a clean slate — required because POS tablets are
 * typically shared between staff/shifts.
 *
 * A [WebView] instance is supplied lazily (the screen that owns the WebView
 * passes it in) because clearing history/cache requires an actual WebView.
 */
class SessionManager(private val context: Context) {

    /**
     * Clears all WebView-related session data: cookies, DOM storage, HTTP cache,
     * form data, and navigation history of the supplied [webView] (if any).
     */
    suspend fun clearSession(webView: WebView?) {
        clearCookies()
        clearWebStorage()
        clearWebViewCacheAndHistory(webView)
        clearFormData(webView)
    }

    private suspend fun clearCookies() = suspendCancellableCoroutine<Unit> { cont ->
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies { _ ->
            cookieManager.flush()
            if (cont.isActive) cont.resume(Unit)
        }
    }

    private fun clearWebStorage() {
        WebStorage.getInstance().deleteAllData()
    }

    private fun clearWebViewCacheAndHistory(webView: WebView?) {
        webView?.apply {
            clearCache(true)
            clearHistory()
            clearSslPreferences()
        }
        // Also remove the on-disk scratch directory used for WebView file uploads.
        context.cacheDir.resolve("webview_uploads").deleteRecursively()
    }

    private fun clearFormData(webView: WebView?) {
        @Suppress("DEPRECATION")
        webView?.clearFormData()
        @Suppress("DEPRECATION")
        WebViewDatabase.getInstance(context).clearFormData()
    }
}
