package com.nightpos.app

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.nightpos.app.data.NetworkConnectivityObserver
import com.nightpos.app.data.PreferencesManager
import com.nightpos.app.data.SessionManager
import com.nightpos.app.ui.screens.dashboard.DashboardViewModel
import com.nightpos.app.ui.screens.settings.SettingsViewModel
import com.nightpos.app.ui.screens.webview.WebViewViewModel

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val preferencesManager: PreferencesManager by lazy { PreferencesManager(appContext) }
    val sessionManager: SessionManager by lazy { SessionManager() }
    val connectivityObserver: NetworkConnectivityObserver by lazy { NetworkConnectivityObserver(appContext) }

    fun dashboardViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { DashboardViewModel(sessionManager) }
    }

    fun settingsViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { SettingsViewModel(preferencesManager, sessionManager) }
    }

    fun webViewViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { WebViewViewModel() }
    }
}
