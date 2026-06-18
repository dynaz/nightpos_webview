import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../theme/theme.dart';
import '../providers/login_provider.dart';
import '../providers/settings_provider.dart';
import '../providers/app_state.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({Key? key}) : super(key: key);

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  late TextEditingController _serverUrlController;
  late TextEditingController _usernameController;
  late TextEditingController _passwordController;

  @override
  void initState() {
    super.initState();
    final settingsProvider = context.read<SettingsProvider>();
    _serverUrlController = TextEditingController(text: settingsProvider.serverUrl);
    _usernameController = TextEditingController();
    _passwordController = TextEditingController();
  }

  @override
  void dispose() {
    _serverUrlController.dispose();
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    final loginProvider = context.read<LoginProvider>();
    final settingsProvider = context.read<SettingsProvider>();
    final appState = context.read<AppState>();

    final success = await loginProvider.login(
      serverUrl: _serverUrlController.text,
      username: _usernameController.text,
      password: _passwordController.text,
    );

    if (success && mounted) {
      // Save server URL to settings
      await settingsProvider.setServerUrl(_serverUrlController.text);

      // Update app state
      appState.setLoggedIn(
        true,
        username: _usernameController.text,
        serverUrl: _serverUrlController.text,
      );

      // Navigate to dashboard
      context.go('/dashboard');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NightPOSColors.nightBlack,
      body: SafeArea(
        child: Consumer<LoginProvider>(
          builder: (context, loginProvider, _) {
            return SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SizedBox(height: 48),
                  // Header
                  Center(
                    child: Column(
                      children: const [
                        Text(
                          'NightPOS',
                          style: TextStyle(
                            color: NightPOSColors.neonPurple,
                            fontSize: 40,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        SizedBox(height: 8),
                        Text(
                          'Soho',
                          style: TextStyle(
                            color: NightPOSColors.textPrimary,
                            fontSize: 18,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 48),
                  // Title
                  Text(
                    'Log In',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  const SizedBox(height: 24),
                  // Error message
                  if (loginProvider.error != null)
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: NightPOSColors.errorRed.withOpacity(0.1),
                        border: Border.all(color: NightPOSColors.errorRed),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.error_outline,
                            color: NightPOSColors.errorRed,
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Text(
                              loginProvider.error!,
                              style: const TextStyle(color: NightPOSColors.errorRed),
                            ),
                          ),
                        ],
                      ),
                    ),
                  if (loginProvider.error != null) const SizedBox(height: 16),
                  // Form fields
                  TextField(
                    controller: _serverUrlController,
                    enabled: !loginProvider.isLoading,
                    decoration: const InputDecoration(
                      labelText: 'Server URL',
                      hintText: 'https://soho.nightpos.com',
                      prefixIcon: Icon(Icons.language),
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _usernameController,
                    enabled: !loginProvider.isLoading,
                    decoration: const InputDecoration(
                      labelText: 'Username',
                      prefixIcon: Icon(Icons.person),
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _passwordController,
                    enabled: !loginProvider.isLoading,
                    obscureText: true,
                    decoration: const InputDecoration(
                      labelText: 'Password',
                      prefixIcon: Icon(Icons.lock),
                    ),
                  ),
                  const SizedBox(height: 32),
                  // Login button
                  ElevatedButton(
                    onPressed: loginProvider.isLoading ? null : _handleLogin,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      child: loginProvider.isLoading
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                              ),
                            )
                          : const Text('Log In'),
                    ),
                  ),
                  const SizedBox(height: 16),
                  // Info message
                  Center(
                    child: Text(
                      'Contact support for account access',
                      style: Theme.of(context).textTheme.bodySmall,
                      textAlign: TextAlign.center,
                    ),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }
}
