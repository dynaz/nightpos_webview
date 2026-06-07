package com.nightpos.app.ui.screens.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nightpos.app.R
import com.nightpos.app.ui.navigation.WebViewKind
import com.nightpos.app.ui.screens.offline.OfflineScreen
import com.nightpos.app.ui.theme.ErrorRed
import com.nightpos.app.ui.theme.NeonPurple
import com.nightpos.app.ui.theme.NightBlack
import com.nightpos.app.webview.PosDownloadListener
import com.nightpos.app.webview.PosWebChromeClient
import com.nightpos.app.webview.PosWebViewClient
import com.nightpos.app.webview.WebViewFactory

/**
 * Hosts the shared [WebView] for "เปิดขาย / รายงาน / ลูกค้า" (POS / Reports / Customers).
 *
 * Responsibilities (per spec section 4 & 10):
 *  - Load [url] into the shared WebView (only once per navigation, to preserve SPA state).
 *  - Wire up [PosWebViewClient] / [PosWebChromeClient] / [PosDownloadListener] for full
 *    JS, storage, cookies, file upload, camera, geolocation, fullscreen, multi-window,
 *    download and SSL-error support.
 *  - Pull-to-refresh, offline screen with auto-reconnect, and load-error retry.
 *  - Kiosk-mode behaviours: hide system bars, intercept back button with confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    kind: WebViewKind,
    title: String,
    url: String,
    webView: WebView,
    viewModel: WebViewViewModel,
    isOnline: Boolean,
    kioskModeEnabled: Boolean,
    keepScreenOnEnabled: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(kind, title) { viewModel.initialize(kind, title) }

    // --- Transient Android-framework callbacks (kept out of the ViewModel; see its KDoc) ---
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingSslError by remember { mutableStateOf<Pair<SslErrorHandler, SslError>?>(null) }
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var fullscreenCallback by remember { mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }
    var popupWebView by remember { mutableStateOf<WebView?>(null) }

    // --- System UI (kiosk mode) ---
    KeepScreenOn(enabled = keepScreenOnEnabled)
    KioskSystemBars(enabled = kioskModeEnabled || fullscreenView != null)

    // --- File chooser launcher (file upload support) ---
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        val uris: Array<Uri> = when {
            result.resultCode != Activity.RESULT_OK || data == null -> emptyArray()
            data.clipData != null -> Array(data.clipData!!.itemCount) { i -> data.clipData!!.getItemAt(i).uri }
            data.data != null -> arrayOf(data.data!!)
            else -> emptyArray()
        }
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    // --- Runtime permission launcher (camera / location for getUserMedia & geolocation) ---
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingGeoRequest by remember { mutableStateOf<Pair<String, GeolocationPermissions.Callback>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        pendingPermissionRequest?.let { request ->
            val allGranted = granted.values.all { it }
            if (allGranted) request.grant(request.resources) else request.deny()
        }
        pendingPermissionRequest = null

        pendingGeoRequest?.let { (origin, callback) ->
            val fineLocationGranted = granted[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                granted[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
            callback.invoke(origin, fineLocationGranted, false)
        }
        pendingGeoRequest = null
    }

    // --- Configure clients once; callbacks reference mutable state via closures ---
    val webViewClient = remember(kind) {
        PosWebViewClient(
            onPageStarted = { viewModel.onPageStarted() },
            onPageFinished = { viewModel.onPageFinished(webView.canGoBack()) },
            onReceivedError = { code, description, failingUrl ->
                viewModel.onPageError(code, description, failingUrl)
            },
            onSslError = { handler, error -> pendingSslError = handler to error },
            onBlockedDomain = { host -> viewModel.onBlockedDomain(host) },
        )
    }

    val chromeClient = remember(kind) {
        PosWebChromeClient(
            onProgressChanged = { progress -> viewModel.onProgressChanged(progress) },
            onShowFileChooser = { callback, _ ->
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                runCatching {
                    val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                        addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    fileChooserLauncher.launch(
                        android.content.Intent.createChooser(intent, null),
                    )
                }.onFailure {
                    filePathCallback = null
                }
                true
            },
            onGeolocationPermissionRequest = { origin, callback ->
                val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_FINE_LOCATION,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (hasFine) {
                    callback.invoke(origin, true, false)
                } else {
                    pendingGeoRequest = origin to callback
                    permissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }
            },
            onPermissionRequest = { request ->
                val androidPermissions = request.resources.mapNotNull {
                    when (it) {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> android.Manifest.permission.CAMERA
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> android.Manifest.permission.RECORD_AUDIO
                        else -> null
                    }
                }.distinct()

                val allGranted = androidPermissions.all {
                    androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                }

                if (androidPermissions.isEmpty() || allGranted) {
                    request.grant(request.resources)
                } else {
                    pendingPermissionRequest = request
                    permissionLauncher.launch(androidPermissions.toTypedArray())
                }
            },
            onShowCustomView = { view, callback ->
                fullscreenView = view
                fullscreenCallback = callback
            },
            onHideCustomView = {
                fullscreenView = null
                fullscreenCallback?.onCustomViewHidden()
                fullscreenCallback = null
            },
            onCreateWindow = { _, _, _, resultMsg ->
                val newWebView = WebView(context).apply {
                    WebViewFactory.configure(this)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
                popupWebView = newWebView
                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()
                true
            },
            onCloseWindow = { popupWebView = null },
            onJsAlert = { _, message, result ->
                // Default JS alert handling: acknowledge so the page doesn't hang.
                result.confirm()
                message != null
            },
        )
    }

    // Attach clients & download listener once; load URL only when it actually changes
    // so switching dashboard tabs doesn't reload an already-open SPA route.
    DisposableEffect(webView, webViewClient, chromeClient) {
        webView.webViewClient = webViewClient
        webView.webChromeClient = chromeClient
        webView.setDownloadListener(PosDownloadListener(context))
        onDispose { }
    }

    LaunchedEffect(url) {
        if (webView.url?.substringBefore('#') != url.substringBefore('#')) {
            webView.loadUrl(url)
        } else {
            // Already showing this URL (e.g. returning from another tab) — reflect current state.
            viewModel.onPageFinished(webView.canGoBack())
        }
    }

    // --- Hardware back button: in-app navigation first, then exit confirmation (kiosk-aware) ---
    BackHandler(enabled = true) {
        when {
            fullscreenView != null -> fullscreenCallback?.onCustomViewHidden()
            popupWebView != null -> popupWebView = null
            webView.canGoBack() -> webView.goBack()
            else -> viewModel.requestExit()
        }
    }

    LaunchedEffect(uiState.blockedDomainMessage) {
        uiState.blockedDomainMessage?.let { host ->
            snackbarHostState.showSnackbar(
                context.getString(R.string.blocked_domain_message, host),
            )
            viewModel.consumeBlockedDomainMessage()
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = NightBlack) {
        Scaffold(
            containerColor = NightBlack,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!kioskModeEnabled) {
                    WebViewTopBar(
                        title = uiState.title,
                        onBack = {
                            if (webView.canGoBack()) webView.goBack() else viewModel.requestExit()
                        },
                        onReload = { webView.reload() },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    !isOnline -> OfflineScreen(onRetry = { webView.reload() })

                    uiState.pageError != null -> WebPageErrorView(
                        error = uiState.pageError!!,
                        onRetry = {
                            viewModel.retry()
                            webView.reload()
                        },
                    )

                    else -> {
                        PullToRefreshBox(
                            isRefreshing = uiState.isLoading && uiState.loadProgress in 1..40,
                            onRefresh = { webView.reload() },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            AndroidView(
                                factory = {
                                    (webView.parent as? ViewGroup)?.removeView(webView)
                                    webView
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                if (uiState.isLoading && uiState.pageError == null && isOnline) {
                    LinearProgressIndicator(
                        progress = { uiState.loadProgress / 100f },
                        color = NeonPurple,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                    )
                }
            }
        }
    }

    // --- Dialogs & overlays ---
    if (uiState.showExitConfirmation) {
        ExitConfirmationDialog(
            kioskMode = kioskModeEnabled,
            onConfirm = { viewModel.dismissExit(); onExit() },
            onDismiss = { viewModel.dismissExit() },
        )
    }

    pendingSslError?.let { (handler, error) ->
        SslErrorDialog(
            error = error,
            onProceed = { handler.proceed(); pendingSslError = null },
            onCancel = { handler.cancel(); pendingSslError = null },
        )
    }

    fullscreenView?.let { view ->
        FullscreenVideoOverlay(view = view)
    }

    popupWebView?.let { popup ->
        PopupWindowDialog(
            webView = popup,
            onDismiss = {
                popup.destroy()
                popupWebView = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebViewTopBar(
    title: String,
    onBack: () -> Unit,
    onReload: () -> Unit,
) {
    TopAppBar(
        title = { Text(title, color = MaterialTheme.colorScheme.onBackground) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        actions = {
            IconButton(onClick = onReload) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.action_reload),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = NightBlack),
    )
}

@Composable
private fun WebPageErrorView(error: PageError, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = error.description ?: "เกิดข้อผิดพลาดในการโหลดหน้า",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        error.failingUrl?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.size(24.dp))
        TextButton(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = NeonPurple)
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.offline_retry), color = NeonPurple)
        }
    }
}

@Composable
private fun ExitConfirmationDialog(kioskMode: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (kioskMode) stringResource(R.string.kiosk_exit_title) else stringResource(R.string.webview_exit_pos_title)) },
        text = { Text(if (kioskMode) stringResource(R.string.kiosk_exit_message) else stringResource(R.string.webview_exit_pos_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    if (kioskMode) stringResource(R.string.kiosk_exit_confirm) else stringResource(R.string.webview_exit_confirm),
                    color = ErrorRed,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (kioskMode) stringResource(R.string.kiosk_exit_cancel) else stringResource(R.string.webview_exit_cancel))
            }
        },
    )
}

@Composable
private fun SslErrorDialog(error: SslError, onProceed: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.ssl_error_title)) },
        text = {
            Column {
                Text(stringResource(R.string.ssl_error_message))
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = error.url ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.ssl_error_cancel)) }
        },
        dismissButton = {
            TextButton(onClick = onProceed) {
                Text(stringResource(R.string.ssl_error_continue), color = ErrorRed)
            }
        },
    )
}

/** Hosts fullscreen `<video>` / custom view content (e.g. media playback) above everything else. */
@Composable
private fun FullscreenVideoOverlay(view: View) {
    Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Black) {
        AndroidView(
            factory = {
                FrameLayout(it).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    (view.parent as? ViewGroup)?.removeView(view)
                    addView(view)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Hosts `window.open()` / `target="_blank"` popups (multi-window support). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PopupWindowDialog(webView: WebView, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NightBlack,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("หน้าต่างใหม่", color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_close),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NightBlack),
                )
                AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** Keeps the screen awake while the POS is open (configurable in Settings). */
@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = (view.context as? Activity)?.window
        if (enabled) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/** Hides system status/navigation bars when kiosk mode (or fullscreen video) is active. */
@Composable
private fun KioskSystemBars(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            applyImmersiveMode(window, enabled)
        }
        onDispose {
            if (window != null) applyImmersiveMode(window, false)
        }
    }
}

private fun applyImmersiveMode(window: Window, hide: Boolean) {
    val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
    if (hide) {
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
    }
}
