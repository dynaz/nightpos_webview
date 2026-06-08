import base64
from odoo import http
from odoo.http import request


class NightposPrinterController(http.Controller):

    @http.route('/nightpos/printers', type='jsonrpc', auth='user', methods=['POST'])
    def list_printers(self):
        """Return all active TCP printers."""
        printers = request.env['nightpos.printer'].get_all_printers()
        return {'success': True, 'printers': printers}

    @http.route('/nightpos/printers/default', type='jsonrpc', auth='user', methods=['POST'])
    def get_default_printer(self):
        """Return default printer."""
        printer = request.env['nightpos.printer'].get_default_printer()
        return {'success': True, 'printer': printer}

    @http.route('/nightpos/printers/escpos_text', type='jsonrpc', auth='user', methods=['POST'])
    def text_to_escpos(self, text, printer_id=None):
        """
        Convert plain text to ESC/POS bytes (base64).
        Frontend JS calls this, then passes the base64 to TcpPrinter handler.

        Args:
            text (str): UTF-8 text to print
            printer_id (int|None): use this printer's config; None = default

        Returns:
            {success, escpos_b64, printer: {ip, port, name}}
        """
        Printer = request.env['nightpos.printer']
        if printer_id:
            printer = Printer.browse(int(printer_id))
            if not printer.exists():
                return {'success': False, 'error': f'Printer {printer_id} not found'}
        else:
            default = Printer.get_default_printer()
            if not default:
                return {'success': False, 'error': 'No printer configured'}
            printer = Printer.browse(default['id'])

        escpos_bytes = printer._text_to_escpos(text)
        return {
            'success': True,
            'escpos_b64': base64.b64encode(escpos_bytes).decode('ascii'),
            'printer': printer._as_dict(),
        }

    @http.route('/nightpos/printers/print_image', type='jsonrpc', auth='user', methods=['POST'])
    def print_image(self, image, printer_id=None):
        """Convert Odoo receipt image (base64) to ESC/POS for NightPOS Android."""
        Printer = request.env['nightpos.printer']
        printer = Printer.browse()
        if printer_id:
            printer = Printer.browse(int(printer_id))
            if not printer.exists():
                return {'success': False, 'error': f'Printer {printer_id} not found'}
        else:
            default = Printer.get_default_printer()
            if default:
                printer = Printer.browse(default['id'])

        try:
            escpos_bytes = Printer._image_to_escpos(image)
        except Exception as exc:
            return {'success': False, 'error': str(exc)}

        return {
            'success': True,
            'escpos_b64': base64.b64encode(escpos_bytes).decode('ascii'),
            'printer': printer._as_dict() if printer else {},
        }

    @http.route('/nightpos/printers/test_escpos', type='jsonrpc', auth='user', methods=['POST'])
    def test_escpos(self, printer_id):
        """Return a test-page ESC/POS payload for the given printer."""
        printer = request.env['nightpos.printer'].browse(int(printer_id))
        if not printer.exists():
            return {'success': False, 'error': 'Printer not found'}
        escpos_bytes = printer._generate_test_escpos()
        return {
            'success': True,
            'escpos_b64': base64.b64encode(escpos_bytes).decode('ascii'),
            'printer': printer._as_dict(),
        }

    @http.route('/nightpos/printers/test_tcp', type='jsonrpc', auth='user', methods=['POST'])
    def test_tcp(self, printer_id=None, ip=None, port=9100):
        """Test TCP reachability from the Odoo server (no Android app)."""
        Printer = request.env['nightpos.printer']
        if printer_id:
            printer = Printer.browse(int(printer_id))
            if not printer.exists():
                return {'success': False, 'error': 'Printer not found'}
            ip, port = printer.ip, int(printer.port or 9100)
        elif not ip:
            return {'success': False, 'error': 'printer_id or ip required'}
        else:
            port = int(port or 9100)
        reachable = Printer._test_tcp_connection(ip, port)
        return {
            'success': True,
            'reachable': reachable,
            'ip': ip,
            'port': port,
        }

    @http.route('/nightpos/printers/send_tcp', type='jsonrpc', auth='user', methods=['POST'])
    def send_tcp(self, escpos_b64, printer_id=None, ip=None, port=9100):
        """Send raw ESC/POS (base64) to a TCP printer from the Odoo server."""
        Printer = request.env['nightpos.printer']
        try:
            escpos_bytes = base64.b64decode(escpos_b64)
        except Exception:
            return {'success': False, 'error': 'Invalid base64 payload'}

        if printer_id:
            printer = Printer.browse(int(printer_id))
            if not printer.exists():
                return {'success': False, 'error': 'Printer not found'}
        elif ip:
            printer = Printer.new({'ip': ip, 'port': int(port or 9100)})
        else:
            return {'success': False, 'error': 'printer_id or ip required'}

        try:
            printer._send_raw_tcp(escpos_bytes)
        except Exception as exc:
            return {'success': False, 'error': str(exc)}

        return {
            'success': True,
            'printer': printer._as_dict() if printer.id else {'ip': ip, 'port': int(port or 9100)},
        }

    @http.route('/nightpos/printers/print_image_tcp', type='jsonrpc', auth='user', methods=['POST'])
    def print_image_tcp(self, image, printer_id=None, ip=None, port=9100):
        """Convert receipt image to ESC/POS and print via server TCP (pos_tcp_esc_printer flow)."""
        Printer = request.env['nightpos.printer']
        if printer_id:
            printer = Printer.browse(int(printer_id))
            if not printer.exists():
                return {'success': False, 'error': f'Printer {printer_id} not found'}
        elif ip:
            printer = Printer.new({'ip': ip, 'port': int(port or 9100)})
        else:
            default = Printer.get_default_printer()
            if not default:
                return {'success': False, 'error': 'No printer configured'}
            printer = Printer.browse(default['id'])

        try:
            escpos_bytes = Printer._image_to_escpos(image)
            printer._send_raw_tcp(escpos_bytes)
        except Exception as exc:
            return {'success': False, 'error': str(exc)}

        return {
            'success': True,
            'printer': printer._as_dict() if printer.id else {'ip': ip, 'port': int(port or 9100)},
        }

    @http.route('/nightpos/device_print/get', type='jsonrpc', auth='user', methods=['POST'])
    def device_print_get(self, pos_config_id, device_key):
        """Load per-device print settings from the database."""
        prefs = request.env['nightpos.device.print'].sudo().get_device_prefs(
            pos_config_id, device_key,
        )
        return {'success': True, 'prefs': prefs}

    @http.route('/nightpos/device_print/set', type='jsonrpc', auth='user', methods=['POST'])
    def device_print_set(
        self,
        pos_config_id,
        device_key,
        receipt_mode=None,
        chrome_printer_name=None,
        nightpos_receipt_printer_id=None,
        name=None,
    ):
        """Save per-device print settings to the database."""
        prefs = request.env['nightpos.device.print'].sudo().set_device_prefs(
            pos_config_id,
            device_key,
            receipt_mode=receipt_mode,
            chrome_printer_name=chrome_printer_name,
            nightpos_receipt_printer_id=nightpos_receipt_printer_id,
            name=name,
        )
        return {'success': True, 'prefs': prefs}
