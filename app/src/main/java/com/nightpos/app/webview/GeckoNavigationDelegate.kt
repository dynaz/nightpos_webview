package com.nightpos.app.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.nightpos.app.util.Constants
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.ContentPermission

/**
 * GeckoView equivalent of [PosWebViewClient]: restricts navigation to the allowed
 * Odoo domain and hands external URLs to the system browser.
 */
class GeckoNavigationDelegate(
    private val context: Context,
    private val onPageStarted: (url: String?) -> Unit,
    private val onPageFinished: (url: String?) -> Unit,
    private val onBlockedDomain: (host: String) -> Unit,
    private val onPageLoadError: (uri: String?, error: WebRequestError) -> Unit,
) : GeckoSession.NavigationDelegate {

    companion object {
        private const val TAG = "NightPOS"
    }

    // GeckoView 100+ signature: url + content permissions list + user-gesture flag.
    override fun onLocationChange(
        session: GeckoSession,
        url: String?,
        perms: List<ContentPermission>,
        hasUserGesture: Boolean,
    ) {
        onPageFinished(url)
    }

    override fun onLoadRequest(
        session: GeckoSession,
        request: GeckoSession.NavigationDelegate.LoadRequest,
    ): GeckoResult<AllowOrDeny>? {
        val uri = Uri.parse(request.uri)
        val scheme = uri.scheme?.lowercase()
        val host = uri.host

        return when {
            (scheme == "http" || scheme == "https") && Constants.isAllowedHost(host) ->
                GeckoResult.fromValue(AllowOrDeny.ALLOW)

            scheme == "http" || scheme == "https" -> {
                onBlockedDomain(host ?: request.uri)
                openExternal(uri)
                GeckoResult.fromValue(AllowOrDeny.DENY)
            }

            scheme == "about" || scheme == "data" || scheme == "blob" ->
                GeckoResult.fromValue(AllowOrDeny.ALLOW)

            else -> {
                openExternal(uri)
                GeckoResult.fromValue(AllowOrDeny.DENY)
            }
        }
    }

    // Previously returned null with no further action: a failed navigation (DNS
    // failure, connection reset, TLS error, etc.) left the GeckoView permanently
    // blank with the progress bar simply disappearing — no error message, no way
    // to retry. Surface the error (with category/code for diagnostics) so the
    // screen can show a retry UI instead of a dead blank page.
    override fun onLoadError(
        session: GeckoSession,
        uri: String?,
        error: WebRequestError,
    ): GeckoResult<String?> {
        Log.w(TAG, "onLoadError(uri=$uri): category=${error.category}, code=${error.code}")
        onPageLoadError(uri, error)
        return GeckoResult.fromValue(null)
    }

    private fun openExternal(uri: Uri) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}
