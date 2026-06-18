/// Represents a WebView page load error or timeout
class PageError {
  final int code;
  final String? description;
  final String? failingUrl;
  final bool isTimeout;

  PageError({
    required this.code,
    this.description,
    this.failingUrl,
    this.isTimeout = false,
  });

  /// Creates a timeout error
  factory PageError.timeout(String? url) {
    return PageError(
      code: 0,
      description: null,
      failingUrl: url,
      isTimeout: true,
    );
  }

  /// Creates an error from a WebView error
  factory PageError.fromWebViewError({
    required int code,
    required String? description,
    required String? failingUrl,
  }) {
    return PageError(
      code: code,
      description: description,
      failingUrl: failingUrl,
      isTimeout: false,
    );
  }

  @override
  String toString() => 'PageError(code: $code, isTimeout: $isTimeout, url: $failingUrl)';
}
