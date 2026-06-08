/**
 * NightPOS Local Printer — content.js
 *
 * Injected into every page. Listens for ESC/POS print events dispatched by
 * the Odoo/NightPOS frontend and forwards them to the local Python print
 * server via a short-lived WebSocket connection.
 *
 * Listens for `nightpos_local_print_event` only (one listener — avoids double prints).
 *
 * Expected event.detail payload:
 *   { printer_name: string, raw_data: string (base64 ESC/POS bytes) }
 */

'use strict';

// ── Constants ────────────────────────────────────────────────────────────────

const DEFAULT_WS_URL  = 'ws://localhost:8765';
const CONNECT_TIMEOUT = 6000; // ms

const PRINT_EVENT = 'nightpos_local_print_event';

/** Ignore duplicate events within this window (same payload). */
const PRINT_DEDUPE_MS = 2500;
/** @type {Map<string, number>} */
const recentPrintJobs = new Map();

// ── Logging helpers ──────────────────────────────────────────────────────────

const TAG = '[NightPOS Printer]';
const log  = (...a) => console.log(TAG, ...a);
const warn = (...a) => console.warn(TAG, ...a);
const err  = (...a) => console.error(TAG, ...a);

function markExtensionReady() {
  document.documentElement.setAttribute('data-nightpos-printer-ready', '1');
}

async function getWsUrl() {
  let wsUrl = DEFAULT_WS_URL;
  try {
    const { settings } = await chrome.storage.local.get('settings');
    if (settings?.wsUrl) wsUrl = settings.wsUrl;
  } catch (_) {}
  return wsUrl;
}

/** List printers via WebSocket (same as popup) — no background worker required. */
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
      ws = new WebSocket(wsUrl);
    } catch (e) {
      clearTimeout(timer);
      reject(new Error(`Cannot open WebSocket: ${e.message}`));
      return;
    }

    ws.onopen = () => ws.send(JSON.stringify({ action: 'list_printers' }));

    ws.onmessage = ({ data }) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      try { ws.close(); } catch (_) {}
      try {
        const res = JSON.parse(data);
        if (!res.success) {
          reject(new Error(res.error || 'list_printers failed'));
          return;
        }
        resolve(res.printers || []);
      } catch (_) {
        reject(new Error('Invalid response from print server'));
      }
    };

    ws.onerror = () => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      reject(new Error(`Cannot reach print server at ${wsUrl}`));
    };
  });
}

// ── WebSocket send ───────────────────────────────────────────────────────────

/**
 * Open a one-shot WebSocket to the local server, send payload, await reply.
 * @param {object} payload
 * @param {string} wsUrl
 * @returns {Promise<object>} Server response
 */
function sendToServer(payload, wsUrl) {
  return new Promise((resolve, reject) => {
    let ws;
    let settled = false;

    const settle = (fn, val) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      fn(val);
      try { ws?.close(); } catch (_) {}
    };

    const timer = setTimeout(() => {
      settle(reject, new Error(
        `Connection timed out (${CONNECT_TIMEOUT}ms). ` +
        `Is the NightPOS print server running at ${wsUrl}?`
      ));
    }, CONNECT_TIMEOUT);

    try {
      ws = new WebSocket(wsUrl);
    } catch (e) {
      clearTimeout(timer);
      return reject(new Error(`Cannot create WebSocket: ${e.message}`));
    }

    ws.onopen = () => {
      log(`Connected to ${wsUrl} — sending job for printer: "${payload.printer_name}"`);
      ws.send(JSON.stringify({
        action:       'print',
        printer_name: payload.printer_name || '',
        raw_data:     payload.raw_data || '',
      }));
    };

    ws.onmessage = ({ data }) => {
      try {
        const res = JSON.parse(data);
        settle(res.success ? resolve : reject,
               res.success ? res : new Error(res.error || 'Server returned failure'));
      } catch (_) {
        settle(reject, new Error('Invalid response from print server'));
      }
    };

    ws.onerror = () =>
      settle(reject, new Error(
        `WebSocket error — the local print server may not be running at ${wsUrl}`
      ));

    ws.onclose = ({ wasClean, code }) => {
      if (!wasClean && !settled)
        settle(reject, new Error(`WebSocket closed unexpectedly (code ${code})`));
    };
  });
}

// ── Toast notification ───────────────────────────────────────────────────────

/**
 * Show a dismissible toast on the page.
 * @param {'error'|'success'} type
 * @param {string} message
 */
function showToast(type, message) {
  const id   = '__nightpos_print_toast__';
  const bg   = type === 'error' ? '#c0392b' : '#27ae60';
  const icon = type === 'error' ? '🖨️ ❌' : '🖨️ ✓';

  document.getElementById(id)?.remove();

  const el = document.createElement('div');
  el.id = id;
  Object.assign(el.style, {
    position:   'fixed',
    top:        '20px',
    right:      '20px',
    zIndex:     '2147483647',
    background: bg,
    color:      '#fff',
    padding:    '14px 18px',
    borderRadius: '10px',
    fontSize:   '13px',
    fontFamily: 'system-ui, sans-serif',
    lineHeight: '1.5',
    boxShadow:  '0 4px 20px rgba(0,0,0,0.35)',
    maxWidth:   '360px',
    cursor:     'pointer',
    userSelect: 'none',
    transition: 'opacity .3s',
  });

  el.innerHTML = `<strong>${icon} NightPOS Printer</strong><br>${message}` +
    (type === 'error'
      ? `<br><small style="opacity:.8">Make sure <code>local_print_server.py</code> is running.<br>Click to dismiss.</small>`
      : '');

  el.onclick = () => el.remove();
  document.body.appendChild(el);
  setTimeout(() => { el.style.opacity = '0'; setTimeout(() => el.remove(), 400); },
             type === 'error' ? 12000 : 4000);
}

// ── Print deduplication ──────────────────────────────────────────────────────

function printJobKey(payload) {
  const name = payload.printer_name || '';
  const data = payload.raw_data || '';
  const tail = data.length > 48 ? data.slice(-48) : data;
  return `${name}\0${data.length}\0${tail}`;
}

function isDuplicatePrint(payload) {
  const key = printJobKey(payload);
  const now = Date.now();
  const last = recentPrintJobs.get(key);
  if (last != null && now - last < PRINT_DEDUPE_MS) {
    return true;
  }
  recentPrintJobs.set(key, now);
  for (const [k, t] of recentPrintJobs) {
    if (now - t > PRINT_DEDUPE_MS * 2) {
      recentPrintJobs.delete(k);
    }
  }
  return false;
}

function notifyPrintResult(success, error, printerName) {
  window.dispatchEvent(
    new CustomEvent('nightpos_print_result', {
      detail: { success, error: error || null, printer_name: printerName || '' },
    })
  );
}

// ── Print event handler ──────────────────────────────────────────────────────

async function handlePrintEvent(event) {
  const payload = event?.detail;

  if (!payload || typeof payload.raw_data !== 'string' || !payload.raw_data) {
    warn('Ignoring print event — missing or empty raw_data:', payload);
    return;
  }

  if (isDuplicatePrint(payload)) {
    log('Skipping duplicate print job (same receipt within dedupe window)');
    notifyPrintResult(true, null, payload.printer_name);
    return;
  }

  log('Print event received:', {
    event_type:   event.type,
    printer_name: payload.printer_name || '(default)',
    data_bytes:   Math.round(payload.raw_data.length * 0.75), // base64 → bytes estimate
  });

  const wsUrl = await getWsUrl();

  try {
    chrome.runtime.sendMessage({ type: 'PRINT_START' }).catch(() => {});
    const res = await sendToServer(payload, wsUrl);
    log('Print job accepted:', res);
    chrome.runtime
      .sendMessage({ type: 'PRINT_SUCCESS', printer_name: payload.printer_name })
      .catch(() => {});
    notifyPrintResult(true, null, payload.printer_name);
    showToast('success', `Sent to printer: ${payload.printer_name || 'default'}`);
  } catch (e) {
    err('Print job failed:', e.message);
    chrome.runtime.sendMessage({ type: 'PRINT_ERROR', error: e.message }).catch(() => {});
    notifyPrintResult(false, e.message, payload.printer_name);
    showToast('error', e.message);
  }
}

// ── Printer list relay (Odoo backend form) ───────────────────────────────────

async function listPrintersForPage() {
  const wsUrl = await getWsUrl();
  return fetchPrintersFromServer(wsUrl);
}

window.addEventListener('nightpos_list_printers_request', async () => {
  try {
    const printers = await listPrintersForPage();
    window.dispatchEvent(new CustomEvent('nightpos_list_printers_response', {
      detail: { success: true, printers },
    }));
  } catch (error) {
    window.dispatchEvent(new CustomEvent('nightpos_list_printers_response', {
      detail: { success: false, error: error.message },
    }));
  }
});

// ── Bootstrap ────────────────────────────────────────────────────────────────

markExtensionReady();

window.addEventListener(PRINT_EVENT, handlePrintEvent);
log(`Listening for "${PRINT_EVENT}" on this page.`);

// Badge/stats only — no message on page load (avoids MV3 "Receiving end" noise).
