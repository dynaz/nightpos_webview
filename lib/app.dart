import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'theme/theme.dart';
import 'screens/splash_screen.dart';
import 'screens/login_screen.dart';
import 'screens/dashboard_screen.dart';
import 'screens/webview_screen.dart';
import 'screens/settings_screen.dart';
import 'providers/app_state.dart';

class NightPOSApp extends StatelessWidget {
  const NightPOSApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'NightPOS Soho',
      theme: NightPOSTheme.lightTheme,
      debugShowCheckedModeBanner: false,
      routerConfig: _buildRouter(context),
    );
  }

  GoRouter _buildRouter(BuildContext context) {
    return GoRouter(
      initialLocation: '/splash',
      routes: [
        GoRoute(
          path: '/splash',
          builder: (context, state) => const SplashScreen(),
        ),
        GoRoute(
          path: '/login',
          builder: (context, state) => const LoginScreen(),
        ),
        GoRoute(
          path: '/dashboard',
          builder: (context, state) => const DashboardScreen(),
          routes: [
            GoRoute(
              path: 'webview',
              builder: (context, state) {
                final extra = state.extra as Map<String, dynamic>?;
                return WebViewScreen(
                  title: extra?['title'] ?? 'POS',
                  url: extra?['url'] ?? '',
                );
              },
            ),
            GoRoute(
              path: 'settings',
              builder: (context, state) => const SettingsScreen(),
            ),
          ],
        ),
      ],
      redirect: (context, state) {
        // Handle authentication redirect
        final appState = context.read<AppState>();
        final isLoggedIn = appState.isLoggedIn;
        final location = state.location;

        if (!isLoggedIn && location != '/splash' && location != '/login') {
          return '/login';
        }

        if (isLoggedIn && (location == '/login' || location == '/splash')) {
          return '/dashboard';
        }

        return null;
      },
    );
  }
}
