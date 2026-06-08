/** @odoo-module **/

import { patch } from "@web/core/utils/patch";
import { PosOrder } from "@point_of_sale/app/models/pos_order";
import { PosOrderlineAccounting } from "@point_of_sale/app/models/accounting/pos_order_line_accounting";
import { PosStore } from "@point_of_sale/app/services/pos_store";
import { HardwareProxy } from "@point_of_sale/app/services/hardware_proxy_service";
import { NightposEscposPrinter } from "@nightpos_printer/pos/nightpos_escpos_printer";
import {
    probeDeviceCapabilities,
    ensureDevicePrintPrefs,
    getNightposPrinterSetup,
    getNightposPreparationPrinterSetup,
    resolveDevicePrintMode,
    usesNightposReceiptPrinter,
} from "@nightpos_printer/js/device_print_router";
import {
    dumpPosConfig,
    formatNightposError,
    npBoot,
    npError,
    npLog,
    npWarn,
} from "@nightpos_printer/js/nightpos_pos_debug";

npBoot("pos_receipt_patch.js loaded");

function _readRelationId(value) {
    if (value == null || value === false) {
        return null;
    }
    if (typeof value === "number") {
        return value;
    }
    if (Array.isArray(value)) {
        return value[0];
    }
    if (typeof value === "object" && value.id != null) {
        return value.id;
    }
    return null;
}

/**
 * Repair pos.config after partial IndexedDB / legacy field-list loads.
 * Odoo 19 POS records expose read-only `raw`; mutations must go through `update()`.
 */
function _repairPosConfigRecord(pos, config) {
    if (!config || typeof config.update !== "function") {
        return;
    }
    const updates = {};

    const trustedIds = config.raw?.trusted_config_ids;
    if (!Array.isArray(trustedIds)) {
        npWarn("pos.config trusted_config_ids invalid, clearing", { was: trustedIds });
        updates.trusted_config_ids = [["clear"]];
    }

    const companies = pos.data?.models?.["res.company"];
    if (companies && !config.company_id?.id) {
        const companyId =
            _readRelationId(config.company_id) ?? _readRelationId(config.raw?.company_id);
        const company =
            (companyId != null ? companies.get(companyId) : null) || companies.getFirst?.();
        if (company) {
            updates.company_id = company;
        }
    }

    const currencies = pos.data?.models?.["res.currency"];
    const companyForCurrency = updates.company_id || config.company_id;
    if (currencies && !config.currency_id?.round) {
        const currencyId =
            _readRelationId(config.currency_id) ??
            _readRelationId(config.raw?.currency_id) ??
            _readRelationId(companyForCurrency?.currency_id);
        const currency =
            (currencyId != null ? currencies.get(currencyId) : null) ||
            companyForCurrency?.currency_id ||
            currencies.getFirst?.();
        if (currency) {
            updates.currency_id = currency;
        }
    }

    if (!Object.keys(updates).length) {
        return;
    }
    try {
        config.update(updates);
    } catch (error) {
        npWarn("_repairPosConfigRecord failed (POS boot continues)", formatNightposError(error));
    }
}

/** Resolve a POS currency record with a working `.round()` (IndexedDB partial loads). */
function _resolvePosCurrency(models, config) {
    const currencies = models?.["res.currency"];
    const tryCurrency = (c) => (c?.round ? c : null);

    const direct =
        tryCurrency(config?.currency_id) ||
        tryCurrency(config?.company_id?.currency_id);
    if (direct) {
        return direct;
    }
    if (!currencies) {
        return null;
    }
    const currencyId =
        _readRelationId(config?.currency_id) ??
        _readRelationId(config?.raw?.currency_id) ??
        _readRelationId(config?.company_id?.currency_id);
    if (currencyId != null) {
        const byId = tryCurrency(currencies.get(currencyId));
        if (byId) {
            return byId;
        }
    }
    return tryCurrency(currencies.getFirst?.());
}

function _currencyRound(currency, amount) {
    if (currency?.round) {
        return currency.round(amount);
    }
    const n = Number(amount);
    return Number.isFinite(n) ? n : 0;
}

export function configureNightposReceiptPrinter(pos) {
    if (!pos?.config || !pos.printer?.setPrinter) {
        return;
    }
    const npCfg = getNightposPrinterSetup(pos.config);
    if (!npCfg) {
        npLog("receipt printer: skipped (no NightPOS setup for this config)");
        return;
    }
    const device = new NightposEscposPrinter({
        pos,
        mode: npCfg.mode,
        printerId: npCfg.printerId,
        ip: npCfg.ip,
        port: npCfg.port,
        chromePrinterName: npCfg.chromePrinterName || "",
    });
    pos.hardwareProxy.printer = device;
    pos.printer.setPrinter(device);
    npBoot("receipt printer configured", { mode: npCfg.mode, printerId: npCfg.printerId });
}

async function _nightposAfterProcessServerData(pos) {
    try {
        npBoot("POS boot: NightPOS printer setup started (background)");
        if (window.__nightpos_pos_debug) {
            window.__nightpos_pos_debug.lastConfig = pos.config;
        }
        await probeDeviceCapabilities();
        await ensureDevicePrintPrefs(pos.config);
        configureNightposReceiptPrinter(pos);

        const npCfg = getNightposPrinterSetup(pos.config);
        if (npCfg?.mode === "sunmi") {
            try {
                const { isSunmiAvailable, startStatusPolling } = await import(
                    "@nightpos_sunmi_printer/js/sunmi_printer_service"
                );
                if (isSunmiAvailable()) {
                    pos._sunmiStopPolling = startStatusPolling(15_000);
                }
            } catch (e) {
                npWarn("Sunmi status polling unavailable:", formatNightposError(e));
            }
        }
        npBoot("POS boot: NightPOS printer setup finished (background)");
    } catch (error) {
        npError("NightPOS receipt setup failed (POS still runs)", formatNightposError(error));
    }
}

try {
    patch(PosStore.prototype, {
        get company() {
            const configured = this.config?.company_id;
            if (configured && typeof configured === "object" && configured.id != null) {
                return configured;
            }
            const companies = this.data?.models?.["res.company"];
            if (companies) {
                const companyId = _readRelationId(configured) ?? _readRelationId(this.config?.raw?.company_id);
                if (companyId != null) {
                    const byId = companies.get(companyId);
                    if (byId) {
                        return byId;
                    }
                }
                const fallback = companies.getFirst?.();
                if (fallback) {
                    return fallback;
                }
            }
            return configured;
        },

        async processServerData() {
            // Core POS spreads trusted_config_ids during processServerData — repair first.
            const configRecord =
                this.data?.models?.["pos.config"]?.getFirst?.() ?? this.config;
            _repairPosConfigRecord(this, configRecord);
            const result = await super.processServerData(...arguments);
            _repairPosConfigRecord(this, this.config);
            if (this.config?.currency_id) {
                this.currency = this.config.currency_id;
            }
            return result;
        },

        async afterProcessServerData() {
            await super.afterProcessServerData(...arguments);
            npBoot("POS boot: afterProcessServerData done (navigate next)");
            // Never block POS boot: initServerData awaits this hook until markReady().
            void _nightposAfterProcessServerData(this);
        },

        createPrinter(config) {
            if (config?.printer_type === "nightpos_tcp") {
                const setup = getNightposPreparationPrinterSetup(this, config);
                if (setup) {
                    npLog("preparation printer configured", {
                        name: config.name,
                        mode: setup.mode,
                        printerId: setup.printerId,
                    });
                    return new NightposEscposPrinter({
                        pos: this,
                        mode: setup.mode,
                        printerId: setup.printerId,
                        ip: setup.ip,
                        port: setup.port,
                        chromePrinterName: setup.chromePrinterName,
                    });
                }
            }
            return super.createPrinter(...arguments);
        },

        _getNightposReceiptMode() {
            return resolveDevicePrintMode(this.config).mode;
        },

        _usesNightposReceiptPrinter() {
            return usesNightposReceiptPrinter(this.config);
        },

        get printOptions() {
            if (!this.config) {
                return { webPrintFallback: true };
            }
            if (this._usesNightposReceiptPrinter()) {
                return { webPrintFallback: false };
            }
            return { webPrintFallback: true };
        },
    });

    patch(PosOrder.prototype, {
        get currency() {
            return _resolvePosCurrency(this.models, this.config);
        },
    });

    patch(PosOrderlineAccounting.prototype, {
        get priceIncl() {
            const prices = this.prices;
            return _currencyRound(
                this.currency,
                (prices?.total_included ?? 0) * (this.order_id?.orderSign ?? 1)
            );
        },
        get priceExcl() {
            const prices = this.prices;
            return _currencyRound(
                this.currency,
                (prices?.total_excluded ?? 0) * (this.order_id?.orderSign ?? 1)
            );
        },
        get priceInclNoDiscount() {
            const prices = this.prices;
            return _currencyRound(
                this.currency,
                (prices?.no_discount_total_included ?? 0) * (this.order_id?.orderSign ?? 1)
            );
        },
        get priceExclNoDiscount() {
            const prices = this.prices;
            return _currencyRound(
                this.currency,
                (prices?.no_discount_total_excluded ?? 0) * (this.order_id?.orderSign ?? 1)
            );
        },
    });

    patch(HardwareProxy.prototype, {
        async openCashbox(action = false) {
            const pos = this.pos;
            if (pos?._usesNightposReceiptPrinter?.() && pos._getNightposReceiptMode() === "sunmi") {
                try {
                    if (window.flutter_inappwebview?.callHandler) {
                        await window.flutter_inappwebview.callHandler("SunmiPrinter", {
                            method: "openDrawer",
                        });
                        if (action) {
                            pos.logEmployeeMessage(action, "CASH_DRAWER_ACTION");
                        }
                        return;
                    }
                } catch (e) {
                    npWarn("Sunmi openDrawer failed:", formatNightposError(e));
                }
            }
            return super.openCashbox(action);
        },
    });

    npBoot("PosStore + PosOrder + PosOrderlineAccounting + HardwareProxy patches applied");

    if (typeof window !== "undefined") {
        window.__nightpos_pos_debug = {
            ...(window.__nightpos_pos_debug || {}),
            patchesApplied: true,
            probeRouter: async (config) => {
                const cfg = config || window.__nightpos_pos_debug?.lastConfig;
                const caps = await probeDeviceCapabilities();
                return {
                    capabilities: caps,
                    resolved: resolveDevicePrintMode(cfg),
                    usesNightpos: usesNightposReceiptPrinter(cfg),
                    setup: getNightposPrinterSetup(cfg),
                    config: dumpPosConfig(cfg),
                };
            },
        };
    }
} catch (error) {
    npError(
        "pos_receipt_patch.js: patch failed (POS runs without NightPOS receipt routing)",
        formatNightposError(error)
    );
}
