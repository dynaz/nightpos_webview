package com.nightpos.app.print

import android.content.Context
import android.util.Base64
import android.webkit.JavascriptInterface
import org.json.JSONObject

/**
 * Android JavascriptInterface injected into the POS WebView as "NightPOSBridge".
 *
 * The nightpos_printer Odoo addon's Sunmi mode calls
 *   window.flutter_inappwebview.callHandler("SunmiPrinter", { method, data })
 * which is the Flutter InAppWebView API. We inject a thin JS shim (see
 * [buildInjectionScript]) that forwards those calls here, so the existing
 * Odoo addon works unmodified in a standard Android WebView.
 *
 * Supported calls:
 *   isPrinterConnected → {"success":true, "connected":true/false}
 *   printRaw           → fire-and-forget ESC/POS bytes; returns {"success":true} immediately
 *   openDrawer         → opens cash drawer via AIDL
 */
class SunmiJsBridge(private val context: Context) {

    // Single persistent connection shared across all JS calls.
    private val connection = SunmiPrinterConnection(context)
    @Volatile private var bound = false

    /** Bind AIDL at bridge creation time so the first print has no delay. */
    fun bindPrinter() {
        if (!bound) bound = connection.bind()
    }

    fun unbindPrinter() {
        connection.unbind()
        bound = false
    }

    // ── JavascriptInterface methods ──────────────────────────────────────────

    /**
     * Called from the injected shim for every flutter_inappwebview.callHandler invocation.
     * Returns a JSON string that the shim resolves into a Promise.
     */
    @JavascriptInterface
    fun callHandler(handlerName: String, argsJson: String): String {
        return runCatching {
            val args = JSONObject(argsJson)
            when (handlerName) {
                "SunmiPrinter" -> handleSunmi(args)
                else -> error("Unknown handler: $handlerName")
            }
        }.getOrElse { e ->
            JSONObject().put("success", false).put("error", e.message).toString()
        }
    }

    // ── Sunmi handler ────────────────────────────────────────────────────────

    private fun handleSunmi(args: JSONObject): String {
        return when (val method = args.optString("method")) {
            "isPrinterConnected" -> {
                ensureBound()
                val connected = bound && connection.awaitReady(timeoutMs = 1_500)
                JSONObject().put("success", true).put("connected", connected).toString()
            }
            "printRaw" -> {
                val b64 = args.optString("data")
                if (b64.isBlank()) return JSONObject().put("success", false)
                    .put("error", "printRaw: empty data").toString()
                Thread { sendRawEscPos(b64) }.start()
                JSONObject().put("success", true).toString()
            }
            "openDrawer" -> {
                Thread { openCashDrawer() }.start()
                JSONObject().put("success", true).toString()
            }
            else -> JSONObject().put("success", false).put("error", "Unknown method: $method").toString()
        }
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private fun ensureBound() {
        if (!bound) bound = connection.bind()
    }

    private fun sendRawEscPos(base64Data: String) {
        runCatching {
            ensureBound()
            if (!connection.awaitReady()) return
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            connection.sendRaw(bytes)
        }
    }

    private fun openCashDrawer() {
        runCatching {
            ensureBound()
            if (!connection.awaitReady()) return
            // ESC/POS command to open drawer: ESC p m t1 t2
            connection.sendRaw(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()))
        }
    }

    companion object {
        /**
         * JavaScript injected at WebView startup (via evaluateJavascript / addJavascriptInterface).
         * Creates window.flutter_inappwebview so the existing Odoo addon needs zero changes.
         */
        fun buildInjectionScript(): String = """
(function() {
  if (window.flutter_inappwebview) return; // already present (real Flutter app)
  if (!window.NightPOSBridge) return;      // bridge not injected yet (non-POS page)

  window.flutter_inappwebview = {
    callHandler: function(handlerName, args) {
      return new Promise(function(resolve, reject) {
        try {
          var result = window.NightPOSBridge.callHandler(
            handlerName,
            JSON.stringify(args || {})
          );
          resolve(JSON.parse(result));
        } catch (e) {
          reject(e);
        }
      });
    }
  };

  console.log('[NightPOS] flutter_inappwebview bridge installed');
})();
""".trimIndent()
    }
}
