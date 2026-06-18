import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'theme/theme.dart';
import 'providers/app_state.dart';
import 'providers/settings_provider.dart';
import 'providers/login_provider.dart';
import 'providers/webview_provider.dart';
import 'services/network_diagnostics.dart';
import 'services/localization_service.dart';
import 'services/platform_service.dart';
import 'app.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize settings
  final settingsProvider = SettingsProvider();
  await settingsProvider.init();

  // Initialize localization from saved language preference
  LocalizationService.setLanguage(settingsProvider.language);

  // Set initial status bar style
  PlatformService.setStatusBarStyle(light: true);

  runApp(
    MultiProvider(
      providers: [
        // App-level state
        ChangeNotifierProvider<SettingsProvider>.value(
          value: settingsProvider,
        ),
        ChangeNotifierProvider<AppState>(
          create: (_) => AppState(),
        ),
        ChangeNotifierProvider<NetworkDiagnostics>(
          create: (_) => NetworkDiagnostics(),
        ),
        // Screen-level state
        ChangeNotifierProvider<LoginProvider>(
          create: (_) => LoginProvider(),
        ),
        ChangeNotifierProvider<WebViewProvider>(
          create: (_) => WebViewProvider(),
        ),
      ],
      child: const NightPOSApp(),
    ),
  );
}
