// Polyfills for Odoo 19 on GeckoView 99 (Firefox 99).

// Promise.withResolvers — added in Firefox 121, used by Odoo 19 OWL framework.
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

// structuredClone — added in Firefox 94, used by various Odoo 19 modules.
if (typeof structuredClone === "undefined") {
    structuredClone = function (obj) {
        return JSON.parse(JSON.stringify(obj));
    };
}
