package com.nightpos.app.ui.screens.settings

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import org.mozilla.geckoview.GeckoView
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Print
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.nightpos.app.BuildConfig
import com.nightpos.app.R
import com.nightpos.app.print.PrintServiceEnabler
import com.nightpos.app.print.SunmiPrinterConnection
import com.nightpos.app.ui.theme.ErrorRed
import com.nightpos.app.ui.theme.NeonPurple
import com.nightpos.app.ui.theme.TextSecondary
import com.nightpos.app.util.TwaDiagnostics
import com.nightpos.app.util.TwaLaunchLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    sharedGeckoView: GeckoView?,
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

            item { SectionHeader(stringResource(R.string.settings_section_printer)) }

            item {
                PrinterSection(
                    paperWidthMm = uiState.printerPaperWidthMm,
                    onPaperWidthChange = viewModel::setPrinterPaperWidthMm,
                    snackbarHostState = snackbarHostState,
                )
            }

            item { SectionHeader(stringResource(R.string.settings_section_pwa)) }

            item { PwaInstallRow(serverUrl = uiState.serverUrl) }

            item { SectionHeader(stringResource(R.string.settings_section_general)) }

            item {
                ClearDataRow(
                    isClearing = uiState.isClearingData || event == SettingsEvent.ClearingStarted,
                    onClear = {
                        scope.launch { viewModel.clearWebViewData(sharedGeckoView?.session) }
                    },
                )
            }

            item { LanguageRow() }

            item {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_auto_startup),
                    description = stringResource(R.string.settings_auto_startup_desc),
                    checked = uiState.autoStartupEnabled,
                    onCheckedChange = viewModel::setAutoStartupEnabled,
                )
            }

            item { SectionHeader(stringResource(R.string.settings_section_about)) }

            item { AboutRow() }

            item { WebViewEngineRow() }

            item {
                DiagnosticsRow(
                    serverUrl = uiState.serverUrl,
                    snackbarHostState = snackbarHostState,
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

/** Maps a BCP-47 language tag prefix to its display string resource. */
private fun languageLabelRes(tag: String): Int = when {
    tag.isBlank() -> R.string.language_system_default
    tag.startsWith("th") -> R.string.language_thai
    tag.startsWith("en") -> R.string.language_english
    else -> R.string.language_system_default
}

@Composable
private fun LanguageRow() {
    var showDialog by remember { mutableStateOf(false) }
    // setApplicationLocales recreates the Activity, so re-reading on each
    // composition keeps this in sync with the persisted choice.
    val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    SettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_language_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(languageLabelRes(currentTag)),
                style = MaterialTheme.typography.bodyLarge,
                color = NeonPurple,
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.language_dialog_title)) },
            text = {
                Column {
                    LanguageOption(
                        label = stringResource(R.string.language_system_default),
                        selected = currentTag.isBlank(),
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                            showDialog = false
                        },
                    )
                    LanguageOption(
                        label = stringResource(R.string.language_thai),
                        selected = currentTag.startsWith("th"),
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("th"))
                            showDialog = false
                        },
                    )
                    LanguageOption(
                        label = stringResource(R.string.language_english),
                        selected = currentTag.startsWith("en"),
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                            showDialog = false
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = NeonPurple),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AboutRow() {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
}

@Composable
private fun PwaInstallRow(serverUrl: String) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    SettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pwa_install_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.pwa_install_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Filled.InstallMobile,
                contentDescription = null,
                tint = NeonPurple,
            )
        }
    }

    if (showDialog) {
        val baseUrl = serverUrl.ifBlank { "https://soho.nightpos.com" }.trimEnd('/')
        val pwaUrl = "$baseUrl/npos"
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.pwa_install_dialog_title)) },
            text = { Text(stringResource(R.string.pwa_install_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pwaUrl)))
                    showDialog = false
                }) {
                    Text(stringResource(R.string.pwa_install_confirm), color = NeonPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
    }
}

@Composable
private fun PrinterSection(
    paperWidthMm: Int,
    onPaperWidthChange: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }
    val hasPermission = remember { PrintServiceEnabler.hasWriteSecureSettings(context) }
    val testSuccessMsg = stringResource(R.string.settings_printer_test_success)
    val testFailedMsg = stringResource(R.string.settings_printer_test_failed)

    if (!hasPermission) {
        SettingsCard {
            Text(
                text = stringResource(R.string.settings_printer_service_disabled),
                style = MaterialTheme.typography.titleMedium,
                color = ErrorRed,
            )
            Text(
                text = stringResource(R.string.settings_printer_service_disabled_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            Text(
                text = PrintServiceEnabler.adbGrantCommand(context),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }

    SettingsCard {
        Text(
            text = stringResource(R.string.settings_printer_paper_width),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.settings_printer_paper_width_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = paperWidthMm == 58,
                onClick = { onPaperWidthChange(58) },
                colors = RadioButtonDefaults.colors(selectedColor = NeonPurple),
            )
            Text(
                text = stringResource(R.string.settings_printer_paper_58mm),
                modifier = Modifier.clickable { onPaperWidthChange(58) },
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(24.dp))
            RadioButton(
                selected = paperWidthMm == 80,
                onClick = { onPaperWidthChange(80) },
                colors = RadioButtonDefaults.colors(selectedColor = NeonPurple),
            )
            Text(
                text = stringResource(R.string.settings_printer_paper_80mm),
                modifier = Modifier.clickable { onPaperWidthChange(80) },
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isPrinting) {
                    isPrinting = true
                    scope.launch {
                        val widthPx = if (paperWidthMm == 80) 576 else 384
                        val success = withContext(Dispatchers.IO) {
                            sunmiTestPrint(context, widthPx)
                        }
                        isPrinting = false
                        snackbarHostState.showSnackbar(if (success) testSuccessMsg else testFailedMsg)
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPrinting) stringResource(R.string.settings_printer_printing)
                    else stringResource(R.string.settings_printer_test),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPrinting) TextSecondary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_printer_test_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Filled.Print,
                contentDescription = null,
                tint = if (isPrinting) TextSecondary else NeonPurple,
            )
        }
    }
}

private fun sunmiTestPrint(context: android.content.Context, widthPx: Int): Boolean {
    val connection = SunmiPrinterConnection(context)
    if (!connection.bind()) return false
    if (!connection.awaitReady()) {
        connection.unbind()
        return false
    }
    val bitmap = buildTestBitmap(widthPx)
    val success = connection.printReceipt(bitmap)
    bitmap.recycle()
    connection.unbind()
    return success
}

private fun buildTestBitmap(widthPx: Int): Bitmap {
    val height = 320
    val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }
    val cx = widthPx / 2f

    paint.textSize = 40f
    canvas.drawText("Test Print", cx, 60f, paint)

    paint.textSize = 28f
    canvas.drawText("NightPOS Soho", cx, 100f, paint)

    paint.strokeWidth = 2f
    canvas.drawLine(20f, 118f, (widthPx - 20).toFloat(), 118f, paint)

    paint.textSize = 24f
    val dateStr = SimpleDateFormat("yyyy-MM-dd  HH:mm:ss", Locale.US).format(Date())
    canvas.drawText(dateStr, cx, 154f, paint)

    val widthLabel = if (widthPx >= 576) "80 mm  (576 px)" else "58 mm  (384 px)"
    canvas.drawText(widthLabel, cx, 190f, paint)

    canvas.drawText("Sunmi T1  —  Printer OK", cx, 230f, paint)

    canvas.drawLine(20f, 250f, (widthPx - 20).toFloat(), 250f, paint)

    return bitmap
}

@Composable
private fun WebViewEngineRow() {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_webview_engine),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "GeckoView ${BuildConfig.GECKOVIEW_VERSION}",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun DiagnosticsRow(
    serverUrl: String,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.diagnostics_copied)
    val scope = rememberCoroutineScope()
    var diagnosticsText by remember { mutableStateOf<String?>(null) }
    var isCollecting by remember { mutableStateOf(false) }

    SettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isCollecting) {
                    isCollecting = true
                    scope.launch {
                        val combined = withContext(Dispatchers.IO) {
                            val sysInfo = TwaDiagnostics.collect(context, serverUrl)
                            val launchLog = TwaLaunchLog.read(context)
                            "=== System Info ===\n$sysInfo\n\n=== TWA Launch Log ===\n$launchLog"
                        }
                        diagnosticsText = combined
                        isCollecting = false
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.diagnostics_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCollecting) TextSecondary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.diagnostics_menu_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Filled.BugReport,
                contentDescription = null,
                tint = if (isCollecting) TextSecondary else NeonPurple,
            )
        }
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
                    diagnosticsText = null
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
