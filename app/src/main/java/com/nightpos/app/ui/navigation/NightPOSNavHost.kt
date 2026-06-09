package com.nightpos.app.ui.navigation

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nightpos.app.AppContainer
import com.nightpos.app.R
import com.nightpos.app.ui.pos.PinLoginScreen
import com.nightpos.app.ui.pos.PinLoginViewModel
import com.nightpos.app.ui.pos.PosMainScreen
import com.nightpos.app.ui.screens.dashboard.DashboardAction
import com.nightpos.app.ui.screens.dashboard.DashboardScreen
import com.nightpos.app.ui.screens.dashboard.DashboardViewModel
import com.nightpos.app.ui.screens.settings.SettingsScreen
import com.nightpos.app.ui.screens.settings.SettingsViewModel
import com.nightpos.app.ui.screens.splash.SplashScreen
import com.nightpos.app.ui.screens.webview.WebViewScreen
import com.nightpos.app.ui.screens.webview.WebViewViewModel
import com.nightpos.app.twa.TwaLauncherActivity
import com.nightpos.app.util.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Composable
fun NightPOSNavHost(
    appContainer: AppContainer,
    sharedWebView: WebView,
    isOnline: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = appContainer.settingsViewModelFactory(),
    )
    val settingsState by settingsViewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = NightPOSDestination.Splash.route) {

        composable(NightPOSDestination.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(NightPOSDestination.PosLogin.route) {
                        popUpTo(NightPOSDestination.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(NightPOSDestination.Dashboard.route) {
            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = appContainer.dashboardViewModelFactory(),
            )
            val context = LocalContext.current
            val baseUrl = settingsState.serverUrl.ifBlank { Constants.DEFAULT_BASE_URL }

            fun launchTwa(url: String) {
                context.startActivity(TwaLauncherActivity.createIntent(context, url))
            }

            DashboardScreen(
                viewModel = dashboardViewModel,
                sharedWebView = sharedWebView,
                onAction = { action ->
                    when (action) {
                        DashboardAction.OpenNposHome -> launchTwa(Constants.nposHomeUrl(baseUrl))
                        DashboardAction.OpenPos -> launchTwa(Constants.openPosUrl(baseUrl))
                        DashboardAction.OpenReports -> navController.navigate(NightPOSDestination.PosMain.route)
                        DashboardAction.OpenCustomers -> launchTwa(Constants.customersUrl(baseUrl))
                        DashboardAction.OpenProducts -> launchTwa(Constants.productsUrl(baseUrl))
                        DashboardAction.OpenDiscountLoyalty -> launchTwa(Constants.discountLoyaltyUrl(baseUrl))
                        DashboardAction.OpenGiftCards -> launchTwa(Constants.giftCardsUrl(baseUrl))
                        DashboardAction.OpenSettings -> navController.navigate(NightPOSDestination.Settings.route)
                        DashboardAction.Logout -> Unit
                    }
                },
                onLoggedOut = { sharedWebView.loadUrl("about:blank") },
            )
        }

        composable(NightPOSDestination.PosLogin.route) {
            val baseUrl = settingsState.serverUrl.ifBlank { Constants.DEFAULT_BASE_URL }
            val prefs = appContainer.preferencesManager
            val db = runBlocking { prefs.odooDb.first() }
            val login = runBlocking { prefs.odooApiLogin.first() }
            val password = runBlocking { prefs.odooApiPassword.first() }

            val pinViewModel: PinLoginViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        PinLoginViewModel(
                            authRepository = appContainer.authRepository,
                            baseUrl = baseUrl,
                            db = db,
                            apiLogin = login,
                            apiPassword = password,
                        )
                    }
                },
            )

            var employeeName by remember { mutableStateOf("") }
            var isManager by remember { mutableStateOf(false) }

            PinLoginScreen(
                viewModel = pinViewModel,
                onLoginSuccess = { manager, name ->
                    employeeName = name
                    isManager = manager
                    navController.navigate(NightPOSDestination.Dashboard.route) {
                        popUpTo(NightPOSDestination.PosLogin.route) { inclusive = true }
                    }
                },
            )
        }

        composable(NightPOSDestination.PosMain.route) {
            val baseUrl = settingsState.serverUrl.ifBlank { Constants.DEFAULT_BASE_URL }
            // Employee context is passed via shared auth repository after successful PIN login
            val employee = appContainer.authRepository.lastEmployee
            PosMainScreen(
                employeeName = employee?.name ?: "",
                isManager = employee?.isManager ?: false,
                repository = appContainer.authRepository.makeRepository(),
                baseUrl = baseUrl,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = NightPOSDestination.WebViewDest.ROUTE_PATTERN,
            arguments = webViewNavArguments(),
        ) { backStackEntry ->
            val kind = WebViewKind.fromRouteArg(
                backStackEntry.arguments?.getString(NightPOSDestination.WebViewDest.ARG_KIND),
            )
            val webViewViewModel: WebViewViewModel = viewModel(
                key = "webview-${kind.routeArg}",
                factory = appContainer.webViewViewModelFactory(),
            )

            val title = when (kind) {
                WebViewKind.POS -> stringResource(R.string.menu_open_pos)
                WebViewKind.REPORTS -> stringResource(R.string.menu_reports)
                WebViewKind.CUSTOMERS -> stringResource(R.string.menu_customers)
            }
            val baseUrl = settingsState.serverUrl.ifBlank { Constants.DEFAULT_BASE_URL }
            val url = when (kind) {
                WebViewKind.POS -> Constants.openPosUrl(baseUrl)
                WebViewKind.REPORTS -> Constants.reportsUrl(baseUrl)
                WebViewKind.CUSTOMERS -> Constants.customersUrl(baseUrl)
            }

            WebViewScreen(
                kind = kind,
                title = title,
                url = url,
                webView = sharedWebView,
                viewModel = webViewViewModel,
                isOnline = isOnline,
                kioskModeEnabled = settingsState.kioskModeEnabled && kind == WebViewKind.POS,
                keepScreenOnEnabled = settingsState.keepScreenOnEnabled,
                onExit = {
                    navController.popBackStack(NightPOSDestination.Dashboard.route, inclusive = false)
                },
            )
        }

        composable(NightPOSDestination.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                sharedWebView = sharedWebView,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private fun webViewNavArguments(): List<NamedNavArgument> = listOf(
    navArgument(NightPOSDestination.WebViewDest.ARG_KIND) {
        type = NavType.StringType
        defaultValue = WebViewKind.POS.routeArg
    },
)
