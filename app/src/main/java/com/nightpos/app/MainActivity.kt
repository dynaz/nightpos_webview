package com.nightpos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nightpos.app.ui.navigation.NightPOSNavHost
import com.nightpos.app.ui.screens.settings.SettingsUiState
import com.nightpos.app.ui.theme.NightPOSTheme
import com.nightpos.app.util.AutoReopenPosEffect
import com.nightpos.app.webview.WebViewFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Single Activity hosting the entire NightPOS UI (Single-Activity Architecture).
 *
 * Owns the one shared [android.webkit.WebView] instance for the whole app —
 * created once here, configured via [WebViewFactory], and threaded through the
 * navigation graph so the Odoo SPA's session survives screen switches and so
 * logout has a single place to clear from.
 */
class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() / setContent.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appContainer = AppContainer(application)

        setContent {
            NightPOSTheme {
                val context = LocalContext.current
                val sharedWebView = remember { WebViewFactory.create(context) }
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()

                val isOnline by appContainer.connectivityObserver.isOnline
                    .distinctUntilChanged()
                    .collectAsState(initial = true)

                val settingsState by produceState(initialValue = SettingsUiState()) {
                    val prefs = appContainer.preferencesManager
                    combine(
                        prefs.serverUrl,
                        prefs.kioskModeEnabled,
                        prefs.keepScreenOnEnabled,
                        prefs.autoReopenPosEnabled,
                    ) { url, kiosk, keepOn, autoReopen ->
                        SettingsUiState(
                            serverUrl = url,
                            kioskModeEnabled = kiosk,
                            keepScreenOnEnabled = keepOn,
                            autoReopenPosEnabled = autoReopen,
                        )
                    }.collect { value = it }
                }

                AutoReopenPosEffect(
                    navController = navController,
                    kioskModeEnabled = settingsState.kioskModeEnabled,
                    autoReopenEnabled = settingsState.autoReopenPosEnabled,
                    currentRoute = backStackEntry?.destination?.route,
                )

                NightPOSNavHost(
                    appContainer = appContainer,
                    sharedWebView = sharedWebView,
                    isOnline = isOnline,
                    navController = navController,
                )
            }
        }
    }
}
