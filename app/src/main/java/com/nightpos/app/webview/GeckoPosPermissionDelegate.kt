package com.nightpos.app.webview

import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

/**
 * Auto-grants all permissions required by the Odoo POS web app.
 *
 * This is an internal POS device on a private network — there is no need to
 * prompt the user for every web permission. Granting everything up-front ensures:
 *  - Service Workers can register and cache offline data (PERMISSION_PERSISTENT_STORAGE)
 *  - IndexedDB & Cache Storage quotas are not restricted (PERMISSION_LOCAL_STORAGE)
 *  - Camera works for barcode scanning without a per-origin prompt
 *  - Geolocation works for any delivery/table map features
 *  - Autoplay is allowed so KDS alert sounds are never blocked
 *  - Media (camera/mic) streams are available for video-call features
 */
class GeckoPosPermissionDelegate : GeckoSession.PermissionDelegate {

    /** Grant every content permission without prompting the user. */
    override fun onContentPermissionRequest(
        session: GeckoSession,
        perm: GeckoSession.PermissionDelegate.ContentPermission,
    ): GeckoResult<Int> =
        GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)

    /**
     * Android-level permissions (camera, microphone, location) are already
     * declared in the manifest and requested by the Activity. Tell GeckoView's
     * web layer that they are granted so it doesn't ask again per-origin.
     */
    override fun onAndroidPermissionsRequest(
        session: GeckoSession,
        permissions: Array<out String>?,
        callback: GeckoSession.PermissionDelegate.Callback,
    ) = callback.grant()

    /**
     * Auto-select first available camera and microphone — used for barcode
     * scanning via <input capture> or getUserMedia().
     */
    override fun onMediaPermissionRequest(
        session: GeckoSession,
        uri: String,
        video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
        audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
        response: GeckoSession.PermissionDelegate.MediaCallback,
    ) = response.grant(video?.firstOrNull(), audio?.firstOrNull())
}
