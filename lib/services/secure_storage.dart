import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Secure credential storage using platform-native keychain/keystore
/// Uses flutter_secure_storage for iOS Keychain integration
class SecureStorage {
  static const String _keySessionId = 'odoo_session_id';
  static const String _keyUserId = 'odoo_user_id';
  static const String _keyUsername = 'odoo_username';
  static const String _keyPassword = 'odoo_password';

  late final FlutterSecureStorage _storage;

  SecureStorage() {
    _storage = const FlutterSecureStorage(
      aOptions: AndroidOptions(
        keyCipherAlgorithm: KeyCipherAlgorithm.RSA_ECB_OAEPwithSHA_256andMGF1Padding,
        storageCipherAlgorithm: StorageCipherAlgorithm.AES_GCM_NoPadding,
      ),
    );
  }

  /// Save session information
  Future<void> saveSession({
    required String sessionId,
    required int userId,
    required String username,
  }) async {
    await Future.wait([
      _storage.write(key: _keySessionId, value: sessionId),
      _storage.write(key: _keyUserId, value: userId.toString()),
      _storage.write(key: _keyUsername, value: username),
    ]);
  }

  /// Load session information
  Future<({String? sessionId, int? userId, String? username})> loadSession() async {
    final sessionId = await _storage.read(key: _keySessionId);
    final userIdStr = await _storage.read(key: _keyUserId);
    final username = await _storage.read(key: _keyUsername);

    int? userId;
    if (userIdStr != null) {
      try {
        userId = int.parse(userIdStr);
      } catch (e) {
        // Ignore parsing errors
      }
    }

    return (sessionId: sessionId, userId: userId, username: username);
  }

  /// Save password (optional, for auto-login)
  Future<void> savePassword(String password) async {
    await _storage.write(key: _keyPassword, value: password);
  }

  /// Load password (optional, for auto-login)
  Future<String?> loadPassword() async {
    return await _storage.read(key: _keyPassword);
  }

  /// Clear all credentials
  Future<void> clearAll() async {
    await Future.wait([
      _storage.delete(key: _keySessionId),
      _storage.delete(key: _keyUserId),
      _storage.delete(key: _keyUsername),
      _storage.delete(key: _keyPassword),
    ]);
  }

  /// Check if session is stored
  Future<bool> hasSession() async {
    final sessionId = await _storage.read(key: _keySessionId);
    return sessionId != null;
  }
}
