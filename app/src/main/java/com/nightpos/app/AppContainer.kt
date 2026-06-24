package com.nightpos.app

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.nightpos.app.data.NetworkConnectivityObserver
import com.nightpos.app.data.PreferencesManager
import com.nightpos.app.data.SessionManager
import com.nightpos.app.ui.screens.dashboard.DashboardViewModel
import com.nightpos.app.ui.screens.login.LoginViewModel
import com.nightpos.app.ui.screens.settings.SettingsViewModel
import com.nightpos.app.ui.screens.webview.WebViewViewModel

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val preferencesManager: PreferencesManager by lazy { PreferencesManager(appContext) }
    val sessionManager: SessionManager by lazy { SessionManager() }
    val connectivityObserver: NetworkConnectivityObserver by lazy { NetworkConnectivityObserver(appContext) }

    fun loginViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { LoginViewModel(preferencesManager) }
    }

    fun dashboardViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        // Use the singleton jsBridge from Application so the posConfigs StateFlow
        // is the same instance that the GeckoView prompt delegate writes to.
        initializer { DashboardViewModel(sessionManager, NightPOSApplication.jsBridge.posConfigs) }
    }

    fun settingsViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { SettingsViewModel(preferencesManager, sessionManager) }
    }

    fun webViewViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { WebViewViewModel() }
    }
}
