package com.nightpos.app.print

import android.content.Context
import android.util.Base64
import android.webkit.JavascriptInterface
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

/**
 * Dual-mode JS bridge for the Sunmi printer:
 *
 * **Android WebView mode** (legacy): registered as "NightPOSBridge" via
 * [android.webkit.WebView.addJavascriptInterface]. JS calls
 * `window.NightPOSBridge.callHandler(name, argsJson)` synchronously.
 *
 * **GeckoView mode**: registered as the session's [GeckoSession.PromptDelegate].
 * JS calls `window.prompt("nightpos:HandlerName", argsJson)` which Gecko
 * suspends until the delegate returns a value — effectively a synchronous bridge.
 *
 * Both modes inject [buildInjectionScript] on every page load so that the
 * existing `nightpos_printer` Odoo addon (which uses
 * `window.flutter_inappwebview.callHandler(...)`) works unmodified.
 */
class SunmiJsBridge(private val context: Context) {

    private val connection = SunmiPrinterConnection(context)
    @Volatile private var bound = false

    fun bindPrinter() {
        if (!bound) bound = connection.bind()
    }

    fun unbindPrinter() {
        connection.unbind()
        bound = false
    }

    // ── Android WebView @JavascriptInterface ─────────────────────────────────

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

    // ── GeckoView PromptDelegate ─────────────────────────────────────────────

    val geckoPromptDelegate: GeckoSession.PromptDelegate = object : GeckoSession.PromptDelegate {
        override fun onTextPrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.TextPrompt,
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
            val msg = prompt.message ?: return GeckoResult.fromValue(prompt.dismiss())
            if (!msg.startsWith("nightpos:")) return GeckoResult.fromValue(prompt.dismiss())

            val handlerName = msg.removePrefix("nightpos:")
            val argsJson = prompt.defaultValue ?: "{}"
            val result = runCatching {
                when (handlerName) {
                    "ping" -> "pong"
                    "SunmiPrinter" -> handleSunmi(JSONObject(argsJson))
                    else -> JSONObject().put("success", false).put("error", "Unknown: $handlerName").toString()
                }
            }.getOrElse { e ->
                JSONObject().put("success", false).put("error", e.message).toString()
            }
            return GeckoResult.fromValue(prompt.confirm(result))
        }
    }

    // ── Shared handler ───────────────────────────────────────────────────────

    private fun handleSunmi(args: JSONObject): String {
        return when (val method = args.optString("method")) {
            "isPrinterConnected" -> {
                ensureBound()
                val connected = bound && connection.awaitReady(1_500)
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

    private fun ensureBound() {
        if (!bound) bound = connection.bind()
    }

    private fun sendRawEscPos(base64Data: String) {
        runCatching {
            ensureBound()
            if (!connection.awaitReady()) return
            connection.sendRaw(Base64.decode(base64Data, Base64.DEFAULT))
        }
    }

    private fun openCashDrawer() {
        runCatching {
            ensureBound()
            if (!connection.awaitReady()) return
            connection.sendRaw(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()))
        }
    }

    companion object {
        /**
         * Injected on every page load.
         *
         * In **WebView mode**: wraps `window.NightPOSBridge.callHandler` into a
         * Promise-based `window.flutter_inappwebview.callHandler`.
         *
         * In **GeckoView mode**: uses `window.prompt("nightpos:Handler", argsJson)`
         * which the [geckoPromptDelegate] intercepts and resolves synchronously.
         */
        fun buildInjectionScript(): String = """
(function() {
  if (window.flutter_inappwebview) return;

  // WebView mode — NightPOSBridge injected via addJavascriptInterface
  if (window.NightPOSBridge) {
    window.flutter_inappwebview = {
      callHandler: function(handlerName, args) {
        return new Promise(function(resolve, reject) {
          try {
            var result = window.NightPOSBridge.callHandler(
              handlerName, JSON.stringify(args || {}));
            resolve(JSON.parse(result));
          } catch (e) { reject(e); }
        });
      }
    };
    console.log('[NightPOS] bridge: WebView mode');
    return;
  }

  // GeckoView mode — use window.prompt interception
  window.flutter_inappwebview = {
    callHandler: function(handlerName, args) {
      return new Promise(function(resolve, reject) {
        try {
          var result = window.prompt(
            'nightpos:' + handlerName,
            JSON.stringify(args || {})
          );
          resolve(result ? JSON.parse(result) : {success: false, error: 'no result'});
        } catch (e) { reject(e); }
      });
    }
  };
  console.log('[NightPOS] bridge: GeckoView mode');
})();
""".trimIndent()
    }
}
