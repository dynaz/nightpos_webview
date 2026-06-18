import 'package:flutter/material.dart';
import '../models/login_state.dart';

/// Manages login form state and authentication
class LoginProvider extends ChangeNotifier {
  LoginState _state = LoginState();

  LoginState get state => _state;

  bool get isLoading => _state.isLoading;
  String? get error => _state.error;
  String? get successMessage => _state.successMessage;

  /// Start login process
  void setLoading(bool loading) {
    _state = _state.copyWith(isLoading: loading);
    notifyListeners();
  }

  /// Set an error message
  void setError(String? error) {
    _state = _state.copyWith(error: error);
    notifyListeners();
  }

  /// Set a success message
  void setSuccess(String? message) {
    _state = _state.copyWith(successMessage: message);
    notifyListeners();
  }

  /// Clear error and success messages
  void clearMessages() {
    _state = _state.clearMessages();
    notifyListeners();
  }

  /// Attempt login (placeholder for Phase 3)
  Future<bool> login({
    required String serverUrl,
    required String username,
    required String password,
  }) async {
    setLoading(true);
    clearMessages();

    try {
      // TODO: Implement actual Odoo authentication in Phase 3
      // - POST to /web/session/authenticate
      // - Handle session cookies
      // - Validate credentials

      await Future.delayed(const Duration(seconds: 1)); // Simulate network delay

      setSuccess('Login successful');
      setLoading(false);
      return true;
    } catch (e) {
      setError('Login failed: ${e.toString()}');
      setLoading(false);
      return false;
    }
  }

  /// Reset to initial state
  void reset() {
    _state = LoginState();
    notifyListeners();
  }
}
