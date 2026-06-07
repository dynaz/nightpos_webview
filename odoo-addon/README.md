# NightPOS Soho — Odoo Addon

`nightpos_twa_assetlinks/` is a small Odoo 19 module that serves
`/.well-known/assetlinks.json` directly from Odoo via an HTTP controller —
**this is an alternative to** the Nginx Proxy Manager custom-location approach
documented in [`../NGINX_PROXY_MANAGER.md`](../NGINX_PROXY_MANAGER.md). Use
whichever is more convenient for your setup; you don't need both.

> Note: a Trusted Web Activity itself runs **on the Android device** (it's how
> the NightPOS Soho app renders your Odoo UI using Chrome's engine — see
> [`../TWA.md`](../TWA.md)). There's nothing to "install on Odoo" for the TWA
> itself; the only server-side piece Odoo needs to provide is this
> `assetlinks.json` file so Chrome can verify the app↔domain link.

## Why you might prefer this over the Nginx approach

- Keeps the file under version control alongside your Odoo deployment instead
  of inside the reverse-proxy config.
- No need to touch Nginx Proxy Manager at all once installed.
- Easier to manage if you run multiple environments (staging/production) that
  each need their own module instance.

## Installing it

Based on the directory layout you shared (`/odoo19/odoo19-server/`, with an
`addons/` folder next to `odoo-bin`):

```bash
# 1. Copy the module into your custom addons path
cp -r nightpos_twa_assetlinks /odoo19/odoo19-server/addons/

# 2. Make sure that addons path is in your Odoo config (odoo.conf), e.g.:
#    addons_path = /odoo19/odoo19-server/addons,/odoo19/odoo19-server/odoo/addons

# 3. Restart Odoo
sudo systemctl restart odoo19   # or however you run start.sh / the service

# 4. Install the module
#    Settings → Apps → remove the "Apps" filter → search "NightPOS Soho - TWA Asset Links" → Install
#    — or from the CLI:
./odoo-bin -c /path/to/odoo.conf -d <your_database> -i nightpos_twa_assetlinks --stop-after-init
```

## Verifying

```bash
curl -s https://soho.nightpos.com/.well-known/assetlinks.json | jq .
```

You should get back the JSON with `package_name: com.nightpos.app` /
`com.nightpos.app.debug` and the fingerprint
`A6:E9:9E:33:88:DF:C2:05:C5:73:42:65:92:E0:39:F4:C1:9C:6B:12:69:A6:81:C3:C9:7D:66:93:67:26:04:FF`
— **not** Odoo's normal 404 page.

If you get a 404, double-check the module shows as **Installed** (not just
present on disk) in Settings → Apps, and that no reverse-proxy rule is
intercepting `/.well-known/` before it reaches Odoo.

## If you ever rotate the signing key

The fingerprint is hardcoded in
`nightpos_twa_assetlinks/controllers/main.py` (`_FINGERPRINT`). If the app's
signing keystore is ever regenerated (it shouldn't be — see the warning in
`../TWA.md`), update it there, bump the module version in `__manifest__.py`,
and reinstall/upgrade the module.
