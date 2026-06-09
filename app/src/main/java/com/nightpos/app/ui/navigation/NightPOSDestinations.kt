package com.nightpos.app.ui.navigation

/**
 * Single source of truth for navigation routes. Using a sealed hierarchy instead
 * of raw strings avoids typos when building NavHost graphs and navigation calls.
 */
sealed class NightPOSDestination(val route: String) {

    data object Splash : NightPOSDestination("splash")
    data object Dashboard : NightPOSDestination("dashboard")
    data object Settings : NightPOSDestination("settings")
    data object PosLogin : NightPOSDestination("pos_login")
    data object PosMain : NightPOSDestination("pos_main")

    /** Generic WebView destination; the `kind` route argument decides which URL + title to load. */
    object WebViewDest {
        const val ROUTE_PATTERN = "webview/{kind}"
        const val ARG_KIND = "kind"

        fun routeFor(kind: WebViewKind) = "webview/${kind.routeArg}"
    }
}

/** Identifies which Odoo backend area a WebView screen instance points to. */
enum class WebViewKind(val routeArg: String) {
    POS("pos"),
    REPORTS("reports"),
    CUSTOMERS("customers");

    companion object {
        fun fromRouteArg(value: String?): WebViewKind =
            entries.firstOrNull { it.routeArg == value } ?: POS
    }
}
