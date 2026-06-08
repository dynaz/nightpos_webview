from odoo import api, fields, models
from odoo.fields import Command


class NightposPrinterPos(models.Model):
    _inherit = 'nightpos.printer'

    product_categories_ids = fields.Many2many(
        'pos.category',
        'nightpos_printer_category_rel',
        'printer_id',
        'category_id',
        string='Printed Product Categories',
        help='Only print order lines for products in these POS categories.',
    )
    pos_config_ids = fields.Many2many(
        'pos.config',
        'pos_config_nightpos_printer_rel',
        'printer_id',
        'config_id',
        string='POS Shops',
        help='Point of Sale configurations that use this printer for kitchen/bar tickets.',
    )

    @api.model_create_multi
    def create(self, vals_list):
        printers = super().create(vals_list)
        printers._sync_pos_configs_from_assignment()
        return printers

    def write(self, vals):
        result = super().write(vals)
        sync_fields = {'name', 'product_categories_ids', 'pos_config_ids', 'active'}
        if sync_fields & set(vals):
            self._sync_pos_configs_from_assignment()
            if {'name', 'product_categories_ids'} & set(vals):
                self._sync_linked_pos_printers()
        return result

    def _sync_pos_configs_from_assignment(self):
        """Push printer assignments from NightPOS Printer app into each pos.config."""
        configs = self.pos_config_ids
        if configs:
            configs._sync_nightpos_preparation_from_printers()

    def _sync_linked_pos_printers(self):
        pos_printers = self.env['pos.printer'].search([
            ('nightpos_printer_id', 'in', self.ids),
        ])
        for pos_printer in pos_printers:
            nightpos = pos_printer.nightpos_printer_id
            pos_printer.write({
                'name': nightpos.name,
                'product_categories_ids': [
                    Command.set(nightpos.product_categories_ids.ids)
                ],
            })
