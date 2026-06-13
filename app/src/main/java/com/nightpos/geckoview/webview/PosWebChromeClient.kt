package com.nightpos.geckoview.webview

import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Implements all the "rich" browser behaviours a modern Odoo POS UI needs that
 * plain [android.webkit.WebViewClient] doesn't cover:
 *
 *  - File uploads (`<input type="file">`, e.g. attaching images to products/customers)
 *  - In-page camera/microphone permission prompts (`getUserMedia`, barcode scanning)
 *  - Geolocation prompts
 *  - Fullscreen video / custom views
 *  - `target="_blank"` / `window.open()` popups (multi-window support)
 *  - JS alert/confirm dialogs routed through Compose-friendly callbacks
 *  - Page load progress
 *
 * Like [PosWebViewClient], every Android-callback is forwarded through plain
 * lambdas so the hosting Composable/Activity owns all UI decisions (dialogs,
 * permission requests, fullscreen container, etc).
 */
class PosWebChromeClient(
    private val onProgressChanged: (progress: Int) -> Unit,
    private val onShowFileChooser: (
        filePathCallback: ValueCallback<Array<android.net.Uri>>,
        fileChooserParams: FileChooserParams,
    ) -> Boolean,
    private val onGeolocationPermissionRequest: (
        origin: String,
        callback: GeolocationPermissions.Callback,
    ) -> Unit,
    private val onPermissionRequest: (request: PermissionRequest) -> Unit,
    private val onShowCustomView: (view: View, callback: CustomViewCallback) -> Unit,
    private val onHideCustomView: () -> Unit,
    private val onCreateWindow: (
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message,
    ) -> Boolean,
    private val onCloseWindow: (window: WebView) -> Unit,
    private val onJsAlert: (
        url: String?,
        message: String?,
        result: JsResult,
    ) -> Boolean,
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<android.net.Uri>>,
        fileChooserParams: FileChooserParams,
    ): Boolean = onShowFileChooser.invoke(filePathCallback, fileChooserParams)

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback,
    ) {
        onGeolocationPermissionRequest(origin, callback)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        onPermissionRequest.invoke(request)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        onShowCustomView.invoke(view, callback)
    }

    override fun onHideCustomView() {
        onHideCustomView.invoke()
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message,
    ): Boolean = onCreateWindow.invoke(view, isDialog, isUserGesture, resultMsg)

    override fun onCloseWindow(window: WebView) {
        onCloseWindow.invoke(window)
    }

    override fun onJsAlert(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
        return onJsAlert.invoke(url, message, result)
    }
}
