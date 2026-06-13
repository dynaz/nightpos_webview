package com.nightpos.geckoview

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.nightpos.geckoview.data.NetworkConnectivityObserver
import com.nightpos.geckoview.data.OdooAuthClient
import com.nightpos.geckoview.data.PreferencesManager
import com.nightpos.geckoview.data.SessionManager
import com.nightpos.geckoview.ui.screens.dashboard.DashboardViewModel
import com.nightpos.geckoview.ui.screens.login.LoginViewModel
import com.nightpos.geckoview.ui.screens.settings.SettingsViewModel
import com.nightpos.geckoview.ui.screens.webview.WebViewViewModel

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val preferencesManager: PreferencesManager by lazy { PreferencesManager(appContext) }
    val sessionManager: SessionManager by lazy { SessionManager() }
    val connectivityObserver: NetworkConnectivityObserver by lazy { NetworkConnectivityObserver(appContext) }
    val odooAuthClient: OdooAuthClient by lazy { OdooAuthClient() }

    fun dashboardViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        // Use the singleton jsBridge from Application so the posConfigs StateFlow
        // is the same instance that the GeckoView prompt delegate writes to.
        initializer { DashboardViewModel(sessionManager, NightPOSApplication.jsBridge.posConfigs, preferencesManager) }
    }

    fun settingsViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { SettingsViewModel(preferencesManager, sessionManager) }
    }

    fun webViewViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { WebViewViewModel() }
    }

    fun loginViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            LoginViewModel(odooAuthClient, preferencesManager, NightPOSApplication.jsBridge.posConfigs)
        }
    }
}
