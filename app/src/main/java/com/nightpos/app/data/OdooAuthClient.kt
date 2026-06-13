package com.nightpos.app.data

import com.nightpos.app.NightPOSApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.mozilla.geckoview.GeckoWebExecutor
import org.mozilla.geckoview.WebRequest

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
 * Uses [GeckoWebExecutor] backed by the shared [NightPOSApplication.geckoRuntime] so the
 * `session_id` cookie set by a successful `/web/session/authenticate` response lands in
 * the same cookie jar that [org.mozilla.geckoview.GeckoSession.loadUri] uses — the Odoo
 * web/POS UI loaded afterwards is then already authenticated, with no manual cookie
 * handling required.
 */
class OdooAuthClient {

    private val executor by lazy { GeckoWebExecutor(NightPOSApplication.geckoRuntime) }

    /**
     * Calls `/web/session/authenticate` with the given credentials. [db] is left blank so
     * Odoo resolves the database from the request's Host header (single-tenant deployments).
     */
    suspend fun authenticate(baseUrl: String, login: String, password: String): OdooAuthResult =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("jsonrpc", "2.0")
                    put("method", "call")
                    put(
                        "params",
                        JSONObject().apply {
                            put("db", "")
                            put("login", login)
                            put("password", password)
                        },
                    )
                }

                val request = WebRequest.Builder("${baseUrl.trimEnd('/')}/web/session/authenticate")
                    .method("POST")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(payload.toString())
                    .build()

                val response = executor.fetch(request).poll(20_000L)
                    ?: return@withContext OdooAuthResult.NetworkError("No response from server")

                if (response.statusCode !in 200..299) {
                    return@withContext OdooAuthResult.NetworkError("HTTP ${response.statusCode}")
                }

                val text = response.body?.use { it.bufferedReader().readText() }.orEmpty()
                val json = JSONObject(text)

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
}
