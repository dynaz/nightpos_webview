package com.nightpos.app.ui.screens.settings

import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nightpos.app.BuildConfig
import com.nightpos.app.R
import com.nightpos.app.ui.theme.ErrorRed
import com.nightpos.app.ui.theme.NeonPurple
import com.nightpos.app.ui.theme.TextSecondary
import com.nightpos.app.util.TwaDiagnostics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    sharedWebView: WebView?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clearedMessage = stringResource(R.string.settings_clear_data_success)

    LaunchedEffect(event) {
        when (event) {
            SettingsEvent.ClearingFinished -> {
                snackbarHostState.showSnackbar(message = clearedMessage)
                viewModel.consumeEvent()
            }
            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        ) {
            item { SectionHeader(stringResource(R.string.settings_section_pos)) }

            item {
                ServerUrlEditor(
                    currentUrl = uiState.serverUrl,
                    onSave = viewModel::setServerUrl,
                )
            }

            item {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_kiosk_mode),
                    description = stringResource(R.string.settings_kiosk_mode_desc),
                    checked = uiState.kioskModeEnabled,
                    onCheckedChange = viewModel::setKioskModeEnabled,
                )
            }

            item {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_auto_reopen_pos),
                    description = stringResource(R.string.settings_auto_reopen_pos_desc),
                    checked = uiState.autoReopenPosEnabled,
                    enabled = uiState.kioskModeEnabled,
                    onCheckedChange = viewModel::setAutoReopenPosEnabled,
                )
            }

            item {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_keep_screen_on),
                    description = stringResource(R.string.settings_keep_screen_on_desc),
                    checked = uiState.keepScreenOnEnabled,
                    onCheckedChange = viewModel::setKeepScreenOnEnabled,
                )
            }

            item { SectionHeader(stringResource(R.string.settings_section_general)) }

            item {
                ClearDataRow(
                    isClearing = uiState.isClearingData || event == SettingsEvent.ClearingStarted,
                    onClear = {
                        scope.launch { viewModel.clearWebViewData(sharedWebView) }
                    },
                )
            }

            item { SectionHeader(stringResource(R.string.settings_section_about)) }

            item {
                AboutRow(
                    serverUrl = uiState.serverUrl,
                    snackbarHostState = snackbarHostState,
                    scope = scope,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = NeonPurple,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun ServerUrlEditor(currentUrl: String, onSave: (String) -> Unit) {
    var text by remember(currentUrl) { mutableStateOf(currentUrl) }
    val isDirty = text.trim().trimEnd('/') != currentUrl.trim().trimEnd('/')

    SettingsCard {
        Text(
            text = stringResource(R.string.settings_server_url),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.settings_server_url_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("https://soho.nightpos.com") },
        )
        if (isDirty) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { text = currentUrl }) {
                    Text(stringResource(R.string.logout_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { onSave(text) }) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else TextSecondary,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(checkedTrackColor = NeonPurple),
            )
        }
    }
}

@Composable
private fun ClearDataRow(isClearing: Boolean, onClear: () -> Unit) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_clear_data),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_clear_data_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            TextButton(onClick = onClear, enabled = !isClearing) {
                Icon(Icons.Filled.CleaningServices, contentDescription = null, tint = ErrorRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isClearing) stringResource(R.string.webview_loading)
                    else stringResource(R.string.settings_clear_data),
                    color = ErrorRed,
                )
            }
        }
    }
}

/** Tapping the version row this many times within [TAP_WINDOW_MS] reveals the diagnostics dialog. */
private const val DIAGNOSTICS_TAP_COUNT = 5
private const val TAP_WINDOW_MS = 2000L

@Composable
private fun AboutRow(
    serverUrl: String,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.diagnostics_copied)
    var tapCount by remember { mutableStateOf(0) }
    var lastTapAtMs by remember { mutableLongStateOf(0L) }
    var diagnosticsText by remember { mutableStateOf<String?>(null) }

    SettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val now = System.currentTimeMillis()
                    tapCount = if (now - lastTapAtMs <= TAP_WINDOW_MS) tapCount + 1 else 1
                    lastTapAtMs = now
                    if (tapCount >= DIAGNOSTICS_TAP_COUNT) {
                        tapCount = 0
                        diagnosticsText = TwaDiagnostics.collect(context, serverUrl)
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.settings_version),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline)
        Text(
            text = "© NightPOS Soho — Odoo POS wrapper for soho.nightpos.com",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }

    diagnosticsText?.let { text ->
        AlertDialog(
            onDismissRequest = { diagnosticsText = null },
            title = { Text(stringResource(R.string.diagnostics_title)) },
            text = {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(text))
                    scope.launch { snackbarHostState.showSnackbar(message = copiedMessage) }
                }) {
                    Text(stringResource(R.string.diagnostics_copy))
                }
            },
            dismissButton = {
                TextButton(onClick = { diagnosticsText = null }) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
    }
}
