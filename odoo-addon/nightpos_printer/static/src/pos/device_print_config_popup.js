/** @odoo-module **/

import { Component, useState, onWillStart } from "@odoo/owl";
import { Dialog } from "@web/core/dialog/dialog";
import { _t } from "@web/core/l10n/translation";
import { fetchLocalPrintersAuto } from "@nightpos_printer/js/local_print_ws";
import {
    RECEIPT_MODE_OPTIONS,
    buildDeviceLabel,
    clearDevicePrintPrefsCache,
    getDeviceKey,
    getDevicePrintPrefs,
    probeDeviceCapabilities,
    resolveDevicePrintMode,
    saveDevicePrintPrefs,
} from "@nightpos_printer/js/device_print_router";
import { configureNightposReceiptPrinter } from "@nightpos_printer/pos/pos_receipt_patch";

export class DevicePrintConfigPopup extends Component {
    static template = "nightpos_printer.DevicePrintConfigPopup";
    static components = { Dialog };
    static props = {
        pos: Object,
        getPayload: Function,
        close: Function,
    };

    setup() {
        const config = this.props.pos.config;
        const prefs = getDevicePrintPrefs(config.id) || {};
        const resolved = resolveDevicePrintMode(config);
        this.state = useState({
            receipt_mode: prefs.receipt_mode || resolved.mode || "auto",
            chrome_printer_name:
                prefs.chrome_printer_name || config.nightpos_chrome_printer_name || "",
            nightpos_receipt_printer_id:
                prefs.nightpos_receipt_printer_id || this._defaultPrinterId(config),
            name: prefs.name || buildDeviceLabel(),
            chromePrinters: [],
            loadingChrome: false,
            saving: false,
            error: null,
        });
        onWillStart(async () => {
            await this._loadChromePrinters();
        });
    }

    _defaultPrinterId(config) {
        const ref = config.nightpos_receipt_printer_id;
        if (Array.isArray(ref)) {
            return ref[0] || false;
        }
        if (typeof ref === "object" && ref?.id) {
            return ref.id;
        }
        return ref || false;
    }

    get modeOptions() {
        return RECEIPT_MODE_OPTIONS;
    }

    get printerOptions() {
        const store = this.props.pos.models?.["nightpos.printer"];
        if (!store?.getAll) {
            return [];
        }
        return store
            .getAll()
            .filter((p) => p.raw?.active !== false)
            .map((p) => ({
                id: p.id,
                name: p.name || p.raw?.name || `#${p.id}`,
            }));
    }

    get showChromePrinter() {
        return ["chrome_extension", "auto"].includes(this.state.receipt_mode);
    }

    get showTcpPrinter() {
        return ["tcp", "sunmi", "auto"].includes(this.state.receipt_mode);
    }

    get deviceKeyShort() {
        const key = getDeviceKey();
        return key.length > 12 ? `${key.slice(0, 8)}…` : key;
    }

    async _loadChromePrinters() {
        this.state.loadingChrome = true;
        try {
            this.state.chromePrinters = await fetchLocalPrintersAuto();
        } catch (_) {
            this.state.chromePrinters = [];
        } finally {
            this.state.loadingChrome = false;
        }
    }

    onModeChange(ev) {
        this.state.receipt_mode = ev.target.value;
    }

    onPrinterChange(ev) {
        const val = parseInt(ev.target.value, 10);
        this.state.nightpos_receipt_printer_id = Number.isFinite(val) ? val : false;
    }

    onChromeChange(ev) {
        this.state.chrome_printer_name = ev.target.value;
    }

    onNameChange(ev) {
        this.state.name = ev.target.value;
    }

    async onSave() {
        this.state.saving = true;
        this.state.error = null;
        try {
            const configId = this.props.pos.config.id;
            clearDevicePrintPrefsCache(configId);
            await saveDevicePrintPrefs(configId, {
                receipt_mode: this.state.receipt_mode,
                chrome_printer_name: this.state.chrome_printer_name,
                nightpos_receipt_printer_id: this.state.nightpos_receipt_printer_id,
                name: this.state.name || buildDeviceLabel(),
            });
            await probeDeviceCapabilities();
            configureNightposReceiptPrinter(this.props.pos);
            this.props.getPayload(true);
            this.props.close();
        } catch (error) {
            this.state.error = String(error.message || error);
        } finally {
            this.state.saving = false;
        }
    }

    onDiscard() {
        this.props.close();
    }
}
