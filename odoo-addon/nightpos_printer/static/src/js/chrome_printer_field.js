/** @odoo-module **/

import { registry } from "@web/core/registry";
import { Component, useState, onWillStart } from "@odoo/owl";
import { standardFieldProps } from "@web/views/fields/standard_field_props";
import { fetchLocalPrintersAuto } from "@nightpos_printer/js/local_print_ws";

export class ChromePrinterField extends Component {
    static template = "nightpos_printer.ChromePrinterField";
    static props = {
        ...standardFieldProps,
    };

    setup() {
        this.state = useState({
            printers: [],
            loading: false,
            error: null,
        });
        onWillStart(() => this.loadPrinters());
    }

    get value() {
        return this.props.record.data[this.props.name] || "";
    }

    async loadPrinters() {
        this.state.loading = true;
        this.state.error = null;
        try {
            this.state.printers = await fetchLocalPrintersAuto();
            if (!this.value && this.state.printers.length === 1) {
                this.props.record.update({ [this.props.name]: this.state.printers[0] });
            }
        } catch (error) {
            this.state.error = String(error.message || error);
            this.state.printers = [];
        } finally {
            this.state.loading = false;
        }
    }

    onSelectChange(ev) {
        this.props.record.update({ [this.props.name]: ev.target.value });
    }
}

export const chromePrinterField = {
    component: ChromePrinterField,
    supportedTypes: ["char"],
};

registry.category("fields").add("nightpos_chrome_printer", chromePrinterField);
