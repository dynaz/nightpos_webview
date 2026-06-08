/** @odoo-module **/

import { LoginScreen } from "@point_of_sale/app/screens/login_screen/login_screen";
import { DevicePrintConfigPopup } from "@nightpos_printer/pos/device_print_config_popup";
import { makeAwaitable } from "@point_of_sale/app/utils/make_awaitable_dialog";
import { patch } from "@web/core/utils/patch";
import { _t } from "@web/core/l10n/translation";
import {
    RECEIPT_MODE_OPTIONS,
    getDevicePrintPrefs,
    resolveDevicePrintMode,
} from "@nightpos_printer/js/device_print_router";

patch(LoginScreen.prototype, {
    get showDevicePrintButton() {
        return this.pos?.config?.nightpos_receipt_device_policy === "per_device";
    },
    get devicePrintButtonLabel() {
        if (!this.showDevicePrintButton || !this.pos?.config?.id) {
            return _t("This device printer");
        }
        const prefs = getDevicePrintPrefs(this.pos.config.id);
        const { mode } = resolveDevicePrintMode(this.pos.config);
        const activeMode = prefs?.receipt_mode || mode;
        const modeLabel =
            RECEIPT_MODE_OPTIONS.find((o) => o.value === activeMode)?.label || activeMode;
        return _t("This device: %(mode)s", { mode: modeLabel });
    },
    async openDevicePrintSettings() {
        await makeAwaitable(this.dialog, DevicePrintConfigPopup, {
            pos: this.pos,
        });
    },
});
