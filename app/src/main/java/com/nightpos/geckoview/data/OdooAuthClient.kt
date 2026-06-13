package com.nightpos.geckoview.data

import com.nightpos.geckoview.NightPOSApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.mozilla.geckoview.GeckoWebExecutor
import org.mozilla.geckoview.WebRequest
import java.net.URI

/** Outcome of an Odoo `/web/session/authenticate` JSON-RPC call. */
sealed interface OdooAuthResult {
    /** Login succeeded; [uid] is the authenticated Odoo user id. */
    data class Success(val uid: Int, val login: String, val name: String) : OdooAuthResult

    /** The server responded but rejected the credentials (wrong login/password/PIN). */
    data object InvalidCredentials : OdooAuthResult

    /** The request could not be completed (no connection, timeout, unexpected response). */
    data class NetworkError(val message: String?) : OdooAuthResult
}

/**
 * Minimal Odoo 19 JSON-RPC client used to authenticate the native Login screen.
 *
 * Resolves the database name automatically (this deployment is single-tenant, one database
 * per URL): tries `/web/database/list` first and falls back to the server's hostname, which
 * matches the database name for this deployment's `db_filter`. The previous implementation
 * passed `db: ""` to `/web/session/authenticate`, which doesn't fall back to host-based
 * routing the way `/web/login` does, resulting in "Database not found".
 *
 * Uses [GeckoWebExecutor] backed by the shared [NightPOSApplication.geckoRuntime] so the
 * `session_id` cookie set by a successful `/web/session/authenticate` response lands in
 * the same cookie jar that [org.mozilla.geckoview.GeckoSession.loadUri] uses — the Odoo
 * web/POS UI loaded afterwards is then already authenticated, with no manual cookie
 * handling required.
 */
class OdooAuthClient {

    private val executor by lazy { GeckoWebExecutor(NightPOSApplication.geckoRuntime) }

    /**
     * Calls `/web/session/authenticate` with the given credentials. When [isPin] is true,
     * [password] is sent as `staff_pin` (PIN-based sign-in) instead of `password`.
     */
    suspend fun authenticate(baseUrl: String, login: String, password: String, isPin: Boolean): OdooAuthResult =
        withContext(Dispatchers.IO) {
            try {
                val db = resolveDatabase(baseUrl)

                val params = JSONObject().apply {
                    put("db", db)
                    put("login", login)
                    if (isPin) put("staff_pin", password) else put("password", password)
                }

                val json = rpcCall(baseUrl, "/web/session/authenticate", params)
                    ?: return@withContext OdooAuthResult.NetworkError("No response from server")

                if (json.has("error")) {
                    return@withContext OdooAuthResult.InvalidCredentials
                }

                val result = json.optJSONObject("result")
                val uid = result?.opt("uid")
                if (result == null || uid == null || uid == false) {
                    return@withContext OdooAuthResult.InvalidCredentials
                }

                OdooAuthResult.Success(
                    uid = (uid as? Number)?.toInt() ?: -1,
                    login = result.optString("username", login),
                    name = result.optString("name", login),
                )
            } catch (t: Throwable) {
                OdooAuthResult.NetworkError(t.message)
            }
        }

    /**
     * Resolves the database name for [baseUrl]: asks `/web/database/list` and uses its result
     * when exactly one database is listed, otherwise falls back to the server's hostname
     * (this single-tenant deployment names its database after the URL).
     */
    private suspend fun resolveDatabase(baseUrl: String): String {
        val listed = runCatching { rpcCall(baseUrl, "/web/database/list", JSONObject()) }
            .getOrNull()
            ?.optJSONArray("result")
        if (listed != null && listed.length() == 1) {
            return listed.getString(0)
        }
        return URI(baseUrl).host.orEmpty()
    }

    private suspend fun rpcCall(baseUrl: String, path: String, params: JSONObject): JSONObject? {
        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("method", "call")
            put("params", params)
        }

        val request = WebRequest.Builder("${baseUrl.trimEnd('/')}$path")
            .method("POST")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .body(payload.toString())
            .build()

        val response = executor.fetch(request).poll(20_000L) ?: return null
        if (response.statusCode !in 200..299) return null

        val text = response.body?.use { it.bufferedReader().readText() }.orEmpty()
        return runCatching { JSONObject(text) }.getOrNull()
    }
}
