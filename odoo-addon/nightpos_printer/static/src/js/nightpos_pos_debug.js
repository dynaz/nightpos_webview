/** @odoo-module **/
/**
 * NightPOS POS load debugger.
 *
 * Enable logging:
 *   - Open POS with ?debug=1 (odoo.debug), or
 *   - localStorage.setItem("nightpos_pos_debug", "1") then reload
 *
 * In the browser console:
 *   window.__nightpos_pos_debug.status()
 *   await window.__nightpos_pos_debug.probeRouter(config)
 */

const TAG = "[NightPOS POS]";

/** Always logged — use for POS boot milestones (visible even without ?debug=1). */
export function npBoot(...args) {
    console.info(TAG, ...args);
}

export function isNightposPosDebugEnabled() {
    if (typeof window === "undefined") {
        return false;
    }
    try {
        if (window.localStorage?.getItem("nightpos_pos_debug") === "1") {
            return true;
        }
    } catch (_) {
        /* private mode */
    }
    return Boolean(typeof odoo !== "undefined" && odoo.debug);
}

/** @param {...unknown} args */
export function npLog(...args) {
    if (!isNightposPosDebugEnabled()) {
        return;
    }
    console.log(TAG, ...args);
}

/** @param {...unknown} args */
export function npWarn(...args) {
    if (!isNightposPosDebugEnabled()) {
        return;
    }
    console.warn(TAG, ...args);
}

/** Always logged — use for failures that may block POS load. */
export function npError(...args) {
    console.error(TAG, ...args);
}

/**
 * @param {string} label
 * @param {() => T} fn
 * @returns {T}
 * @template T
 */
export function npGroup(label, fn) {
    if (!isNightposPosDebugEnabled()) {
        return fn();
    }
    console.groupCollapsed(`${TAG} ${label}`);
    try {
        return fn();
    } finally {
        console.groupEnd();
    }
}

/**
 * @param {string} label
 * @param {() => Promise<T>} fn
 * @returns {Promise<T>}
 * @template T
 */
export async function npGroupAsync(label, fn) {
    if (!isNightposPosDebugEnabled()) {
        return fn();
    }
    console.groupCollapsed(`${TAG} ${label}`);
    try {
        return await fn();
    } finally {
        console.groupEnd();
    }
}

/**
 * @param {Error|unknown} error
 * @returns {object}
 */
export function formatNightposError(error) {
    if (error instanceof Error) {
        return {
            name: error.name,
            message: error.message,
            stack: error.stack,
        };
    }
    return { message: String(error) };
}

/**
 * Safe snapshot of pos.config for the console (no circular refs).
 * @param {object|null|undefined} config
 */
export function dumpPosConfig(config) {
    if (!config) {
        return { present: false };
    }
    const ref = config.nightpos_receipt_printer_id;
    let printer = null;
    if (ref && typeof ref === "object") {
        const raw = ref.raw || ref;
        printer = {
            id: ref.id ?? raw.id,
            name: raw.name,
            ip: raw.ip,
            port: raw.port,
            chrome_printer_name: raw.chrome_printer_name,
        };
    } else if (ref) {
        printer = { rawRef: ref };
    }
    return {
        present: true,
        id: config.id,
        nightpos_receipt_mode: config.nightpos_receipt_mode,
        nightpos_receipt_device_policy: config.nightpos_receipt_device_policy,
        nightpos_receipt_enabled: config.nightpos_receipt_enabled,
        nightpos_chrome_printer_name: config.nightpos_chrome_printer_name,
        nightpos_receipt_printer_id: printer,
        other_devices: config.other_devices,
        epson_printer_ip: config.epson_printer_ip,
        iface_print_via_proxy: config.iface_print_via_proxy,
    };
}

function _installGlobalHooks() {
    if (typeof window === "undefined" || window.__nightpos_pos_debug_hooks) {
        return;
    }
    window.__nightpos_pos_debug_hooks = true;

    window.addEventListener(
        "error",
        (ev) => {
            const file = ev.filename || "";
            if (!file.includes("nightpos_printer")) {
                return;
            }
            npError("uncaught error in NightPOS asset", {
                message: ev.message,
                file,
                line: ev.lineno,
                col: ev.colno,
                error: formatNightposError(ev.error),
            });
        },
        true
    );

    window.addEventListener("unhandledrejection", (ev) => {
        const stack = ev.reason?.stack || String(ev.reason);
        if (!stack.includes("nightpos_printer") && !stack.includes("NightPOS")) {
            return;
        }
        npError("unhandled promise rejection", formatNightposError(ev.reason));
    });
}

function _buildDebugApi() {
    return {
        enabled: isNightposPosDebugEnabled,
        enable() {
            localStorage.setItem("nightpos_pos_debug", "1");
            npLog("Debug enabled — reload POS tab");
        },
        disable() {
            localStorage.removeItem("nightpos_pos_debug");
            npLog("Debug disabled — reload POS tab");
        },
        status() {
            const info = {
                debugEnabled: isNightposPosDebugEnabled(),
                odoo_debug: typeof odoo !== "undefined" ? odoo.debug : undefined,
                chromeExtension: document.documentElement?.getAttribute(
                    "data-nightpos-printer-ready"
                ),
                pos_session_id: typeof odoo !== "undefined" ? odoo.pos_session_id : undefined,
                hooksInstalled: Boolean(window.__nightpos_pos_debug_hooks),
            };
            console.table(info);
            return info;
        },
        dumpPosConfig,
    };
}

_installGlobalHooks();

if (typeof window !== "undefined") {
    window.__nightpos_pos_debug = {
        ..._buildDebugApi(),
        ...(window.__nightpos_pos_debug || {}),
    };
}

npLog("nightpos_pos_debug.js loaded", {
    odoo_debug: typeof odoo !== "undefined" ? odoo.debug : undefined,
    force: typeof window !== "undefined" && window.localStorage?.getItem("nightpos_pos_debug"),
});
