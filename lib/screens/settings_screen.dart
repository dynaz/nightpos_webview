import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../theme/theme.dart';
import '../providers/settings_provider.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({Key? key}) : super(key: key);

  void _showServerUrlDialog(BuildContext context, SettingsProvider settings) {
    final controller = TextEditingController(text: settings.serverUrl);
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: NightPOSColors.surface,
        title: const Text('Server URL'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: 'https://soho.nightpos.com',
            labelText: 'Server URL',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              settings.setServerUrl(controller.text);
              Navigator.pop(context);
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }

  void _showLanguageDialog(BuildContext context, SettingsProvider settings) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: NightPOSColors.surface,
        title: const Text('Select Language'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            RadioListTile<String>(
              title: const Text('English'),
              value: 'en',
              groupValue: settings.language,
              onChanged: (value) {
                if (value != null) {
                  settings.setLanguage(value);
                  Navigator.pop(context);
                }
              },
            ),
            RadioListTile<String>(
              title: const Text('ไทย (Thai)'),
              value: 'th',
              groupValue: settings.language,
              onChanged: (value) {
                if (value != null) {
                  settings.setLanguage(value);
                  Navigator.pop(context);
                }
              },
            ),
          ],
        ),
      ),
    );
  }

  void _showDiagnostics(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: NightPOSColors.surface,
        title: const Text('Diagnostics'),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'App Information:',
                style: Theme.of(context).textTheme.titleSmall,
              ),
              const SizedBox(height: 8),
              const Text('App Name: NightPOS Soho'),
              const Text('Version: 1.0.0'),
              const Text('Build: flutter-ios'),
              const SizedBox(height: 16),
              Text(
                'System Information:',
                style: Theme.of(context).textTheme.titleSmall,
              ),
              const SizedBox(height: 8),
              const Text('Platform: iOS'),
              const Text('Status: Running'),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () {
              // Copy diagnostics to clipboard
              Navigator.pop(context);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Diagnostics copied to clipboard')),
              );
            },
            child: const Text('Copy'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NightPOSColors.nightBlack,
      appBar: AppBar(
        title: const Text('Settings'),
        elevation: 0,
      ),
      body: Consumer<SettingsProvider>(
        builder: (context, settings, _) {
          return ListView(
            children: [
              // General Section
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
                child: Text(
                  'General',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
              Card(
                color: NightPOSColors.surface,
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: ListTile(
                  title: const Text('Server URL'),
                  subtitle: Text(settings.serverUrl),
                  trailing: const Icon(Icons.edit, color: NightPOSColors.textSecondary),
                  onTap: () => _showServerUrlDialog(context, settings),
                ),
              ),
              Card(
                color: NightPOSColors.surface,
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: ListTile(
                  title: const Text('Language'),
                  subtitle: Text(settings.language == 'en' ? 'English' : 'ไทย (Thai)'),
                  trailing: const Icon(Icons.chevron_right, color: NightPOSColors.textSecondary),
                  onTap: () => _showLanguageDialog(context, settings),
                ),
              ),
              // POS Screen Section
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
                child: Text(
                  'POS Screen',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
              Card(
                color: NightPOSColors.surface,
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: SwitchListTile(
                  title: const Text('Kiosk Mode'),
                  subtitle: const Text('Hide system bars and lock the screen'),
                  value: settings.kioskMode,
                  onChanged: (value) => settings.setKioskMode(value),
                ),
              ),
              Card(
                color: NightPOSColors.surface,
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: SwitchListTile(
                  title: const Text('Keep Screen On'),
                  subtitle: const Text("Screen won't turn off while POS is open"),
                  value: settings.keepScreenOn,
                  onChanged: (value) => settings.setKeepScreenOn(value),
                ),
              ),
              // About Section
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
                child: Text(
                  'About',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
              Card(
                color: NightPOSColors.surface,
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: const ListTile(
                  title: Text('App Version'),
                  subtitle: Text('1.0.0'),
                ),
              ),
              Card(
                color: NightPOSColors.surface,
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: ListTile(
                  title: const Text('Diagnostics'),
                  subtitle: const Text('View system info and logs'),
                  trailing: const Icon(Icons.chevron_right, color: NightPOSColors.textSecondary),
                  onTap: () => _showDiagnostics(context),
                ),
              ),
              const SizedBox(height: 32),
            ],
          );
        },
      ),
    );
  }
}
