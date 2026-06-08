// Fetches the active POS configurations from Odoo and sends them to the
// NightPOS Android app via the window.prompt bridge so the "Open POS" FAB
// can display real outlet names. Runs once per top-level page load on
// nightpos.com after the page is ready (document_end).
(function fetchPosConfigs() {
  // GeckoView content scripts require absolute URLs — relative paths do not
  // resolve against the page origin inside a built-in extension context.
  fetch(window.location.origin + '/web/dataset/call_kw', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      jsonrpc: '2.0',
      method: 'call',
      id: 1,
      params: {
        model: 'pos.config',
        method: 'search_read',
        args: [[['active', '=', true]]],
        kwargs: {
          fields: ['id', 'name'],
          context: {},
          limit: 20
        }
      }
    })
  })
  .then(function(r) { return r.json(); })
  .then(function(data) {
    if (data && Array.isArray(data.result) && data.result.length > 0) {
      window.prompt('nightpos:posConfigs', JSON.stringify(data.result));
    }
  })
  .catch(function() { /* not logged in or network error — silently skip */ });
})();
