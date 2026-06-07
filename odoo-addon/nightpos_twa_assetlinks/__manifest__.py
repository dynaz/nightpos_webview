{
    "name": "NightPOS Soho - TWA Asset Links",
    "version": "19.0.1.0.0",
    "category": "Tools",
    "summary": "Serves /.well-known/assetlinks.json for the NightPOS Soho Trusted Web Activity",
    "description": """
Serves the Digital Asset Links file (/.well-known/assetlinks.json) that the
NightPOS Soho Android app's Trusted Web Activity needs to verify it's allowed
to render this domain without Chrome's address bar.

This is an alternative to serving the file from the reverse proxy (see
NGINX_PROXY_MANAGER.md / TWA.md in the nightpos_webview repo) — useful if you'd
rather keep the file alongside the Odoo deployment than edit the proxy config,
or if multiple environments (staging/production) need different fingerprints.
""",
    "author": "NightPOS",
    "license": "LGPL-3",
    "depends": ["web"],
    "installable": True,
    "application": False,
}
