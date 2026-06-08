/** @odoo-module **/
/**
 * NightPOS Local Printer — device_print_status.js  (POS UI component)
 *
 * Displays a small status indicator in the POS chrome showing whether
 * the local print server is reachable.
 *
 * The component polls the WebSocket server on mount and every 30 seconds.
 */

import { Component, useState, onMounted, onWillUnmount } from "@odoo/owl";
import { registry } from "@web/core/registry";

const DEFAULT_WS_URL  = "ws://localhost:8765";
const POLL_INTERVAL   = 30_000; // ms

function checkServer(wsUrl) {
    return new Promise((resolve) => {
        let ws;
        const timer = setTimeout(() => {
            try { ws?.close(); } catch (_) {}
            resolve({ ok: false, error: "Timeout" });
        }, 3500);

        try {
            ws = new WebSocket(wsUrl);
        } catch (e) {
            clearTimeout(timer);
            return resolve({ ok: false, error: e.message });
        }

        ws.onopen = () => ws.send(JSON.stringify({ action: "ping" }));

        ws.onmessage = ({ data }) => {
            clearTimeout(timer);
            try { ws.close(); } catch (_) {}
            try {
                const res = JSON.parse(data);
                resolve({ ok: true, version: res.version });
            } catch (_) {
                resolve({ ok: true });
            }
        };

        ws.onerror = () => {
            clearTimeout(timer);
            resolve({ ok: false, error: "Connection refused" });
        };
    });
}

export class DevicePrintStatus extends Component {
    static template = "nightpos_printer.DevicePrintStatus";
    static props = { wsUrl: { type: String, optional: true } };

    setup() {
        this.state = useState({ online: null, version: null });
        let timer = null;

        const poll = async () => {
            const url = this.props.wsUrl || DEFAULT_WS_URL;
            const res  = await checkServer(url);
            this.state.online  = res.ok;
            this.state.version = res.version || null;
        };

        onMounted(() => {
            poll();
            timer = setInterval(poll, POLL_INTERVAL);
        });

        onWillUnmount(() => clearInterval(timer));
    }

    get statusLabel() {
        if (this.state.online === null) return "Checking…";
        if (this.state.online) {
            return this.state.version
                ? `Printer online (v${this.state.version})`
                : "Printer online";
        }
        return "Printer offline";
    }

    get statusClass() {
        if (this.state.online === null) return "checking";
        return this.state.online ? "online" : "offline";
    }
}

registry.category("pos_components").add("DevicePrintStatus", DevicePrintStatus);
