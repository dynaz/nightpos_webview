/** @odoo-module **/

/**
 * Register the NightPOS service worker and track the PWA install prompt.
 * Runs once when the web client bundle loads.
 */
(function () {
    'use strict';

    if (!('serviceWorker' in navigator)) {
        return;
    }

    /* Register on first load */
    window.addEventListener('load', function () {
        navigator.serviceWorker
            .register('/nightpos-sw.js', { scope: '/' })
            .then(function (registration) {
                /* Check for waiting SW update and activate immediately */
                if (registration.waiting) {
                    registration.waiting.postMessage({ type: 'SKIP_WAITING' });
                }
                registration.addEventListener('updatefound', function () {
                    var newWorker = registration.installing;
                    if (!newWorker) return;
                    newWorker.addEventListener('statechange', function () {
                        if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                            newWorker.postMessage({ type: 'SKIP_WAITING' });
                        }
                    });
                });
            })
            .catch(function (err) {
                /* Non-fatal — app still works without SW */
                console.warn('[NightPOS PWA] Service Worker registration failed:', err);
            });
    });

    /* Capture the browser's "Add to Home Screen" prompt so it can be
       triggered programmatically if needed in the future. */
    window.addEventListener('beforeinstallprompt', function (e) {
        e.preventDefault();
        window.__nightposPwaInstallPrompt = e;
    });
})();
