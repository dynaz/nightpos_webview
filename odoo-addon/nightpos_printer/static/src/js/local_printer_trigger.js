/** @odoo-module **/
import { npLog } from "@nightpos_printer/js/nightpos_pos_debug";

/**
 * NightPOS Local Printer — local_printer_trigger.js
 *
 * Provides the low-level helper to fire a local-print CustomEvent.
 * Import and call `triggerLocalPrint` anywhere in the Odoo JS stack
 * (e.g., from a POS receipt model or a backend report action).
 */

const EVENT_NAMES = [
    "nightpos_local_print_event",
    // odoo_local_print_event intentionally removed from dispatch — content.js still
    // listens to it for truly old callers, but firing both caused double printing.
];

/** @returns {boolean} True when the NightPOS Chrome extension content script is active. */
export function isChromeExtensionAvailable() {
    return (
        document.documentElement?.getAttribute("data-nightpos-printer-ready") === "1" ||
        Boolean(window.__nightpos_printer_ready__)
    );
}

/**
 * @param {object} detail
 * @returns {object}
 */
export function buildLocalPrintDetail(detail) {
    return {
        printer_name: detail.printer_name || "",
        raw_data: detail.raw_data || "",
        encoding: detail.encoding || "base64",
        protocol: detail.protocol || "escpos",
        ...(detail.printer !== undefined && { printer: detail.printer }),
        ...(detail.source && { source: detail.source }),
        ...(detail.meta && { meta: detail.meta }),
    };
}

/**
 * Dispatch a cancelable print event for the Chrome extension.
 * @param {object} detail
 * @returns {boolean} False when a listener called preventDefault().
 */
export function dispatchLocalPrintEvent(detail) {
    const normalized = buildLocalPrintDetail(detail);
    let cancelled = false;
    for (const name of EVENT_NAMES) {
        const event = new CustomEvent(name, {
            detail: normalized,
            cancelable: true,
        });
        if (!window.dispatchEvent(event)) {
            cancelled = true;
        }
    }
    return !cancelled;
}

/** @param {object} params Same shape as Python `_prepare_local_print_params()`. */
export function dispatchLocalPrintFromParams(params) {
    return dispatchLocalPrintEvent(params);
}

/**
 * Dispatch a print event to the active Chrome extension content script.
 *
 * @param {string} printerName  Printer name as configured in the print server
 * @param {string} rawDataB64   Base64-encoded ESC/POS byte string
 */
export function triggerLocalPrint(printerName, rawDataB64) {
    dispatchLocalPrintEvent({
        printer_name: printerName,
        raw_data: rawDataB64,
    });
}

/**
 * Convert a Uint8Array of ESC/POS bytes to a base64 string.
 * @param {Uint8Array} bytes
 * @returns {string}
 */
export function uint8ToBase64(bytes) {
    let binary = "";
    for (let i = 0; i < bytes.length; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary);
}

npLog("local_printer_trigger.js loaded", {
    extensionReady: isChromeExtensionAvailable(),
});
