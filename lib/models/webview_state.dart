import 'page_error.dart';

/// Holds the UI state for WebView screens
class WebViewState {
  final bool isLoading;
  final int loadProgress;
  final bool canGoBack;
  final PageError? pageError;
  final String? currentUrl;
  final bool showExitConfirmation;

  WebViewState({
    this.isLoading = true,
    this.loadProgress = 0,
    this.canGoBack = false,
    this.pageError,
    this.currentUrl,
    this.showExitConfirmation = false,
  });

  WebViewState copyWith({
    bool? isLoading,
    int? loadProgress,
    bool? canGoBack,
    PageError? pageError,
    String? currentUrl,
    bool? showExitConfirmation,
  }) {
    return WebViewState(
      isLoading: isLoading ?? this.isLoading,
      loadProgress: loadProgress ?? this.loadProgress,
      canGoBack: canGoBack ?? this.canGoBack,
      pageError: pageError ?? this.pageError,
      currentUrl: currentUrl ?? this.currentUrl,
      showExitConfirmation: showExitConfirmation ?? this.showExitConfirmation,
    );
  }
}
