// ── NightPOS device injection ─────────────────────────────────────────────────
// Runs synchronously at document_start so window.__nightpos_device is available
// before any page JS. Uses window.prompt which GeckoView intercepts natively
// without opening a dialog (the geckoPromptDelegate handles nightpos: prefixed calls).
(function () {
    if (navigator.userAgent.indexOf("NightPOS") === -1) return;
    var paperWidth = "58";
    try {
        var resp = window.prompt("nightpos:SunmiPrinter", '{"method":"getPaperWidth"}');
        if (resp) {
            var info = JSON.parse(resp);
            if (info.paperWidth === "80" || info.paperWidth === 80) {
                paperWidth = "80";
            }
        }
    } catch (e) {
        // Older builds don't support getPaperWidth — default to 58mm.
    }
    window.__nightpos_device = { type: "sunmi", manufacturer: "SUNMI", paperWidth: paperWidth };
})();

// Polyfills for Odoo 19 on GeckoView 99 (Firefox 99).

// Promise.withResolvers — added in Firefox 121, used by Odoo 19 OWL framework.
if (typeof Promise.withResolvers === "undefined") {
    Promise.withResolvers = function () {
        var resolve, reject;
        var promise = new Promise(function (res, rej) {
            resolve = res;
            reject = rej;
        });
        return { promise: promise, resolve: resolve, reject: reject };
    };
}

// structuredClone — added in Firefox 94, used by various Odoo 19 modules.
if (typeof structuredClone === "undefined") {
    structuredClone = function (obj) {
        return JSON.parse(JSON.stringify(obj));
    };
}

// ── POS outlet config fetch ───────────────────────────────────────────────────
// Fetches active pos.config records from Odoo and sends them to the Android
// app via the window.prompt bridge so the "Open POS" FAB shows real outlet
// names (e.g. "SOHO Club", "AfroRoom"). Only runs on nightpos.com top frames.
// Embedded here (rather than only in pos-configs.js) to work even when the
// GeckoView extension profile cache hasn't picked up the manifest update.
(function() {
  if (window.self !== window.top) return; // top frame only
  var host = window.location.hostname;
  if (!host || host.indexOf('nightpos.com') === -1) return;

  console.log('[NightPOS] pos-config fetch starting on ' + window.location.href);
  function doFetch() {
    // Content scripts in GeckoView require absolute URLs for fetch() —
    // relative paths are not resolved against the page origin here.
    var apiUrl = window.location.origin + '/web/dataset/call_kw';
    fetch(apiUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        jsonrpc: '2.0', method: 'call', id: 1,
        params: {
          model: 'pos.config',
          method: 'search_read',
          args: [[['active', '=', true]]],
          kwargs: { fields: ['id', 'name'], context: {}, limit: 20 }
        }
      })
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
      if (data && Array.isArray(data.result) && data.result.length > 0) {
        window.prompt('nightpos:posConfigs', JSON.stringify(data.result));
      }
    })
    .then(function() { console.log('[NightPOS] pos-config fetch sent to bridge'); })
    .catch(function(e) { console.log('[NightPOS] pos-config fetch error: ' + e); });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', doFetch);
  } else {
    doFetch();
  }
})();
