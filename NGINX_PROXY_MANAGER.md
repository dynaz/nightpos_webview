# Setting up Nginx Proxy Manager for soho.nightpos.com

This guide covers configuring **Nginx Proxy Manager (NPM)** so that
`https://soho.nightpos.com`:

1. reverse-proxies to your Odoo 19 backend (with HTTPS + WebSocket support
   for the POS longpolling/bus), and
2. serves `/.well-known/assetlinks.json`, which the NightPOS Soho app's
   Trusted Web Activity needs for Digital Asset Links verification (see
   [`TWA.md`](./TWA.md) for why this file exists and what it must contain).

If you already have a working Proxy Host for `soho.nightpos.com`, skip to
**Step 3**.

## Step 1 — Add the Proxy Host

In the NPM admin UI: **Hosts → Proxy Hosts → Add Proxy Host**.

**Details tab**

| Field | Value |
|---|---|
| Domain Names | `soho.nightpos.com` |
| Scheme | `http` (or `https` if Odoo itself terminates TLS) |
| Forward Hostname / IP | the Odoo server's address, e.g. `10.0.0.5` or `odoo` (Docker service name) |
| Forward Port | Odoo's port, typically `8069` |
| Cache Assets | off (Odoo handles its own asset caching/versioning) |
| Block Common Exploits | on |
| Websockets Support | **on** — required for Odoo's longpolling/`bus` (POS real-time updates won't work without this) |

## Step 2 — SSL tab

1. **SSL Certificate** → *Request a new SSL Certificate* (Let's Encrypt).
2. Enable **Force SSL** and **HTTP/2 Support**.
3. Enable **HSTS** if you want (optional, but recommended once everything works).

Save. Confirm `https://soho.nightpos.com` loads Odoo correctly before continuing.

## Step 3 — Add the assetlinks.json custom location

This is the part the TWA app specifically needs. Edit the same Proxy Host →
**Advanced** tab, and add the following to the **Custom Nginx Configuration**
box:

```nginx
location /.well-known/assetlinks.json {
    default_type application/json;
    return 200 '[{"relation":["delegate_permission/common.handle_all_urls"],"target":{"namespace":"android_app","package_name":"com.nightpos.app","sha256_cert_fingerprints":["A6:E9:9E:33:88:DF:C2:05:C5:73:42:65:92:E0:39:F4:C1:9C:6B:12:69:A6:81:C3:C9:7D:66:93:67:26:04:FF"]}},{"relation":["delegate_permission/common.handle_all_urls"],"target":{"namespace":"android_app","package_name":"com.nightpos.app.debug","sha256_cert_fingerprints":["A6:E9:9E:33:88:DF:C2:05:C5:73:42:65:92:E0:39:F4:C1:9C:6B:12:69:A6:81:C3:C9:7D:66:93:67:26:04:FF"]}}]';
}
```

This serves the file **directly from Nginx** — no changes to the Odoo
backend are needed, and it takes priority over whatever Odoo would otherwise
return for that path.

Click **Save**. NPM regenerates the Nginx config and reloads automatically
(no container restart needed).

> If you ever rotate the app's signing key, you must update the fingerprint
> in this block (and in `TWA.md`) — see the warning in `TWA.md` about why the
> committed keystore should never be regenerated.

## Step 4 — Verify

Open `https://soho.nightpos.com/.well-known/assetlinks.json` in a browser.
You should see the JSON above returned with `Content-Type: application/json`
— **not** an Odoo 404 page.

```bash
curl -s https://soho.nightpos.com/.well-known/assetlinks.json | jq .
```

If you get Odoo's 404 instead of the JSON, double check:
- The custom config was saved on the **same Proxy Host** that serves
  `soho.nightpos.com` (not a different host/domain entry).
- There's no conflicting `location /.well-known/` block elsewhere overriding it.
- You saved (NPM shows a brief "success" toast and the host's "Online"
  indicator stays green).

## Step 5 — Test from the device

1. On the Sunmi device, install the NightPOS Soho APK.
2. Open it and tap "เปิดขาย" / "รายงาน" / "ลูกค้า".
3. **Success** = the page opens full-screen with no Chrome address bar.
4. If a Chrome toolbar appears, Chrome couldn't verify the Digital Asset
   Link — re-check Step 4, then clear Chrome's app storage on the device
   (Android Settings → Apps → Chrome → Storage → Clear storage) and retry,
   since Chrome caches verification results for a while.

See [`TWA.md`](./TWA.md) for the full background on why this is needed and
what trade-offs the TWA approach introduces (separate Chrome session storage,
kiosk mode behavior).
