package com.treasurehunt.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.treasurehunt.app.data.HuntImportException
import com.treasurehunt.app.data.LocationDraft

@Composable
fun ListEditorScreen(
    viewModel: HuntViewModel,
    listId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    var listName by remember { mutableStateOf("") }
    var spots by remember { mutableStateOf(emptyList<LocationDraft>()) }
    var loaded by remember { mutableStateOf(listId == 0L) }

    var spotName by remember { mutableStateOf("") }
    var spotDesc by remember { mutableStateOf("") }
    var spotLat by remember { mutableStateOf("") }
    var spotLon by remember { mutableStateOf("") }

    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(listId) {
        if (listId != 0L) {
            viewModel.loadList(listId) { list ->
                if (list != null) {
                    listName = list.name
                    spots = list.locations.map {
                        LocationDraft(it.name, it.description, it.lat, it.lon)
                    }
                }
                loaded = true
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val text = context.contentResolver.openInputStream(uri).use { it.readBytes().toString(Charsets.UTF_8) }
            val parsed = viewModel.importJson(text, "Imported hunt")
            if (listName.isBlank()) listName = parsed.name
            spots = spots + parsed.locations
            error = null
        } catch (e: HuntImportException) {
            error = "Import failed: " + e.message
        } catch (e: Exception) {
            error = "Could not read the file."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text(
                if (listId == 0L) "New hunt list" else "Edit list",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(Modifier.height(8.dp))

        if (loaded) {
            OutlinedTextField(
                value = listName,
                onValueChange = { listName = it },
                label = { Text("List name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.InsertDriveFile, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Import spots from a JSON file")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "JSON shape: { "name": "...", "locations": [ { "name", "description", "lat", "lon" } ] }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            Text("Add a spot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = spotName,
                        onValueChange = { spotName = it },
                        label = { Text("Spot name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = spotDesc,
                        onValueChange = { spotDesc = it },
                        label = { Text("Description / clue (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(
                            value = spotLat,
                            onValueChange = { spotLat = it },
                            label = { Text("Latitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = spotLon,
                            onValueChange = { spotLon = it },
                            label = { Text("Longitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val lat = spotLat.toDoubleOrNull()
                            val lon = spotLon.toDoubleOrNull()
                            if (spotName.isBlank()) {
                                error = "Give the spot a name."
                            } else if (lat == null || lon == null) {
                                error = "Latitude and longitude must be numbers."
                            } else if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                                error = "Coordinates out of range (lat -90..90, lon -180..180)."
                            } else {
                                spots = spots + LocationDraft(spotName.trim(), spotDesc.trim(), lat, lon)
                                spotName = ""; spotDesc = ""; spotLat = ""; spotLon = ""
                                error = null
                            }
                        },
                    ) {
                        Text("Add spot")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                spots.size.toString() + " spots in this list",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(spots, key = { _, spot -> spot.name + spot.lat + spot.lon }) { index, spot ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    (index + 1).toString() + ". " + spot.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    String.format("%.5f, %.5f", spot.lat, spot.lon) +
                                        (if (spot.description.isNotBlank()) " — " + spot.description else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                spots = spots.filterIndexed { i, _ -> i != index }
                            }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            error?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (listName.isBlank()) {
                        error = "Give the list a name."
                    } else if (spots.isEmpty()) {
                        error = "Add at least one spot."
                    } else {
                        saving = true
                        viewModel.saveList(listName, spots) { onBack() }
                    }
                },
                enabled = !saving,
            ) {
                Text(if (saving) "Saving…" else "Save list")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
