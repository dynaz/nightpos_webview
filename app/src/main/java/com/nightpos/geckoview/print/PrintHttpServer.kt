package com.nightpos.geckoview.print

import android.util.Base64
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

/**
 * Local HTTP server that exposes the Sunmi printer to any browser running on the
 * device (Firefox Custom Tabs, system WebView, etc.).
 *
 * Endpoints:
 *   GET  /ping          → {"status":"ok","printer":"sunmi"}
 *   POST /print         → {"method":"printRaw"|"openDrawer"|"isPrinterConnected", ...}
 *
 * CORS headers are added to every response so the Odoo page (served over HTTPS)
 * can call http://localhost:[PORT] — modern browsers allow HTTPS → http://localhost
 * as a "secure context" exception.
 */
class PrintHttpServer(private val connection: SunmiPrinterConnection) : NanoHTTPD(PORT) {

    @Volatile private var bound = false

    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.OPTIONS) {
            return cors(newFixedLengthResponse(""))
        }
        return cors(
            when {
                session.uri == "/ping" ->
                    json("""{"status":"ok","printer":"sunmi"}""")
                session.uri == "/print" && session.method == Method.POST ->
                    handlePrint(session)
                else ->
                    json(Response.Status.NOT_FOUND, """{"error":"not found"}""")
            }
        )
    }

    private fun handlePrint(session: IHTTPSession): Response {
        return try {
            val body = mutableMapOf<String, String>()
            session.parseBody(body)
            val args = JSONObject(body["postData"] ?: "{}")
            json(dispatch(args))
        } catch (e: Exception) {
            json("""{"success":false,"error":"${e.message}"}""")
        }
    }

    private fun dispatch(args: JSONObject): String {
        return when (val method = args.optString("method")) {
            "isPrinterConnected" -> {
                ensureBound()
                JSONObject().put("success", true).put("connected", connection.awaitReady(1_500)).toString()
            }
            "printRaw" -> {
                val b64 = args.optString("data")
                if (b64.isBlank()) return JSONObject().put("success", false).put("error", "empty data").toString()
                Thread { sendRaw(b64) }.start()
                JSONObject().put("success", true).toString()
            }
            "openDrawer" -> {
                Thread { openDrawer() }.start()
                JSONObject().put("success", true).toString()
            }
            else -> JSONObject().put("success", false).put("error", "Unknown method: $method").toString()
        }
    }

    private fun ensureBound() {
        if (!bound) bound = connection.bind()
    }

    private fun sendRaw(b64: String) {
        runCatching {
            ensureBound()
            if (!connection.awaitReady()) return
            connection.sendRaw(Base64.decode(b64, Base64.DEFAULT))
        }
    }

    private fun openDrawer() {
        runCatching {
            ensureBound()
            if (!connection.awaitReady()) return
            connection.sendRaw(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()))
        }
    }

    private fun json(body: String) = newFixedLengthResponse(Response.Status.OK, "application/json", body)
    private fun json(status: Response.Status, body: String) = newFixedLengthResponse(status, "application/json", body)

    private fun cors(r: Response): Response {
        r.addHeader("Access-Control-Allow-Origin", "*")
        r.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        r.addHeader("Access-Control-Allow-Headers", "Content-Type")
        return r
    }

    companion object {
        const val PORT = 8585
    }
}
