/// Represents the login form state
class LoginState {
  final bool isLoading;
  final String? error;
  final String? successMessage;

  LoginState({
    this.isLoading = false,
    this.error,
    this.successMessage,
  });

  LoginState copyWith({
    bool? isLoading,
    String? error,
    String? successMessage,
  }) {
    return LoginState(
      isLoading: isLoading ?? this.isLoading,
      error: error,
      successMessage: successMessage,
    );
  }

  LoginState clearMessages() {
    return LoginState(
      isLoading: isLoading,
      error: null,
      successMessage: null,
    );
  }
}
