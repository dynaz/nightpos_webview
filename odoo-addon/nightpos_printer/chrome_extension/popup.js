'use strict';

// ── DOM refs ─────────────────────────────────────────────────────────────────

const $  = id => document.getElementById(id);
const dot          = $('status-dot');
const statusText   = $('status-text');
const statusDetail = $('status-detail');
const refreshBtn   = $('refresh-btn');
const statSuccess  = $('stat-success');
const statError    = $('stat-error');
const statPrinter  = $('stat-printer');
const lastTime     = $('last-time');
const resetBtn     = $('reset-btn');
const wsUrlInput   = $('ws-url');
const saveBtn      = $('save-btn');
const saveMsg      = $('save-msg');
const printerList  = $('printer-list');
const listPrintersBtn = $('list-printers-btn');
const lastErrorText   = $('last-error-text');

// ── Helpers ──────────────────────────────────────────────────────────────────

function send(msg) {
  return new Promise((resolve, reject) => {
    try {
      chrome.runtime.sendMessage(msg, (res) => {
        const err = chrome.runtime.lastError;
        if (err) {
          reject(new Error(err.message));
          return;
        }
        resolve(res);
      });
    } catch (e) {
      reject(e);
    }
  });
}

function fmtTime(iso) {
  if (!iso) return '';
  try {
    const d = new Date(iso);
    return `Last print: ${d.toLocaleDateString()} ${d.toLocaleTimeString()}`;
  } catch (_) { return ''; }
}

function setSpinning(btn, on) {
  btn.classList.toggle('spinning', on);
}

// ── Server status ─────────────────────────────────────────────────────────────

async function checkServer() {
  dot.className = 'dot checking';
  statusText.textContent = 'Checking…';
  statusDetail.textContent = '';
  setSpinning(refreshBtn, true);

  try {
    const res = await send({ type: 'CHECK_SERVER' });
    if (res?.ok) {
      dot.className = 'dot online';
      statusText.textContent = 'Print server online';
      statusDetail.textContent = res.version ? `Server version: ${res.version}` : '';
      refreshPrinters();
    } else {
      dot.className = 'dot offline';
      statusText.textContent = 'Server offline';
      statusDetail.textContent = res?.error || 'Cannot reach ws://localhost:8765';
    }
  } catch (e) {
    dot.className = 'dot offline';
    statusText.textContent = 'Server offline';
    statusDetail.textContent = e.message || 'Unknown error';
  } finally {
    setSpinning(refreshBtn, false);
  }
}

// ── Stats ─────────────────────────────────────────────────────────────────────

async function loadStats() {
  try {
    const { stats } = await send({ type: 'GET_STATS' });
    const s = stats || {};
    statSuccess.textContent = s.successCount ?? 0;
    statError.textContent   = s.errorCount   ?? 0;
    statPrinter.textContent = s.lastPrinter  || '—';
    lastTime.textContent    = fmtTime(s.lastTime);

    if (s.lastError && s.lastStatus === 'error') {
      lastErrorText.textContent = `Last error: ${s.lastError}`;
    } else {
      lastErrorText.textContent = '';
    }
  } catch (_) {}
}

resetBtn.addEventListener('click', async () => {
  await send({ type: 'RESET_STATS' });
  await loadStats();
});

// ── Settings ──────────────────────────────────────────────────────────────────

async function loadSettings() {
  try {
    const { settings } = await send({ type: 'GET_SETTINGS' });
    wsUrlInput.value = settings?.wsUrl || 'ws://localhost:8765';
  } catch (_) {}
}

saveBtn.addEventListener('click', async () => {
  const url = wsUrlInput.value.trim();
  if (!url.startsWith('ws://') && !url.startsWith('wss://')) {
    saveMsg.textContent = 'URL must start with ws:// or wss://';
    saveMsg.className = 'save-msg error';
    return;
  }
  try {
    await send({ type: 'SAVE_SETTINGS', settings: { wsUrl: url } });
    saveMsg.textContent = 'Saved!';
    saveMsg.className = 'save-msg';
    setTimeout(() => { saveMsg.textContent = ''; }, 2000);
    checkServer();
  } catch (e) {
    saveMsg.textContent = 'Save failed: ' + e.message;
    saveMsg.className = 'save-msg error';
  }
});

// ── Printer list ──────────────────────────────────────────────────────────────

async function refreshPrinters() {
  setSpinning(listPrintersBtn, true);
  printerList.innerHTML = '<li class="muted">Loading…</li>';

  try {
    const { settings } = await send({ type: 'GET_SETTINGS' });
    const wsUrl = settings?.wsUrl || 'ws://localhost:8765';

    const printers = await fetchPrinters(wsUrl);
    if (!printers || printers.length === 0) {
      printerList.innerHTML = '<li class="muted">No printers found.</li>';
    } else {
      printerList.innerHTML = printers
        .map(p => `<li>${escHtml(p)}</li>`)
        .join('');
    }
  } catch (e) {
    printerList.innerHTML = `<li class="muted">Error: ${escHtml(e.message)}</li>`;
  } finally {
    setSpinning(listPrintersBtn, false);
  }
}

function fetchPrinters(wsUrl) {
  return new Promise((resolve, reject) => {
    let ws;
    let settled = false;

    const timer = setTimeout(() => {
      settled = true;
      try { ws?.close(); } catch (_) {}
      reject(new Error('Timed out waiting for printer list'));
    }, 4000);

    try { ws = new WebSocket(wsUrl); } catch (e) {
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
        resolve(res.printers || []);
      } catch (_) {
        reject(new Error('Invalid response from server'));
      }
    };

    ws.onerror = () => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      reject(new Error('WebSocket error'));
    };
  });
}

listPrintersBtn.addEventListener('click', refreshPrinters);

// ── Utility ───────────────────────────────────────────────────────────────────

function escHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// ── Init ──────────────────────────────────────────────────────────────────────

(async () => {
  await Promise.all([loadSettings(), loadStats()]);
  checkServer();
})();

refreshBtn.addEventListener('click', checkServer);
