package com.nightpos.geckoview.webview

import org.mozilla.geckoview.GeckoSession

class GeckoProgressDelegate(
    private val onPageStarted: (url: String?) -> Unit,
    private val onProgressChanged: (Int) -> Unit,
    private val onPageStopped: () -> Unit,
) : GeckoSession.ProgressDelegate {

    override fun onPageStart(session: GeckoSession, url: String) {
        onPageStarted(url)
        onProgressChanged(10)
    }

    override fun onPageStop(session: GeckoSession, success: Boolean) {
        onProgressChanged(100)
        onPageStopped()
    }

    override fun onProgressChange(session: GeckoSession, progress: Int) {
        onProgressChanged(progress)
    }

    override fun onSecurityChange(
        session: GeckoSession,
        securityInfo: GeckoSession.ProgressDelegate.SecurityInformation,
    ) = Unit
}
