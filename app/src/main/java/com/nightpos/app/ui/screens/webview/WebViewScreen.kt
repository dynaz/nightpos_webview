package com.nightpos.app.ui.screens.webview

import android.app.Activity
import android.view.ViewGroup
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.nightpos.app.webview.GeckoNavigationDelegate
import com.nightpos.app.webview.GeckoProgressDelegate
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    kind: WebViewKind,
    title: String,
    url: String,
    geckoView: GeckoView,
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

    KeepScreenOn(enabled = keepScreenOnEnabled)
    KioskSystemBars(enabled = kioskModeEnabled)

    val session = geckoView.session

    // Wire up delegates once per screen composition
    val navigationDelegate = remember(kind) {
        GeckoNavigationDelegate(
            context = context,
            onPageStarted = { viewModel.onPageStarted() },
            onPageFinished = { viewModel.onPageFinished(false) },
            onBlockedDomain = { host -> viewModel.onBlockedDomain(host) },
        )
    }

    val progressDelegate = remember(kind) {
        GeckoProgressDelegate(
            onPageStarted = { viewModel.onPageStarted() },
            onProgressChanged = { progress -> viewModel.onProgressChanged(progress) },
            onPageStopped = { viewModel.onPageFinished(false) },
        )
    }

    DisposableEffect(session, navigationDelegate, progressDelegate) {
        session?.navigationDelegate = navigationDelegate
        session?.progressDelegate = progressDelegate
        onDispose {
            session?.navigationDelegate = null
            session?.progressDelegate = null
        }
    }

    // Load URL when it changes (skip if already on this URL)
    LaunchedEffect(url) {
        val current = runCatching {
            // GeckoSession doesn't expose currentUrl directly; use a flag in ViewModel
            uiState.currentUrl
        }.getOrNull()
        if (current?.substringBefore('#') != url.substringBefore('#')) {
            session?.loadUri(url)
            viewModel.setCurrentUrl(url)
        }
    }

    BackHandler(enabled = true) {
        // GeckoSession.canGoBack is async; use ViewModel's tracked state
        if (uiState.canGoBack) {
            session?.goBack()
        } else {
            viewModel.requestExit()
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
                            if (uiState.canGoBack) session?.goBack() else viewModel.requestExit()
                        },
                        onReload = { session?.reload() },
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
                    !isOnline -> OfflineScreen(onRetry = { session?.reload() })

                    else -> {
                        PullToRefreshBox(
                            isRefreshing = uiState.isLoading && uiState.loadProgress in 1..40,
                            onRefresh = { session?.reload() },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            AndroidView(
                                factory = {
                                    (geckoView.parent as? ViewGroup)?.removeView(geckoView)
                                    geckoView
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                if (uiState.isLoading && isOnline) {
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

    if (uiState.showExitConfirmation) {
        ExitConfirmationDialog(
            kioskMode = kioskModeEnabled,
            onConfirm = { viewModel.dismissExit(); onExit() },
            onDismiss = { viewModel.dismissExit() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebViewTopBar(title: String, onBack: () -> Unit, onReload: () -> Unit) {
    TopAppBar(
        title = { Text(title, color = MaterialTheme.colorScheme.onBackground) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        actions = {
            IconButton(onClick = onReload) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.action_reload),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = NightBlack),
    )
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
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = (view.context as? Activity)?.window
        if (enabled) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

@Composable
private fun KioskSystemBars(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = (view.context as? Activity)?.window
        if (window != null) applyImmersiveMode(window, enabled)
        onDispose { if (window != null) applyImmersiveMode(window, false) }
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
