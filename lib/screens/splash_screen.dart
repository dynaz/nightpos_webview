import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../providers/app_state.dart';
import '../providers/settings_provider.dart';
import '../providers/login_provider.dart';
import '../theme/theme.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({Key? key}) : super(key: key);

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    _navigateToNextScreen();
  }

  Future<void> _navigateToNextScreen() async {
    if (!mounted) return;

    final appState = context.read<AppState>();
    final settingsProvider = context.read<SettingsProvider>();
    final loginProvider = context.read<LoginProvider>();

    // Try to restore previous session
    if (!appState.isLoggedIn) {
      final serverUrl = settingsProvider.serverUrl;
      final restored = await loginProvider.restoreSession(serverUrl);

      if (restored && mounted) {
        appState.setLoggedIn(
          true,
          username: loginProvider._state.successMessage ?? 'User',
          serverUrl: serverUrl,
        );
      }
    }

    if (!mounted) return;

    // Add a minimum splash screen duration for visual consistency
    await Future.delayed(const Duration(seconds: 1));

    if (!mounted) return;

    if (appState.isLoggedIn) {
      context.go('/dashboard');
    } else {
      context.go('/login');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NightPOSColors.nightBlack,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text(
              'NightPOS',
              style: TextStyle(
                color: NightPOSColors.neonPurple,
                fontSize: 48,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            const Text(
              'Soho',
              style: TextStyle(
                color: NightPOSColors.textPrimary,
                fontSize: 24,
              ),
            ),
            const SizedBox(height: 48),
            const CircularProgressIndicator(
              valueColor: AlwaysStoppedAnimation<Color>(
                NightPOSColors.neonPurple,
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'Starting up...',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
        ),
      ),
    );
  }
}
