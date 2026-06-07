{
    'name': 'NightPOS PWA',
    'version': '19.0.1.0.0',
    'category': 'Point of Sale',
    'summary': 'Progressive Web App support — standalone display, offline shell, installable',
    'author': 'NightPOS',
    'website': 'https://soho.nightpos.com',
    'depends': ['web'],
    'data': [],
    'assets': {
        'web.assets_web': [
            'nightpos_pwa/static/src/js/pwa_register.js',
        ],
    },
    'installable': True,
    'auto_install': False,
    'license': 'LGPL-3',
}
