import '../l10n/app_en.dart';
import '../l10n/app_th.dart';

/// Localization service for Thai/English language switching
/// Mirrors Android LanguageSwitcher pattern
class LocalizationService {
  static String _currentLanguage = 'en';
  static Map<String, String> _currentStrings = enStrings;

  /// Set the current language
  static void setLanguage(String language) {
    _currentLanguage = language;
    _currentStrings = language == 'th' ? thStrings : enStrings;
  }

  /// Get current language code
  static String get currentLanguage => _currentLanguage;

  /// Get a localized string by key
  static String tr(String key, [String? arg]) {
    final value = _currentStrings[key] ?? enStrings[key] ?? key;
    if (arg != null) {
      return value.replaceAll('%s', arg);
    }
    return value;
  }

  /// Get available languages
  static List<({String code, String name})> get availableLanguages => [
        (code: 'en', name: 'English'),
        (code: 'th', name: 'ไทย (Thai)'),
      ];
}
