import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SettingsProvider extends ChangeNotifier {
  late SharedPreferences _prefs;

  // Settings keys
  static const String _keyServerUrl = 'server_url';
  static const String _keyLanguage = 'language';
  static const String _keyKioskMode = 'kiosk_mode';
  static const String _keyKeepScreenOn = 'keep_screen_on';
  static const String _keyAutoReopenPos = 'auto_reopen_pos';

  // Default values
  String _serverUrl = 'https://soho.nightpos.com';
  String _language = 'en'; // 'en' or 'th'
  bool _kioskMode = false;
  bool _keepScreenOn = true;
  bool _autoReopenPos = false;

  // Getters
  String get serverUrl => _serverUrl;
  String get language => _language;
  bool get kioskMode => _kioskMode;
  bool get keepScreenOn => _keepScreenOn;
  bool get autoReopenPos => _autoReopenPos;

  Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
    _loadSettings();
  }

  void _loadSettings() {
    _serverUrl = _prefs.getString(_keyServerUrl) ?? _serverUrl;
    _language = _prefs.getString(_keyLanguage) ?? _language;
    _kioskMode = _prefs.getBool(_keyKioskMode) ?? _kioskMode;
    _keepScreenOn = _prefs.getBool(_keyKeepScreenOn) ?? _keepScreenOn;
    _autoReopenPos = _prefs.getBool(_keyAutoReopenPos) ?? _autoReopenPos;
  }

  Future<void> setServerUrl(String url) async {
    _serverUrl = url;
    await _prefs.setString(_keyServerUrl, url);
    notifyListeners();
  }

  Future<void> setLanguage(String lang) async {
    _language = lang;
    await _prefs.setString(_keyLanguage, lang);
    notifyListeners();
  }

  Future<void> setKioskMode(bool enabled) async {
    _kioskMode = enabled;
    await _prefs.setBool(_keyKioskMode, enabled);
    notifyListeners();
  }

  Future<void> setKeepScreenOn(bool enabled) async {
    _keepScreenOn = enabled;
    await _prefs.setBool(_keyKeepScreenOn, enabled);
    notifyListeners();
  }

  Future<void> setAutoReopenPos(bool enabled) async {
    _autoReopenPos = enabled;
    await _prefs.setBool(_keyAutoReopenPos, enabled);
    notifyListeners();
  }
}
