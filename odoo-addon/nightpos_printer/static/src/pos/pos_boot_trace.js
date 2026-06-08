/** @odoo-module **/
/**
 * Early POS boot milestones (always logged via npBoot).
 * Helps diagnose stuck loading before NightPOS printer setup runs.
 */

import { patch } from "@web/core/utils/patch";
import { PosStore } from "@point_of_sale/app/services/pos_store";
import { npBoot, npError, formatNightposError } from "@nightpos_printer/js/nightpos_pos_debug";

patch(PosStore.prototype, {
    async setup() {
        npBoot("POS boot: PosStore.setup started");
        try {
            await super.setup(...arguments);
            npBoot("POS boot: PosStore.setup finished");
        } catch (error) {
            npError("POS boot: PosStore.setup failed", formatNightposError(error));
            throw error;
        }
    },

    async processServerData() {
        npBoot("POS boot: processServerData started");
        try {
            const result = await super.processServerData(...arguments);
            npBoot("POS boot: processServerData finished");
            return result;
        } catch (error) {
            npError("POS boot: processServerData failed", formatNightposError(error));
            throw error;
        }
    },

    async afterProcessServerData() {
        npBoot("POS boot: afterProcessServerData started");
        try {
            const result = await super.afterProcessServerData(...arguments);
            npBoot("POS boot: afterProcessServerData finished");
            return result;
        } catch (error) {
            npError("POS boot: afterProcessServerData failed", formatNightposError(error));
            throw error;
        }
    },
});
