import 'package:flutter/material.dart';

class AppState extends ChangeNotifier {
  bool _isLoggedIn = false;
  String? _username;
  String? _serverUrl;
  String? _sessionToken;

  // Getters
  bool get isLoggedIn => _isLoggedIn;
  String? get username => _username;
  String? get serverUrl => _serverUrl;
  String? get sessionToken => _sessionToken;

  // Setters
  void setLoggedIn(bool value, {String? username, String? serverUrl, String? sessionToken}) {
    _isLoggedIn = value;
    _username = username;
    _serverUrl = serverUrl;
    _sessionToken = sessionToken;
    notifyListeners();
  }

  void logout() {
    _isLoggedIn = false;
    _username = null;
    _serverUrl = null;
    _sessionToken = null;
    notifyListeners();
  }

  void updateServerUrl(String url) {
    _serverUrl = url;
    notifyListeners();
  }
}
