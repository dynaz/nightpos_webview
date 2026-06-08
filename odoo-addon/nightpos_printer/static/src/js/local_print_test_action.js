/** @odoo-module **/

import { registry } from "@web/core/registry";
import { _t } from "@web/core/l10n/translation";
import { dispatchLocalPrintFromParams } from "@nightpos_printer/js/local_printer_trigger";
import { LocalPrintTestDialog } from "@nightpos_printer/js/local_print_test_dialog";

async function handleLocalPrintTestAction(env, action) {
    env.services.dialog.add(LocalPrintTestDialog, action.params);
}

async function handleLocalPrintAction(env, action) {
    const notification = env.services.notification;
    try {
        const ok = dispatchLocalPrintFromParams(action.params);
        if (ok) {
            notification.add(
                _t('Print job sent to Chrome Extension (%s).', action.params.printer_name),
                { type: "success" }
            );
        } else {
            notification.add(
                _t("Chrome Extension cancelled the print event."),
                { type: "warning" }
            );
        }
    } catch (error) {
        notification.add(String(error.message || error), { type: "danger" });
    }
}

registry.category("actions").add(
    "nightpos_printer.local_print_test",
    handleLocalPrintTestAction
);
registry.category("actions").add("nightpos_printer.local_print", handleLocalPrintAction);
