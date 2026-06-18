import 'package:flutter/material.dart';
import '../models/webview_state.dart';
import '../models/page_error.dart';

/// Manages WebView screen state (similar to Android WebViewViewModel)
class WebViewProvider extends ChangeNotifier {
  WebViewState _state = WebViewState();

  WebViewState get state => _state;

  bool get isLoading => _state.isLoading;
  int get loadProgress => _state.loadProgress;
  bool get canGoBack => _state.canGoBack;
  PageError? get pageError => _state.pageError;
  String? get currentUrl => _state.currentUrl;

  /// Called when a page starts loading
  void onPageStarted() {
    _state = _state.copyWith(
      isLoading: true,
      pageError: null,
    );
    notifyListeners();
  }

  /// Called when a page finishes loading
  void onPageFinished({required bool canGoBack}) {
    _state = _state.copyWith(
      isLoading: false,
      canGoBack: canGoBack,
    );
    notifyListeners();
  }

  /// Called when loading progress changes
  void onProgressChanged(int progress) {
    _state = _state.copyWith(loadProgress: progress);
    notifyListeners();
  }

  /// Called when a page load error occurs
  void onPageError({
    required int code,
    required String? description,
    required String? failingUrl,
  }) {
    _state = _state.copyWith(
      isLoading: false,
      pageError: PageError.fromWebViewError(
        code: code,
        description: description,
        failingUrl: failingUrl,
      ),
    );
    notifyListeners();
  }

  /// Called when a page load takes too long (45s timeout)
  void onLoadTimeout(String? url) {
    if (_state.isLoading && _state.pageError == null) {
      _state = _state.copyWith(
        isLoading: false,
        pageError: PageError.timeout(url),
      );
      notifyListeners();
    }
  }

  /// Retry loading the page
  void retry() {
    _state = _state.copyWith(
      pageError: null,
      isLoading: true,
    );
    notifyListeners();
  }

  /// Update the current URL
  void setCurrentUrl(String url) {
    _state = _state.copyWith(currentUrl: url);
    notifyListeners();
  }

  /// Update canGoBack state
  void updateCanGoBack(bool canGoBack) {
    _state = _state.copyWith(canGoBack: canGoBack);
    notifyListeners();
  }

  /// Request exit confirmation
  void requestExit() {
    _state = _state.copyWith(showExitConfirmation: true);
    notifyListeners();
  }

  /// Dismiss exit confirmation
  void dismissExit() {
    _state = _state.copyWith(showExitConfirmation: false);
    notifyListeners();
  }

  /// Reset to initial state
  void reset() {
    _state = WebViewState();
    notifyListeners();
  }
}
