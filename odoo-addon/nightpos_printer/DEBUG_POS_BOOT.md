# Debug POS stuck on load (soho.nightpos.com)

Use this on: `https://soho.nightpos.com/pos/ui/10?from_backend=True&debug=1`

## 1. Log in first

If you see the **NightPOS login page** (email / password), POS UI will not load until you are logged in. Open POS from the backend **while logged in**, or sign in first.

## 2. Upgrade modules on the server

```bash
./odoo-bin -c /etc/odoo19-server.conf -d soho.nightpos.com -u nightpos_printer,nightpos_device,pos_product_stock --stop-after-init
```

Critical fixes:

- `pos_product_stock`: must `await super.processServerData()` (was breaking boot).
- `nightpos_device`: device login must not block `afterProcessServerData`.
- `nightpos_printer`: boot watchdog + session recovery (no infinite Promise).

## 3. Hard refresh POS assets

After upgrade: **Ctrl+Shift+R** on the POS tab (or clear site data for soho.nightpos.com).

## 4. Console commands (?debug=1)

Open DevTools → Console, then:

```js
__nightpos_boot_report()
```

| Field | Meaning |
|-------|---------|
| `markReady: true` | Core POS called `markReady()` — loader should hide |
| `setupDone: true` | `PosStore.setup()` finished |
| `lastStep` | Where boot stopped if stuck |
| `errorCount` | JS errors during boot |

Also:

```js
__nightpos_pos_debug.status()
await __nightpos_pos_debug.probeRouter()
```

## 5. Expected boot log order

```
[NightPOS POS] pos_boot_watchdog.js loaded
[NightPOS POS] POS boot: PosStore.setup started
[NightPOS POS boot] initServerData.start
[NightPOS POS boot] afterProcessServerData.start
[NightPOS POS boot] markReady.called
[NightPOS POS boot] afterProcessServerData.done
[NightPOS POS boot] initServerData.done
[NightPOS POS boot] PosStore.setup.done
[NightPOS POS] POS boot: afterProcessServerData done (navigate next)
```

If logs stop before `markReady.called`, boot is stuck in `syncAllOrders` or an RPC — check **Network** tab for pending requests.

## 6. Network tab

Filter: `XHR` / `Fetch`. Look for:

- `load_data` / `pos.session` — hanging or 500
- `/nightpos/device/register_login` — must **not** block UI (runs in background)
- Long `sync` — many orders syncing

## 7. Disable suspects (test)

Temporarily uninstall one module at a time and retest:

1. `pos_product_stock`
2. `nightpos_restaurant`
3. `nightpos_vip` (if zone minimum spend is used)
3. `nightpos_device`

If POS works after removing one, that module needs the latest fix from this repo.

## 8. Share for support

Copy from console:

1. Output of `__nightpos_boot_report()`
2. Last red error (full message + stack)
3. Screenshot of Network — any request **Pending** > 30s
