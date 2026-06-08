from odoo import _, api, fields, models
from odoo.exceptions import UserError
from odoo.fields import Command

from .nightpos_print_modes import NIGHTPOS_PRINTER_TYPE_SELECTION


class PosConfig(models.Model):
    _inherit = 'pos.config'

    nightpos_receipt_mode = fields.Selection(
        selection=NIGHTPOS_PRINTER_TYPE_SELECTION,
        string='Receipt Printer',
        default='auto',
        required=True,
        help='Preferred receipt printing. With "Auto" or device policy "Auto-detect", '
             'each tablet/PC picks TCP, Sunmi, Chrome Extension, or browser print.',
    )
    nightpos_receipt_device_policy = fields.Selection(
        selection=[
            ('shop', 'Shop setting on every device'),
            ('auto', 'Auto-detect on each device'),
            ('per_device', 'Per-device choice (saved in database)'),
        ],
        string='Device Print Policy',
        default='auto',
        required=True,
        help='How printing adapts when devices differ (Sunmi tablet, PC + extension, etc.).',
    )
    nightpos_receipt_enabled = fields.Boolean(
        string='NightPOS Receipt Printer',
        help='Print POS receipts via a NightPOS TCP/ESC-POS printer without an IoT Box.',
    )
    nightpos_receipt_printer_id = fields.Many2one(
        'nightpos.printer',
        string='Receipt Printer',
        domain="[('active', '=', True)]",
    )
    nightpos_chrome_printer_name = fields.Char(
        string='Local Printer (Chrome)',
        help='Exact Windows or CUPS printer name from the NightPOS print server. '
             'Use Refresh on the POS shop form to load the list from your PC.',
    )
    nightpos_preparation_enabled = fields.Boolean(
        string='NightPOS Preparation Printers',
        help='Print kitchen, bar, and other preparation tickets via NightPOS TCP printers.',
    )
    nightpos_device_print_ids = fields.One2many(
        'nightpos.device.print',
        'pos_config_id',
        string='Device print profiles',
    )
    nightpos_preparation_printer_ids = fields.Many2many(
        'nightpos.printer',
        'pos_config_nightpos_printer_rel',
        'config_id',
        'printer_id',
        string='Preparation Printers',
        domain="[('active', '=', True)]",
    )

    def _action_test_nightpos_receipt_for_mode(self, mode):
        self.ensure_one()
        if not self.nightpos_receipt_printer_id:
            raise UserError(_('Select a Receipt printer before running a test print.'))
        return self.nightpos_receipt_printer_id._action_test_print_for_mode(
            mode, pos_config=self,
        )

    def action_test_nightpos_receipt_printer(self):
        """Test print using the shop's configured receipt mode."""
        self.ensure_one()
        return self._action_test_nightpos_receipt_for_mode(self.nightpos_receipt_mode)

    def action_test_nightpos_receipt_print_odoo(self):
        return self._action_test_nightpos_receipt_for_mode('odoo')

    def action_test_nightpos_receipt_print_auto(self):
        return self._action_test_nightpos_receipt_for_mode('auto')

    def action_test_nightpos_receipt_print_tcp(self):
        return self._action_test_nightpos_receipt_for_mode('tcp')

    def action_test_nightpos_receipt_print_sunmi(self):
        return self._action_test_nightpos_receipt_for_mode('sunmi')

    def action_test_nightpos_receipt_print_chrome(self):
        return self._action_test_nightpos_receipt_for_mode('chrome_extension')

    def action_test_nightpos_receipt_printer_extension(self):
        """Alias kept for older views; same as Test (Chrome)."""
        return self.action_test_nightpos_receipt_print_chrome()

    @api.model_create_multi
    def create(self, vals_list):
        configs = super().create([
            self._apply_nightpos_preparation_vals(
                self._apply_nightpos_receipt_vals(vals)
            )
            for vals in vals_list
        ])
        configs._sync_nightpos_preparation_printers()
        return configs

    def write(self, vals):
        vals = self._apply_nightpos_receipt_vals(vals)
        vals = self._apply_nightpos_preparation_vals(vals)
        result = super().write(vals)
        if {'nightpos_preparation_enabled', 'nightpos_preparation_printer_ids'} & set(vals):
            self._sync_nightpos_preparation_printers()
        return result

    @api.model
    def _apply_nightpos_preparation_vals(self, vals):
        if vals.get('nightpos_preparation_enabled'):
            vals['is_order_printer'] = True
        elif 'nightpos_preparation_enabled' in vals and not vals['nightpos_preparation_enabled']:
            vals['nightpos_preparation_printer_ids'] = [Command.clear()]
            vals['is_order_printer'] = False
        if vals.get('is_order_printer'):
            vals['nightpos_preparation_enabled'] = True
        elif 'is_order_printer' in vals and not vals['is_order_printer']:
            vals['nightpos_preparation_enabled'] = False
            vals['nightpos_preparation_printer_ids'] = [Command.clear()]
        return vals

    @api.model
    def _apply_nightpos_receipt_vals(self, vals):
        mode = vals.get('nightpos_receipt_mode')
        if mode == 'tcp':
            vals['nightpos_receipt_enabled'] = True
            vals['other_devices'] = True
            vals['epson_printer_ip'] = False
            if 'nightpos_sunmi_enabled' in self.env['pos.config']._fields:
                vals['nightpos_sunmi_enabled'] = False
        elif mode in ('chrome_extension', 'auto'):
            vals['nightpos_receipt_enabled'] = True
            vals['other_devices'] = True
            vals['epson_printer_ip'] = False
            if 'nightpos_sunmi_enabled' in self.env['pos.config']._fields:
                vals['nightpos_sunmi_enabled'] = False
        elif mode == 'sunmi':
            vals['nightpos_receipt_enabled'] = False
            vals['nightpos_receipt_printer_id'] = False
            vals['other_devices'] = True
            vals['epson_printer_ip'] = False
            if 'nightpos_sunmi_enabled' in self.env['pos.config']._fields:
                vals['nightpos_sunmi_enabled'] = True
        elif mode == 'odoo':
            vals['nightpos_receipt_enabled'] = False
            vals['nightpos_receipt_printer_id'] = False
            if 'nightpos_sunmi_enabled' in self.env['pos.config']._fields:
                vals['nightpos_sunmi_enabled'] = False
        elif vals.get('nightpos_receipt_enabled'):
            vals['nightpos_receipt_mode'] = 'tcp'
            vals['other_devices'] = True
            vals['epson_printer_ip'] = False
        elif 'nightpos_receipt_enabled' in vals and not vals['nightpos_receipt_enabled']:
            if vals.get('nightpos_receipt_mode', 'tcp') == 'tcp':
                vals['nightpos_receipt_mode'] = 'odoo'
            vals['other_devices'] = False
            vals['nightpos_receipt_printer_id'] = False
        if vals.get('other_devices') and not mode:
            vals.setdefault('nightpos_receipt_mode', 'tcp')
            vals['nightpos_receipt_enabled'] = True
            vals['epson_printer_ip'] = False
        elif 'other_devices' in vals and not vals['other_devices']:
            vals['nightpos_receipt_enabled'] = False
            vals['nightpos_receipt_printer_id'] = False
            vals.setdefault('nightpos_receipt_mode', 'odoo')
        return vals

    def _sync_nightpos_preparation_from_printers(self):
        """Rebuild preparation printer links from NightPOS Printer app assignments."""
        Printer = self.env['nightpos.printer']
        for config in self:
            printers = Printer.search([
                ('active', '=', True),
                ('pos_config_ids', 'in', config.id),
            ])
            super(PosConfig, config).write({
                'nightpos_preparation_printer_ids': [Command.set(printers.ids)],
            })
        self._sync_nightpos_preparation_printers()

    def _sync_nightpos_preparation_printers(self):
        """Mirror nightpos.printer records into pos.printer for POS session loading."""
        PosPrinter = self.env['pos.printer']
        for config in self:
            legacy = config.printer_ids.filtered(
                lambda p: p.printer_type != 'nightpos_tcp'
            )
            printers = config.nightpos_preparation_printer_ids.filtered('active')
            if not printers:
                config.printer_ids.filtered(
                    lambda p: p.printer_type == 'nightpos_tcp'
                ).unlink()
                super(PosConfig, config).write({
                    'printer_ids': [Command.set(legacy.ids)],
                    'is_order_printer': False,
                })
                continue

            linked = PosPrinter
            for nightpos_printer in printers:
                pos_printer = PosPrinter.search([
                    ('nightpos_printer_id', '=', nightpos_printer.id),
                    ('company_id', '=', config.company_id.id),
                ], limit=1)
                printer_vals = {
                    'name': nightpos_printer.name,
                    'printer_type': 'nightpos_tcp',
                    'nightpos_printer_id': nightpos_printer.id,
                    'product_categories_ids': [
                        Command.set(nightpos_printer.product_categories_ids.ids)
                    ],
                    'company_id': config.company_id.id,
                    'pos_config_ids': [Command.link(config.id)],
                }
                if pos_printer:
                    pos_printer.write(printer_vals)
                else:
                    pos_printer = PosPrinter.create(printer_vals)
                linked |= pos_printer

            orphans = config.printer_ids.filtered(
                lambda p: p.printer_type == 'nightpos_tcp'
                and p.nightpos_printer_id not in printers
            )
            orphans.unlink()
            super(PosConfig, config).write({
                'printer_ids': [Command.set((linked | legacy).ids)],
                'is_order_printer': True,
            })

    @api.model
    def _load_pos_data_fields(self, config):
        fields_list = super()._load_pos_data_fields(config)
        # Core POS uses an empty list to load the full pos.config (all fields).
        # Returning only NightPOS field names would omit trusted_config_ids and
        # break the POS UI (TypeError: config.raw.trusted_config_ids is not iterable).
        if not fields_list:
            return fields_list
        extra = [
            'nightpos_receipt_mode',
            'nightpos_receipt_device_policy',
            'nightpos_receipt_enabled',
            'nightpos_receipt_printer_id',
            'nightpos_chrome_printer_name',
        ]
        if 'nightpos_sunmi_enabled' in self._fields:
            extra.append('nightpos_sunmi_enabled')
        return list(dict.fromkeys([*fields_list, *extra]))

    def open_existing_session_cb(self):
        self.ensure_one()
        if self.current_session_id and not self.current_session_id.exists():
            return self._action_to_open_ui()
        return super().open_existing_session_cb()
