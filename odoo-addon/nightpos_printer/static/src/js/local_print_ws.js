/** @odoo-module **/
/**
 * WebSocket client for the NightPOS local print server (local_print_server.py).
 */

export const DEFAULT_WS_URL = "ws://localhost:8765";

/** Content-script marker on documentElement (visible from Odoo page JS). */
export function isExtensionContentScriptActive() {
    return (
        document.documentElement?.getAttribute("data-nightpos-printer-ready") === "1" ||
        Boolean(window.__nightpos_printer_ready__)
    );
}

/**
 * @param {string} [wsUrl]
 * @param {number} [timeoutMs]
 * @returns {Promise<string[]>}
 */
export function fetchLocalPrinters(wsUrl = DEFAULT_WS_URL, timeoutMs = 4000) {
    return new Promise((resolve, reject) => {
        let ws;
        let settled = false;

        const timer = setTimeout(() => {
            if (settled) {
                return;
            }
            settled = true;
            try {
                ws?.close();
            } catch (_) {
                /* ignore */
            }
            reject(new Error("Timed out waiting for printer list from the print server"));
        }, timeoutMs);

        try {
            ws = new WebSocket(wsUrl);
        } catch (error) {
            clearTimeout(timer);
            reject(new Error(`Cannot open WebSocket: ${error.message}`));
            return;
        }

        ws.onopen = () => ws.send(JSON.stringify({ action: "list_printers" }));

        ws.onmessage = ({ data }) => {
            if (settled) {
                return;
            }
            settled = true;
            clearTimeout(timer);
            try {
                ws.close();
            } catch (_) {
                /* ignore */
            }
            try {
                const res = JSON.parse(data);
                if (!res.success) {
                    reject(new Error(res.error || "list_printers failed"));
                    return;
                }
                resolve(res.printers || []);
            } catch (_) {
                reject(new Error("Invalid response from print server"));
            }
        };

        ws.onerror = () => {
            if (settled) {
                return;
            }
            settled = true;
            clearTimeout(timer);
            reject(
                new Error(
                    `Cannot reach print server at ${wsUrl}. ` +
                        "Start local_print_server.py or the NightPOS Print Service."
                )
            );
        };
    });
}

/**
 * Ask the Chrome extension content script to list printers (WebSocket relay).
 * @param {number} [timeoutMs]
 * @returns {Promise<string[]>}
 */
export function fetchLocalPrintersViaExtension(timeoutMs = 5000) {
    return new Promise((resolve, reject) => {
        if (!isExtensionContentScriptActive()) {
            reject(
                new Error(
                    "NightPOS Chrome Extension is not active on this tab. " +
                        "Reload the page after installing the extension."
                )
            );
            return;
        }

        const timer = setTimeout(() => {
            window.removeEventListener("nightpos_list_printers_response", onResponse);
            reject(new Error("Timed out waiting for printer list from Chrome Extension"));
        }, timeoutMs);

        const onResponse = (event) => {
            clearTimeout(timer);
            const detail = event.detail || {};
            if (detail.success) {
                resolve(detail.printers || []);
            } else {
                reject(new Error(detail.error || "Failed to list printers"));
            }
        };

        window.addEventListener("nightpos_list_printers_response", onResponse, { once: true });
        window.dispatchEvent(new CustomEvent("nightpos_list_printers_request"));
    });
}

/** WebSocket first; fall back to extension relay when the direct call fails. */
export async function fetchLocalPrintersAuto(wsUrl = DEFAULT_WS_URL) {
    try {
        return await fetchLocalPrinters(wsUrl);
    } catch (directError) {
        if (isExtensionContentScriptActive()) {
            return fetchLocalPrintersViaExtension();
        }
        throw directError;
    }
}
