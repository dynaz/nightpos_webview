/** @odoo-module **/
/**
 * Registers nightpos_printer.test_print_routed as early as possible.
 * Delegates to nightpos_printer.test_print once the full handler module loads.
 * Fixes KeyNotFoundError when the server DB/Python still references the routed tag.
 */

import { registry } from "@web/core/registry";
import { _t } from "@web/core/l10n/translation";

async function handleTestPrintRouted(env, action) {
    const actions = registry.category("actions");
    if (actions.contains("nightpos_printer.test_print")) {
        return actions.get("nightpos_printer.test_print")(env, action);
    }
    env.services.notification.add(
        _t(
            "NightPOS Printer JavaScript is not loaded. Upgrade module "
            + "nightpos_printer, restart Odoo, then hard-refresh this page (Ctrl+Shift+R)."
        ),
        { type: "danger", sticky: true }
    );
}

registry.category("actions").add(
    "nightpos_printer.test_print_routed",
    handleTestPrintRouted
);
