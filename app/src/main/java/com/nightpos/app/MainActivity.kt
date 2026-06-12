package com.nightpos.app

import android.content.ComponentCallbacks2
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
    private var geckoSession: GeckoSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        appContainer = AppContainer(application)

        setContent {
            NightPOSTheme {
                val context = LocalContext.current

                // One GeckoView + one GeckoSession shared across all screens.
                // The session is opened lazily inside WebViewScreen so that content
                // processes are not spawned until GeckoView is actually shown.
                // (On Sunmi T1 / kernel 3.10, the POS opens in Firefox Custom Tabs
                // and GeckoView is never used, avoiding the SELinux IPC crash loop.)
                val sharedGeckoView = remember {
                    GeckoView(context).also { view ->
                        val session = GeckoSessionFactory.create()
                        view.setSession(session)
                        geckoSession = session
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    // Re-apply immersive mode on trim — low-memory dialogs (OOM dialog, etc.)
    // can cause the system bars to reappear and not re-hide automatically.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            Log.w("NightPOS/Memory", "MainActivity.onTrimMemory level=$level")
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        geckoSession?.close()
        geckoSession = null
        super.onDestroy()
    }
}
