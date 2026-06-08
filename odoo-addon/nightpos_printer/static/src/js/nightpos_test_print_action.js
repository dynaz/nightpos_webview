/** @odoo-module **/

import { registry } from "@web/core/registry";
import { Component, useState, onWillStart } from "@odoo/owl";
import { useService } from "@web/core/utils/hooks";
import { Dialog } from "@web/core/dialog/dialog";
import {
    normalizePrinterPort,
    formatTcpPrinterError,
} from "@nightpos_printer/js/nightpos_printer_service";
import { isChromeExtensionAvailable } from "@nightpos_printer/js/local_printer_trigger";
import { LocalPrintTestDialog } from "@nightpos_printer/js/local_print_test_dialog";

// ─── Test-print dialog ────────────────────────────────────────────────────────

class TestPrintDialog extends Component {
    static template = "nightpos_printer.TestPrintDialog";
    static components = { Dialog };
    static props = {
        ip: { type: String, optional: true },
        port: { type: Number, optional: true },
        name: String,
        escpos_b64: String,
        shop_mode: { type: String, optional: true },
        printer_id: { type: Number, optional: true },
        close: Function,
    };

    setup() {
        this.printer = useService("nightpos_printer");
        this.state = useState({
            phase: "idle",   // idle | testing | printing | ok | error | no_app
            message: "",
        });
        onWillStart(() => this._run());
    }

    async _run() {
        const { name, escpos_b64, shop_mode: shopMode, printer_id: printerId } = this.props;
        const ip = this.props.ip || "";
        const port = normalizePrinterPort(this.props.port);
        const useSunmi = shopMode === "sunmi";
        const useServerTcp = shopMode === "tcp";

        if (!useServerTcp && !useSunmi && !this.printer.isAvailable()) {
            this.state.phase = "no_app";
            this.state.message =
                "ไม่ได้รันในแอป NightPOS\nกรุณาเปิดหน้านี้บน Android ที่ติดตั้งแอป NightPOS แล้ว";
            return;
        }
        if (useSunmi && !window.flutter_inappwebview?.callHandler) {
            this.state.phase = "no_app";
            this.state.message =
                "ไม่ได้รันในแอป NightPOS\nกรุณาเปิดหน้านี้บน Android (Sunmi) ที่ติดตั้งแอป NightPOS";
            return;
        }

        if (!useSunmi) {
            this.state.phase = "testing";
            this.state.message = useServerTcp
                ? `กำลังทดสอบการเชื่อมต่อจากเซิร์ฟเวอร์ → ${ip}:${port} ...`
                : `กำลังทดสอบการเชื่อมต่อ ${ip}:${port} ...`;
            try {
                const connRes = await this.printer.testConnection(ip, port);
                if (!connRes.reachable) {
                    this.state.phase = "error";
                    this.state.message =
                        `เชื่อมต่อ ${ip}:${port} ไม่ได้\n` +
                        (useServerTcp
                            ? "ตรวจสอบว่าพริ้นเตอร์เปิดอยู่และเซิร์ฟเวอร์ Odoo เข้าถึง IP นี้ได้"
                            : "ตรวจสอบว่าพริ้นเตอร์เปิดอยู่และอยู่ในเครือข่ายเดียวกัน");
                    return;
                }
            } catch (e) {
                this.state.phase = "error";
                this.state.message = `ทดสอบเชื่อมต่อล้มเหลว: ${e.message}`;
                return;
            }
        }

        this.state.phase = "printing";
        this.state.message = useSunmi
            ? `กำลังส่งหน้าทดสอบไปยัง Sunmi (${name}) ...`
            : useServerTcp
              ? `กำลังส่งหน้าทดสอบจากเซิร์ฟเวอร์ → ${name} (${ip}:${port}) ...`
              : `กำลังส่งหน้าทดสอบไปยัง ${name} ...`;
        try {
            let res;
            if (useSunmi && window.flutter_inappwebview?.callHandler) {
                res = await window.flutter_inappwebview.callHandler("SunmiPrinter", {
                    method: "printRaw",
                    data: escpos_b64,
                });
            } else if (useServerTcp) {
                res = await this.printer.printRawViaServer(escpos_b64, {
                    ip,
                    port,
                    printerId,
                });
            } else {
                res = await this.printer.printRaw(escpos_b64, { ip, port });
            }
            if (res && res.success !== false) {
                this.state.phase = "ok";
                this.state.message = useSunmi
                    ? `พิมพ์สำเร็จ (Sunmi)!\n${name}`
                    : `พิมพ์สำเร็จ!\n${name}  (${ip}:${port})`;
            } else {
                this.state.phase = "error";
                this.state.message = formatTcpPrinterError(res?.error || "Unknown error", {
                    ip,
                    port,
                });
            }
        } catch (e) {
            this.state.phase = "error";
            this.state.message = formatTcpPrinterError(e, { ip, port });
        }
    }
}

// ─── Generic print action (tag: nightpos_printer.print) ──────────────────────

async function handlePrintAction(env, action) {
    const { params } = action;
    const printer = env.services.nightpos_printer;
    const notification = env.services.notification;
    const port = normalizePrinterPort(params.port);

    const loadingNotif = notification.add("กำลังส่งข้อมูลไปยังพริ้นเตอร์...", {
        type: "info",
        sticky: true,
    });

    try {
        const res = await printer.printRawViaServer(params.escpos_b64, {
            ip: params.ip,
            port,
            printerId: params.printer_id,
        });
        loadingNotif();
        if (res && res.success) {
            notification.add(`พิมพ์สำเร็จ — ${params.name}`, {
                type: "success",
            });
        } else {
            notification.add(
                formatTcpPrinterError(res?.error || "Unknown", {
                    ip: params.ip,
                    port,
                }),
                { type: "danger" }
            );
        }
    } catch (e) {
        loadingNotif();
        notification.add(formatTcpPrinterError(e, { ip: params.ip, port }), {
            type: "danger",
        });
    }
}

function _shouldUseChromeTestPrint(params) {
    const shopMode = params.shop_mode || "tcp";
    if (shopMode === "chrome_extension") {
        return true;
    }
    if (shopMode === "auto") {
        return isChromeExtensionAvailable();
    }
    return false;
}

function _openChromeTestPrintDialog(env, params) {
    const printerName = params.chrome_printer_name || params.name || "";
    env.services.dialog.add(LocalPrintTestDialog, {
        printer_name: printerName,
        raw_data: params.escpos_b64,
        encoding: "base64",
        protocol: "escpos",
        printer: {
            id: params.printer_id,
            name: params.name,
            ip: params.ip,
            port: normalizePrinterPort(params.port),
        },
        source: "nightpos.pos_config_test",
    });
}

// ─── Test-print action (tag: nightpos_printer.test_print) ────────────────────

async function handleTestPrintAction(env, action) {
    const { params } = action;
    if (params && _shouldUseChromeTestPrint(params)) {
        _openChromeTestPrintDialog(env, params);
        return;
    }
    env.services.dialog.add(TestPrintDialog, {
        ip: params.ip || "",
        port: normalizePrinterPort(params.port),
        name: params.name || "",
        escpos_b64: params.escpos_b64,
        shop_mode: params.shop_mode,
        printer_id: params.printer_id,
    });
}

async function handleTestPrintOdooAction(env, action) {
    const { params } = action;
    env.services.notification.add(
        `Browser / IoT Box printing uses the standard POS receipt printer, not TCP record "${params.name}". Configure it under Point of Sale → Settings → Receipt Printer.`,
        { type: "info", sticky: true }
    );
}

registry.category("actions").add("nightpos_printer.print", handlePrintAction);
registry.category("actions").add("nightpos_printer.test_print_odoo", handleTestPrintOdooAction);
registry.category("actions").add("nightpos_printer.test_print", handleTestPrintAction);
// Routed tag: stub registers first; override with full handler when this module loads.
registry.category("actions").add(
    "nightpos_printer.test_print_routed",
    handleTestPrintAction,
    { force: true }
);
