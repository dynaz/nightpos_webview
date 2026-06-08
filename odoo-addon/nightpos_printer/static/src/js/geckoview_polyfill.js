/**
 * Polyfills for GeckoView 99 (Firefox 99) running Odoo 19.
 *
 * GeckoView 99 is the last arm64-split build on Mozilla Maven that avoids the
 * SELinux socket-ioctl crash on Sunmi T1 (Android 6.0.1 / kernel 3.10).
 * Odoo 19 uses APIs that were added in later Firefox versions; this file
 * patches them so the POS UI loads correctly.
 *
 * This file is prepended to web.assets_web so it runs before any OWL/Odoo JS.
 */

// Promise.withResolvers — added Firefox 121; used by OWL module loader (Odoo 19).
if (typeof Promise.withResolvers === "undefined") {
    Promise.withResolvers = function () {
        var resolve, reject;
        var promise = new Promise(function (res, rej) {
            resolve = res;
            reject = rej;
        });
        return { promise: promise, resolve: resolve, reject: reject };
    };
}

// structuredClone — added Firefox 94; used in various Odoo 19 utilities.
if (typeof structuredClone === "undefined") {
    /* eslint-disable no-global-assign */
    structuredClone = function (value) {
        return JSON.parse(JSON.stringify(value));
    };
}
