/// Response from Odoo authentication endpoint
class AuthResponse {
  final int userId;
  final String username;
  final String? companyId;
  final String? partnerName;
  final String sessionCookie;
  final bool success;
  final String? errorMessage;

  AuthResponse({
    required this.userId,
    required this.username,
    this.companyId,
    this.partnerName,
    required this.sessionCookie,
    this.success = true,
    this.errorMessage,
  });

  factory AuthResponse.error(String message) {
    return AuthResponse(
      userId: -1,
      username: '',
      sessionCookie: '',
      success: false,
      errorMessage: message,
    );
  }

  @override
  String toString() => 'AuthResponse(userId: $userId, username: $username, success: $success)';
}
