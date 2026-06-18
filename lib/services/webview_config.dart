import 'package:webview_flutter/webview_flutter.dart';

/// Utility class for WebView configuration and domain management
/// Mirrors Android Constants.isAllowedHost + GeckoNavigationDelegate
class WebViewConfig {
  /// Allowed hostnames for navigation
  /// Matches Android Constants.ALLOWED_HOSTS pattern
  static const List<String> allowedHosts = [
    'nightpos.com',
    'soho.nightpos.com',
  ];

  /// Check if a host is in the allowed list
  static bool isAllowedHost(String? host) {
    if (host == null) return false;
    return allowedHosts.any(
      (allowed) => host == allowed || host.endsWith('.$allowed'),
    );
  }

  /// Configure WebView with standard settings
  static void applyDefaultSettings(WebViewController controller) {
    controller
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..enableZoom(true);
  }

  /// Build POS URL from server base URL
  static String buildPosUrl(String serverUrl) {
    final base = serverUrl.replaceAll(RegExp(r'/+$'), '');
    return '$base/pos/ui';
  }

  /// Build specific module URL
  static String buildModuleUrl(String serverUrl, String module) {
    final base = serverUrl.replaceAll(RegExp(r'/+$'), '');
    return '$base/$module';
  }
}
