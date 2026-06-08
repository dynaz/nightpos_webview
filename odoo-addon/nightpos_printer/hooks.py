def post_init_hook(env):
    """Map legacy boolean flags to nightpos_receipt_mode."""
    if 'nightpos_receipt_mode' not in env['pos.config']._fields:
        return
    Config = env['pos.config'].sudo()
    for config in Config.search([]):
        if config.nightpos_receipt_mode not in (False, 'odoo'):
            continue
        mode = 'odoo'
        if getattr(config, 'nightpos_sunmi_enabled', False):
            mode = 'sunmi'
        elif config.nightpos_receipt_enabled:
            mode = 'tcp'
        config.nightpos_receipt_mode = mode
