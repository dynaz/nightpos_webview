from odoo import api, fields, models


class PosPrinter(models.Model):
    _inherit = 'pos.printer'

    printer_type = fields.Selection(
        selection_add=[('nightpos_tcp', 'NightPOS TCP printer')],
        ondelete={'nightpos_tcp': 'set default'},
    )
    nightpos_printer_id = fields.Many2one(
        'nightpos.printer',
        string='NightPOS Printer',
        ondelete='cascade',
    )
    nightpos_printer_ip = fields.Char(related='nightpos_printer_id.ip')
    nightpos_printer_port = fields.Integer(related='nightpos_printer_id.port')

    @api.model
    def _load_pos_data_fields(self, config):
        fields_to_load = super()._load_pos_data_fields(config)
        fields_to_load += ['nightpos_printer_id', 'nightpos_printer_ip', 'nightpos_printer_port']
        return fields_to_load
