package com.nightpos.app.webview

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

/**
 * Creates a [GeckoSession] pre-configured for Odoo 19 POS on Sunmi D2s Plus:
 *  - Desktop UA so Odoo renders in tablet layout
 *  - Tracking protection off at session level (also off at runtime level)
 *  - Private mode off — cookies/storage must persist for Odoo login session
 *  - Media not suspended when inactive — receipt audio / KDS alerts keep playing
 */
object GeckoSessionFactory {

    fun create(): GeckoSession {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .useTrackingProtection(false)
            .suspendMediaWhenInactive(false)
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
            .userAgentOverride(
                "Mozilla/5.0 (Linux; Android 6.0.1) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) GeckoView/134.0 Mobile Safari/537.36 NightPOS/1.0"
            )
            .build()
        return GeckoSession(settings)
    }
}
