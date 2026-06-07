package com.nightpos.app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.nightpos.app.ui.navigation.NightPOSDestination
import com.nightpos.app.ui.navigation.WebViewKind
import kotlinx.coroutines.delay

/**
 * Implements "Auto reopen POS if accidentally closed" (spec section 10):
 * while kiosk + auto-reopen are both enabled, if the app is ever resumed while
 * sitting on the Dashboard (i.e. POS isn't the foreground destination — which can
 * only happen if staff backed out or the Activity was recreated), it navigates
 * straight back into the POS screen.
 *
 * This intentionally does *not* fight the user inside the POS screen itself —
 * [com.nightpos.app.ui.screens.webview.WebViewScreen]'s [androidx.activity.compose.BackHandler]
 * already requires an explicit confirmation before leaving, so reaching the
 * dashboard at all means the staff member actively chose to exit.
 */
@Composable
fun AutoReopenPosEffect(
    navController: NavController,
    kioskModeEnabled: Boolean,
    autoReopenEnabled: Boolean,
    currentRoute: String?,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, kioskModeEnabled, autoReopenEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && kioskModeEnabled && autoReopenEnabled) {
                val onDashboard = navController.currentDestination?.route == NightPOSDestination.Dashboard.route
                if (onDashboard) {
                    navController.navigate(NightPOSDestination.WebViewDest.routeFor(WebViewKind.POS)) {
                        launchSingleTop = true
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Defensive re-check shortly after composition (covers the case where the
    // dashboard becomes the start destination again after a logout/back-stack reset).
    LaunchedEffect(currentRoute, kioskModeEnabled, autoReopenEnabled) {
        if (kioskModeEnabled && autoReopenEnabled && currentRoute == NightPOSDestination.Dashboard.route) {
            delay(800)
            if (navController.currentDestination?.route == NightPOSDestination.Dashboard.route) {
                navController.navigate(NightPOSDestination.WebViewDest.routeFor(WebViewKind.POS)) {
                    launchSingleTop = true
                }
            }
        }
    }
}
