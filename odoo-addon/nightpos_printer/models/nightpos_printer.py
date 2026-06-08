import re
import base64
import socket
from datetime import datetime
from odoo import models, fields, api, _
from odoo.exceptions import UserError

from .nightpos_print_modes import NIGHTPOS_PRINTER_TYPE_SELECTION

TCP_CONNECT_TIMEOUT = 5
TCP_SEND_TIMEOUT = 15


class NightposPrinter(models.Model):
    _name = 'nightpos.printer'
    _description = 'NightPOS TCP Printer'
    _inherit = ['pos.load.mixin']
    _order = 'sequence, name'

    name = fields.Char('ชื่อพริ้นเตอร์', required=True)
    ip = fields.Char('IP Address')
    port = fields.Integer('Port', default=9100)
    sequence = fields.Integer(default=10)
    is_default = fields.Boolean('Default')
    active = fields.Boolean(default=True)
    note = fields.Char('หมายเหตุ')
    printer_type = fields.Selection(
        selection=NIGHTPOS_PRINTER_TYPE_SELECTION,
        string='Printer Type',
        default='tcp',
        required=True,
        help='How this printer record is tested and used from POS / Chrome / Android.',
    )
    chrome_printer_name = fields.Char(
        string='Windows / Local Printer',
        help='Exact Windows or CUPS queue name for Chrome Extension mode '
             '(from the NightPOS print server list).',
    )

    # ── Constraints ────────────────────────────────────────────────────────────

    @api.constrains('port', 'printer_type')
    def _check_port(self):
        for rec in self:
            if rec.printer_type != 'tcp':
                continue
            if not (1 <= (rec.port or 0) <= 65535):
                raise UserError(_('Port ต้องอยู่ระหว่าง 1–65535'))

    @api.constrains('ip', 'printer_type')
    def _check_ip(self):
        pattern = re.compile(r'^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$')
        for rec in self:
            if rec.printer_type == 'sunmi':
                continue
            if rec.printer_type == 'tcp':
                if not pattern.match(rec.ip or ''):
                    raise UserError(_('IP Address ไม่ถูกต้อง'))
            elif rec.ip and not pattern.match(rec.ip):
                raise UserError(_('IP Address ไม่ถูกต้อง'))

    # ── ORM overrides ──────────────────────────────────────────────────────────

    # Odoo 17+ requires @api.model_create_multi — create() receives a list of dicts
    @api.model_create_multi
    def create(self, vals_list):
        no_printers_yet = not self.search_count([])
        for i, vals in enumerate(vals_list):
            if vals.get('is_default'):
                # Clear default on all existing records
                self.search([]).write({'is_default': False})
            elif no_printers_yet and i == 0:
                # First printer ever → auto-set as default
                vals['is_default'] = True
        return super().create(vals_list)

    def write(self, vals):
        if vals.get('is_default'):
            self.search([('id', 'not in', self.ids)]).write({'is_default': False})
        return super().write(vals)

    # ── Actions ────────────────────────────────────────────────────────────────

    def action_set_default(self):
        self.ensure_one()
        self.write({'is_default': True})  # write() clears others automatically

    def _resolve_chrome_printer_name(self, pos_config=None):
        """Windows/CUPS queue name for the local print server (not the Odoo label)."""
        if pos_config:
            return (pos_config.nightpos_chrome_printer_name or '').strip()
        if self.chrome_printer_name:
            return self.chrome_printer_name.strip()
        ctx_name = (self.env.context.get('nightpos_chrome_printer_name') or '').strip()
        if ctx_name:
            return ctx_name
        linked = self.env['pos.config'].search([
            ('nightpos_receipt_printer_id', '=', self.id),
            ('nightpos_chrome_printer_name', '!=', False),
        ], limit=1)
        return (linked.nightpos_chrome_printer_name or '').strip()

    def _prepare_local_print_params(
        self, escpos_b64=None, text=None, protocol='escpos', pos_config=None,
    ):
        """Payload for JS `nightpos_local_print_event` (Chrome Extension bridge)."""
        self.ensure_one()
        if escpos_b64 is None:
            if text is not None:
                payload = base64.b64encode(self._text_to_escpos(text)).decode('ascii')
            else:
                payload = base64.b64encode(self._generate_test_escpos()).decode('ascii')
        else:
            payload = escpos_b64
        chrome_name = self._resolve_chrome_printer_name(pos_config=pos_config)
        return {
            'printer_name': chrome_name,
            'raw_data': payload,
            'encoding': 'base64',
            'protocol': protocol,
            'printer': self._as_dict(),
            'source': 'nightpos.printer',
        }

    def _test_print_client_params(self, escpos_bytes, pos_config=None, shop_mode=None):
        """Build client-action params; JS routes per physical device."""
        self.ensure_one()
        params = {
            'printer_id': self.id,
            'name': self.name,
            'ip': self.ip or '',
            'port': int(self.port or 9100),
            'escpos_b64': base64.b64encode(escpos_bytes).decode('ascii'),
            'shop_mode': shop_mode or self.printer_type,
            'chrome_printer_name': self._resolve_chrome_printer_name(pos_config=pos_config),
        }
        if pos_config:
            params.update({
                'pos_config_id': pos_config.id,
                'shop_mode': shop_mode or pos_config.nightpos_receipt_mode,
                'device_policy': pos_config.nightpos_receipt_device_policy,
                'chrome_printer_name': pos_config.nightpos_chrome_printer_name or params['chrome_printer_name'],
            })
        else:
            linked = self.env['pos.config'].search([
                ('nightpos_receipt_printer_id', '=', self.id),
            ], limit=1)
            if linked:
                params.update({
                    'pos_config_id': linked.id,
                    'shop_mode': shop_mode or linked.nightpos_receipt_mode,
                    'device_policy': linked.nightpos_receipt_device_policy,
                    'chrome_printer_name': linked.nightpos_chrome_printer_name or params['chrome_printer_name'],
                })
        return params

    def _action_test_print_for_mode(self, mode, pos_config=None):
        self.ensure_one()
        if mode == 'chrome_extension':
            return self.action_local_print_test(pos_config=pos_config)
        if mode == 'odoo':
            return {
                'type': 'ir.actions.client',
                'tag': 'nightpos_printer.test_print_odoo',
                'params': self._test_print_client_params(
                    self._generate_test_escpos(), pos_config=pos_config, shop_mode=mode,
                ),
            }
        return {
            'type': 'ir.actions.client',
            'tag': 'nightpos_printer.test_print',
            'params': self._test_print_client_params(
                self._generate_test_escpos(), pos_config=pos_config, shop_mode=mode,
            ),
        }

    def action_test_print(self):
        """Test print using this record's Printer Type."""
        self.ensure_one()
        return self._action_test_print_for_mode(self.printer_type)

    def action_test_print_odoo(self):
        self.ensure_one()
        return self._action_test_print_for_mode('odoo')

    def action_test_print_auto(self):
        self.ensure_one()
        return self._action_test_print_for_mode('auto')

    def action_test_print_tcp(self):
        self.ensure_one()
        return self._action_test_print_for_mode('tcp')

    def action_test_print_sunmi(self):
        self.ensure_one()
        return self._action_test_print_for_mode('sunmi')

    def action_test_print_chrome(self):
        self.ensure_one()
        return self._action_test_print_for_mode('chrome_extension')

    def action_test_print_android(self):
        """Legacy: force NightPOS Android TcpPrinter test dialog."""
        self.ensure_one()
        return {
            'type': 'ir.actions.client',
            'tag': 'nightpos_printer.test_print',
            'params': self._test_print_client_params(self._generate_test_escpos()),
        }

    def action_local_print_test(self, pos_config=None):
        """Dispatch ESC/POS test page via CustomEvent for Chrome Extension."""
        self.ensure_one()
        return {
            'type': 'ir.actions.client',
            'tag': 'nightpos_printer.local_print_test',
            'params': self._prepare_local_print_params(pos_config=pos_config),
        }

    def action_local_print(self, text=None, escpos_b64=None, protocol='escpos'):
        """Dispatch arbitrary ESC/POS or text to Chrome Extension."""
        self.ensure_one()
        return {
            'type': 'ir.actions.client',
            'tag': 'nightpos_printer.local_print',
            'params': self._prepare_local_print_params(
                escpos_b64=escpos_b64,
                text=text,
                protocol=protocol,
            ),
        }

    def get_local_print_action(self, text=None, escpos_b64=None, protocol='escpos'):
        """Helper for other modules: return client action dict."""
        self.ensure_one()
        return self.action_local_print(text=text, escpos_b64=escpos_b64, protocol=protocol)

    # ── Server-side TCP (pos_tcp_esc_printer style, no Android app) ───────────

    def _send_raw_tcp(self, escpos_bytes):
        """Send raw ESC/POS bytes to this printer over TCP (port 9100 by default)."""
        self.ensure_one()
        ip = (self.ip or '').strip()
        port = int(self.port or 9100)
        try:
            with socket.create_connection((ip, port), timeout=TCP_SEND_TIMEOUT) as sock:
                sock.sendall(escpos_bytes)
        except OSError as exc:
            raise UserError(_(
                'Cannot reach printer at %(ip)s:%(port)s — %(error)s',
                ip=ip, port=port, error=exc,
            )) from exc
        return True

    @api.model
    def _test_tcp_connection(self, ip, port=9100):
        """Return whether the Odoo server can open a TCP socket to the printer."""
        try:
            with socket.create_connection(
                (ip.strip(), int(port or 9100)),
                timeout=TCP_CONNECT_TIMEOUT,
            ):
                return True
        except OSError:
            return False

    # ── ESC/POS generation ─────────────────────────────────────────────────────

    def _generate_test_escpos(self):
        ESC = b'\x1b'
        GS  = b'\x1d'
        d = bytearray()
        d += ESC + b'@'
        d += ESC + b'a\x01'
        d += ESC + b'E\x01'
        d += ESC + b'!\x30'
        d += 'NightPOS\n'.encode('utf-8')
        d += ESC + b'!\x00'
        d += ESC + b'E\x00'
        d += ('-' * 32 + '\n').encode()
        d += ESC + b'a\x00'
        d += f'Printer : {self.name}\n'.encode('utf-8')
        if self.printer_type == 'sunmi':
            d += 'Type    : Sunmi built-in\n'.encode('utf-8')
        elif self.ip:
            d += f'IP      : {self.ip}:{self.port}\n'.encode('utf-8')
        d += f'Time    : {datetime.now().strftime("%d/%m/%Y %H:%M:%S")}\n'.encode('utf-8')
        d += ('-' * 32 + '\n').encode()
        d += ESC + b'a\x01'
        d += ESC + b'E\x01'
        d += '*** TEST PRINT OK ***\n'.encode('utf-8')
        d += ESC + b'E\x00'
        d += ESC + b'a\x00'
        d += ESC + b'd\x04'
        d += GS  + b'V\x41\x00'
        return bytes(d)

    def _text_to_escpos(self, text):
        ESC = b'\x1b'
        GS  = b'\x1d'
        d = bytearray()
        d += ESC + b'@'
        # ASCII-safe plain text for legacy printers (no UTF-8 box-drawing).
        safe = (
            text.replace('\u2014', '-').replace('\u2013', '-')
            .replace('\u00d7', 'x').replace('\u00b7', '.')
        )
        d += safe.encode('ascii', errors='replace')
        if not safe.endswith('\n'):
            d += b'\n'
        d += ESC + b'd\x04'
        d += GS + b'V\x41\x00'
        return bytes(d)

    @api.model
    def _imgcrop(self, im):
        """Split tall receipt images into strips (same approach as pos_tcp_esc_printer)."""
        ret = []
        imgwidth, imgheight = im.size
        y_pieces = max(1, int(imgheight / 20))
        height = imgheight // y_pieces
        width = imgwidth
        for i in range(y_pieces):
            end = (i + 1) * height
            if y_pieces == i + 1:
                end = imgheight
            box = (0, i * height, width, end)
            ret.append(im.crop(box))
        return ret

    @api.model
    def _image_to_escpos(self, image_b64):
        """Convert a JPEG/PNG receipt image to ESC/POS bytes (Odoo receipt layout)."""
        try:
            from io import BytesIO
            from PIL import Image
            from escpos.printer import Dummy
        except ImportError as exc:
            raise UserError(_(
                'Receipt image printing requires Python packages '
                '"pillow" and "python-escpos" on the server.'
            )) from exc

        im = Image.open(BytesIO(base64.b64decode(image_b64)))
        if im.mode != 'RGB':
            im = im.convert('RGB')

        dummy = Dummy()
        for chunk in self._imgcrop(im):
            dummy.image(chunk)
        dummy.cut()
        return dummy.output

    # ── POS session data ───────────────────────────────────────────────────────

    @api.model
    def _load_pos_data_domain(self, data, config):
        if config.nightpos_receipt_device_policy == 'per_device':
            return [('active', '=', True)]
        printer_ids = []
        if config.nightpos_receipt_printer_id:
            printer_ids.append(config.nightpos_receipt_printer_id.id)
        printer_ids.extend(config.nightpos_preparation_printer_ids.ids)
        if not printer_ids:
            return [('id', '=', 0)]
        return [('id', 'in', list(set(printer_ids)))]

    @api.model
    def _load_pos_data_fields(self, config):
        return ['id', 'name', 'ip', 'port', 'printer_type', 'chrome_printer_name', 'active']

    # ── Public API for other modules ──────────────────────────────────────────

    @api.model
    def get_default_printer(self):
        printer = self.search([('is_default', '=', True)], limit=1)
        if not printer:
            printer = self.search([], limit=1)
        return printer._as_dict() if printer else {}

    @api.model
    def get_all_printers(self):
        return self.search([])._as_dict_list()

    def _as_dict(self):
        self.ensure_one()
        return {
            'id': self.id,
            'name': self.name,
            'ip': self.ip or '',
            'port': int(self.port or 9100),
            'printer_type': self.printer_type,
            'is_default': self.is_default,
        }

    def _as_dict_list(self):
        return [p._as_dict() for p in self]

    def get_print_action(self, text=None, escpos_b64=None):
        """Helper: other modules call this to trigger a print from JS."""
        self.ensure_one()
        if escpos_b64 is None and text is not None:
            escpos_b64 = base64.b64encode(
                self._text_to_escpos(text)
            ).decode('ascii')
        return {
            'type': 'ir.actions.client',
            'tag': 'nightpos_printer.print',
            'params': {
                'printer_id': self.id,
                'name': self.name,
                'ip': self.ip or '',
                'port': int(self.port or 9100),
                'escpos_b64': escpos_b64,
            },
        }
