# Trusted Web Activity (TWA)

## Why

Some POS devices (e.g. Sunmi D2S Plus) ship an "Android System WebView"
component stuck on an old Chromium build (v83) that can't render Odoo 19's
modern UI — and that component **cannot be updated from device settings** on
these devices. The device's **Chrome browser app**, however, is a separate,
independently-versioned APK and can be far newer (v148 on the reference
device).

A Trusted Web Activity renders the Odoo backend full-screen using the
installed **Chrome browser's rendering engine** instead of the system WebView,
so the app gets a modern engine without requiring any device-level changes —
as long as Chrome itself is reasonably up to date.

"เปิดขาย / รายงาน / ลูกค้า" now launch in a TWA instead of the in-app
`android.webkit.WebView` screen.

## How it's wired up

- **`com.nightpos.app.twa.TwaLauncherActivity`**
  (`app/src/main/java/com/nightpos/app/twa/TwaLauncherActivity.kt`) extends
  `com.google.androidbrowserhelper.trusted.LauncherActivity` and overrides
  `getLaunchingUrl()` to read the target URL from an Intent extra
  (`TwaLauncherActivity.EXTRA_URL`, set via `TwaLauncherActivity.createIntent`)
  — this lets one launcher Activity serve all three destinations
  (POS / Reports / Customers) with whatever base URL is configured in Settings.
- **`AndroidManifest.xml`** registers the activity with the standard
  `android.support.customtabs.trusted.*` meta-data
  (`DEFAULT_URL` fallback, status/navigation bar colors).
- **`NightPOSNavHost.kt`** starts the TWA via
  `context.startActivity(TwaLauncherActivity.createIntent(context, url))`
  for `OpenPos` / `OpenReports` / `OpenCustomers` instead of navigating to the
  in-app `WebViewScreen`.
- **`gradle/libs.versions.toml` / `app/build.gradle.kts`** add the
  `com.google.androidbrowserhelper:androidbrowserhelper` dependency.
  > Pinned to **2.5.0** — newer versions (2.6.x) transitively pull in an alpha
  > `androidx.browser` that requires `compileSdk 36` + AGP `8.9.1`, which this
  > project doesn't use yet (see
  > [android-browser-helper#538](https://github.com/GoogleChrome/android-browser-helper/issues/538)).

## Stable signing keystore

TWA verification (Digital Asset Links) ties the app to a domain by its
**signing certificate SHA-256 fingerprint**. AGP's auto-generated debug
keystore produces a *different* fingerprint on every machine/CI runner, which
would silently break verification. To keep the fingerprint constant, a
keystore is committed at `keystore/nightpos-release.keystore` and wired into
**both** `debug` and `release` build types via a `signingConfigs` block in
`app/build.gradle.kts`.

| | |
|---|---|
| Path | `keystore/nightpos-release.keystore` |
| Alias | `nightpos` |
| Store / key password | `nightpos123` |
| SHA-256 fingerprint | `A6:E9:9E:33:88:DF:C2:05:C5:73:42:65:92:E0:39:F4:C1:9C:6B:12:69:A6:81:C3:C9:7D:66:93:67:26:04:FF` |

> Regenerating this keystore produces a *different* fingerprint and breaks
> Digital Asset Links verification — back it up and never delete it.

## Hosting `assetlinks.json`

Digital Asset Links requires a JSON file at
`https://soho.nightpos.com/.well-known/assetlinks.json` listing the app's
package name(s) and signing fingerprint. Both build variants
(`com.nightpos.app` for release, `com.nightpos.app.debug` for debug — note
the `applicationIdSuffix`) sign with the same keystore, so both are listed
with the same fingerprint:

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.nightpos.app",
      "sha256_cert_fingerprints": [
        "A6:E9:9E:33:88:DF:C2:05:C5:73:42:65:92:E0:39:F4:C1:9C:6B:12:69:A6:81:C3:C9:7D:66:93:67:26:04:FF"
      ]
    }
  },
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.nightpos.app.debug",
      "sha256_cert_fingerprints": [
        "A6:E9:9E:33:88:DF:C2:05:C5:73:42:65:92:E0:39:F4:C1:9C:6B:12:69:A6:81:C3:C9:7D:66:93:67:26:04:FF"
      ]
    }
  }
]
```

### Serving it via Nginx Proxy Manager

In NPM: open the **Proxy Host** for `soho.nightpos.com` → **Advanced** tab →
add a custom location that returns the JSON directly from Nginx (no backend
changes needed):

```nginx
location /.well-known/assetlinks.json {
    default_type application/json;
    return 200 '[{"relation":["delegate_permission/common.handle_all_urls"],"target":{"namespace":"android_app","package_name":"com.nightpos.app","sha256_cert_fingerprints":["A6:E9:9E:33:88:DF:C2:05:C5:73:42:65:92:E0:39:F4:C1:9C:6B:12:69:A6:81:C3:C9:7D:66:93:67:26:04:FF"]}},{"relation":["delegate_permission/common.handle_all_urls"],"target":{"namespace":"android_app","package_name":"com.nightpos.app.debug","sha256_cert_fingerprints":["A6:E9:9E:33:88:DF:C2:05:C5:73:42:65:92:E0:39:F4:C1:9C:6B:12:69:A6:81:C3:C9:7D:66:93:67:26:04:FF"]}}]';
}
```

Save (NPM reloads Nginx automatically), then verify by opening
`https://soho.nightpos.com/.well-known/assetlinks.json` in a browser — you
should see the JSON returned with `Content-Type: application/json`.

## Verifying on-device

1. Install the APK and confirm `assetlinks.json` is reachable at the URL above.
2. Tap "เปิดขาย" (or Reports / Customers).
3. **Verification succeeded** if the page opens full-screen with **no Chrome
   address bar / toolbar**. If a Chrome toolbar briefly appears at the top,
   verification didn't match — re-check the JSON content/URL, or clear
   Chrome's app storage on the device and retry (Chrome caches verification
   results).

## Known trade-offs

- **Separate cookie/session storage.** A TWA uses **Chrome's** cookie jar, not
  the in-app `android.webkit.WebView`'s. "ล้างข้อมูลแคชและคุกกี้" in Settings
  only clears the in-app WebView's session — it does **not** log the TWA out
  of Odoo. To log out of an Odoo session opened via TWA, log out from within
  the Odoo web UI, or clear Chrome's app storage from Android Settings.
- **Kiosk mode.** The existing in-app kiosk mode (hiding system bars,
  intercepting the back button in `WebViewScreen`) doesn't carry over to the
  TWA Activity — Chrome manages its own UI/back-stack while running a TWA.
