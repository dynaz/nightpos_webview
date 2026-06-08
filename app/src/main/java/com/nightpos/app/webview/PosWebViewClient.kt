package com.nightpos.app.webview

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import com.nightpos.app.util.Constants

/**
 * Enforces the security requirements from the spec:
 *  - Navigation is restricted to [Constants.ALLOWED_DOMAIN] (and its subdomains).
 *  - Links to other domains are handed off to the system browser instead of being
 *    blocked silently, so staff can still follow e.g. payment-provider redirects
 *    that intentionally leave the POS domain when that's expected — but anything
 *    unexpected never gets a foothold inside the app's WebView/cookie jar.
 *  - SSL errors are surfaced to the user instead of being silently accepted or
 *    silently rejected (both of which are bad defaults for a payments app).
 *
 * All callbacks are forwarded to the hosting screen via simple lambdas so this
 * class stays unit-testable and free of Compose/Activity references.
 */
class PosWebViewClient(
    private val onPageStarted: (url: String?) -> Unit,
    private val onPageFinished: (url: String?) -> Unit,
    private val onReceivedError: (errorCode: Int, description: String?, failingUrl: String?) -> Unit,
    private val onSslError: (handler: SslErrorHandler, error: SslError) -> Unit,
    private val onBlockedDomain: (host: String) -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return handleUrl(view, request.url)
    }

    // Pre-Lollipop fallback (minSdk is 23, i.e. Lollipop+, so this practically never triggers, kept for completeness).
    @Suppress("DEPRECATION", "OverridingDeprecatedMember")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return handleUrl(view, Uri.parse(url))
    }

    private fun handleUrl(view: WebView, uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        val host = uri.host

        return when {
            // Allowed domain: let the WebView load it normally (and keep cookies/session).
            (scheme == "http" || scheme == "https") && Constants.isAllowedHost(host) -> false

            // http/https to a foreign domain: hand off to the system browser per spec
            // ("Handle external links with Android browser") and stay blocked inside the app.
            scheme == "http" || scheme == "https" -> {
                onBlockedDomain(host ?: uri.toString())
                openInExternalBrowser(view, uri)
                true
            }

            // tel:, mailto:, intent:, market:// etc — delegate to the OS so the
            // relevant app (dialer, mail client, Play Store…) can handle it.
            else -> {
                openInExternalBrowser(view, uri)
                true
            }
        }
    }

    private fun openInExternalBrowser(view: WebView, uri: Uri) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            ContextCompat.startActivity(view.context, intent, null)
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted(url)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished(url)
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        super.onReceivedError(view, request, error)
        if (request.isForMainFrame) {
            onReceivedError(error.errorCode, error.description?.toString(), request.url.toString())
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        // Never silently proceed: surface the decision to the user (see WebViewScreen's dialog).
        onSslError(handler, error)
    }
}
