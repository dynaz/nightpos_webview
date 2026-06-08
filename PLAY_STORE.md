# Publishing to Google Play

## 1. Get the release bundle (AAB)

Google Play requires an **Android App Bundle (`.aab`)**, not an APK, for new
app submissions. CI now builds one automatically:

- Workflow: [`.github/workflows/build-release.yml`](.github/workflows/build-release.yml)
- Trigger it from the **Actions** tab (`Build APK and Release` → **Run workflow**)
  or push a `v*` tag. Each run also publishes a **GitHub Release** (tag
  `build-<version>-<short-sha>` for manual runs) with `app-release.aab` and the
  debug APK attached as release assets — e.g.
  https://github.com/dynaz/nightpos_webview/releases/tag/build-1.0.0-9bf11e4
  has `app-release.aab` ready to download right now.
- (There's also an `nightpos-release-aab` Actions-artifact upload step, but
  Actions artifact storage on this repo is currently at quota — "Usage is
  recalculated every 6-12 hours" — so the **Release assets are the reliable
  way to grab the build** until that clears. You can free it up sooner by
  deleting old artifacts/logs from old workflow runs under the repo's
  **Actions** tab.)
- `app-release.aab` is signed with the committed
  `keystore/nightpos-release.keystore` (alias `nightpos`, password
  `nightpos123` — see [`TWA.md`](TWA.md) for the full keystore story and the
  asset-links fingerprint it produces).

Upload that `.aab` directly under **Play Console → Production (or Internal
testing) → Create new release**.

> ⚠️ **Important — Play App Signing changes the fingerprint.** By default,
> Play Console enrolls new apps in **Play App Signing**: Google keeps your
> uploaded `nightpos-release.keystore` only as the *upload* key, and
> **re-signs the app with its own key** before distributing it to users. That
> means the SHA-256 fingerprint actually shipped on Play-installed devices
> will be **different** from `A6:E9:9E:33:88:DF:C2:05:C5:73:42:65:92:E0:39:F4:
> C1:9C:6B:12:69:A6:81:C3:C9:7D:66:93:67:26:04:FF` — and the TWA will fail
> Digital Asset Links verification (Chrome shows the address bar / "flickers"
> exactly like the issue already being chased in `TWA.md`) for anyone who
> installed via the Play Store.
>
> **Fix**: after the first upload, go to **Play Console → Setup → App
> integrity → App signing** and copy the **"App signing key certificate" →
> SHA-256 certificate fingerprint**. Add a *second* fingerprint entry for
> both `com.nightpos.app` and `com.nightpos.app.debug` in `assetlinks.json`
> (update `odoo-addon/nightpos_twa_assetlinks/controllers/main.py`'s
> `_FINGERPRINT`/`_ASSET_LINKS`, or the Nginx custom-location block from
> `NGINX_PROXY_MANAGER.md`, to include **both** fingerprints in the
> `sha256_cert_fingerprints` array) — sideloaded/CI-built APKs keep working
> with the original keystore fingerprint, and Play-distributed installs verify
> with the Play signing fingerprint.

## 2. Store listing graphics you'll need

Play Console → **Grow → Store presence → Main store listing**:

| Asset | Spec | Status |
| --- | --- | --- |
| App icon | 512×512 PNG, 32-bit with alpha | ✅ already in repo: [`app/ic_launcher_playstore_512.png`](app/ic_launcher_playstore_512.png) |
| Feature graphic | 1024×500 JPG/PNG, no alpha | ⬜ not yet created — simple option: dark `#0B0B12` background + the logo + "NightPOS Soho" in `#A35CFF`/`#FF4FD8` |
| Phone screenshots | 2–8 images, JPG/PNG, 16:9 or 9:16, each side 320–3840px | ⬜ capture from a real device (see below) |
| 7" / 10" tablet screenshots | same format, optional but recommended since this is a POS/tablet app | ⬜ capture from a real device |

## 3. Capturing screenshots (do this on the test device/tablet)

This sandbox can't run an Android emulator or a browser renderer (no network
access to download emulator images / browser binaries), so screenshots have to
be captured on real hardware — which also gives you the most authentic result
since the app's whole pitch is "looks great on the actual POS tablet."

Recommended shots (landscape, since `MainActivity` is locked to landscape):

1. **Dashboard** — the menu grid (เปิดขาย / รายงาน / ลูกค้า / Products /
   Discount & Loyalty / Gift Cards / Settings / ออกจากระบบ)
2. **POS in action** — `เปิดขาย` running against a real order/cart screen
3. **Reports** — a populated sales report
4. **Settings** — server URL + kiosk-mode toggles panel

How to capture, on-device:
```bash
# with the device connected over USB / adb over network and developer options on
adb shell screencap -p /sdcard/shot1.png
adb pull /sdcard/shot1.png
```
Or just use the device's own screenshot gesture (power + volume-down on most
Android tablets/phones) and AirDrop/transfer the PNGs off.

Keep the dark theme (`#0B0B12` background, `#A35CFF` neon-purple /
`#FF4FD8` neon-pink accents) as-is in the screenshots — that's the app's
actual look and the contrast reads well as a store listing for a
nightlife/restaurant POS.

## 4. Listing copy checklist

- **Short description** (≤80 chars): e.g. "ระบบจุดขาย Odoo POS สำหรับร้านอาหารและไนท์คลับ"
- **Full description**: pull from `dashboard_subtitle` /
  `strings.xml` for the Thai pitch, expand with the feature list (POS,
  Reports, Customers, Discount & Loyalty, Gift Cards, kiosk mode, offline
  detection)
- **Privacy policy URL**: required by Play even for internal-use apps —
  point it at a page on `soho.nightpos.com` (Odoo can serve a static page or
  another small controller route, similar to the `assetlinks.json` addon in
  `odoo-addon/`)
- **Content rating questionnaire**: answer based on actual app behavior (no
  user-generated content, no ads, handles payments via the wrapped Odoo POS)
- **Data safety form**: declare what the app/TWA collects — at minimum it
  talks to `soho.nightpos.com` over HTTPS or networking the merchant device

## 5. Internal testing first

Before a production release, push to **Internal testing** track with the same
AAB — lets you and a couple of test accounts install via the Play Store
listing flow and confirm the TWA opens full-screen (no Chrome address bar) on
real Play-installed builds, since Play App Signing changes the signing
certificate that ships to users (see note in step 1 — Play re-signs with its
own upload key by default unless you opted out).
