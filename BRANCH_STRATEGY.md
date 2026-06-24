# Branch & Build Strategy

## The Problem

NightPOS runs on multiple Sunmi device families with different hardware
and Android versions:

| Family | Devices | Android | ABI | WebView |
|--------|---------|---------|-----|---------|
| Desktop | T1, T2 | 6.0 / 7.1 (API 23/25) | armeabi-v7a (32-bit) | System Chrome too old → **GeckoView** |
| Handheld | D2s Lite, D2s, D2s Plus | 11.0 (API 30) | arm64-v8a (64-bit) | System WebView (Chrome) is fine |

These two families need **different APKs** — different CPU ABI and different
WebView engine. The solution is Android **product flavors**, not Git branches.

---

## Git Branch Design

```
main          ← stable, production-ready for all devices
 └── dev      ← integration target; all PRs merge here first
      ├── feature/xxx   short-lived, deleted after merge
      └── fix/xxx       short-lived, deleted after merge
```

### Rules

- **`main` is always releasable** — CI must pass before any merge
- **`dev` is the PR target** — never commit directly to `main` or `dev`
- **Feature branches are deleted after merge** — no long-lived per-device branches
- **Device differences live in code or build config**, not branches
- **Releases are tagged on `main`** — one tag per version (e.g. `v2.1.0`)

### Workflow

```
feature/sunmi-paper-width
        │
        ▼  PR → code review → merge
       dev  ←─── QA on real T2 + D2s
        │
        ▼  PR → merge when stable
       main
        │
        ▼  tag
    v2.1.0
        │
        ├── app-legacy-release.apk   → install on T1, T2
        └── app-modern-release.apk   → install on D2s Lite, D2s, D2s Plus
```

### Why not one branch per device?

With 5 device models × 3 Android versions that is 15 potential branches.
Every bug fix would need to be cherry-picked to all of them. Branches
slowly diverge, conflicts pile up, and there is no single source of
truth for what is running in production. **Runtime detection is the
correct solution for Android device fragmentation.**

---

## Android Build Flavors

Two flavors produce two APKs from one branch:

```
flavorDimensions: "target"

legacy  → minSdk 21, armeabi-v7a, GeckoView 142, USE_GECKO=true
modern  → minSdk 26, arm64-v8a,   SystemWebView,  USE_GECKO=false
```

Build commands:

```bash
./gradlew assembleLegacyRelease   # → app-legacy-release.apk   (T1, T2)
./gradlew assembleModernRelease   # → app-modern-release.apk   (D2s family)
./gradlew assembleRelease         # → both APKs at once
```

---

## Source Layout

```
app/src/
├── main/                       ← shared code (90%+ of the app)
│   ├── java/com/nightpos/app/
│   │   ├── NightPOSApplication.kt
│   │   ├── MainActivity.kt
│   │   ├── data/PreferencesManager.kt
│   │   └── print/
│   │       ├── SunmiJsBridge.kt
│   │       ├── SunmiPrinterConnection.kt
│   │       └── PrintHttpServer.kt
│   └── assets/extensions/polyfill/
│       ├── content.js          ← injects window.__nightpos_device at document_start
│       └── manifest.json
│
├── legacy/                     ← T1, T2 only (GeckoView)
│   └── java/com/nightpos/app/webview/
│       └── WebViewFactory.kt   ← returns GeckoView + geckoPromptDelegate
│
└── modern/                     ← D2s family only (system WebView)
    └── java/com/nightpos/app/webview/
        └── WebViewFactory.kt   ← returns WebView + @JavascriptInterface
```

Both flavors expose the same `WebViewFactory.create()` signature. Shared
code in `main/` calls it without knowing which engine is running.

---

## Runtime Device Detection

Within each APK, remaining device differences are handled at runtime:

```kotlin
object DeviceProfile {
    val isSunmi = Build.MANUFACTURER.equals("SUNMI", ignoreCase = true)
    val model   = Build.MODEL.uppercase()       // "T1", "D2S", "D2S LITE", …
    val sdkInt  = Build.VERSION.SDK_INT         // 23=Android 6, 25=7, 30=11

    // Default paper width by model family; user can override in Settings
    val defaultPaperWidthMm: Int
        get() = if (model.startsWith("T")) 80 else 58
}
```

Paper width priority (from highest to lowest):

1. User selection in app Settings (`PreferencesManager.printerPaperWidthMm`)
2. `window.__nightpos_device.paperWidth` (read by Odoo addon)
3. Printer record setting in Odoo backend
4. Shop-level config (`nightpos_sunmi_paper` field on `pos.config`)

---

## Printer Bridge Architecture

```
Odoo POS (HTTPS page)
        │
        │  window.flutter_inappwebview.callHandler("SunmiPrinter", {...})
        │
        ├── legacy (GeckoView)
        │   window.prompt("nightpos:SunmiPrinter", argsJson)
        │   └── geckoPromptDelegate → SunmiJsBridge.handleSunmi()
        │
        └── modern (system WebView)
            fetch("http://localhost:8585/print")         ← Chrome allows localhost
            └── PrintHttpServer.dispatch()
            OR
            window.NightPOSBridge.callHandler(...)       ← @JavascriptInterface
            └── SunmiJsBridge.callHandler()
```

`window.__nightpos_device` is injected:
- **legacy**: by `content.js` at `document_start` via GeckoView extension
- **modern**: by `WebViewFactory` via `evaluateJavascript()` in `onPageStarted`

---

## Releasing

Tag `main` after merging and QA:

```bash
git tag v2.1.0
git push origin v2.1.0
```

Attach both APKs to the GitHub release as assets:
- `app-legacy-release.apk` — label: **T1 / T2 (Android 6/7)**
- `app-modern-release.apk` — label: **D2s Lite / D2s / D2s Plus (Android 11)**

---

## Adding a New Device

1. Identify which flavor it fits (`legacy` = 32-bit Android ≤7, `modern` = 64-bit Android ≥8)
2. Test with the existing APK for that flavor
3. If behavior differs, add a `Build.MODEL` check in `DeviceProfile` — **no new branch**
4. If it needs a new ABI or minSdk floor, adjust the flavor in `build.gradle.kts`
5. Update the device table in `README.md`
