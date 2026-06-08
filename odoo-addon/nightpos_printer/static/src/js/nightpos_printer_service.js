/** @odoo-module **/

import { registry } from "@web/core/registry";
import { rpc } from "@web/core/network/rpc";
import { npLog } from "@nightpos_printer/js/nightpos_pos_debug";

/**
 * NightPOS Printer Service
 *
 * Bridges Odoo backend ↔ NightPOS Android app ↔ TCP ESC/POS printer.
 *
 * Flow:
 *   1. Odoo generates ESC/POS bytes (via Python controller) or plain text
 *   2. This service calls window.flutter_inappwebview.callHandler('TcpPrinter', ...)
 *   3. Android app opens TCP socket → sends bytes to printer (port 9100)
 *
 * Also exposed as window.NightPOSPrinter for use from POS / other JS.
 */

const isNightPOS = () => typeof window.flutter_inappwebview !== "undefined";

/** Coerce printer TCP port; Odoo/OWL may pass strings (e.g. "9100"). */
export function normalizePrinterPort(port, fallback = 9100) {
    if (typeof port === "number" && Number.isFinite(port)) {
        return port;
    }
    const parsed = parseInt(String(port ?? "").replace(/,/g, ""), 10);
    return parsed >= 1 && parsed <= 65535 ? parsed : fallback;
}

/** Dart SocketException often shows the local ephemeral port, not destination 9100. */
export function formatTcpPrinterError(error, { ip, port } = {}) {
    const msg = String(error?.error || error?.message || error || "Unknown error");
    const target = ip ? `${ip}:${normalizePrinterPort(port)}` : "";
    if (/connection refused/i.test(msg)) {
        return (
            `เชื่อมต่อพริ้นเตอร์ไม่ได้ (${target || "TCP"})\n` +
            "• เปิดเครื่องพิมพ์และเชื่อม Wi‑Fi เดียวกับแท็บเล็ต\n" +
            "• ตรวจสอบ IP ว่าถูกต้อง (ping จากเครื่อง POS)\n" +
            "• พอร์ตควรเป็น 9100 (ข้อความ errno อาจแสดงเลขพอร์ตอื่น — ไม่ใช่พอร์ตที่ตั้งค่า)\n" +
            (msg.includes("401") || /\bport = \d{4,5}\b/.test(msg)
                ? `\nรายละเอียด: ${msg}`
                : "")
        );
    }
    return msg;
}

const nightposPrinterService = {
    dependencies: ["notification"],

    async start(env, { notification }) {

        const svc = {

            /** True when running inside the NightPOS Android WebView. */
            isAvailable() {
                return isNightPOS();
            },

            /**
             * Send raw ESC/POS bytes (Base-64 encoded) to a printer.
             *
             * @param {string} base64Data  - Base64 ESC/POS payload
             * @param {object} opts
             * @param {string} [opts.ip]        - Direct IP (skips printer registry)
             * @param {number} [opts.port=9100] - TCP port
             * @param {string} [opts.printerId] - Android printer UUID (alternative to ip)
             */
            async printRaw(base64Data, { ip, port = 9100, printerId } = {}) {
                _assertAvailable();
                const payload = { method: "printRaw", data: base64Data };
                if (ip) {
                    payload.ip = ip;
                    payload.port = normalizePrinterPort(port);
                } else if (printerId) {
                    payload.printerId = printerId;
                }
                return window.flutter_inappwebview.callHandler("TcpPrinter", payload);
            },

            /**
             * Send plain UTF-8 text to a printer.
             * The Android app wraps it in a minimal ESC/POS envelope automatically.
             */
            async printText(text, { ip, port = 9100, printerId } = {}) {
                _assertAvailable();
                const payload = { method: "printText", text };
                if (ip) {
                    payload.ip = ip;
                    payload.port = normalizePrinterPort(port);
                } else if (printerId) {
                    payload.printerId = printerId;
                }
                return window.flutter_inappwebview.callHandler("TcpPrinter", payload);
            },

            /**
             * Fetch ESC/POS bytes from Odoo server (Python generates them),
             * then push to the printer via Android TcpPrinter handler.
             *
             * @param {string} text        - Plain text receipt content
             * @param {number} [printerId] - Odoo nightpos.printer ID (null = default)
             */
            async printTextViaServer(text, printerId = null) {
                _assertAvailable();
                const result = await rpc("/nightpos/printers/escpos_text", {
                    text,
                    printer_id: printerId,
                });
                if (!result.success) throw new Error(result.error || "Server error");
                const { escpos_b64, printer } = result;
                return svc.printRaw(escpos_b64, {
                    ip: printer.ip,
                    port: normalizePrinterPort(printer.port),
                });
            },

            /** List all configured printers from Android app. */
            async listAndroidPrinters() {
                if (!isNightPOS()) return [];
                const res = await window.flutter_inappwebview.callHandler("TcpPrinter", {
                    method: "listPrinters",
                });
                return res.printers || [];
            },

            /** List all printers from Odoo database. */
            async listOdooPrinters() {
                const result = await rpc("/nightpos/printers", {});
                return result.success ? result.printers : [];
            },

            /** Test TCP connection to ip:port (Android app when available, else Odoo server). */
            async testConnection(ip, port = 9100) {
                const normalizedPort = normalizePrinterPort(port);
                if (isNightPOS()) {
                    return window.flutter_inappwebview.callHandler("TcpPrinter", {
                        method: "testConnection",
                        ip,
                        port: normalizedPort,
                    });
                }
                return svc.testConnectionViaServer(ip, normalizedPort);
            },

            /**
             * Send raw ESC/POS via Odoo server → printer IP (pos_tcp_esc_printer style).
             * Works from any browser; no NightPOS Android app required.
             */
            async printRawViaServer(base64Data, { ip, port = 9100, printerId } = {}) {
                const payload = {
                    escpos_b64: base64Data,
                    port: normalizePrinterPort(port),
                };
                if (printerId) {
                    payload.printer_id = printerId;
                } else if (ip) {
                    payload.ip = ip;
                } else {
                    throw new Error("printerId or ip required for server TCP print");
                }
                const result = await rpc("/nightpos/printers/send_tcp", payload);
                if (!result.success) {
                    throw new Error(result.error || "Server TCP print failed");
                }
                return result;
            },

            /** Test TCP reachability from the Odoo server. */
            async testConnectionViaServer(ip, port = 9100) {
                const result = await rpc("/nightpos/printers/test_tcp", {
                    ip,
                    port: normalizePrinterPort(port),
                });
                if (!result.success) {
                    throw new Error(result.error || "Server connection test failed");
                }
                return { reachable: result.reachable, ip: result.ip, port: result.port };
            },
        };

        // Expose globally so POS JS / other scripts can call without service injection
        window.NightPOSPrinter = svc;

        return svc;

        function _assertAvailable() {
            if (!isNightPOS()) {
                throw new Error(
                    "NightPOSPrinter: ต้องรันในแอป NightPOS Android เท่านั้น"
                );
            }
        }
    },
};

registry.category("services").add("nightpos_printer", nightposPrinterService);

npLog("nightpos_printer_service.js loaded (service registered, not auto-started in POS)");
