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

    const val POS_PATH = "/npos"
    const val BACKEND_PATH = "/web"

    fun openPosUrl(baseUrl: String = DEFAULT_BASE_URL): String = "${baseUrl.trimEnd('/')}$POS_PATH"
    fun reportsUrl(baseUrl: String = DEFAULT_BASE_URL): String = "${baseUrl.trimEnd('/')}$BACKEND_PATH"
    fun customersUrl(baseUrl: String = DEFAULT_BASE_URL): String = "${baseUrl.trimEnd('/')}$BACKEND_PATH"

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
