/**
 * NightPOS Local Printer — background.js (MV3 Service Worker)
 *
 * Responsibilities:
 *  - Relay print status to the popup via chrome.storage
 *  - Update the toolbar badge to reflect print state
 *  - Respond to popup queries (server health check, stats, settings)
 */

'use strict';

const DEFAULT_WS_URL = 'ws://localhost:8765';
const BADGE_CLEAR_DELAY = 4000; // ms

// ── Badge helpers ────────────────────────────────────────────────────────────

let badgeClearTimer = null;

function setBadge(text, color, autoClear = false) {
  chrome.action.setBadgeText({ text: text });
  if (color) chrome.action.setBadgeBackgroundColor({ color: color });
  if (badgeClearTimer) clearTimeout(badgeClearTimer);
  if (autoClear) {
    badgeClearTimer = setTimeout(() => chrome.action.setBadgeText({ text: '' }), BADGE_CLEAR_DELAY);
  }
}

// ── Stats helpers ────────────────────────────────────────────────────────────

async function recordEvent(type, data) {
  const { stats = {} } = await chrome.storage.local.get('stats');
  const base = {
    successCount: stats.successCount || 0,
    errorCount:   stats.errorCount   || 0,
    lastStatus:   stats.lastStatus   || null,
    lastPrinter:  stats.lastPrinter  || null,
    lastError:    stats.lastError    || null,
    lastTime:     stats.lastTime     || null,
  };

  if (type === 'PRINT_SUCCESS') {
    base.successCount++;
    base.lastStatus  = 'success';
    base.lastPrinter = data.printer_name || null;
    base.lastError   = null;
    base.lastTime    = new Date().toISOString();
  } else if (type === 'PRINT_ERROR') {
    base.errorCount++;
    base.lastStatus  = 'error';
    base.lastError   = data.error || 'Unknown error';
    base.lastTime    = new Date().toISOString();
  }

  await chrome.storage.local.set({ stats: base });
}

// ── Server health check ──────────────────────────────────────────────────────

function fetchPrintersFromServer(wsUrl) {
  return new Promise((resolve, reject) => {
    let ws;
    let settled = false;
    const timer = setTimeout(() => {
      if (settled) return;
      settled = true;
      try { ws?.close(); } catch (_) {}
      reject(new Error('Timed out waiting for printer list'));
    }, 4000);

    try {
      ws = new WebSocket(wsUrl || DEFAULT_WS_URL);
    } catch (e) {
      clearTimeout(timer);
      return reject(new Error(`Cannot open WebSocket: ${e.message}`));
    }

    ws.onopen = () => ws.send(JSON.stringify({ action: 'list_printers' }));

    ws.onmessage = ({ data }) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      try { ws.close(); } catch (_) {}
      try {
        const res = JSON.parse(data);
        if (!res.success) return reject(new Error(res.error || 'list_printers failed'));
        resolve(res.printers || []);
      } catch (_) {
        reject(new Error('Invalid response from print server'));
      }
    };

    ws.onerror = () => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      reject(new Error('WebSocket error while listing printers'));
    };
  });
}

function checkServer(wsUrl) {
  return new Promise((resolve) => {
    let ws;
    const timer = setTimeout(() => {
      try { ws?.close(); } catch (_) {}
      resolve({ ok: false, error: 'Connection timed out' });
    }, 3500);

    try {
      ws = new WebSocket(wsUrl || DEFAULT_WS_URL);
    } catch (e) {
      clearTimeout(timer);
      return resolve({ ok: false, error: e.message });
    }

    ws.onopen = () => {
      clearTimeout(timer);
      // Send a ping and wait for response
      ws.send(JSON.stringify({ action: 'ping' }));
    };

    ws.onmessage = ({ data }) => {
      clearTimeout(timer);
      try { ws.close(); } catch (_) {}
      try {
        const res = JSON.parse(data);
        resolve({ ok: true, version: res.version || null });
      } catch (_) {
        resolve({ ok: true });
      }
    };

    ws.onerror = () => {
      clearTimeout(timer);
      resolve({ ok: false, error: 'Cannot connect to print server' });
    };
  });
}

// ── Message listener ─────────────────────────────────────────────────────────

/** MV3: if return true, must call sendResponse before the worker sleeps. */
function replyAsync(promise, sendResponse) {
  promise
    .then((result) => {
      try {
        sendResponse(result);
      } catch (_) {
        /* channel already closed */
      }
    })
    .catch((err) => {
      try {
        sendResponse({ ok: false, success: false, error: String(err?.message || err) });
      } catch (_) {
        /* channel already closed */
      }
    });
}

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  switch (message.type) {

    case 'PRINT_START':
      setBadge('...', '#3498db');
      return false;

    case 'PRINT_SUCCESS':
      setBadge('OK', '#27ae60', true);
      recordEvent('PRINT_SUCCESS', { printer_name: message.printer_name }).catch(() => {});
      return false;

    case 'PRINT_ERROR':
      setBadge('ERR', '#c0392b');
      recordEvent('PRINT_ERROR', { error: message.error }).catch(() => {});
      return false;

    case 'CHECK_SERVER':
      replyAsync(
        chrome.storage.local.get('settings').then(({ settings }) => {
          const url = settings?.wsUrl || DEFAULT_WS_URL;
          return checkServer(url);
        }),
        sendResponse
      );
      return true;

    case 'LIST_PRINTERS':
      replyAsync(
        chrome.storage.local.get('settings').then(({ settings }) => {
          const url = settings?.wsUrl || DEFAULT_WS_URL;
          return fetchPrintersFromServer(url);
        }).then((printers) => ({ success: true, printers })),
        sendResponse
      );
      return true;

    case 'GET_STATS':
      replyAsync(
        chrome.storage.local.get('stats').then(({ stats }) => ({ stats: stats || {} })),
        sendResponse
      );
      return true;

    case 'RESET_STATS':
      replyAsync(
        chrome.storage.local.set({ stats: {} }).then(() => ({ ok: true })),
        sendResponse
      );
      return true;

    case 'GET_SETTINGS':
      replyAsync(
        chrome.storage.local.get('settings').then(({ settings }) => ({
          settings: settings || { wsUrl: DEFAULT_WS_URL },
        })),
        sendResponse
      );
      return true;

    case 'SAVE_SETTINGS':
      replyAsync(
        chrome.storage.local.set({ settings: message.settings }).then(() => ({ ok: true })),
        sendResponse
      );
      return true;

    default:
      return false;
  }
});

// ── Startup ──────────────────────────────────────────────────────────────────

chrome.runtime.onInstalled.addListener(({ reason }) => {
  if (reason === 'install') {
    chrome.storage.local.set({
      settings: { wsUrl: DEFAULT_WS_URL },
      stats: {},
    });
    console.log('[NightPOS Printer] Extension installed. Default WS URL:', DEFAULT_WS_URL);
  }
});
