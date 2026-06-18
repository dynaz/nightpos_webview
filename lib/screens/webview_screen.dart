import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:webview_flutter/webview_flutter.dart';
import '../theme/theme.dart';
import '../providers/webview_provider.dart';
import '../providers/settings_provider.dart';
import '../providers/app_state.dart';
import '../services/network_diagnostics.dart';
import '../services/platform_service.dart';
import '../services/localization_service.dart';
import 'offline_screen.dart';

const int _loadTimeoutMs = 45000;

class WebViewScreen extends StatefulWidget {
  final String title;
  final String url;

  const WebViewScreen({
    Key? key,
    required this.title,
    required this.url,
  }) : super(key: key);

  @override
  State<WebViewScreen> createState() => _WebViewScreenState();
}

class _WebViewScreenState extends State<WebViewScreen> {
  late WebViewController _controller;
  Timer? _loadTimeoutTimer;

  @override
  void initState() {
    super.initState();
    _initWebView();
    _applyPlatformSettings();
  }

  @override
  void dispose() {
    _loadTimeoutTimer?.cancel();
    _restorePlatformSettings();
    super.dispose();
  }

  void _applyPlatformSettings() {
    final settings = context.read<SettingsProvider>();
    if (settings.keepScreenOn) {
      PlatformService.setKeepScreenOn(true);
    }
    if (settings.kioskMode) {
      PlatformService.setKioskMode(true);
    }
  }

  void _restorePlatformSettings() {
    PlatformService.setKeepScreenOn(false);
    PlatformService.setKioskMode(false);
  }

  void _initWebView() {
    final webviewProvider = context.read<WebViewProvider>();
    webviewProvider.reset();
    webviewProvider.setCurrentUrl(widget.url);

    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setNavigationDelegate(
        NavigationDelegate(
          onPageStarted: (url) {
            webviewProvider.onPageStarted();
            _startLoadTimeout();
          },
          onPageFinished: (url) {
            _cancelLoadTimeout();
            _controller.canGoBack().then((canGoBack) {
              webviewProvider.onPageFinished(canGoBack: canGoBack);
            });
            webviewProvider.setCurrentUrl(url);
          },
          onProgress: (progress) {
            webviewProvider.onProgressChanged(progress);
          },
          onWebResourceError: (error) {
            _cancelLoadTimeout();
            // Only surface main frame errors, not subresource failures
            if (error.isForMainFrame ?? true) {
              webviewProvider.onPageError(
                code: error.errorCode,
                description: '${error.errorType ?? 'unknown'}: ${error.description}',
                failingUrl: error.url,
              );
            }
          },
          onNavigationRequest: (request) {
            // Allow all navigation within the same domain
            return NavigationDecision.navigate;
          },
        ),
      )
      ..setBackgroundColor(NightPOSColors.nightBlack)
      ..loadRequest(Uri.parse(widget.url));
  }

  void _startLoadTimeout() {
    _cancelLoadTimeout();
    _loadTimeoutTimer = Timer(
      const Duration(milliseconds: _loadTimeoutMs),
      () {
        context.read<WebViewProvider>().onLoadTimeout(widget.url);
      },
    );
  }

  void _cancelLoadTimeout() {
    _loadTimeoutTimer?.cancel();
    _loadTimeoutTimer = null;
  }

  void _handleReload() {
    final webviewProvider = context.read<WebViewProvider>();
    webviewProvider.retry();
    _controller.reload();
    _startLoadTimeout();
  }

  void _handleRetry() {
    final webviewProvider = context.read<WebViewProvider>();
    webviewProvider.retry();
    _controller.loadRequest(Uri.parse(widget.url));
    _startLoadTimeout();
  }

  Future<bool> _handleBackPress() async {
    final canGoBack = await _controller.canGoBack();
    if (canGoBack) {
      _controller.goBack();
      return false;
    }
    _showExitDialog();
    return false;
  }

  void _showExitDialog() {
    final settings = context.read<SettingsProvider>();
    final isKiosk = settings.kioskMode;

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: NightPOSColors.surface,
        title: Text(isKiosk ? 'Exit Kiosk Mode' : 'Exit POS Screen'),
        content: Text(isKiosk
            ? 'Do you want to exit the app? This will close the POS system'
            : 'Do you want to exit the POS screen and return to the main menu?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(isKiosk ? 'Cancel' : 'Stay'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
              context.pop();
            },
            child: Text(
              isKiosk ? 'Exit App' : 'Exit',
              style: const TextStyle(color: NightPOSColors.errorRed),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<SettingsProvider>();
    final isKiosk = settings.kioskMode;

    return WillPopScope(
      onWillPop: _handleBackPress,
      child: Consumer2<WebViewProvider, NetworkDiagnostics>(
        builder: (context, webviewProvider, networkDiagnostics, _) {
          return Scaffold(
            backgroundColor: NightPOSColors.nightBlack,
            appBar: isKiosk
                ? null
                : AppBar(
                    title: Text(widget.title),
                    elevation: 0,
                    leading: IconButton(
                      icon: const Icon(Icons.arrow_back),
                      onPressed: () async {
                        final canGoBack = await _controller.canGoBack();
                        if (canGoBack) {
                          _controller.goBack();
                        } else {
                          _showExitDialog();
                        }
                      },
                      tooltip: 'Back',
                    ),
                    actions: [
                      IconButton(
                        icon: const Icon(Icons.home),
                        onPressed: () => context.pop(),
                        tooltip: 'Home',
                      ),
                      IconButton(
                        icon: const Icon(Icons.refresh),
                        onPressed: _handleReload,
                        tooltip: 'Reload',
                      ),
                      IconButton(
                        icon: const Icon(Icons.settings),
                        onPressed: () => context.go('/dashboard/settings'),
                        tooltip: 'Settings',
                      ),
                    ],
                  ),
            body: Stack(
              children: [
                // Main content
                if (!networkDiagnostics.isOnline)
                  OfflineScreen(onRetry: _handleReload)
                else if (webviewProvider.pageError != null)
                  _buildErrorScreen(webviewProvider)
                else
                  WebViewWidget(controller: _controller),

                // Progress bar
                if (webviewProvider.isLoading &&
                    networkDiagnostics.isOnline &&
                    webviewProvider.pageError == null)
                  Positioned(
                    top: 0,
                    left: 0,
                    right: 0,
                    child: LinearProgressIndicator(
                      value: webviewProvider.loadProgress > 0
                          ? webviewProvider.loadProgress / 100.0
                          : null,
                      color: NightPOSColors.neonPurple,
                      backgroundColor: NightPOSColors.surfaceVariant,
                    ),
                  ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildErrorScreen(WebViewProvider webviewProvider) {
    final error = webviewProvider.pageError!;
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.error_outline,
              color: NightPOSColors.errorRed,
              size: 64,
            ),
            const SizedBox(height: 24),
            Text(
              LocalizationService.tr(error.isTimeout ? 'webview_error_timeout_message' : 'webview_error_title'),
              style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              LocalizationService.tr(error.isTimeout ? 'webview_error_timeout_message' : 'webview_error_message'),
              style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                    color: NightPOSColors.textSecondary,
                  ),
              textAlign: TextAlign.center,
            ),
            if (!error.isTimeout) ...[
              const SizedBox(height: 4),
              Text(
                LocalizationService.tr('webview_error_detail', '${error.code} (${error.description})'),
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: NightPOSColors.textSecondary,
                    ),
                textAlign: TextAlign.center,
              ),
            ],
            const SizedBox(height: 32),
            SizedBox(
              height: 56,
              child: ElevatedButton.icon(
                onPressed: _handleRetry,
                icon: const Icon(Icons.refresh),
                label: Text(
                  LocalizationService.tr('offline_retry'),
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
