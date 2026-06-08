/** @odoo-module **/
/**
 * NightPOS Local Printer — device_print_test_action.js
 *
 * Client action "nightpos_print_test" — sends a short ESC/POS test page
 * to verify end-to-end connectivity (Chrome extension → print server → printer).
 */

import { registry } from "@web/core/registry";
import { triggerLocalPrint, uint8ToBase64 } from "./local_printer_trigger";

/** Build a minimal ESC/POS test receipt (ASCII-safe, no graphics). */
function buildTestReceipt(printerName) {
    const ESC  = 0x1b;
    const LF   = 0x0a;
    const GS   = 0x1d;
    const line = (text) => [...text].map(c => c.charCodeAt(0));

    const bytes = [
        // Init
        ESC, 0x40,
        // Center align
        ESC, 0x61, 0x01,
        // Bold on
        ESC, 0x45, 0x01,
        ...line("NightPOS Printer Test"), LF,
        // Bold off
        ESC, 0x45, 0x00,
        ...line("------------------------"), LF,
        ...line(`Printer: ${printerName || "default"}`), LF,
        ...line(new Date().toLocaleString()), LF,
        ...line("------------------------"), LF,
        ...line("If you see this, the"), LF,
        ...line("extension + server are"), LF,
        ...line("working correctly!"), LF,
        LF, LF,
        // Cut
        GS, 0x56, 0x42, 0x00,
    ];

    return new Uint8Array(bytes);
}

async function nightposPrintTestAction(env, action) {
    const ctx         = action.context || {};
    const printerName = ctx.printer_name || "";

    const bytes  = buildTestReceipt(printerName);
    const b64    = uint8ToBase64(bytes);

    triggerLocalPrint(printerName, b64);

    env.services.notification.add(
        `Test page sent to "${printerName || "default printer"}"`,
        { type: "info", sticky: false }
    );
}

registry
    .category("actions")
    .add("nightpos_print_test", nightposPrintTestAction);
