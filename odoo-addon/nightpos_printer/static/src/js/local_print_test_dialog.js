/** @odoo-module **/

import { Component, useState, onWillStart } from "@odoo/owl";
import { Dialog } from "@web/core/dialog/dialog";
import { dispatchLocalPrintFromParams } from "@nightpos_printer/js/local_printer_trigger";

/**
 * Simple dialog for backend "Test (Chrome Extension)" button.
 */
export class LocalPrintTestDialog extends Component {
    static template = "nightpos_printer.LocalPrintTestDialog";
    static components = { Dialog };
    static props = {
        printer_name: String,
        raw_data: String,
        encoding: { type: String, optional: true },
        protocol: { type: String, optional: true },
        printer: { type: Object, optional: true },
        source: { type: String, optional: true },
        close: Function,
    };

    setup() {
        this.state = useState({ phase: "idle", message: "" });
        onWillStart(() => this._run());
    }

    _run() {
        const params = {
            printer_name: this.props.printer_name,
            raw_data: this.props.raw_data,
            encoding: this.props.encoding || "base64",
            protocol: this.props.protocol || "escpos",
            printer: this.props.printer,
            source: this.props.source || "nightpos.printer",
        };
        try {
            const ok = dispatchLocalPrintFromParams(params);
            if (ok) {
                this.state.phase = "ok";
                this.state.message =
                    `Print event dispatched for "${params.printer_name}".\n` +
                    "The NightPOS Chrome Extension should forward it to the local print server.";
            } else {
                this.state.phase = "error";
                this.state.message = "Event was cancelled by a listener.";
            }
        } catch (error) {
            this.state.phase = "error";
            this.state.message = String(error.message || error);
        }
    }
}
