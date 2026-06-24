# NightPOS Android App

Android WebView app that renders the Odoo 19 POS UI on Sunmi hardware, with a
built-in bridge to the Sunmi internal printer.

---

## Supported Devices

| Device | Android | CPU ABI | WebView Engine | Paper Width |
|--------|---------|---------|----------------|-------------|
| Sunmi T1 | 6.0.1 (API 23) | armeabi-v7a | GeckoView 142 | 80 mm |
| Sunmi T2 | 7.1.2 (API 25) | armeabi-v7a | GeckoView 142 | 80 mm |
| Sunmi D2s Lite | 11.0 (API 30) | arm64-v8a | System WebView | 58 mm |
| Sunmi D2s | 11.0 (API 30) | arm64-v8a | System WebView | 58 mm |
| Sunmi D2s Plus | 11.0 (API 30) | arm64-v8a | System WebView | 58 mm |

Device model and Android version are detected at runtime via `Build.MODEL` and
`Build.VERSION.SDK_INT` — one APK supports all devices.

---

## Branch Strategy

```
main          ← stable, production-ready for all devices
dev           ← integration target for feature branches
feature/xxx   ← short-lived, merged to dev then deleted
fix/xxx       ← short-lived bug fix, merged to dev then deleted
```

### Rules

- **Never commit directly to `main`** — always go through a PR from `dev`
- **`dev` is the PR target** for all feature and fix branches
- **Feature branches are deleted after merge** — no long-lived per-device branches
- **Device differences live in code**, not in branches (see `DeviceProfile`)
- **Releases are tagged on `main`** — e.g. `v2.1.0`

### Workflow

```
feature/sunmi-paper-width
        │
        ▼ PR → merge
       dev
        │
        ▼ PR → merge (after QA on real device)
       main
        │
        ▼ tag
      v2.1.0
```

### Why not one branch per device?

With 5 device models × 3 Android versions, per-device branches mean:
- Every bug fix must be cherry-picked to 5+ branches
- Branches slowly diverge and conflicts pile up
- No single source of truth for what is actually running in production

Runtime detection is the correct solution for Android device fragmentation.

---

## Runtime Device Detection

All device and Android version differences are resolved in `DeviceProfile`:

```kotlin
object DeviceProfile {
    val isSunmi = Build.MANUFACTURER.equals("SUNMI", ignoreCase = true)
    val model   = Build.MODEL.uppercase()          // "T1", "D2S", "D2S LITE" …
    val sdkInt  = Build.VERSION.SDK_INT            // 23=Android6, 25=7, 30=11

    // GeckoView required on Android 6/7 — system WebView is too old for Odoo 19
    val needsGeckoView = sdkInt < Build.VERSION_CODES.P   // < API 28

    // Default paper width per model family; user can override in app Settings
    val defaultPaperWidthMm = if (model.startsWith("T")) 80 else 58
}
```

---

## Printer Bridge Architecture

```
Odoo POS (HTTPS page)
        │
        │  window.flutter_inappwebview.callHandler("SunmiPrinter", {...})
        │
        ├── GeckoView mode (Android 6/7)
        │       window.prompt("nightpos:SunmiPrinter", argsJson)
        │       └── geckoPromptDelegate → SunmiJsBridge.handleSunmi()
        │
        └── HTTP bridge mode (Android 11 / Custom Tabs)
                fetch("http://localhost:8585/print")
                └── PrintHttpServer.dispatch() → SunmiPrinterConnection
```

`window.__nightpos_device` is injected synchronously at `document_start` by the
GeckoView polyfill extension (`content.js`) so the Odoo addon knows the device
type and paper width before any page JS runs.

---

## Paper Width

Users select 58 mm or 80 mm in the app Settings. The choice is stored in
`PreferencesManager.printerPaperWidthMm` (Jetpack DataStore).

The Odoo addon reads it in priority order:
1. Printer record setting (per-printer override in Odoo backend)
2. `window.__nightpos_device.paperWidth` (injected by this app)
3. Shop-level config (`nightpos_sunmi_paper` field on `pos.config`)

---

## Building

```bash
./gradlew assembleRelease
```

Signed APK output: `app/build/outputs/apk/release/app-release.apk`

Keystore config is in `keystore/` — credentials in `keystore.properties` (not
committed, must be present locally or in CI secrets).

---

## Changelog

### 2.0.10 (versionCode 15)
- Inject `window.__nightpos_device` (type, paperWidth) at `document_start` via
  GeckoView polyfill extension so Odoo detects Sunmi and paper size automatically
- `SunmiJsBridge`: add `getPaperWidth` bridge method reading `PreferencesManager`
- `PrintHttpServer`: add `getPaperWidth` to HTTP dispatch; include `paperWidth`
  in `/ping` response; accept `context` parameter for DataStore access
- Bump polyfill extension to v1.5 to force GeckoView startup cache refresh

### 2.0.x
- GeckoView login screen, D2s Plus support, Android 15 compatibility

### 1.0.0
- Initial GeckoView 142 integration for Sunmi D2s (Android 7.1.2)
- Built-in polyfill extension (`Promise.withResolvers`, `structuredClone`)
- Local HTTP printer bridge via NanoHTTPD on port 8585
- Mixed-content GeckoView prefs allowing HTTPS → `http://localhost`
- `onTrimMemory` cache clearing to avoid OOM on 2 GB RAM hardware
