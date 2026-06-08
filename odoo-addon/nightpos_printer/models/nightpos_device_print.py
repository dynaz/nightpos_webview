# -*- coding: utf-8 -*-

from odoo import api, fields, models

from .nightpos_print_modes import NIGHTPOS_PRINTER_TYPE_SELECTION


class NightposDevicePrint(models.Model):
    _name = 'nightpos.device.print'
    _description = 'NightPOS Per-Device Print Settings'
    _order = 'last_seen desc, id desc'

    name = fields.Char(
        string='Device label',
        help='Optional friendly name (browser, tablet, etc.).',
    )
    pos_config_id = fields.Many2one(
        'pos.config',
        string='Point of Sale',
        required=True,
        ondelete='cascade',
        index=True,
    )
    device_key = fields.Char(
        string='Device key',
        required=True,
        index=True,
        help='Stable identifier for this browser or tablet (stored locally on the device).',
    )
    receipt_mode = fields.Selection(
        selection=NIGHTPOS_PRINTER_TYPE_SELECTION,
        string='Receipt mode',
        required=True,
        default='auto',
    )
    chrome_printer_name = fields.Char(
        string='Local printer (Chrome)',
        help='Windows or CUPS queue name for Chrome Extension mode on this device.',
    )
    nightpos_receipt_printer_id = fields.Many2one(
        'nightpos.printer',
        string='Receipt printer',
        domain="[('active', '=', True)]",
        ondelete='set null',
    )
    last_seen = fields.Datetime(string='Last seen', readonly=True)
    user_id = fields.Many2one(
        'res.users',
        string='Last updated by',
        readonly=True,
    )
    company_id = fields.Many2one(
        related='pos_config_id.company_id',
        store=True,
        readonly=True,
    )

    _nightpos_device_print_unique = models.Constraint(
        'unique(pos_config_id, device_key)',
        'Each device can only have one print profile per Point of Sale.',
    )

    def _to_client_dict(self):
        self.ensure_one()
        return {
            'receipt_mode': self.receipt_mode,
            'chrome_printer_name': self.chrome_printer_name or '',
            'nightpos_receipt_printer_id': self.nightpos_receipt_printer_id.id or False,
            'name': self.name or '',
        }

    @api.model
    def get_device_prefs(self, pos_config_id, device_key):
        if not pos_config_id or not device_key:
            return {}
        record = self.search([
            ('pos_config_id', '=', int(pos_config_id)),
            ('device_key', '=', device_key),
        ], limit=1)
        if not record:
            return {}
        record.write({'last_seen': fields.Datetime.now()})
        return record._to_client_dict()

    @api.model
    def set_device_prefs(
        self,
        pos_config_id,
        device_key,
        receipt_mode=None,
        chrome_printer_name=None,
        nightpos_receipt_printer_id=None,
        name=None,
    ):
        if not pos_config_id or not device_key:
            return {}
        if receipt_mode is not None:
            valid_modes = dict(NIGHTPOS_PRINTER_TYPE_SELECTION)
            if receipt_mode not in valid_modes:
                receipt_mode = 'auto'

        vals = {
            'last_seen': fields.Datetime.now(),
            'user_id': self.env.uid,
        }
        if receipt_mode is not None:
            vals['receipt_mode'] = receipt_mode
        if chrome_printer_name is not None:
            vals['chrome_printer_name'] = chrome_printer_name or False
        if nightpos_receipt_printer_id is not None:
            vals['nightpos_receipt_printer_id'] = int(nightpos_receipt_printer_id) or False
        if name is not None:
            vals['name'] = name or False

        record = self.search([
            ('pos_config_id', '=', int(pos_config_id)),
            ('device_key', '=', device_key),
        ], limit=1)
        if record:
            record.write(vals)
        else:
            vals.update({
                'pos_config_id': int(pos_config_id),
                'device_key': device_key,
                'receipt_mode': receipt_mode or 'auto',
            })
            record = self.create(vals)
        return record._to_client_dict()
