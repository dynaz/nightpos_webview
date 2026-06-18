import 'package:flutter/services.dart';
import 'package:wakelock_plus/wakelock_plus.dart';

/// Platform-specific features for iOS
/// Mirrors Android KeepScreenOn / KioskSystemBars / applyImmersiveMode
class PlatformService {
  static bool _kioskModeActive = false;
  static bool _keepScreenOnActive = false;

  /// Enable/disable kiosk mode (hide system bars)
  /// iOS equivalent of Android's WindowInsetsController.hide(systemBars())
  static Future<void> setKioskMode(bool enabled) async {
    _kioskModeActive = enabled;
    if (enabled) {
      // Hide status bar and navigation indicators
      await SystemChrome.setEnabledSystemUIMode(
        SystemUiMode.immersiveSticky,
        overlays: [],
      );
      // Lock to landscape or portrait based on device
      // (optional — uncomment if needed for POS terminals)
      // await SystemChrome.setPreferredOrientations([
      //   DeviceOrientation.landscapeLeft,
      //   DeviceOrientation.landscapeRight,
      // ]);
    } else {
      // Restore system bars
      await SystemChrome.setEnabledSystemUIMode(
        SystemUiMode.edgeToEdge,
        overlays: SystemUiOverlay.values,
      );
      // Restore all orientations
      // await SystemChrome.setPreferredOrientations(DeviceOrientation.values);
    }
  }

  /// Enable/disable keep screen on
  /// iOS equivalent of Android's FLAG_KEEP_SCREEN_ON
  static Future<void> setKeepScreenOn(bool enabled) async {
    _keepScreenOnActive = enabled;
    if (enabled) {
      await WakelockPlus.enable();
    } else {
      await WakelockPlus.disable();
    }
  }

  /// Set status bar style (light/dark icons)
  static void setStatusBarStyle({bool light = true}) {
    SystemChrome.setSystemUIOverlayStyle(
      light
          ? SystemUiOverlayStyle.light.copyWith(
              statusBarColor: const Color(0x00000000),
            )
          : SystemUiOverlayStyle.dark.copyWith(
              statusBarColor: const Color(0x00000000),
            ),
    );
  }

  /// Restore all platform defaults (call on app dispose)
  static Future<void> restoreDefaults() async {
    if (_kioskModeActive) {
      await setKioskMode(false);
    }
    if (_keepScreenOnActive) {
      await setKeepScreenOn(false);
    }
  }

  /// Get current kiosk mode state
  static bool get isKioskModeActive => _kioskModeActive;

  /// Get current keep screen on state
  static bool get isKeepScreenOnActive => _keepScreenOnActive;
}
