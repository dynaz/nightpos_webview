from odoo import _, api, models
from odoo.exceptions import UserError


class PosSession(models.Model):
    _inherit = "pos.session"

    @api.model
    def _load_pos_data_models(self, config):
        result = list(super()._load_pos_data_models(config))
        if "nightpos.printer" not in result:
            result.append("nightpos.printer")
        return result

    def _nightpos_resolve_open_session(self):
        PosSession = self.env["pos.session"].sudo()
        base_domain = [
            ("state", "in", ["opening_control", "opened"]),
            ("rescue", "=", False),
        ]
        config_id = self.env.context.get("pos_config_id")
        if not config_id and self.exists():
            config_id = self.config_id.id
        if not config_id:
            config = self.env.user.pos_config_id
            config_id = config.id if config else None
        if config_id:
            session = PosSession.search(base_domain + [("config_id", "=", config_id)], limit=1)
            if session:
                return session
        return PosSession.search(base_domain + [("user_id", "=", self.env.uid)], limit=1)

    def _nightpos_ensure_session(self):
        session = self.exists() and self or self._nightpos_resolve_open_session()
        if not session:
            raise UserError(
                _(
                    "This POS session is no longer available (closed or removed). "
                    "Close this browser tab and open Point of Sale again from the menu."
                )
            )
        return session

    def load_data(self, models_to_load):
        return super(PosSession, self._nightpos_ensure_session()).load_data(models_to_load)

    def filter_local_data(self, models_to_filter):
        return super(PosSession, self._nightpos_ensure_session()).filter_local_data(
            models_to_filter
        )

    def load_data_params(self):
        return super(PosSession, self._nightpos_ensure_session()).load_data_params()

    def delete_opening_control_session(self):
        if not self.exists():
            return {"status": "success"}
        return super().delete_opening_control_session()
