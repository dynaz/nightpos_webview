package com.nightpos.app.util

/**
 * Central place for the POS server configuration and navigation allow-list.
 *
 * Keeping these in one object makes it trivial to point the app at a different
 * Odoo instance (e.g. staging) by changing a single value, and lets the
 * [com.nightpos.app.webview.PosWebViewClient] enforce the domain restriction
 * required by the security spec.
 */
object Constants {

    /** Default Odoo POS host. Can be overridden at runtime via Settings (stored in DataStore). */
    const val DEFAULT_BASE_URL = "https://soho.nightpos.com"

    const val POS_PATH = "/npos/point-of-sale"
    const val BACKEND_PATH = "/web"
    const val REPORTS_PATH = "/npos/dashboards?dashboard_id=7"
    const val NPOS_HOME_PATH = "/npos"
    const val CUSTOMERS_PATH = "/npos/customers"
    const val PRODUCTS_PATH = "/npos/action-770"
    const val DISCOUNT_LOYALTY_PATH = "/npos/discount-loyalty"
    const val GIFT_CARDS_PATH = "/npos/gift-cards-ewallet"
    const val EMPLOYEES_PATH = "/npos/employees"
    const val PRINTERS_PATH = "/npos/action-968"
    const val POS_SETTINGS_PATH = "/npos/action-759?debug=1"

    fun nposHomeUrl(baseUrl: String = DEFAULT_BASE_URL): String = "${baseUrl.trimEnd('/')}$NPOS_HOME_PATH"
    fun openPosUrl(baseUrl: String = DEFAULT_BASE_URL): String = "${baseUrl.trimEnd('/')}$POS_PATH"
    fun reportsUrl(baseUrl: String = DEFAULT_BASE_URL): String = "${baseUrl.trimEnd('/')}$REPORTS_PATH"
    fun customersUrl(baseUrl: String = DEFAULT_BASE_URL): String = "${baseUrl.trimEnd('/')}$CUSTOMERS_PATH"
    fun productsUrl(baseUrl: String = DEFAULT_BASE_URL): String = "${baseUrl.trimEnd('/')}$PRODUCTS_PATH"
    fun discountLoyaltyUrl(baseUrl: String = DEFAULT_BASE_URL): String = "${baseUrl.trimEnd('/')}$DISCOUNT_LOYALTY_PATH"
    fun giftCardsUrl(baseUrl: String = DEFAULT_BASE_URL): String = "${baseUrl.trimEnd('/')}$GIFT_CARDS_PATH"
    fun employeesUrl(baseUrl: String = DEFAULT_BASE_URL): String = "${baseUrl.trimEnd('/')}$EMPLOYEES_PATH"
    fun printersUrl(baseUrl: String = DEFAULT_BASE_URL): String = "${baseUrl.trimEnd('/')}$PRINTERS_PATH"
    fun posSettingsUrl(baseUrl: String = DEFAULT_BASE_URL): String = "${baseUrl.trimEnd('/')}$POS_SETTINGS_PATH"

    /** Root domain that all WebView navigation is restricted to (subdomains included). */
    const val ALLOWED_DOMAIN = "nightpos.com"

    /**
     * Returns true if [host] is the allowed domain or one of its subdomains
     * (e.g. "soho.nightpos.com", "www.nightpos.com", "nightpos.com").
     */
    fun isAllowedHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val normalized = host.lowercase()
        return normalized == ALLOWED_DOMAIN || normalized.endsWith(".$ALLOWED_DOMAIN")
    }
}
