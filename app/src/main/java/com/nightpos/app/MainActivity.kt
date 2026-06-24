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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nightpos.app.ui.navigation.NightPOSNavHost
import com.nightpos.app.ui.screens.settings.SettingsUiState
import com.nightpos.app.ui.theme.NightPOSTheme
import com.nightpos.app.util.AutoReopenPosEffect
import com.nightpos.app.webview.GeckoSessionFactory
import com.nightpos.app.webview.WebViewConfigurator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer
    private var geckoSession: GeckoSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appContainer = AppContainer(application)

        setContent {
            NightPOSTheme {
                val context = LocalContext.current

                // GeckoView flavors (arm32, arm64): one shared GeckoView + GeckoSession.
                // d2splus flavor: GeckoView crashes on Rockchip RK30; use system WebView instead.
                val sharedGeckoView: GeckoView? = remember {
                    if (BuildConfig.USE_GECKO) {
                        GeckoView(context).also { view ->
                            val session = GeckoSessionFactory.create()
                            view.setSession(session)
                            geckoSession = session
                        }
                    } else null
                }

                val sharedSystemWebView: android.webkit.WebView? = remember {
                    if (!BuildConfig.USE_GECKO) {
                        android.webkit.WebView(context).also { wv ->
                            WebViewConfigurator.configure(wv)
                            wv.addJavascriptInterface(NightPOSApplication.jsBridge, "NightPOSBridge")
                        }
                    } else null
                }

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
                    sharedGeckoView = sharedGeckoView,
                    sharedSystemWebView = sharedSystemWebView,
                    isOnline = isOnline,
                    navController = navController,
                )
            }
        }
    }

    override fun onDestroy() {
        geckoSession?.close()
        geckoSession = null
        super.onDestroy()
    }
}
