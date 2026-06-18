import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../theme/theme.dart';
import '../providers/settings_provider.dart';
import '../services/localization_service.dart';
import '../services/platform_service.dart';
import '../services/network_diagnostics.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({Key? key}) : super(key: key);

  void _showServerUrlDialog(BuildContext context, SettingsProvider settings) {
    final controller = TextEditingController(text: settings.serverUrl);
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: NightPOSColors.surface,
        title: Text(LocalizationService.tr('settings_server_url')),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: 'https://soho.nightpos.com',
            labelText: 'Server URL',
          ),
          keyboardType: TextInputType.url,
          autocorrect: false,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(LocalizationService.tr('logout_cancel')),
          ),
          TextButton(
            onPressed: () {
              settings.setServerUrl(controller.text.trim());
              Navigator.pop(context);
            },
            child: Text(LocalizationService.tr('action_save')),
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
        title: Text(LocalizationService.tr('language_dialog_title')),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: LocalizationService.availableLanguages.map((lang) {
            return RadioListTile<String>(
              title: Text(lang.name),
              value: lang.code,
              groupValue: settings.language,
              activeColor: NightPOSColors.neonPurple,
              onChanged: (value) {
                if (value != null) {
                  settings.setLanguage(value);
                  LocalizationService.setLanguage(value);
                  Navigator.pop(context);
                }
              },
            );
          }).toList(),
        ),
      ),
    );
  }

  void _showClearDataDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: NightPOSColors.surface,
        title: Text(LocalizationService.tr('settings_clear_data')),
        content: Text(LocalizationService.tr('settings_clear_data_desc')),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(LocalizationService.tr('logout_cancel')),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text(LocalizationService.tr('settings_clear_data_success'))),
              );
            },
            child: Text(
              LocalizationService.tr('logout_confirm'),
              style: const TextStyle(color: NightPOSColors.errorRed),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _showDiagnostics(BuildContext context) async {
    final networkDiagnostics = context.read<NetworkDiagnostics>();
    final diagnosticsInfo = await networkDiagnostics.getDiagnosticsInfo();

    final fullInfo = '''
App Information:
- App Name: NightPOS Soho
- Version: 1.0.0+1
- Platform: iOS (Flutter)
- WebView Engine: WKWebView

$diagnosticsInfo''';

    if (!context.mounted) return;

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: NightPOSColors.surface,
        title: Text(LocalizationService.tr('diagnostics_title')),
        content: SingleChildScrollView(
          child: SelectableText(
            fullInfo,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  fontFamily: 'monospace',
                  height: 1.5,
                ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () {
              Clipboard.setData(ClipboardData(text: fullInfo));
              Navigator.pop(context);
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text(LocalizationService.tr('diagnostics_copied'))),
              );
            },
            child: Text(LocalizationService.tr('diagnostics_copy')),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(LocalizationService.tr('action_close')),
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
        title: Text(LocalizationService.tr('settings_title')),
        elevation: 0,
      ),
      body: Consumer<SettingsProvider>(
        builder: (context, settings, _) {
          return ListView(
            children: [
              // General Section
              _buildSectionHeader(context, LocalizationService.tr('settings_section_general')),
              _buildCard(
                child: ListTile(
                  title: Text(LocalizationService.tr('settings_server_url')),
                  subtitle: Text(
                    settings.serverUrl,
                    style: const TextStyle(color: NightPOSColors.textSecondary),
                  ),
                  trailing: const Icon(Icons.edit, color: NightPOSColors.textSecondary),
                  onTap: () => _showServerUrlDialog(context, settings),
                ),
              ),
              _buildCard(
                child: ListTile(
                  title: Text(LocalizationService.tr('settings_language')),
                  subtitle: Text(
                    settings.language == 'en' ? 'English' : 'ไทย (Thai)',
                    style: const TextStyle(color: NightPOSColors.textSecondary),
                  ),
                  trailing: const Icon(Icons.chevron_right, color: NightPOSColors.textSecondary),
                  onTap: () => _showLanguageDialog(context, settings),
                ),
              ),

              // POS Screen Section
              _buildSectionHeader(context, LocalizationService.tr('settings_section_pos')),
              _buildCard(
                child: SwitchListTile(
                  title: Text(LocalizationService.tr('settings_kiosk_mode')),
                  subtitle: Text(
                    LocalizationService.tr('settings_kiosk_mode_desc'),
                    style: const TextStyle(color: NightPOSColors.textSecondary),
                  ),
                  value: settings.kioskMode,
                  activeColor: NightPOSColors.neonPurple,
                  onChanged: (value) {
                    settings.setKioskMode(value);
                    PlatformService.setKioskMode(value);
                  },
                ),
              ),
              _buildCard(
                child: SwitchListTile(
                  title: Text(LocalizationService.tr('settings_keep_screen_on')),
                  subtitle: Text(
                    LocalizationService.tr('settings_keep_screen_on_desc'),
                    style: const TextStyle(color: NightPOSColors.textSecondary),
                  ),
                  value: settings.keepScreenOn,
                  activeColor: NightPOSColors.neonPurple,
                  onChanged: (value) {
                    settings.setKeepScreenOn(value);
                    PlatformService.setKeepScreenOn(value);
                  },
                ),
              ),
              _buildCard(
                child: ListTile(
                  title: Text(LocalizationService.tr('settings_clear_data')),
                  subtitle: Text(
                    LocalizationService.tr('settings_clear_data_desc'),
                    style: const TextStyle(color: NightPOSColors.textSecondary),
                  ),
                  trailing: const Icon(Icons.chevron_right, color: NightPOSColors.textSecondary),
                  onTap: () => _showClearDataDialog(context),
                ),
              ),

              // About Section
              _buildSectionHeader(context, LocalizationService.tr('settings_section_about')),
              _buildCard(
                child: ListTile(
                  title: Text(LocalizationService.tr('settings_version')),
                  subtitle: const Text(
                    '1.0.0+1',
                    style: TextStyle(color: NightPOSColors.textSecondary),
                  ),
                ),
              ),
              _buildCard(
                child: ListTile(
                  title: Text(LocalizationService.tr('diagnostics_title')),
                  subtitle: Text(
                    LocalizationService.tr('diagnostics_copy'),
                    style: const TextStyle(color: NightPOSColors.textSecondary),
                  ),
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

  Widget _buildSectionHeader(BuildContext context, String title) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
      child: Text(
        title,
        style: Theme.of(context).textTheme.titleMedium?.copyWith(
              color: NightPOSColors.neonPurple,
            ),
      ),
    );
  }

  Widget _buildCard({required Widget child}) {
    return Card(
      color: NightPOSColors.surface,
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: child,
    );
  }
}
