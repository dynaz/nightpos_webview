import json

from odoo import http
from odoo.http import request

# Must match the SHA-256 fingerprint of the keystore committed at
# keystore/nightpos-release.keystore in the nightpos_webview repo (see TWA.md).
# Both build variants (release: com.nightpos.app, debug: com.nightpos.app.debug)
# sign with that same keystore, so both package names share this fingerprint.
_FINGERPRINT = (
    "A6:E9:9E:33:88:DF:C2:05:C5:73:42:65:92:E0:39:F4:"
    "C1:9C:6B:12:69:A6:81:C3:C9:7D:66:93:67:26:04:FF"
)

_ASSET_LINKS = [
    {
        "relation": ["delegate_permission/common.handle_all_urls"],
        "target": {
            "namespace": "android_app",
            "package_name": "com.nightpos.app",
            "sha256_cert_fingerprints": [_FINGERPRINT],
        },
    },
    {
        "relation": ["delegate_permission/common.handle_all_urls"],
        "target": {
            "namespace": "android_app",
            "package_name": "com.nightpos.app.debug",
            "sha256_cert_fingerprints": [_FINGERPRINT],
        },
    },
]


class NightPosTwaAssetLinksController(http.Controller):

    @http.route(
        "/.well-known/assetlinks.json",
        type="http",
        auth="public",
        csrf=False,
        methods=["GET"],
    )
    def assetlinks(self, **kwargs):
        return request.make_response(
            json.dumps(_ASSET_LINKS),
            headers=[("Content-Type", "application/json")],
        )
