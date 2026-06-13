package com.nightpos.app.ui.navigation

import android.net.Uri

/**
 * Single source of truth for navigation routes. Using a sealed hierarchy instead
 * of raw strings avoids typos when building NavHost graphs and navigation calls.
 */
sealed class NightPOSDestination(val route: String) {

    data object Splash : NightPOSDestination("splash")
    data object Dashboard : NightPOSDestination("dashboard")
    data object Settings : NightPOSDestination("settings")

    /** Generic WebView destination; the `kind` route argument decides which URL + title to load. */
    object WebViewDest {
        const val ROUTE_PATTERN = "webview/{kind}"
        const val ARG_KIND = "kind"

        fun routeFor(kind: WebViewKind) = "webview/${kind.routeArg}"
    }

    /** WebView destination for a fully custom URL (e.g. a specific POS outlet). */
    object OutletDest {
        const val ROUTE_PATTERN = "outlet?url={url}&title={title}"
        const val ARG_URL = "url"
        const val ARG_TITLE = "title"

        fun routeFor(url: String, title: String) =
            "outlet?url=${Uri.encode(url)}&title=${Uri.encode(title)}"
    }
}

/** Identifies which Odoo backend area a WebView screen instance points to. */
enum class WebViewKind(val routeArg: String) {
    POS("pos"),
    REPORTS("reports"),
    CUSTOMERS("customers"),
    PRODUCTS("products"),
    DISCOUNT_LOYALTY("discount-loyalty"),
    GIFT_CARDS("gift-cards"),
    EMPLOYEES("employees"),
    PRINTERS("printers"),
    POS_SETTINGS("pos-settings");

    companion object {
        fun fromRouteArg(value: String?): WebViewKind =
            entries.firstOrNull { it.routeArg == value } ?: POS
    }
}
