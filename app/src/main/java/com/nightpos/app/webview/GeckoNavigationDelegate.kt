package com.nightpos.app.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
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
) : GeckoSession.NavigationDelegate {

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

    override fun onLoadError(
        session: GeckoSession,
        uri: String?,
        error: WebRequestError,
    ): GeckoResult<String?> = GeckoResult.fromValue(null)

    private fun openExternal(uri: Uri) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}
