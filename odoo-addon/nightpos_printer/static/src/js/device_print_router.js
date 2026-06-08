/** @odoo-module **/
/**
 * NightPOS device print routing for POS and backend test actions.
 * Per-device overrides are stored in nightpos.device.print (database).
 */

import { registry } from "@web/core/registry";
import { rpc } from "@web/core/network/rpc";
import { isChromeExtensionAvailable } from "@nightpos_printer/js/local_printer_trigger";
import { isNightposPosDebugEnabled, npLog } from "@nightpos_printer/js/nightpos_pos_debug";

const EXTENSION_EVENT = "nightpos_local_print_event";
const DEVICE_KEY_STORAGE = "nightpos_device_key";
/** @deprecated Migrated to database on load */
const LEGACY_OVERRIDE_KEY = "nightpos_device_print_override";

/** @type {{ chromeExtension: boolean, nightposApp: boolean, sunmi: boolean }} */
let _capabilities = {
    chromeExtension: false,
    nightposApp: false,
    sunmi: false,
};

/** @type {Map<number, object>} pos.config id → prefs from server */
const _devicePrefsByConfigId = new Map();

/** @type {string|null} */
let _deviceKey = null;

export function isNightposAppAvailable() {
    // flutter_inappwebview = Flutter InAppWebView (original target)
    // NightPOSBridge = standard Android WebView with SunmiJsBridge injected
    return typeof window.flutter_inappwebview !== "undefined" ||
           typeof window.NightPOSBridge !== "undefined";
}

export function isExtensionAvailable() {
    return isChromeExtensionAvailable();
}

const BRIDGE_PROBE_MS = 2500;

/**
 * @param {Promise<T>} promise
 * @param {number} ms
 * @returns {Promise<T>}
 * @template T
 */
function withTimeout(promise, ms) {
    return Promise.race([
        promise,
        new Promise((_, reject) =>
            setTimeout(() => reject(new Error("NightPOS bridge probe timeout")), ms)
        ),
    ]);
}

/**
 * Stable device id (UUID in localStorage). Only the key is local; print settings live in DB.
 * @returns {string}
 */
export function getDeviceKey() {
    if (_deviceKey) {
        return _deviceKey;
    }
    try {
        let key = localStorage.getItem(DEVICE_KEY_STORAGE);
        if (!key) {
            key =
                typeof crypto !== "undefined" && crypto.randomUUID
                    ? crypto.randomUUID()
                    : `np-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
            localStorage.setItem(DEVICE_KEY_STORAGE, key);
        }
        _deviceKey = key;
        return key;
    } catch (_) {
        _deviceKey = _deviceKey || `np-session-${Date.now()}`;
        return _deviceKey;
    }
}

/**
 * @param {number} configId
 * @returns {object|null}
 */
export function getDevicePrintPrefs(configId) {
    if (!configId) {
        return null;
    }
    return _devicePrefsByConfigId.get(configId) || null;
}

function _readLegacyOverride(configId) {
    try {
        const overrides = JSON.parse(localStorage.getItem(LEGACY_OVERRIDE_KEY) || "{}");
        return overrides[configId] || null;
    } catch (_) {
        return null;
    }
}

function _clearLegacyOverride(configId) {
    try {
        const overrides = JSON.parse(localStorage.getItem(LEGACY_OVERRIDE_KEY) || "{}");
        if (overrides[configId]) {
            delete overrides[configId];
            localStorage.setItem(LEGACY_OVERRIDE_KEY, JSON.stringify(overrides));
        }
    } catch (_) {
        /* ignore */
    }
}

/**
 * Load (and optionally migrate) per-device prefs from the server.
 * @param {number} configId
 * @returns {Promise<object|null>}
 */
export async function loadDevicePrintPrefs(configId) {
    if (!configId) {
        return null;
    }
    const deviceKey = getDeviceKey();
    try {
        const result = await rpc("/nightpos/device_print/get", {
            pos_config_id: configId,
            device_key: deviceKey,
        });
        let prefs = result?.prefs && Object.keys(result.prefs).length ? result.prefs : null;

        if (!prefs) {
            const legacyMode = _readLegacyOverride(configId);
            if (legacyMode) {
                const saved = await saveDevicePrintPrefs(configId, { receipt_mode: legacyMode });
                prefs = saved;
                _clearLegacyOverride(configId);
                npLog("Migrated browser print override to database", { configId, legacyMode });
            }
        }

        if (prefs) {
            _devicePrefsByConfigId.set(configId, prefs);
        }
        return prefs;
    } catch (error) {
        npLog("Failed to load device print prefs", { configId, error: String(error) });
        return null;
    }
}

/**
 * @param {number} configId
 * @param {{ receipt_mode?: string, chrome_printer_name?: string, nightpos_receipt_printer_id?: number|false, name?: string }} vals
 * @returns {Promise<object|null>}
 */
export async function saveDevicePrintPrefs(configId, vals) {
    if (!configId) {
        return null;
    }
    const deviceKey = getDeviceKey();
    try {
        const result = await rpc("/nightpos/device_print/set", {
            pos_config_id: configId,
            device_key: deviceKey,
            receipt_mode: vals.receipt_mode,
            chrome_printer_name: vals.chrome_printer_name,
            nightpos_receipt_printer_id: vals.nightpos_receipt_printer_id,
            name: vals.name,
        });
        const prefs = result?.prefs || null;
        if (prefs) {
            _devicePrefsByConfigId.set(configId, prefs);
        }
        return prefs;
    } catch (error) {
        npLog("Failed to save device print prefs", { configId, error: String(error) });
        throw error;
    }
}

/**
 * @param {object} config
 * @returns {Promise<object|null>}
 */
function _getShopPrinterId(config) {
    const ref = config?.nightpos_receipt_printer_id;
    if (Array.isArray(ref)) {
        return ref[0] || false;
    }
    if (typeof ref === "object" && ref !== null) {
        return ref.id ?? ref.raw?.id ?? false;
    }
    return ref || false;
}

/**
 * Human-readable label stored on the device profile (Android, Chrome, etc.).
 * @returns {string}
 */
export function buildDeviceLabel() {
    if (isNightposAppAvailable()) {
        return _capabilities.sunmi ? "NightPOS Android (Sunmi)" : "NightPOS Android";
    }
    if (isExtensionAvailable()) {
        return "Chrome / PC (extension)";
    }
    const ua = typeof navigator !== "undefined" ? navigator.userAgent : "";
    if (/Android/i.test(ua)) {
        return "Android browser";
    }
    if (/iPhone|iPad/i.test(ua)) {
        return "iOS browser";
    }
    return "Web browser";
}

/**
 * @param {object} config
 * @returns {Promise<object|null>}
 */
export async function ensureDevicePrintPrefs(config) {
    if (!config?.id || _getDevicePolicy(config) !== "per_device") {
        return null;
    }
    let prefs = _devicePrefsByConfigId.get(config.id);
    if (!prefs) {
        prefs = await loadDevicePrintPrefs(config.id);
    }
    if (!prefs?.receipt_mode) {
        const detected = _resolveAutoMode(config);
        prefs = await saveDevicePrintPrefs(config.id, {
            receipt_mode: detected,
            name: buildDeviceLabel(),
            nightpos_receipt_printer_id: _getShopPrinterId(config) || false,
        });
        npLog("Created per-device print profile", {
            configId: config.id,
            deviceKey: getDeviceKey(),
            prefs,
        });
    }
    return prefs;
}

/** Clear cached prefs (e.g. after user saves new settings). */
export function clearDevicePrintPrefsCache(configId) {
    if (configId) {
        _devicePrefsByConfigId.delete(configId);
    }
}

/**
 * Set receipt mode for this device (saved in database).
 * @param {number} configId
 * @param {string} mode
 * @param {{ chrome_printer_name?: string, nightpos_receipt_printer_id?: number }} [options]
 * @returns {Promise<object|null>}
 */
export async function setDeviceOverride(configId, mode, options = {}) {
    return saveDevicePrintPrefs(configId, {
        receipt_mode: mode,
        chrome_printer_name: options.chrome_printer_name,
        nightpos_receipt_printer_id: options.nightpos_receipt_printer_id,
        name: options.name,
    });
}

/**
 * Probe bridges once per POS session start.
 * @returns {Promise<object>}
 */
export async function probeDeviceCapabilities() {
    _capabilities = {
        chromeExtension: isExtensionAvailable(),
        nightposApp: isNightposAppAvailable(),
        sunmi: false,
    };
    if (_capabilities.nightposApp) {
        try {
            const res = await withTimeout(
                window.flutter_inappwebview.callHandler("SunmiPrinter", {
                    method: "isPrinterConnected",
                }),
                BRIDGE_PROBE_MS
            );
            if (res?.success === false) {
                _capabilities.sunmi = false;
            } else {
                _capabilities.sunmi = Boolean(res?.connected);
            }
        } catch (_) {
            _capabilities.sunmi = false;
        }
    }
    return { ..._capabilities };
}

function _getShopMode(config) {
    return config?.nightpos_receipt_mode || "odoo";
}

function _getDevicePolicy(config) {
    return config?.nightpos_receipt_device_policy || "auto";
}

function _resolveAutoMode(config) {
    if (_capabilities.chromeExtension) {
        return "chrome_extension";
    }
    if (_capabilities.sunmi) {
        return "sunmi";
    }
    if (_capabilities.nightposApp && _getEffectivePrinterId(config)) {
        return "tcp";
    }
    return "odoo";
}

function _getEffectivePrinterId(config) {
    const prefs = config?.id ? getDevicePrintPrefs(config.id) : null;
    if (_getDevicePolicy(config) === "per_device" && prefs?.nightpos_receipt_printer_id) {
        return prefs.nightpos_receipt_printer_id;
    }
    const ref = config?.nightpos_receipt_printer_id;
    if (Array.isArray(ref)) {
        return ref[0];
    }
    if (typeof ref === "object" && ref !== null) {
        return ref.id ?? ref.raw?.id;
    }
    return ref || null;
}

/**
 * Chrome / Windows queue name for this device when policy is per_device.
 * @param {object} config
 * @returns {string}
 */
export function getDeviceChromePrinterName(config) {
    const prefs = config?.id ? getDevicePrintPrefs(config.id) : null;
    if (_getDevicePolicy(config) === "per_device" && prefs?.chrome_printer_name) {
        return prefs.chrome_printer_name;
    }
    return config?.nightpos_chrome_printer_name || "";
}

/**
 * @param {object} config  pos.config record in the POS store
 * @returns {{ mode: string, capabilities: object }}
 */
export function resolveDevicePrintMode(config) {
    const policy = _getDevicePolicy(config);
    let mode = _getShopMode(config);

    if (policy === "shop") {
        mode = _getShopMode(config);
    } else if (policy === "per_device" && config?.id) {
        const prefs = getDevicePrintPrefs(config.id);
        if (prefs?.receipt_mode) {
            mode = prefs.receipt_mode;
        } else {
            mode = "auto";
        }
    }

    if (mode === "auto" || policy === "auto") {
        mode = _resolveAutoMode(config);
    }

    if (["tcp", "chrome_extension", "sunmi"].includes(mode) && !_getEffectivePrinterId(config)) {
        if (mode !== "chrome_extension") {
            mode = "odoo";
        }
    }
    if (mode === "sunmi" && !_capabilities.nightposApp && !_capabilities.sunmi) {
        mode = _resolveAutoMode(config);
    }

    return { mode, capabilities: { ..._capabilities } };
}

/**
 * @param {object} config
 * @returns {boolean}
 */
export function usesNightposReceiptPrinter(config) {
    const { mode } = resolveDevicePrintMode(config);
    return ["tcp", "sunmi", "chrome_extension"].includes(mode);
}

function _getPrinterFromConfig(config) {
    const printerId = _getEffectivePrinterId(config);
    if (!printerId) {
        return null;
    }
    const store = config?.models?.["nightpos.printer"];
    if (store?.get) {
        const record = store.get(printerId);
        if (record) {
            const raw = record.raw || record;
            return {
                id: record.id ?? raw.id,
                name: raw.name,
                ip: raw.ip || "",
                port: raw.port ?? 9100,
                printer_type: raw.printer_type,
                chrome_printer_name: raw.chrome_printer_name,
            };
        }
    }
    const ref = config?.nightpos_receipt_printer_id;
    if (typeof ref === "object" && ref !== null) {
        const raw = ref.raw || ref;
        if (raw.ip !== undefined) {
            return {
                id: ref.id ?? raw.id,
                name: raw.name,
                ip: raw.ip,
                port: raw.port ?? 9100,
                chrome_printer_name: raw.chrome_printer_name,
            };
        }
    }
    return null;
}

/**
 * @param {object} config
 * @returns {{ mode: string, printerId: number, ip: string, port: number } | null}
 */
export function getNightposPrinterSetup(config) {
    if (!usesNightposReceiptPrinter(config)) {
        return null;
    }
    const { mode } = resolveDevicePrintMode(config);
    const printer = _getPrinterFromConfig(config);
    if (!printer && mode !== "chrome_extension") {
        return null;
    }
    const chromeName =
        getDeviceChromePrinterName(config) ||
        printer?.chrome_printer_name ||
        config?.nightpos_chrome_printer_name ||
        "";
    return {
        mode,
        printerId: printer?.id,
        ip: mode === "sunmi" ? (printer?.ip || "") : (printer?.ip || "127.0.0.1"),
        port: printer?.port ?? 9100,
        chromePrinterName: chromeName,
    };
}

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

function _resolvePreparationPrinterMode(rawPrinterType, hasIp) {
    let mode = rawPrinterType || "tcp";
    if (mode === "auto") {
        if (_capabilities.chromeExtension) {
            mode = "chrome_extension";
        } else if (_capabilities.sunmi) {
            mode = "sunmi";
        } else if (hasIp) {
            mode = "tcp";
        } else {
            mode = "odoo";
        }
    }
    return mode;
}

/**
 * Build NightposEscposPrinter options for a pos.printer (kitchen / bar ticket).
 * @param {object} pos
 * @param {object} posPrinterConfig  raw pos.printer from the POS data store
 * @returns {{ mode: string, printerId: number, ip: string, port: number, chromePrinterName: string } | null}
 */
export function getNightposPreparationPrinterSetup(pos, posPrinterConfig) {
    const nightposId = _readRelationId(posPrinterConfig?.nightpos_printer_id);
    const store = pos?.data?.models?.["nightpos.printer"];
    const record = nightposId && store?.get?.(nightposId);
    const raw = record?.raw ?? record;

    const ip = raw?.ip ?? posPrinterConfig?.nightpos_printer_ip;
    const port = raw?.port ?? posPrinterConfig?.nightpos_printer_port ?? 9100;
    const printerId = nightposId ?? raw?.id;

    const mode = _resolvePreparationPrinterMode(
        raw?.printer_type,
        Boolean(ip) || raw?.printer_type === "sunmi"
    );
    if (!printerId && !ip && mode !== "sunmi") {
        return null;
    }
    if (mode === "odoo") {
        return null;
    }

    return {
        mode,
        printerId,
        ip: mode === "sunmi" ? (ip || "") : (ip || "127.0.0.1"),
        port: port ?? 9100,
        chromePrinterName: raw?.chrome_printer_name || "",
    };
}

async function printViaExtension(printerName, rawDataBase64) {
    return new Promise((resolve, reject) => {
        const timeout = setTimeout(
            () => reject(new Error("NightPOS extension did not respond in time")),
            8000
        );

        const onResult = (event) => {
            clearTimeout(timeout);
            window.removeEventListener("nightpos_print_result", onResult);
            if (event.detail?.success) {
                resolve();
            } else {
                reject(new Error(event.detail?.error || "Print failed"));
            }
        };

        window.addEventListener("nightpos_print_result", onResult, { once: true });

        window.dispatchEvent(
            new CustomEvent(EXTENSION_EVENT, {
                detail: {
                    printer_name: printerName || "",
                    raw_data: rawDataBase64,
                },
            })
        );
    });
}

/** @deprecated Use getNightposPrinterSetup / POS printer service instead */
export async function routePrintJob(printerName, rawDataBase64) {
    if (isExtensionAvailable()) {
        return printViaExtension(printerName, rawDataBase64);
    }
    window.dispatchEvent(
        new CustomEvent(EXTENSION_EVENT, {
            detail: { printer_name: printerName || "", raw_data: rawDataBase64 },
        })
    );
}

registry.category("nightpos_print_router").add("local", { routePrintJob });

if (typeof window !== "undefined") {
    window.NightPOSDevicePrint = {
        getDeviceKey,
        getDevicePrintPrefs,
        loadDevicePrintPrefs,
        saveDevicePrintPrefs,
        setDeviceOverride,
        ensureDevicePrintPrefs,
        clearDevicePrintPrefsCache,
        resolveDevicePrintMode,
        getDeviceChromePrinterName,
        buildDeviceLabel,
    };
}

export const RECEIPT_MODE_OPTIONS = [
    { value: "auto", label: "Auto (this device)" },
    { value: "tcp", label: "TCP/IP (Server)" },
    { value: "sunmi", label: "Sunmi built-in" },
    { value: "chrome_extension", label: "Chrome Extension" },
    { value: "odoo", label: "Browser / IoT Box" },
];

npLog("device_print_router.js loaded", {
    exports: [
        "probeDeviceCapabilities",
        "resolveDevicePrintMode",
        "getNightposPrinterSetup",
        "usesNightposReceiptPrinter",
        "ensureDevicePrintPrefs",
        "setDeviceOverride",
        "isExtensionAvailable",
        "isNightposAppAvailable",
    ],
    debug: isNightposPosDebugEnabled(),
});
