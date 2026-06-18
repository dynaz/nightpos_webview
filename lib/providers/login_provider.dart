import 'package:flutter/material.dart';
import '../models/login_state.dart';
import '../services/odoo_client.dart';
import '../services/secure_storage.dart';

/// Manages login form state and authentication
class LoginProvider extends ChangeNotifier {
  LoginState _state = LoginState();
  late final OdooClient _odooClient;
  late final SecureStorage _secureStorage;

  String? _sessionId;
  int? _userId;

  LoginState get state => _state;

  bool get isLoading => _state.isLoading;
  String? get error => _state.error;
  String? get successMessage => _state.successMessage;
  String? get sessionId => _sessionId;
  int? get userId => _userId;

  LoginProvider() {
    _secureStorage = SecureStorage();
  }

  /// Initialize OdooClient with server URL
  void initializeOdooClient(String serverUrl) {
    _odooClient = OdooClient(serverUrl: serverUrl);
  }

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

  /// Attempt login with real Odoo authentication
  Future<bool> login({
    required String serverUrl,
    required String username,
    required String password,
  }) async {
    setLoading(true);
    clearMessages();

    try {
      // Initialize Odoo client if not already done
      if (_odooClient == null) {
        initializeOdooClient(serverUrl);
      }

      // Verify server connectivity first
      final isConnected = await _odooClient.verifyServerConnectivity();
      if (!isConnected) {
        setError('Cannot reach server. Check your server URL and connection.');
        setLoading(false);
        return false;
      }

      // Attempt authentication
      final authResponse = await _odooClient.authenticate(
        username: username,
        password: password,
      );

      if (!authResponse.success) {
        setError(authResponse.errorMessage ?? 'Login failed');
        setLoading(false);
        return false;
      }

      // Store session securely
      _sessionId = authResponse.sessionCookie;
      _userId = authResponse.userId;

      await _secureStorage.saveSession(
        sessionId: authResponse.sessionCookie,
        userId: authResponse.userId,
        username: authResponse.username,
      );

      setSuccess('Login successful');
      setLoading(false);
      return true;
    } catch (e) {
      setError('Login failed: ${e.toString()}');
      setLoading(false);
      return false;
    }
  }

  /// Try to restore session from secure storage
  Future<bool> restoreSession(String serverUrl) async {
    try {
      initializeOdooClient(serverUrl);

      final session = await _secureStorage.loadSession();

      if (session.sessionId == null) {
        return false;
      }

      // Verify session is still valid
      final sessionInfo = await _odooClient.getSessionInfo();

      if (sessionInfo == null) {
        await _secureStorage.clearAll();
        return false;
      }

      _sessionId = session.sessionId;
      _userId = session.userId;

      return true;
    } catch (e) {
      return false;
    }
  }

  /// Logout and clear stored credentials
  Future<void> logout() async {
    try {
      await _odooClient.logout();
    } catch (e) {
      // Ignore logout errors
    }

    await _secureStorage.clearAll();
    _sessionId = null;
    _userId = null;
    _odooClient.dispose();
  }

  /// Reset to initial state
  void reset() {
    _state = LoginState();
    notifyListeners();
  }
}
