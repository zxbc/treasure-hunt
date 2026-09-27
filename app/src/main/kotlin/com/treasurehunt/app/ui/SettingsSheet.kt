package com.treasurehunt.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.treasurehunt.app.LocalePrefs
import com.treasurehunt.app.R
import com.treasurehunt.app.hunt.HuntEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(radius: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val currentLanguage = LocalePrefs.current(context)
    var menuExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
        ) {
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.alert_radius),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.alert_radius_hint, radius),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Slider(
                value = radius.toFloat(),
                onValueChange = { HuntEngine.saveRadius(it.toInt()) },
                valueRange = 100f..2000f,
                steps = 37,
            )
            RowOfLabels(radius)

            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.language),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.language_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            MenuSelectField(
                label = stringResource(R.string.language),
                value = stringResource(
                    if (currentLanguage == LocalePrefs.TAG_CHINESE)
                        R.string.language_chinese
                    else R.string.language_english,
                ),
                expanded = menuExpanded,
                onOpen = { menuExpanded = true },
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.language_english)) },
                    onClick = {
                        menuExpanded = false
                        selectLanguage(LocalePrefs.TAG_ENGLISH, context)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.language_chinese)) },
                    onClick = {
                        menuExpanded = false
                        selectLanguage(LocalePrefs.TAG_CHINESE, context)
                    },
                )
            }
        }
    }
}

private fun selectLanguage(tag: String, context: android.content.Context) {
    if (tag == LocalePrefs.current(context)) return
    LocalePrefs.set(context, tag)
    // Recreate so the new locale takes effect in attachBaseContext.
    (context as? ComponentActivity)?.recreate()
}

@Composable
private fun RowOfLabels(radius: Int) {
    Text(
        stringResource(
            when {
                radius <= 300 -> R.string.radius_short
                radius <= 800 -> R.string.radius_balanced
                else -> R.string.radius_long
            },
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
}
