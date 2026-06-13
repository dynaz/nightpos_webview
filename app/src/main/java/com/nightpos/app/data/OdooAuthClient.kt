package com.nightpos.app.data

import com.nightpos.app.NightPOSApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.mozilla.geckoview.GeckoWebExecutor
import org.mozilla.geckoview.WebRequest
import org.mozilla.geckoview.WebResponse
import java.net.URLEncoder

/** Outcome of an Odoo login attempt. */
sealed interface OdooAuthResult {
    /** Login succeeded; [uid] is the authenticated Odoo user id (-1 if unknown). */
    data class Success(val uid: Int, val login: String, val name: String) : OdooAuthResult

    /** The server responded but rejected the credentials (wrong login/password/PIN). */
    data object InvalidCredentials : OdooAuthResult

    /** The request could not be completed (no connection, timeout, unexpected response). */
    data class NetworkError(val message: String?) : OdooAuthResult
}

/**
 * Minimal Odoo 19 authentication client used by the native Login screen.
 *
 * Submits the same `/web/login` form a browser would, instead of calling the
 * `/web/session/authenticate` JSON-RPC endpoint directly. This matters for
 * single-tenant deployments: Odoo resolves the database from the request's
 * Host header (`db_filter`) for normal `type='http'` routes such as
 * `/web/login`, but the JSON-RPC endpoint requires an explicit `db` and does
 * not fall back to host-based resolution — passing `db: ""` there results in
 * "Database not found" even though the database is selected automatically
 * for `/web/login`.
 *
 * Uses [GeckoWebExecutor] backed by the shared [NightPOSApplication.geckoRuntime] so the
 * `session_id` cookie set by a successful `/web/login` response lands in the same cookie
 * jar that [org.mozilla.geckoview.GeckoSession.loadUri] uses — the Odoo web/POS UI loaded
 * afterwards is then already authenticated, with no manual cookie handling required.
 */
class OdooAuthClient {

    private val executor by lazy { GeckoWebExecutor(NightPOSApplication.geckoRuntime) }

    /** Authenticates by submitting the standard `/web/login` form, mirroring a browser login. */
    suspend fun authenticate(baseUrl: String, login: String, password: String): OdooAuthResult =
        withContext(Dispatchers.IO) {
            try {
                val loginUrl = "${baseUrl.trimEnd('/')}/web/login"

                // Fetch the login page first to obtain a fresh session + CSRF token.
                val loginPage = fetch(loginUrl, "GET")
                    ?: return@withContext OdooAuthResult.NetworkError("No response from server")

                val csrfToken = Regex("""name="csrf_token"\s+value="([^"]+)"""")
                    .find(loginPage.text)
                    ?.groupValues
                    ?.get(1)
                    ?: return@withContext OdooAuthResult.NetworkError("Could not find CSRF token")

                val formBody = listOf(
                    "csrf_token" to csrfToken,
                    "login" to login,
                    "password" to password,
                ).joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }

                val loginResponse = fetch(
                    url = loginUrl,
                    method = "POST",
                    body = formBody,
                    headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                ) ?: return@withContext OdooAuthResult.NetworkError("No response from server")

                val success = loginResponse.statusCode in 300..399 ||
                    (loginResponse.redirected && !loginResponse.uri.contains("/web/login"))

                if (!success) {
                    return@withContext OdooAuthResult.InvalidCredentials
                }

                val sessionInfo = runCatching { fetchSessionInfo(baseUrl) }.getOrNull()

                OdooAuthResult.Success(
                    uid = sessionInfo?.optInt("uid", -1) ?: -1,
                    login = sessionInfo?.optString("username", login) ?: login,
                    name = sessionInfo?.optString("name", login) ?: login,
                )
            } catch (t: Throwable) {
                OdooAuthResult.NetworkError(t.message)
            }
        }

    /** Calls `/web/session/get_session_info` to retrieve the authenticated user's uid/name. */
    private suspend fun fetchSessionInfo(baseUrl: String): JSONObject? {
        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("method", "call")
            put("params", JSONObject())
        }

        val response = fetch(
            url = "${baseUrl.trimEnd('/')}/web/session/get_session_info",
            method = "POST",
            body = payload.toString(),
            headers = mapOf("Content-Type" to "application/json", "Accept" to "application/json"),
        ) ?: return null

        if (response.statusCode !in 200..299) return null

        val result = JSONObject(response.text).optJSONObject("result") ?: return null
        val uid = result.opt("uid")
        if (uid == null || uid == false) return null
        return result
    }

    private class FetchResult(val statusCode: Int, val uri: String, val redirected: Boolean, val text: String)

    private suspend fun fetch(
        url: String,
        method: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): FetchResult? {
        val builder = WebRequest.Builder(url).method(method)
        headers.forEach { (key, value) -> builder.header(key, value) }
        body?.let { builder.body(it) }

        val response: WebResponse = executor.fetch(builder.build()).poll(20_000L) ?: return null
        val text = response.body?.use { it.bufferedReader().readText() }.orEmpty()
        return FetchResult(response.statusCode, response.uri, response.redirected, text)
    }
}
