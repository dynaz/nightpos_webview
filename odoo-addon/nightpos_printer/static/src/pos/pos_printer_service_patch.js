/** @odoo-module **/

import { patch } from "@web/core/utils/patch";
import { PosPrinterService } from "@point_of_sale/app/services/pos_printer_service";
import { NightposEscposPrinter } from "@nightpos_printer/pos/nightpos_escpos_printer";

/**
 * Odoo 19 PosPrinterService resets this.device from hardware_proxy on every print.
 * Keep NightposEscposPrinter when it was configured in afterProcessServerData.
 */
patch(PosPrinterService.prototype, {
    _useNightposDevice() {
        return this.device instanceof NightposEscposPrinter;
    },
    async print(...args) {
        if (!this._useNightposDevice()) {
            this.setPrinter(this.hardware_proxy.printer);
        }
        return super.print(...args);
    },
    async printHtml(...args) {
        if (!this._useNightposDevice()) {
            this.setPrinter(this.hardware_proxy.printer);
        }
        return super.printHtml(...args);
    },
});
