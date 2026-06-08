/** @odoo-module **/
/**
 * POS boot watchdog for live debugging (?debug=1).
 * Console: __nightpos_boot_report()
 */

import { patch } from "@web/core/utils/patch";
import { PosStore } from "@point_of_sale/app/services/pos_store";
import { npBoot, npError, npWarn, formatNightposError, isNightposPosDebugEnabled } from "@nightpos_printer/js/nightpos_pos_debug";

const BOOT_TIMEOUT_MS = 90_000;
const TAG = "[NightPOS POS boot]";

function bootEnabled() {
    return isNightposPosDebugEnabled();
}

function ensureBootState() {
    if (typeof window === "undefined") {
        return null;
    }
    if (!window.__nightpos_boot) {
        window.__nightpos_boot = {
            startedAt: Date.now(),
            steps: [],
            errors: [],
            markReadyAt: null,
            setupFinishedAt: null,
        };
    }
    return window.__nightpos_boot;
}

function recordStep(name, extra = {}) {
    const state = ensureBootState();
    if (!state) {
        return;
    }
    state.steps.push({ at: Date.now(), name, ...extra });
    if (bootEnabled()) {
        console.info(TAG, name, extra);
    }
}

function recordError(label, error) {
    const state = ensureBootState();
    const formatted = formatNightposError(error);
    if (state) {
        state.errors.push({ at: Date.now(), label, ...formatted });
    }
    npError(`boot error: ${label}`, formatted);
}

function wrapMarkReady(pos) {
    if (pos._nightposMarkReadyWrapped || typeof pos.markReady !== "function") {
        return;
    }
    const orig = pos.markReady;
    pos._nightposMarkReadyWrapped = true;
    pos.markReady = () => {
        const state = ensureBootState();
        if (state) {
            state.markReadyAt = Date.now();
        }
        recordStep("markReady.called");
        npBoot("POS boot: markReady() — loader should hide");
        return orig();
    };
}

function installGlobalBootHooks() {
    if (typeof window === "undefined" || window.__nightpos_boot_hooks) {
        return;
    }
    window.__nightpos_boot_hooks = true;
    ensureBootState();

    const isPosAsset = (file = "", stack = "") => {
        const s = `${file} ${stack}`;
        return /point_of_sale|nightpos_|pos_restaurant|pos_product_stock|pos_fixed|pos_tcp/i.test(s);
    };

    window.addEventListener(
        "error",
        (ev) => {
            if (!isPosAsset(ev.filename, ev.error?.stack)) {
                return;
            }
            recordError(`uncaught: ${ev.message}`, ev.error || ev.message);
        },
        true
    );

    window.addEventListener("unhandledrejection", (ev) => {
        const reason = ev.reason;
        if (!isPosAsset("", reason?.stack || String(reason))) {
            return;
        }
        recordError("unhandled rejection", reason);
    });

    setTimeout(() => {
        const state = window.__nightpos_boot;
        if (!state || state.setupFinishedAt) {
            return;
        }
        npWarn(
            `POS setup not finished after ${BOOT_TIMEOUT_MS / 1000}s — run __nightpos_boot_report()`,
            { steps: state.steps, errors: state.errors }
        );
    }, BOOT_TIMEOUT_MS);
}

installGlobalBootHooks();

window.__nightpos_boot_report = function bootReport() {
    const state = window.__nightpos_boot;
    if (!state) {
        console.warn(TAG, "No boot data — reload POS tab");
        return null;
    }
    const summary = {
        ageMs: Date.now() - state.startedAt,
        markReady: Boolean(state.markReadyAt),
        setupDone: Boolean(state.setupFinishedAt),
        lastStep: state.steps.at(-1)?.name,
        errorCount: state.errors.length,
        pos_session_id: typeof odoo !== "undefined" ? odoo.pos_session_id : undefined,
        pos_config_id: typeof odoo !== "undefined" ? odoo.pos_config_id : undefined,
    };
    console.table(summary);
    console.log(TAG, "steps", state.steps);
    if (state.errors.length) {
        console.log(TAG, "errors", state.errors);
    }
    return { summary, state };
};

patch(PosStore.prototype, {
    async initServerData() {
        wrapMarkReady(this);
        recordStep("initServerData.start");
        const t0 = performance.now();
        try {
            const result = await super.initServerData(...arguments);
            recordStep("initServerData.done", { ms: Math.round(performance.now() - t0) });
            return result;
        } catch (error) {
            recordError("initServerData", error);
            throw error;
        }
    },

    async setup(env, deps) {
        recordStep("PosStore.setup.start");
        npBoot("POS boot watchdog: setup started");
        const t0 = performance.now();
        try {
            await super.setup(env, deps);
            const state = ensureBootState();
            if (state) {
                state.setupFinishedAt = Date.now();
            }
            recordStep("PosStore.setup.done", { ms: Math.round(performance.now() - t0) });
            npBoot("POS boot watchdog: setup finished", {
                ms: Math.round(performance.now() - t0),
                configId: this.config?.id,
            });
        } catch (error) {
            recordError("PosStore.setup", error);
            throw error;
        }
    },

    async afterProcessServerData() {
        wrapMarkReady(this);
        recordStep("afterProcessServerData.start");
        try {
            const result = await super.afterProcessServerData(...arguments);
            recordStep("afterProcessServerData.done");
            return result;
        } catch (error) {
            recordError("afterProcessServerData", error);
            throw error;
        }
    },
});

npBoot("pos_boot_watchdog.js loaded — if stuck, run __nightpos_boot_report()");
