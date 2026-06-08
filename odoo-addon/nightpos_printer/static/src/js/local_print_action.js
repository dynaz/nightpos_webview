/** @odoo-module **/
/**
 * NightPOS Local Printer — local_print_action.js
 *
 * Registers a client action handler "nightpos_local_print" that the Python
 * backend can trigger via `{"type": "ir.actions.client", "tag": "nightpos_local_print"}`.
 *
 * The action's `context` is expected to contain:
 *   - printer_name {string}
 *   - raw_data     {string}  Base64-encoded ESC/POS bytes
 */

import { registry } from "@web/core/registry";
import { triggerLocalPrint } from "./local_printer_trigger";

async function nightposLocalPrintAction(env, action) {
    const ctx = action.context || {};
    const printerName = ctx.printer_name || "";
    const rawData     = ctx.raw_data     || "";

    if (!rawData) {
        env.services.notification.add(
            "NightPOS Printer: no print data in action context.",
            { type: "warning" }
        );
        return;
    }

    triggerLocalPrint(printerName, rawData);

    env.services.notification.add(
        `Print job sent to "${printerName || "default printer"}"`,
        { type: "success", sticky: false }
    );
}

registry
    .category("actions")
    .add("nightpos_local_print", nightposLocalPrintAction);
