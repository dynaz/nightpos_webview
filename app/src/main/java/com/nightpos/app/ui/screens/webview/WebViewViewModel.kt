package com.nightpos.app.ui.screens.webview

import androidx.lifecycle.ViewModel
import com.nightpos.app.ui.navigation.WebViewKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** A page failed to load and we want to show a retry affordance instead of a blank WebView. */
data class PageError(val code: Int, val description: String?, val failingUrl: String?)

data class WebViewUiState(
    val kind: WebViewKind = WebViewKind.POS,
    val title: String = "",
    val isLoading: Boolean = true,
    val loadProgress: Int = 0,
    val canGoBack: Boolean = false,
    val pageError: PageError? = null,
    val blockedDomainMessage: String? = null,
    val showExitConfirmation: Boolean = false,
)

/**
 * Holds the pure (parcelable-friendly) UI state for [com.nightpos.app.ui.screens.webview.WebViewScreen].
 *
 * Transient Android framework callbacks that the WebView hands us (SslErrorHandler,
 * file-chooser ValueCallback, popup WebViews, …) are intentionally **not** stored
 * here — holding them in a ViewModel risks leaking Activity/View references across
 * configuration changes. Those stay as `remember { mutableStateOf(...) }` in the
 * Composable itself, which is recreated with the Activity.
 */
class WebViewViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WebViewUiState())
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()

    fun initialize(kind: WebViewKind, title: String) {
        _uiState.update { it.copy(kind = kind, title = title) }
    }

    fun onPageStarted() = _uiState.update { it.copy(isLoading = true, pageError = null) }

    fun onPageFinished(canGoBack: Boolean) =
        _uiState.update { it.copy(isLoading = false, canGoBack = canGoBack) }

    fun onProgressChanged(progress: Int) = _uiState.update { it.copy(loadProgress = progress) }

    fun onPageError(code: Int, description: String?, failingUrl: String?) =
        _uiState.update { it.copy(isLoading = false, pageError = PageError(code, description, failingUrl)) }

    fun retry() = _uiState.update { it.copy(pageError = null, isLoading = true) }

    fun onBlockedDomain(host: String) = _uiState.update { it.copy(blockedDomainMessage = host) }

    fun consumeBlockedDomainMessage() = _uiState.update { it.copy(blockedDomainMessage = null) }

    fun requestExit() = _uiState.update { it.copy(showExitConfirmation = true) }

    fun dismissExit() = _uiState.update { it.copy(showExitConfirmation = false) }

    fun updateCanGoBack(canGoBack: Boolean) = _uiState.update { it.copy(canGoBack = canGoBack) }
}
