package com.nightpos.geckoview.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nightpos.geckoview.AppContainer
import com.nightpos.geckoview.NightPOSApplication
import com.nightpos.geckoview.R
import com.nightpos.geckoview.ui.screens.dashboard.DashboardAction
import com.nightpos.geckoview.ui.screens.dashboard.DashboardScreen
import com.nightpos.geckoview.ui.screens.dashboard.DashboardViewModel
import com.nightpos.geckoview.ui.screens.login.LoginScreen
import com.nightpos.geckoview.ui.screens.login.LoginViewModel
import com.nightpos.geckoview.ui.screens.settings.SettingsScreen
import com.nightpos.geckoview.ui.screens.settings.SettingsViewModel
import com.nightpos.geckoview.ui.screens.splash.SplashScreen
import com.nightpos.geckoview.ui.screens.webview.WebViewScreen
import com.nightpos.geckoview.ui.screens.webview.WebViewViewModel
import com.nightpos.geckoview.twa.TwaLauncherActivity
import com.nightpos.geckoview.util.Constants
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoView

/**
 * Single-Activity navigation graph. A single shared [GeckoView] (backed by one
 * GeckoSession) is created in [com.nightpos.geckoview.MainActivity] and threaded through
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
            val isLoggedIn by appContainer.preferencesManager.isLoggedIn.collectAsState(initial = false)
            SplashScreen(
                onFinished = {
                    val target = if (isLoggedIn) NightPOSDestination.Dashboard.route else NightPOSDestination.Login.route
                    navController.navigate(target) {
                        popUpTo(NightPOSDestination.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(NightPOSDestination.Login.route) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = appContainer.loginViewModelFactory(),
            )
            val baseUrl = settingsState.serverUrl.ifBlank { Constants.DEFAULT_BASE_URL }

            LoginScreen(
                viewModel = loginViewModel,
                baseUrl = baseUrl,
                onLoginSuccess = {
                    // Re-load the npos backend so pos-configs.js can populate the
                    // "Open POS" outlet FAB now that the session cookie is set.
                    sharedGeckoView.session?.loadUri("$baseUrl/npos")
                    navController.navigate(NightPOSDestination.Dashboard.route) {
                        popUpTo(NightPOSDestination.Login.route) { inclusive = true }
                    }
                },
                onOpenSettings = { navController.navigate(NightPOSDestination.Settings.route) },
                onOpenPosOutlet = { url, name ->
                    navController.navigate(NightPOSDestination.OutletDest.routeFor(url, name))
                },
            )
        }

        composable(NightPOSDestination.Dashboard.route) {
            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = appContainer.dashboardViewModelFactory(),
            )
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
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
                        DashboardAction.OpenReports -> navController.navigate(
                            NightPOSDestination.WebViewDest.routeFor(WebViewKind.REPORTS)
                        )
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
                        DashboardAction.OpenPosSettings -> navController.navigate(
                            NightPOSDestination.WebViewDest.routeFor(WebViewKind.POS_SETTINGS)
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
                    coroutineScope.launch { appContainer.preferencesManager.setLoggedIn(false) }
                    navController.navigate(NightPOSDestination.Login.route) {
                        popUpTo(NightPOSDestination.Dashboard.route) { inclusive = true }
                    }
                },
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
                WebViewKind.POS_SETTINGS -> stringResource(R.string.menu_pos_settings)
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
                WebViewKind.POS_SETTINGS -> Constants.posSettingsUrl(baseUrl)
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
                onHome = {
                    navController.popBackStack(NightPOSDestination.Dashboard.route, inclusive = false)
                },
                onPrinter = {
                    navController.navigate(NightPOSDestination.WebViewDest.routeFor(WebViewKind.PRINTERS))
                },
                onOpenSettings = { navController.navigate(NightPOSDestination.Settings.route) },
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
                    // Reached from either Dashboard or Login — return to whichever
                    // screen pushed this outlet route.
                    navController.popBackStack()
                },
                onHome = {
                    navController.popBackStack()
                },
                onPrinter = {
                    navController.navigate(NightPOSDestination.WebViewDest.routeFor(WebViewKind.PRINTERS))
                },
                onOpenSettings = { navController.navigate(NightPOSDestination.Settings.route) },
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
