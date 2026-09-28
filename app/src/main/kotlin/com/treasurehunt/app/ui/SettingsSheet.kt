package com.treasurehunt.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import com.treasurehunt.app.data.BuiltInLists
import com.treasurehunt.app.hunt.HuntEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    radius: Int,
    huntActive: Boolean,
    onManageLists: () -> Unit,
    onStopHunt: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val currentLanguage = LocalePrefs.current(context)

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
                stringResource(R.string.stop_hunt_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onStopHunt,
                enabled = huntActive,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.stop_hunt))
            }

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
            // Inline buttons instead of a dropdown: a DropdownMenu anchored
            // inside a ModalBottomSheet mispositions (it pops up at the
            // sheet's top-left), and with two options the choice is clearer
            // when both are always visible — the current one is filled.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (currentLanguage == LocalePrefs.TAG_ENGLISH) {
                    Button(onClick = {}, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.language_english))
                    }
                } else {
                    OutlinedButton(
                        onClick = { selectLanguage(LocalePrefs.TAG_ENGLISH, context) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.language_english)) }
                }
                if (currentLanguage == LocalePrefs.TAG_CHINESE) {
                    Button(onClick = {}, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.language_chinese))
                    }
                } else {
                    OutlinedButton(
                        onClick = { selectLanguage(LocalePrefs.TAG_CHINESE, context) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.language_chinese)) }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.title_hunt_lists),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.manage_lists_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onManageLists, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.manage_lists))
            }
        }
    }
}

private fun selectLanguage(tag: String, context: android.content.Context) {
    if (tag == LocalePrefs.current(context)) return
    LocalePrefs.set(context, tag)
    // Load the bundled hunt list in the new language as the default.
    BuiltInLists.applyLanguageDefault(context)
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
