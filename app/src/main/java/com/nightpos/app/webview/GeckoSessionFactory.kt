package com.nightpos.app.webview

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

/**
 * Creates a [GeckoSession] pre-configured for Odoo 19 POS:
 *  - Desktop user-agent suffix so the responsive UI renders in tablet layout.
 *  - JavaScript, tracking protection off (Odoo's own analytics must not be blocked).
 *  - Multi-process (e10s) via LOAD_FLAGS defaults.
 */
object GeckoSessionFactory {

    fun create(): GeckoSession {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .useTrackingProtection(false)
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
            .userAgentOverride(
                "Mozilla/5.0 (Linux; Android 6.0.1) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) GeckoView/134.0 Mobile Safari/537.36 NightPOS/1.0"
            )
            .build()
        return GeckoSession(settings)
    }
}
