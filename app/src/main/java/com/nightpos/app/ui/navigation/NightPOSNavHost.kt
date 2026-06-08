package com.nightpos.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.nightpos.app.AppContainer
import com.nightpos.app.R
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

    NavHost(navController = navController, startDestination = NightPOSDestination.Splash.route) {

        composable(NightPOSDestination.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(NightPOSDestination.Dashboard.route) {
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
                onAction = { action ->
                    when (action) {
                        DashboardAction.OpenNposHome -> launchTwa(Constants.nposHomeUrl(baseUrl))
                        DashboardAction.OpenPos -> navController.navigate(
                            NightPOSDestination.WebViewDest.routeFor(WebViewKind.POS)
                        )
                        DashboardAction.OpenReports -> launchTwa(Constants.reportsUrl(baseUrl))
                        DashboardAction.OpenCustomers -> launchTwa(Constants.customersUrl(baseUrl))
                        DashboardAction.OpenProducts -> launchTwa(Constants.productsUrl(baseUrl))
                        DashboardAction.OpenDiscountLoyalty -> launchTwa(Constants.discountLoyaltyUrl(baseUrl))
                        DashboardAction.OpenGiftCards -> launchTwa(Constants.giftCardsUrl(baseUrl))
                        DashboardAction.OpenSettings -> navController.navigate(NightPOSDestination.Settings.route)
                        DashboardAction.Logout -> Unit
                    }
                },
                onLoggedOut = {
                    sharedGeckoView.session?.loadUri("about:blank")
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
