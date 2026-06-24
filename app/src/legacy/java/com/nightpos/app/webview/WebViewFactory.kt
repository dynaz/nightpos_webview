package com.nightpos.app.webview

import android.content.Context
import android.view.View
import com.nightpos.app.NightPOSApplication
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

/**
 * Legacy flavor — returns a GeckoView for T1/T2 (Android 6/7).
 *
 * System WebView on these devices is Chrome 60 or older, which cannot run
 * Odoo 19. GeckoView 142 ships its own JS engine so it works regardless of
 * what Android version is installed.
 */
object WebViewFactory {

    fun create(context: Context, onPageStarted: (String) -> Unit): View {
        val geckoView = GeckoView(context)
        val session = GeckoSession()

        session.settings.allowJavascript = true
        session.promptDelegate = NightPOSApplication.jsBridge.geckoPromptDelegate

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean,
            ) {
                url?.let { onPageStarted(it) }
            }
        }

        session.open(NightPOSApplication.geckoRuntime)
        geckoView.setSession(session)
        return geckoView
    }
}
