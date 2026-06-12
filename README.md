# NightPOS Soho — Sunmi D2s (GeckoView)

Android POS app for Sunmi D2s terminals (Android 7.1.2, armeabi-v7a) that renders
the Odoo 19 POS UI inside an embedded GeckoView 142, with a local HTTP bridge to
the Sunmi internal printer.

## Changelog

All notable changes to this app are documented here. Versions follow
`versionName` / `versionCode` in `app/build.gradle.kts`.

### 1.0.0 (versionCode 1)

- Switch the rendering engine to GeckoView 142 (armeabi-v7a) for full Odoo 19
  POS support on Sunmi D2s (Android 7.1.2).
- Singleton `GeckoRuntime`, JavaScript enabled, Private Browsing and Tracking
  Protection disabled, hardware acceleration enabled at the application level.
- Built-in polyfill extension (`Promise.withResolvers`, `structuredClone`)
  required by Odoo 19, installed at startup with a startup-cache invalidation
  check so updated polyfills always load.
- `onTrimMemory` cache clearing — when Android signals memory pressure, drop
  GeckoView's network/image caches (`StorageController.ClearFlags.ALL_CACHES`)
  while preserving cookies, localStorage, IndexedDB and the active session, to
  avoid OOM kills/throttling on 2GB-RAM POS hardware.
- Explicit `dom.serviceWorkers.enabled` / `dom.indexedDB.enabled` prefs to keep
  Odoo 19's PWA offline order queue working regardless of upstream Gecko
  defaults.
- Local HTTP printer bridge (NanoHTTPD) for the Sunmi internal printer, with a
  JS bridge exposed to the POS web app, plus mixed-content prefs allowing
  HTTPS pages to call the `localhost` bridge.
- Domain allowlist for navigation, restricting the WebView to trusted
  Odoo backend/POS destinations.
