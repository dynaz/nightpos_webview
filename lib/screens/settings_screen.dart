import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/theme.dart';
import '../providers/settings_provider.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({Key? key}) : super(key: key);

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
              ListTile(
                title: const Text('Server URL'),
                subtitle: Text(settings.serverUrl),
                onTap: () {
                  // TODO: Show input dialog for server URL in Phase 5
                },
              ),
              ListTile(
                title: const Text('Language'),
                subtitle: Text(settings.language == 'en' ? 'English' : 'ไทย (Thai)'),
                onTap: () {
                  // TODO: Show language selector in Phase 5
                },
              ),
              // POS Screen Section
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
                child: Text(
                  'POS Screen',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
              SwitchListTile(
                title: const Text('Kiosk Mode'),
                subtitle: const Text('Hide system bars and lock the screen'),
                value: settings.kioskMode,
                onChanged: (value) {
                  settings.setKioskMode(value);
                },
              ),
              SwitchListTile(
                title: const Text('Keep Screen On'),
                subtitle: const Text("Screen won't turn off while POS is open"),
                value: settings.keepScreenOn,
                onChanged: (value) {
                  settings.setKeepScreenOn(value);
                },
              ),
              // About Section
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
                child: Text(
                  'About',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
              ListTile(
                title: const Text('App Version'),
                subtitle: const Text('1.0.0'),
              ),
              ListTile(
                title: const Text('Diagnostics'),
                subtitle: const Text('View system info and logs'),
                onTap: () {
                  // TODO: Show diagnostics panel in Phase 5
                },
              ),
            ],
          );
        },
      ),
    );
  }
}
