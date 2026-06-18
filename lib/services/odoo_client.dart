import 'package:http/http.dart' as http;
import 'dart:convert';
import 'dart:async';
import '../models/auth_response.dart';

/// Odoo API client for authentication and session management
/// Mirrors Android OdooAuthClient pattern with HTTP cookie handling
class OdooClient {
  final String serverUrl;
  late final http.Client _client;
  String? _sessionCookie;
  int? _userId;
  String? _username;

  // Timeout constants (matching Android)
  static const Duration defaultTimeout = Duration(seconds: 35);
  static const Duration loginTimeout = Duration(seconds: 45);

  OdooClient({required this.serverUrl}) {
    _client = http.Client();
  }

  // Getters
  String? get sessionCookie => _sessionCookie;
  int? get userId => _userId;
  String? get username => _username;
  bool get isAuthenticated => _sessionCookie != null && _userId != null;

  /// Authenticate with Odoo server
  /// POST /web/session/authenticate
  Future<AuthResponse> authenticate({
    required String username,
    required String password,
    String? database,
  }) async {
    try {
      final url = Uri.parse('$serverUrl/web/session/authenticate');

      final body = {
        'jsonrpc': '2.0',
        'method': 'call',
        'params': {
          'login': username,
          'password': password,
          if (database != null) 'db': database,
        },
      };

      final response = await _client
          .post(
            url,
            headers: {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
            },
            body: jsonEncode(body),
          )
          .timeout(loginTimeout);

      if (response.statusCode == 200) {
        final jsonResponse = jsonDecode(response.body);

        // Check for RPC errors
        if (jsonResponse['error'] != null) {
          final error = jsonResponse['error'];
          final errorMsg = error['data']?['message'] ?? error['message'] ?? 'Unknown error';
          return AuthResponse.error('Login failed: $errorMsg');
        }

        // Extract session info from response
        final result = jsonResponse['result'];
        if (result != null && result['uid'] != null) {
          _userId = result['uid'] as int;
          _username = result['name'] ?? username;

          // Extract session cookie from response headers
          final setCookieHeader = response.headers['set-cookie'];
          if (setCookieHeader != null) {
            _sessionCookie = _extractSessionId(setCookieHeader);
          }

          return AuthResponse(
            userId: _userId!,
            username: _username!,
            sessionCookie: _sessionCookie ?? '',
            companyId: result['company_id']?[0]?.toString(),
            partnerName: result['partner_id']?[1],
            success: true,
          );
        } else {
          return AuthResponse.error('Invalid login credentials');
        }
      } else if (response.statusCode == 401 || response.statusCode == 403) {
        return AuthResponse.error('Invalid username or password');
      } else {
        return AuthResponse.error('Server error: ${response.statusCode}');
      }
    } on TimeoutException {
      return AuthResponse.error('Connection timeout. Please check your server URL.');
    } catch (e) {
      return AuthResponse.error('Authentication failed: ${e.toString()}');
    }
  }

  /// Get current session info (to verify authentication)
  /// GET /web/session/get_session_info
  Future<AuthResponse?> getSessionInfo() async {
    if (_sessionCookie == null) return null;

    try {
      final url = Uri.parse('$serverUrl/web/session/get_session_info');

      final response = await _client
          .get(
            url,
            headers: {
              'Cookie': _sessionCookie!,
              'Accept': 'application/json',
            },
          )
          .timeout(defaultTimeout);

      if (response.statusCode == 200) {
        final jsonResponse = jsonDecode(response.body);
        final result = jsonResponse['result'];

        if (result != null && result['uid'] != null) {
          _userId = result['uid'] as int;
          _username = result['name'] ?? _username;

          return AuthResponse(
            userId: _userId!,
            username: _username!,
            sessionCookie: _sessionCookie!,
            companyId: result['company_id']?[0]?.toString(),
            partnerName: result['partner_id']?[1],
            success: true,
          );
        }
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  /// Load POS specific data (like outlets/terminals)
  /// This mirrors Android's outlets fetching in PosOutletFab
  Future<List<Map<String, dynamic>>> getPosOutlets() async {
    if (_sessionCookie == null || _userId == null) return [];

    try {
      final url = Uri.parse('$serverUrl/pos/config');

      final response = await _client
          .get(
            url,
            headers: {
              'Cookie': _sessionCookie!,
              'Accept': 'application/json',
            },
          )
          .timeout(defaultTimeout);

      if (response.statusCode == 200) {
        // Parse response based on Odoo structure
        // This is a placeholder - actual structure depends on Odoo version
        return [];
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  /// Logout from Odoo server
  Future<void> logout() async {
    try {
      if (_sessionCookie != null) {
        final url = Uri.parse('$serverUrl/web/session/destroy');

        await _client
            .post(
              url,
              headers: {
                'Cookie': _sessionCookie!,
                'Content-Type': 'application/json',
              },
            )
            .timeout(defaultTimeout);
      }
    } catch (e) {
      // Ignore logout errors
    } finally {
      _sessionCookie = null;
      _userId = null;
      _username = null;
    }
  }

  /// Extract session ID from Set-Cookie header
  /// Format: session_id=abc123; Path=/; HttpOnly
  String? _extractSessionId(String setCookieHeader) {
    try {
      final parts = setCookieHeader.split(';');
      for (var part in parts) {
        part = part.trim();
        if (part.startsWith('session_id=')) {
          return part.replaceFirst('session_id=', '');
        }
      }
    } catch (e) {
      // Ignore parsing errors
    }
    return null;
  }

  /// Set custom headers (useful for proxy/VPN scenarios)
  void setCustomHeaders(Map<String, String> headers) {
    // Can be extended for custom headers if needed
  }

  /// Verify server connectivity (ping)
  Future<bool> verifyServerConnectivity() async {
    try {
      final url = Uri.parse('$serverUrl/web/health');

      final response = await _client
          .get(url)
          .timeout(const Duration(seconds: 10));

      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  /// Dispose client on app close
  void dispose() {
    _client.close();
  }
}
