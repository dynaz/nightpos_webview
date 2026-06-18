# Flutter Setup & iOS Configuration Guide

## Phase 1: Flutter Project Setup & Apple Configuration

This guide covers the manual setup of the Flutter NightPOS iOS app and Apple Developer configuration.

### Prerequisites
- Flutter SDK 3.13.0+ installed and in PATH
- Xcode 14+ installed
- macOS 12+
- Apple Developer Account with Team ID

### Step 1: Verify Flutter Installation

```bash
flutter --version
flutter doctor -v
```

Ensure all checks pass (especially Xcode, CocoaPods, and iOS deployment target).

### Step 2: Get Flutter Dependencies

From the repository root:

```bash
flutter pub get
```

This installs all dependencies defined in `pubspec.yaml`.

### Step 3: Set Up iOS Deployment Target

Edit `ios/Podfile` and ensure deployment target is set to iOS 12.0+:

```ruby
post_install do |installer|
  installer.pods_project.targets.each do |target|
    flutter_additional_ios_build_settings(target)
    target.build_configurations.each do |config|
      config.build_settings['GCC_PREPROCESSOR_DEFINITIONS'] ||= [
        '$(inherited)',
        'FLUTTER_BUILD_NAME=\$(FLUTTER_BUILD_NAME)',
        'FLUTTER_BUILD_NUMBER=\$(FLUTTER_BUILD_NUMBER)',
      ]
    end
  end
end
```

### Step 4: Apple Developer Setup

#### 4.1 Create App ID on Apple Developer Portal

1. Go to https://developer.apple.com/account/resources/identifiers/add/bundleId
2. Select "App IDs"
3. Click "+" to create new App ID
4. Fill in:
   - **App Name:** NightPOS Soho iOS
   - **Bundle ID:** `com.nightpos.ios` (exact match required)
   - **Capabilities:** Push Notifications (optional for MVP)
5. Click "Continue" → "Register" → "Done"

#### 4.2 Create Signing Certificate

1. Go to https://developer.apple.com/account/resources/certificates/add
2. Select "iOS App Development" (for development) or "Apple Distribution" (for App Store)
3. Click "Continue"
4. Follow CSR (Certificate Signing Request) creation:
   - Open Keychain Access on your Mac
   - Keychain Access → Certificate Assistant → Request a Certificate from a Certificate Authority
   - Email: your Apple ID email
   - Common Name: Your Name
   - Request: "Saved to disk"
   - Save the `.certSigningRequest` file
5. Upload the `.certSigningRequest` file
6. Download the certificate (`.cer` file)
7. Double-click to import into Keychain

#### 4.3 Create Provisioning Profiles

1. Go to https://developer.apple.com/account/resources/profiles/add
2. Select "iOS App Development" or "App Store"
3. Select the App ID `com.nightpos.ios`
4. Select the certificate you just created
5. Select devices (for development only; skip for App Store)
6. Name: `NightPOS iOS Development` or `NightPOS iOS Distribution`
7. Download the `.mobileprovision` file
8. Double-click to install (auto-imports to Xcode)

### Step 5: Configure Xcode Project

Open the Xcode project:

```bash
open ios/Runner.xcworkspace
```

**Important:** Always use `Runner.xcworkspace`, not `Runner.xcodeproj`.

1. Select "Runner" in the left sidebar
2. Select "Runner" target
3. Go to "Signing & Capabilities" tab
4. **Signing:**
   - Team: Select your Apple Team ID
   - Bundle Identifier: `com.nightpos.ios`
   - Provisioning Profile: Auto-selected (should show "iOS Development" or "App Store")
   - Signing Certificate: Auto-selected

5. **Capabilities:**
   - Ensure no unsupported capabilities are enabled for iOS 12

6. **Build Settings:**
   - Deployment Target: 12.0 or later
   - Product Name: `NightPOS`

### Step 6: Test Local Build

Build for iOS simulator:

```bash
flutter build ios --debug
```

Or run directly:

```bash
flutter run
```

When prompted, select the iOS simulator you want to use.

**Expected output:**
- Build completes without errors
- App launches on simulator
- Splash screen shows "NightPOS Soho"
- App transitions to Login screen

### Step 7: Generate App Store Connect API Key (Optional for CI/CD)

For automated App Store submissions in future CI/CD:

1. Go to https://appstoreconnect.apple.com/access/api
2. Click "Keys"
3. Click "+" to generate new key
4. Fill in:
   - **Key Name:** NightPOS iOS Release
   - **Access:** Admin (needed for releases)
5. Click "Generate"
6. Download the API key (`.p8` file) — **save securely**
7. Note the **Issuer ID** and **Key ID**

Store these credentials securely (e.g., in CI/CD secrets, not in version control).

---

## Troubleshooting

### Build Fails with "SDK not found"
- Ensure Xcode command-line tools are installed:
  ```bash
  xcode-select --install
  ```

### CocoaPods Dependency Issues
- Clean and reinstall:
  ```bash
  rm -rf ios/Pods ios/Podfile.lock
  flutter clean
  flutter pub get
  cd ios && pod install && cd ..
  ```

### Provisioning Profile Mismatch
- In Xcode, select "Runner" target → "Signing & Capabilities"
- Check that Bundle ID matches `com.nightpos.ios`
- Re-download provisioning profile if needed

### Simulator Build Fails
- Try a different simulator or reset the current one:
  ```bash
  flutter clean
  flutter run -d all  # Lists available devices
  flutter run -d <device-id>
  ```

---

## Next Steps

Once Phase 1 is complete:
1. Verify app builds and runs on iOS simulator
2. Proceed to Phase 2: Core App Structure & Navigation
3. Implement placeholder screens (already created)
4. Move to Phase 3: Odoo Integration & Authentication

---

## References

- [Flutter iOS Setup](https://docs.flutter.dev/get-started/install/macos)
- [Apple Developer Account Setup](https://developer.apple.com/account)
- [App Store Connect Guide](https://appstoreconnect.apple.com)
- [Xcode Documentation](https://developer.apple.com/xcode/)
