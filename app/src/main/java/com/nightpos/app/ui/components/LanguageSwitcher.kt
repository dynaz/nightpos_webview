package com.nightpos.app.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.nightpos.app.R
import com.nightpos.app.ui.theme.NeonPurple

/**
 * Icon button that opens the same language picker dialog used in Settings.
 * Lets screens shown before the user reaches Settings (e.g. Login) still offer
 * a quick language switch. [setApplicationLocales] recreates the Activity.
 */
@Composable
fun LanguageSwitcherButton(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Language,
) {
    var showDialog by remember { mutableStateOf(false) }
    val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    IconButton(onClick = { showDialog = true }, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.settings_language),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.language_dialog_title)) },
            text = {
                androidx.compose.foundation.layout.Column {
                    LanguageOptionRow(
                        label = stringResource(R.string.language_system_default),
                        selected = currentTag.isBlank(),
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                            showDialog = false
                        },
                    )
                    LanguageOptionRow(
                        label = stringResource(R.string.language_thai),
                        selected = currentTag.startsWith("th"),
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("th"))
                            showDialog = false
                        },
                    )
                    LanguageOptionRow(
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
private fun LanguageOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
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
