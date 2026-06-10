package com.nightpos.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.nightpos.app.NightPOSApplication
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
import org.mozilla.geckoview.GeckoView

/**
 * Single-Activity navigation graph. A single shared [GeckoView] (backed by one
 * GeckoSession) is created in [com.nightpos.app.MainActivity] and threaded through
 * every screen that needs it so the Odoo SPA session survives tab switches.
 */
@Composable
fun NightPOSNavHost(
    appContainer: AppContainer,
    sharedGeckoView: GeckoView,
    isOnline: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = appContainer.settingsViewModelFactory(),
    )
    val settingsState by settingsViewModel.uiState.collectAsState()

    // ── Pre-fetch POS outlet configs on startup ───────────────────────────────
    // Open the GeckoSession and load the server base URL as soon as we know it.
    // pos-configs.js fires at document_end and sends outlet names (SOHO Club,
    // AfroRoom, …) back via window.prompt("nightpos:posConfigs", …) so the
    // "Open POS" FAB circles are populated before the user opens any WebView.
    LaunchedEffect(settingsState.serverUrl) {
        val session = sharedGeckoView.session ?: run {
            android.util.Log.w("NightPOS", "prefetch: session is null, skipping")
            return@LaunchedEffect
        }
        val baseUrl = settingsState.serverUrl.ifBlank { Constants.DEFAULT_BASE_URL }
        android.util.Log.i("NightPOS", "prefetch: opening session and loading $baseUrl/npos")
        // Ensure the prompt delegate is wired so posConfigs messages are received
        session.promptDelegate = NightPOSApplication.jsBridge.geckoPromptDelegate
        if (!session.isOpen) {
            session.open(NightPOSApplication.geckoRuntime)
        }
        // A lightweight page load — pos-configs.js content script will fetch
        // /web/dataset/call_kw, parse pos.config records and emit to the bridge
        session.loadUri("$baseUrl/npos")
    }

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
                sharedGeckoView = sharedGeckoView,
                baseUrl = baseUrl,
                onAction = { action ->
                    when (action) {
                        DashboardAction.OpenNposHome -> launchTwa(Constants.nposHomeUrl(baseUrl))
                        DashboardAction.OpenPos -> navController.navigate(
                            NightPOSDestination.WebViewDest.routeFor(WebViewKind.POS)
                        )
                        DashboardAction.OpenReports -> navController.navigate(NightPOSDestination.PosMain.route)
                        DashboardAction.OpenCustomers -> navController.navigate(
                            NightPOSDestination.WebViewDest.routeFor(WebViewKind.CUSTOMERS)
                        )
                        DashboardAction.OpenProducts -> navController.navigate(
                            NightPOSDestination.WebViewDest.routeFor(WebViewKind.PRODUCTS)
                        )
                        DashboardAction.OpenDiscountLoyalty -> navController.navigate(
                            NightPOSDestination.WebViewDest.routeFor(WebViewKind.DISCOUNT_LOYALTY)
                        )
                        DashboardAction.OpenGiftCards -> navController.navigate(
                            NightPOSDestination.WebViewDest.routeFor(WebViewKind.GIFT_CARDS)
                        )
                        DashboardAction.OpenEmployees -> navController.navigate(
                            NightPOSDestination.WebViewDest.routeFor(WebViewKind.EMPLOYEES)
                        )
                        DashboardAction.OpenPrinters -> navController.navigate(
                            NightPOSDestination.WebViewDest.routeFor(WebViewKind.PRINTERS)
                        )
                        DashboardAction.OpenSettings -> navController.navigate(NightPOSDestination.Settings.route)
                        DashboardAction.Logout -> Unit
                        is DashboardAction.OpenPosOutlet -> navController.navigate(
                            NightPOSDestination.OutletDest.routeFor(action.url, action.name)
                        )
                    }
                },
                onLoggedOut = {
                    sharedGeckoView.session?.loadUri("about:blank")
                },
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
                WebViewKind.PRODUCTS -> stringResource(R.string.menu_products)
                WebViewKind.DISCOUNT_LOYALTY -> stringResource(R.string.menu_discount_loyalty)
                WebViewKind.GIFT_CARDS -> stringResource(R.string.menu_gift_cards)
                WebViewKind.EMPLOYEES -> stringResource(R.string.menu_employees)
                WebViewKind.PRINTERS -> stringResource(R.string.menu_printers)
            }
            val baseUrl = settingsState.serverUrl.ifBlank { Constants.DEFAULT_BASE_URL }
            val url = when (kind) {
                WebViewKind.POS -> Constants.openPosUrl(baseUrl)
                WebViewKind.REPORTS -> Constants.reportsUrl(baseUrl)
                WebViewKind.CUSTOMERS -> Constants.customersUrl(baseUrl)
                WebViewKind.PRODUCTS -> Constants.productsUrl(baseUrl)
                WebViewKind.DISCOUNT_LOYALTY -> Constants.discountLoyaltyUrl(baseUrl)
                WebViewKind.GIFT_CARDS -> Constants.giftCardsUrl(baseUrl)
                WebViewKind.EMPLOYEES -> Constants.employeesUrl(baseUrl)
                WebViewKind.PRINTERS -> Constants.printersUrl(baseUrl)
            }

            WebViewScreen(
                kind = kind,
                title = title,
                url = url,
                geckoView = sharedGeckoView,
                viewModel = webViewViewModel,
                isOnline = isOnline,
                kioskModeEnabled = settingsState.kioskModeEnabled && kind == WebViewKind.POS,
                keepScreenOnEnabled = settingsState.keepScreenOnEnabled,
                onExit = {
                    navController.popBackStack(NightPOSDestination.Dashboard.route, inclusive = false)
                },
            )
        }

        // Outlet WebView — custom URL passed directly (e.g. specific POS outlet)
        composable(
            route = NightPOSDestination.OutletDest.ROUTE_PATTERN,
            arguments = listOf(
                navArgument(NightPOSDestination.OutletDest.ARG_URL) { type = NavType.StringType },
                navArgument(NightPOSDestination.OutletDest.ARG_TITLE) {
                    type = NavType.StringType
                    defaultValue = "POS"
                },
            ),
        ) { backStackEntry ->
            val outletUrl = backStackEntry.arguments?.getString(NightPOSDestination.OutletDest.ARG_URL) ?: ""
            val outletTitle = backStackEntry.arguments?.getString(NightPOSDestination.OutletDest.ARG_TITLE) ?: "POS"
            val outletViewModel: WebViewViewModel = viewModel(
                key = "outlet-${outletUrl.hashCode()}",
                factory = appContainer.webViewViewModelFactory(),
            )
            WebViewScreen(
                kind = WebViewKind.POS,
                title = outletTitle,
                url = outletUrl,
                geckoView = sharedGeckoView,
                viewModel = outletViewModel,
                isOnline = isOnline,
                kioskModeEnabled = false,
                keepScreenOnEnabled = settingsState.keepScreenOnEnabled,
                onExit = {
                    navController.popBackStack(NightPOSDestination.Dashboard.route, inclusive = false)
                },
            )
        }

        composable(NightPOSDestination.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                sharedGeckoView = sharedGeckoView,
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
