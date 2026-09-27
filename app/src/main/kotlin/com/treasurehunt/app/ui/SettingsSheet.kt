package com.treasurehunt.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.treasurehunt.app.hunt.HuntEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(radius: Int, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
        ) {
            Spacer(Modifier.size(8.dp))
            Text("Hunt settings", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            Text(
                "Alert radius",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "You start buzzing when you are within " + radius.toString() +
                    " m of your closest spot. Buzzing gets more urgent the closer you get.",
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
        }
    }
}

@Composable
private fun RowOfLabels(radius: Int) {
    Text(
        if (radius <= 300) "Short radius — alerts only when you are quite close"
        else if (radius <= 800) "Balanced radius"
        else "Long radius — alerts from far away",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
}
