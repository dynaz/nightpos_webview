import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../theme/theme.dart';
import '../providers/webview_provider.dart';

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
  @override
  void initState() {
    super.initState();
    // Initialize WebView state when screen is shown
    final webviewProvider = context.read<WebViewProvider>();
    webviewProvider.reset();
    webviewProvider.setCurrentUrl(widget.url);
    webviewProvider.onPageStarted();
  }

  void _handleReload() {
    context.read<WebViewProvider>().onPageStarted();
    // TODO: Call webViewController.reload() in Phase 4
  }

  void _handleExitConfirmation() {
    context.read<WebViewProvider>().requestExit();
    _showExitDialog();
  }

  void _showExitDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: NightPOSColors.surface,
        title: const Text('Exit POS Screen'),
        content: const Text('Do you want to exit the POS screen and return to the main menu?'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              context.read<WebViewProvider>().dismissExit();
            },
            child: const Text('Stay'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              context.read<WebViewProvider>().dismissExit();
              context.pop(); // Go back to dashboard
            },
            child: const Text('Exit', style: TextStyle(color: NightPOSColors.errorRed)),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NightPOSColors.nightBlack,
      appBar: AppBar(
        title: Text(widget.title),
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _handleReload,
            tooltip: 'Reload',
          ),
          IconButton(
            icon: const Icon(Icons.home),
            onPressed: () => context.pop(),
            tooltip: 'Back',
          ),
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () => context.go('/dashboard/settings'),
            tooltip: 'Settings',
          ),
        ],
      ),
      body: Consumer<WebViewProvider>(
        builder: (context, webviewProvider, _) {
          return Stack(
            children: [
              // Main content
              if (webviewProvider.pageError != null)
                _buildErrorScreen(webviewProvider)
              else
                _buildWebViewPlaceholder(),

              // Progress bar
              if (webviewProvider.isLoading && webviewProvider.pageError == null)
                Positioned(
                  top: 0,
                  left: 0,
                  right: 0,
                  child: LinearProgressIndicator(
                    value: webviewProvider.loadProgress > 0
                        ? webviewProvider.loadProgress / 100.0
                        : null,
                    color: NightPOSColors.neonPurple,
                    backgroundColor: NightPOSColors.surface,
                  ),
                ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildWebViewPlaceholder() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(
            Icons.language,
            color: NightPOSColors.neonPurple,
            size: 64,
          ),
          const SizedBox(height: 24),
          Text(
            'WebView Loading...',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 12),
          Text(
            widget.url,
            style: Theme.of(context).textTheme.bodySmall,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 24),
          const SizedBox(
            width: 40,
            height: 40,
            child: CircularProgressIndicator(
              valueColor: AlwaysStoppedAnimation<Color>(
                NightPOSColors.neonPurple,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorScreen(WebViewProvider webviewProvider) {
    final error = webviewProvider.pageError!;
    return SingleChildScrollView(
      child: Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              const SizedBox(height: 48),
              Icon(
                Icons.error_outline,
                color: NightPOSColors.errorRed,
                size: 64,
              ),
              const SizedBox(height: 24),
              Text(
                error.isTimeout ? 'Loading is taking too long' : 'Failed to Load Page',
                style: Theme.of(context).textTheme.headlineSmall,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),
              Text(
                error.isTimeout
                    ? 'Please check your connection and try again.'
                    : 'A connection error occurred. Please try again.',
                style: Theme.of(context).textTheme.bodyLarge,
                textAlign: TextAlign.center,
              ),
              if (!error.isTimeout) ...[
                const SizedBox(height: 8),
                Text(
                  'Error Code: ${error.code}\nDetails: ${error.description ?? 'Unknown'}',
                  style: Theme.of(context).textTheme.bodySmall,
                  textAlign: TextAlign.center,
                ),
              ],
              const SizedBox(height: 32),
              ElevatedButton.icon(
                onPressed: _handleReload,
                icon: const Icon(Icons.refresh),
                label: const Text('Retry'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
