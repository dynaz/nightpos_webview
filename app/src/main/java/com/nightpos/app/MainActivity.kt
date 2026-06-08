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
import com.nightpos.app.print.SunmiJsBridge
import com.nightpos.app.ui.navigation.NightPOSNavHost
import com.nightpos.app.ui.screens.settings.SettingsUiState
import com.nightpos.app.ui.theme.NightPOSTheme
import com.nightpos.app.util.AutoReopenPosEffect
import com.nightpos.app.webview.GeckoSessionFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer
    private var sunmiJsBridge: SunmiJsBridge? = null
    private var geckoSession: GeckoSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appContainer = AppContainer(application)

        setContent {
            NightPOSTheme {
                val context = LocalContext.current

                // One GeckoView + one GeckoSession shared across all screens so the
                // Odoo SPA session survives navigation between Dashboard tabs.
                val sharedGeckoView = remember {
                    GeckoView(context).also { view ->
                        val session = GeckoSessionFactory.create()
                        val bridge = SunmiJsBridge(context)
                        bridge.bindPrinter()
                        session.promptDelegate = bridge.geckoPromptDelegate
                        session.open(NightPOSApplication.geckoRuntime)
                        view.setSession(session)
                        geckoSession = session
                        sunmiJsBridge = bridge
                    }
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
                    isOnline = isOnline,
                    navController = navController,
                )
            }
        }
    }

    override fun onDestroy() {
        sunmiJsBridge?.unbindPrinter()
        sunmiJsBridge = null
        geckoSession?.close()
        geckoSession = null
        super.onDestroy()
    }
}
