package com.nightpos.geckoview.data

import com.nightpos.geckoview.NightPOSApplication
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.StorageController

/**
 * Centralizes logout: clears GeckoRuntime storage (cookies, cache, DOM storage)
 * and resets the shared session to a clean state so the next user starts fresh.
 */
class SessionManager {

    suspend fun clearSession(session: GeckoSession?) {
        runCatching {
            NightPOSApplication.geckoRuntime.storageController.clearData(
                StorageController.ClearFlags.ALL
            )
        }
        session?.loadUri("about:blank")
    }
}
