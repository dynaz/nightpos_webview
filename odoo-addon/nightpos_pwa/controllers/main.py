import json

from odoo import http
from odoo.http import request


class NightPosPwaController(http.Controller):

    @http.route(
        '/web/manifest.webmanifest',
        type='http',
        auth='public',
        methods=['GET'],
        csrf=False,
    )
    def web_manifest(self, **kwargs):
        """Override Odoo's default manifest so the NightPOS web client launches
        in standalone mode (no browser address bar) and uses the correct
        start_url, theme colours, and app shortcuts."""
        manifest = {
            "name": "NightPOS Soho",
            "short_name": "NightPOS",
            "description": "ระบบจุดขายสำหรับร้านอาหารและไนท์คลับ",
            "start_url": "/npos",
            "scope": "/",
            "display": "standalone",
            "orientation": "any",
            "background_color": "#0B0B12",
            "theme_color": "#A35CFF",
            "lang": "th",
            "dir": "ltr",
            "prefer_related_applications": False,
            "icons": [
                {
                    "src": "/web/static/img/odoo-icon-192.png",
                    "sizes": "192x192",
                    "type": "image/png",
                    "purpose": "any maskable",
                },
                {
                    "src": "/web/static/img/odoo-icon-512.png",
                    "sizes": "512x512",
                    "type": "image/png",
                    "purpose": "any maskable",
                },
            ],
            "shortcuts": [
                {
                    "name": "เปิดขาย",
                    "short_name": "POS",
                    "description": "เปิดหน้าขาย Point of Sale",
                    "url": "/pos/ui",
                    "icons": [{"src": "/web/static/img/odoo-icon-192.png", "sizes": "192x192"}],
                },
                {
                    "name": "สินค้า",
                    "short_name": "Products",
                    "description": "จัดการสินค้า",
                    "url": "/npos/action-770",
                    "icons": [{"src": "/web/static/img/odoo-icon-192.png", "sizes": "192x192"}],
                },
                {
                    "name": "ส่วนลด & สะสมแต้ม",
                    "short_name": "Loyalty",
                    "description": "โปรแกรมส่วนลดและสะสมแต้ม",
                    "url": "/npos/action-918",
                    "icons": [{"src": "/web/static/img/odoo-icon-192.png", "sizes": "192x192"}],
                },
                {
                    "name": "Gift Card & eWallet",
                    "short_name": "Gift Card",
                    "description": "จัดการ Gift Card และ eWallet",
                    "url": "/npos/action-919",
                    "icons": [{"src": "/web/static/img/odoo-icon-192.png", "sizes": "192x192"}],
                },
            ],
            "categories": ["business", "productivity"],
        }
        return request.make_response(
            json.dumps(manifest, ensure_ascii=False),
            headers=[
                ('Content-Type', 'application/manifest+json'),
                ('Cache-Control', 'no-cache'),
            ],
        )

    @http.route(
        '/nightpos-sw.js',
        type='http',
        auth='public',
        methods=['GET'],
        csrf=False,
    )
    def service_worker(self, **kwargs):
        """Serve the NightPOS service worker from the root scope.

        Strategy:
        - Static assets (/web/assets/, /web/static/): cache-first
        - HTML pages (/npos, /pos/ui, ...): network-first, fall back to cached shell
        - JSON-RPC / bus / longpolling: skip (always network)
        """
        sw_js = r"""
'use strict';

var CACHE_NAME = 'nightpos-v1';
var SHELL_URL   = '/npos';

var PRECACHE = [
    '/npos',
];

/* ── Install: pre-cache the app shell ──────────────────────────── */
self.addEventListener('install', function (event) {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(function (cache) {
                return cache.addAll(PRECACHE).catch(function () { /* ignore */ });
            })
            .then(function () { return self.skipWaiting(); })
    );
});

/* ── Activate: remove old caches ──────────────────────────────── */
self.addEventListener('activate', function (event) {
    event.waitUntil(
        caches.keys().then(function (keys) {
            return Promise.all(
                keys
                    .filter(function (k) { return k !== CACHE_NAME; })
                    .map(function (k) { return caches.delete(k); })
            );
        }).then(function () { return self.clients.claim(); })
    );
});

/* ── Fetch ─────────────────────────────────────────────────────── */
self.addEventListener('fetch', function (event) {
    var req = event.request;

    /* Only handle GET from our own origin */
    if (req.method !== 'GET') return;
    if (!req.url.startsWith(self.location.origin + '/')) return;

    var pathname = new URL(req.url).pathname;

    /* Skip Odoo API / real-time routes — always go to network */
    if (
        pathname.startsWith('/web/dataset') ||
        pathname.startsWith('/web/action')  ||
        pathname.startsWith('/longpolling') ||
        pathname.startsWith('/bus/')        ||
        pathname.startsWith('/web/webclient/load_menus')
    ) {
        return;
    }

    /* Static assets: cache-first, update in background */
    if (pathname.startsWith('/web/assets/') || pathname.startsWith('/web/static/')) {
        event.respondWith(
            caches.match(req).then(function (cached) {
                var networkFetch = fetch(req).then(function (response) {
                    if (response && response.status === 200 && response.type === 'basic') {
                        caches.open(CACHE_NAME).then(function (cache) {
                            cache.put(req, response.clone());
                        });
                    }
                    return response;
                });
                return cached || networkFetch;
            })
        );
        return;
    }

    /* HTML navigation: network-first, fall back to app shell */
    var accept = req.headers.get('accept') || '';
    if (accept.indexOf('text/html') !== -1) {
        event.respondWith(
            fetch(req).then(function (response) {
                /* Cache a fresh copy of the shell pages */
                if (response && response.status === 200) {
                    caches.open(CACHE_NAME).then(function (cache) {
                        cache.put(req, response.clone());
                    });
                }
                return response;
            }).catch(function () {
                return caches.match(req).then(function (cached) {
                    return cached || caches.match(SHELL_URL);
                });
            })
        );
        return;
    }
});
"""
        return request.make_response(
            sw_js,
            headers=[
                ('Content-Type', 'application/javascript'),
                ('Service-Worker-Allowed', '/'),
                ('Cache-Control', 'no-cache, no-store'),
            ],
        )
