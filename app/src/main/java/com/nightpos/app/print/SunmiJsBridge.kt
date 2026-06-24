package com.nightpos.app.print

import android.content.Context
import android.util.Base64
import android.webkit.JavascriptInterface
import com.nightpos.app.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
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
/** A single active POS configuration fetched from Odoo's pos.config model. */
data class PosConfig(val id: Int, val name: String)

class SunmiJsBridge(private val context: Context) {

    private val connection = SunmiPrinterConnection(context)
    @Volatile private var bound = false

    private val _posConfigs = MutableStateFlow<List<PosConfig>>(emptyList())
    val posConfigs: StateFlow<List<PosConfig>> = _posConfigs.asStateFlow()

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
            android.util.Log.i("NightPOS", "onTextPrompt: msg=$msg")
            if (!msg.startsWith("nightpos:")) return GeckoResult.fromValue(prompt.dismiss())

            val handlerName = msg.removePrefix("nightpos:")
            val argsJson = prompt.defaultValue ?: "{}"
            val result = runCatching {
                when (handlerName) {
                    "ping" -> "pong"
                    "SunmiPrinter" -> handleSunmi(JSONObject(argsJson))
                    "posConfigs" -> {
                        // Persist the POS config list fetched by pos-configs.js
                        val array = JSONArray(argsJson)
                        val configs = (0 until array.length()).map { i ->
                            val obj = array.getJSONObject(i)
                            PosConfig(id = obj.getInt("id"), name = obj.getString("name"))
                        }
                        android.util.Log.i("NightPOS", "posConfigs received: $configs")
                        _posConfigs.value = configs
                        "ok"
                    }
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
            "getPaperWidth" -> {
                val widthMm = runBlocking {
                    PreferencesManager(context).printerPaperWidthMm.first()
                }
                JSONObject().put("success", true).put("paperWidth", widthMm.toString()).toString()
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

  // ── localhost:8585 fetch/XHR interceptor ─────────────────────────────────
  // Firefox/GeckoView blocks fetch("http://localhost:8585") from an HTTPS page
  // as Mixed Content (Chrome has a localhost exception — Firefox does not).
  // We intercept those calls and re-route them through window.prompt so they
  // never hit the network at all.
  var _PRINT_RE = /^http:\/\/(localhost|127\.0\.0\.1):8585/;

  function _bridgeCall(url, bodyStr) {
    if (/\/ping/.test(url)) {
      // Health-check: confirm the bridge is alive without a real printer call.
      return '{"status":"ok","printer":"sunmi"}';
    }
    var args = {};
    try { args = JSON.parse(bodyStr || '{}'); } catch(e) {}
    return window.prompt('nightpos:SunmiPrinter', JSON.stringify(args))
           || '{"success":false,"error":"bridge returned null"}';
  }

  // -- fetch intercept --
  var _origFetch = window.fetch;
  window.fetch = function(input, init) {
    var url = typeof input === 'string' ? input
            : (input && typeof input.url === 'string') ? input.url
            : String(input);
    if (_PRINT_RE.test(url)) {
      return new Promise(function(resolve) {
        var bodyStr = '{}';
        if (init && init.body) {
          bodyStr = typeof init.body === 'string' ? init.body
                  : JSON.stringify(init.body);
        }
        var responseBody = _bridgeCall(url, bodyStr);
        resolve(new Response(responseBody, {
          status: 200,
          headers: { 'Content-Type': 'application/json' }
        }));
      });
    }
    return _origFetch.apply(this, arguments);
  };

  // -- XMLHttpRequest intercept (for older addon code) --
  var _origOpen = XMLHttpRequest.prototype.open;
  var _origSend = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function(method, url) {
    this._nightposUrl = String(url);
    this._nightposIntercept = _PRINT_RE.test(this._nightposUrl);
    if (!this._nightposIntercept) {
      _origOpen.apply(this, arguments);
    }
  };
  XMLHttpRequest.prototype.send = function(body) {
    if (!this._nightposIntercept) {
      return _origSend.apply(this, arguments);
    }
    var xhr = this;
    var bodyStr = (typeof body === 'string') ? body : '{}';
    setTimeout(function() {
      var responseBody = _bridgeCall(xhr._nightposUrl, bodyStr);
      Object.defineProperty(xhr, 'readyState',   { get: function() { return 4; } });
      Object.defineProperty(xhr, 'status',       { get: function() { return 200; } });
      Object.defineProperty(xhr, 'responseText', { get: function() { return responseBody; } });
      Object.defineProperty(xhr, 'response',     { get: function() { return responseBody; } });
      if (typeof xhr.onreadystatechange === 'function') xhr.onreadystatechange();
      if (typeof xhr.onload === 'function') xhr.onload();
    }, 0);
  };

  console.log('[NightPOS] localhost:8585 fetch/XHR interceptor installed');

  // ── flutter_inappwebview bridge ──────────────────────────────────────────
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
