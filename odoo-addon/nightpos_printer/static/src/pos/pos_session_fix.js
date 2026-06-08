/** @odoo-module **/

import { PosData } from "@point_of_sale/app/services/data_service";
import { RPCError } from "@web/core/network/rpc";
import { patch } from "@web/core/utils/patch";
import { npWarn } from "@nightpos_printer/js/nightpos_pos_debug";

let recoveringSession = false;

function isMissingPosSessionError(error) {
    if (!(error instanceof RPCError)) {
        return false;
    }
    const message = `${error.data?.message || ""} ${error.message || ""}`;
    if (error.exceptionName === "odoo.exceptions.MissingError" && message.includes("pos.session")) {
        return true;
    }
    return (
        message.includes("pos.session") &&
        (message.includes("does not exist") || message.includes("has been deleted"))
    );
}

function redirectToFreshPosUi() {
    if (recoveringSession) {
        return;
    }
    recoveringSession = true;
    const configId = odoo.pos_config_id;
    if (!configId) {
        window.location.assign("/npos/action-point_of_sale.action_client_pos_menu");
        return;
    }
    const url = new URL(`/pos/ui/${configId}`, window.location.origin);
    url.searchParams.set("from_backend", "1");
    url.searchParams.set("_nightpos_session_refresh", String(Date.now()));
    window.location.replace(url.toString());
}

async function recoverFromMissingPosSession(posData) {
    if (recoveringSession) {
        return false;
    }
    try {
        if (posData.indexedDB) {
            await posData.resetIndexedDB();
        }
    } catch {
        // IndexedDB may not be initialized yet.
    }
    redirectToFreshPosUi();
    return true;
}

function withPosConfigContext(model, kwargs = {}) {
    if (model !== "pos.session" || !odoo.pos_config_id) {
        return kwargs;
    }
    return {
        ...kwargs,
        context: {
            ...(kwargs.context || {}),
            pos_config_id: odoo.pos_config_id,
        },
    };
}

function wrapOrmMethod(orm, methodName, posData) {
    const original = orm[methodName].bind(orm);
    orm[methodName] = async (...args) => {
        if (methodName === "call" && args[0] === "pos.session") {
            args[3] = withPosConfigContext(args[0], args[3] || {});
        } else if (methodName === "read" && args[0] === "pos.session") {
            args[3] = withPosConfigContext(args[0], args[3] || {});
        }
        try {
            return await original(...args);
        } catch (error) {
            if (isMissingPosSessionError(error)) {
                await recoverFromMissingPosSession(posData);
                npWarn("POS session missing — reload scheduled; do not block boot");
                if (methodName === "read") {
                    return [];
                }
                return null;
            }
            throw error;
        }
    };
}

function installOrmRecoveryWrapper(orm, posData) {
    if (orm._nightposSessionRecoveryInstalled) {
        return;
    }
    wrapOrmMethod(orm, "call", posData);
    wrapOrmMethod(orm, "read", posData);
    orm._nightposSessionRecoveryInstalled = true;
}

patch(PosData.prototype, {
    async setup(env, services) {
        installOrmRecoveryWrapper(services.orm, this);
        try {
            await super.setup(env, services);
        } catch (error) {
            if (isMissingPosSessionError(error)) {
                await recoverFromMissingPosSession(this);
                return;
            }
            throw error;
        }
        installOrmRecoveryWrapper(this.orm, this);
    },

    async loadInitialData() {
        if (recoveringSession) {
            return {};
        }
        let localData;
        try {
            localData = await super.loadInitialData(...arguments);
        } catch (error) {
            if (isMissingPosSessionError(error)) {
                await recoverFromMissingPosSession(this);
                return {};
            }
            throw error;
        }
        if (recoveringSession) {
            return localData || {};
        }
        const session = localData?.["pos.session"]?.[0];
        if (session?.id && session.id !== odoo.pos_session_id) {
            odoo.pos_session_id = session.id;
        }
        return localData;
    },

    async loadFieldsAndRelations() {
        if (recoveringSession) {
            const key = `pos_data_params_${odoo.pos_config_id}`;
            return JSON.parse(localStorage.getItem(key) || "{}");
        }
        try {
            return await super.loadFieldsAndRelations(...arguments);
        } catch (error) {
            if (isMissingPosSessionError(error)) {
                await recoverFromMissingPosSession(this);
                return {};
            }
            throw error;
        }
    },

    async initData(hard = false, limit = true) {
        if (recoveringSession) {
            return;
        }
        try {
            await super.initData(hard, limit);
        } catch (error) {
            if (isMissingPosSessionError(error)) {
                await recoverFromMissingPosSession(this);
                return;
            }
            throw error;
        }
        const session = this.models?.["pos.session"]?.getFirst?.();
        if (session?.id && session.id !== odoo.pos_session_id) {
            odoo.pos_session_id = session.id;
        }
    },
});
