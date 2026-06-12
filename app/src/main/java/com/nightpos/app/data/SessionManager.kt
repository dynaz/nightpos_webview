package com.nightpos.app.data

import com.nightpos.app.webview.GeckoRuntimeHolder
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.StorageController

/**
 * Centralizes logout: clears GeckoRuntime storage (cookies, cache, DOM storage)
 * and resets the shared session to a clean state so the next user starts fresh.
 */
class SessionManager {

    suspend fun clearSession(session: GeckoSession?) {
        runCatching {
            GeckoRuntimeHolder.runtime.storageController.clearData(
                StorageController.ClearFlags.ALL
            )
        }
        session?.loadUri("about:blank")
    }
}
