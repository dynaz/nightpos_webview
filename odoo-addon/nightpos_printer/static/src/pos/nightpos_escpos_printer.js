/** @odoo-module **/

import { BasePrinter } from "@point_of_sale/app/utils/printer/base_printer";
import { _t } from "@web/core/l10n/translation";
import { rpc } from "@web/core/network/rpc";
import { normalizePrinterPort } from "@nightpos_printer/js/nightpos_printer_service";
import { dispatchLocalPrintEvent } from "@nightpos_printer/js/local_printer_trigger";
import { npLog } from "@nightpos_printer/js/nightpos_pos_debug";

/**
 * Renders the same OrderReceipt HTML as Odoo, converts to ESC/POS on the server,
 * then sends raw bytes to the NightPOS Android bridge (TCP or Sunmi).
 */
export class NightposEscposPrinter extends BasePrinter {
    setup({ pos, mode, printerId, ip, port, chromePrinterName }) {
        super.setup(...arguments);
        this.pos = pos;
        this.mode = mode;
        this.printerId = printerId;
        this.ip = ip;
        this.port = normalizePrinterPort(port);
        this.chromePrinterName = chromePrinterName || "";
    }

    async sendPrintingJob(imageBase64) {
        if (this.mode === "tcp") {
            return this._sendTcpReceipt(imageBase64);
        }
        const result = await rpc("/nightpos/printers/print_image", {
            image: imageBase64,
            printer_id: this.printerId,
        });
        if (!result.success) {
            return {
                result: false,
                canRetry: true,
                message: {
                    title: _t("Printing failed"),
                    body: result.error || _t("Could not prepare receipt data."),
                },
            };
        }
        try {
            await this._sendRaw(result.escpos_b64, result.printer);
            return { result: true };
        } catch (error) {
            return {
                result: false,
                canRetry: true,
                message: {
                    title: _t("Printing failed"),
                    body: String(error.message || error),
                },
            };
        }
    }

    async _sendTcpReceipt(imageBase64) {
        try {
            const result = await rpc("/nightpos/printers/print_image_tcp", {
                image: imageBase64,
                printer_id: this.printerId,
                ip: this.ip,
                port: this.port,
            });
            if (!result.success) {
                return {
                    result: false,
                    canRetry: true,
                    message: {
                        title: _t("Printing failed"),
                        body: result.error || _t("Could not print to TCP printer."),
                    },
                };
            }
            return { result: true };
        } catch (error) {
            return {
                result: false,
                canRetry: true,
                message: {
                    title: _t("Printing failed"),
                    body: String(error.message || error),
                },
            };
        }
    }

    async _sendRaw(escposB64, printer) {
        if (this.mode === "chrome_extension") {
            const printerName =
                this.chromePrinterName || printer?.chrome_printer_name || printer?.name || "";
            const dispatched = dispatchLocalPrintEvent({
                printer_name: printerName,
                raw_data: escposB64,
                encoding: "base64",
                protocol: "escpos",
                printer: printer
                    ? {
                          id: printer.id,
                          name: printer.name,
                          ip: printer.ip,
                          port: normalizePrinterPort(printer.port),
                      }
                    : null,
                source: "point_of_sale.receipt",
                meta: { mode: this.mode, printer_id: this.printerId },
            });
            if (!dispatched) {
                throw new Error(_t("Chrome Extension cancelled the print event."));
            }
            return { success: true, dispatched: true };
        }
        if (this.mode === "tcp") {
            const result = await rpc("/nightpos/printers/send_tcp", {
                escpos_b64: escposB64,
                printer_id: this.printerId,
                ip: printer?.ip || this.ip,
                port: normalizePrinterPort(printer?.port ?? this.port),
            });
            if (!result.success) {
                throw new Error(result.error || "TCP print failed");
            }
            return result;
        }
        if (!window.flutter_inappwebview?.callHandler) {
            throw new Error(_t("NightPOS printer bridge is not available."));
        }
        if (this.mode === "sunmi") {
            const res = await window.flutter_inappwebview.callHandler("SunmiPrinter", {
                method: "printRaw",
                data: escposB64,
            });
            if (res?.success === false) {
                throw new Error(res.error || "Sunmi printRaw failed");
            }
            return res;
        }
        const res = await window.flutter_inappwebview.callHandler("TcpPrinter", {
            method: "printRaw",
            data: escposB64,
            ip: printer?.ip || this.ip,
            port: normalizePrinterPort(printer?.port ?? this.port),
        });
        if (res?.success === false) {
            throw new Error(res.error || "TcpPrinter printRaw failed");
        }
        return res;
    }

    openCashbox() {
        if (this.mode === "sunmi") {
            window.flutter_inappwebview?.callHandler("SunmiPrinter", { method: "openDrawer" });
        }
    }
}

npLog("nightpos_escpos_printer.js loaded");
