from odoo import _, fields, models


class ResConfigSettings(models.TransientModel):
    _inherit = 'res.config.settings'

    pos_nightpos_receipt_mode = fields.Selection(
        related='pos_config_id.nightpos_receipt_mode',
        readonly=False,
    )
    pos_nightpos_receipt_device_policy = fields.Selection(
        related='pos_config_id.nightpos_receipt_device_policy',
        readonly=False,
    )
    pos_nightpos_receipt_enabled = fields.Boolean(
        related='pos_config_id.nightpos_receipt_enabled',
        readonly=False,
    )
    pos_nightpos_receipt_printer_id = fields.Many2one(
        related='pos_config_id.nightpos_receipt_printer_id',
        readonly=False,
    )
    pos_nightpos_chrome_printer_name = fields.Char(
        related='pos_config_id.nightpos_chrome_printer_name',
        readonly=False,
    )
    pos_nightpos_preparation_enabled = fields.Boolean(
        related='pos_config_id.nightpos_preparation_enabled',
        readonly=False,
    )
    pos_nightpos_preparation_printer_ids = fields.Many2many(
        related='pos_config_id.nightpos_preparation_printer_ids',
        readonly=False,
    )

    def action_test_nightpos_receipt_printer(self):
        self.ensure_one()
        return self.pos_config_id.action_test_nightpos_receipt_printer()

    def action_test_nightpos_receipt_print_odoo(self):
        self.ensure_one()
        return self.pos_config_id.action_test_nightpos_receipt_print_odoo()

    def action_test_nightpos_receipt_print_auto(self):
        self.ensure_one()
        return self.pos_config_id.action_test_nightpos_receipt_print_auto()

    def action_test_nightpos_receipt_print_tcp(self):
        self.ensure_one()
        return self.pos_config_id.action_test_nightpos_receipt_print_tcp()

    def action_test_nightpos_receipt_print_sunmi(self):
        self.ensure_one()
        return self.pos_config_id.action_test_nightpos_receipt_print_sunmi()

    def action_test_nightpos_receipt_print_chrome(self):
        self.ensure_one()
        return self.pos_config_id.action_test_nightpos_receipt_print_chrome()

    def action_test_nightpos_receipt_printer_extension(self):
        self.ensure_one()
        return self.pos_config_id.action_test_nightpos_receipt_printer_extension()

    def action_pos_printer_dialog(self):
        form_view = self.env.ref('nightpos_printer.nightpos_printer_form')
        return {
            'name': _('Add Printer'),
            'type': 'ir.actions.act_window',
            'res_model': 'nightpos.printer',
            'view_mode': 'form',
            'views': [(form_view.id, 'form')],
            'target': 'new',
            'context': {
                'default_pos_config_ids': [self.pos_config_id.id],
            },
        }

    def action_open_nightpos_printer_list(self):
        list_view = self.env.ref('nightpos_printer.nightpos_printer_list')
        form_view = self.env.ref('nightpos_printer.nightpos_printer_form')
        return {
            'name': _('Printers'),
            'type': 'ir.actions.act_window',
            'res_model': 'nightpos.printer',
            'view_mode': 'list,form',
            'views': [(list_view.id, 'list'), (form_view.id, 'form')],
            'search_view_id': self.env.ref('nightpos_printer.nightpos_printer_search').id,
            'context': {'search_default_active': 1},
        }
