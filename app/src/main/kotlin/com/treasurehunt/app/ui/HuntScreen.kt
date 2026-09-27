@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.treasurehunt.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.treasurehunt.app.data.HuntListWithLocations
import com.treasurehunt.app.hunt.HuntEngine
import kotlin.math.roundToInt

private fun formatDistance(meters: Double): String =
    if (meters < 1000.0) meters.roundToInt().toString() + " m"
    else String.format("%.1f km", meters / 1000.0)

@Composable
fun HuntScreen(
    viewModel: HuntViewModel,
    engine: HuntEngine.State,
    onRequestStart: (Long) -> Unit,
    onOpenLists: () -> Unit,
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (engine.isActive) {
            ActiveHuntContent(viewModel, engine)
        } else {
            InactiveHuntContent(lists, engine.radiusM, onRequestStart, onOpenLists)
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun InactiveHuntContent(
    lists: List<HuntListWithLocations>,
    radiusM: Int,
    onRequestStart: (Long) -> Unit,
    onOpenLists: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (lists.isEmpty()) {
            Text(
                "No hunt lists yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Create a list of spots — by hand or from a JSON file — and the hunt can start.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onOpenLists) { Text("Create a list") }
        } else {
            val rememberedId = HuntEngine.activeListFromPrefs()
            var selected by remember {
                mutableStateOf(lists.firstOrNull { it.id == rememberedId } ?: lists.first())
            }
            Text(
                "Pick a hunt to start",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))

            var menuExpanded by remember { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selected.name,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { menuExpanded = true },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    lists.forEach { list ->
                        DropdownMenuItem(
                            text = { Text(list.name) },
                            onClick = { selected = list },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                selected.locations.size.toString() + " spots · alert radius " +
                    radiusM.toString() + " m (change it via ⚙ Settings)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { if (selected.locations.isNotEmpty()) onRequestStart(selected.id) },
                enabled = selected.locations.isNotEmpty(),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Start hunt")
            }
        }
    }
}

@Composable
private fun ActiveHuntContent(viewModel: HuntViewModel, engine: HuntEngine.State) {
    val nearest = engine.nearest
    val inRange = nearest != null && nearest.distanceM <= engine.radiusM

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            engine.listName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            engine.totalLocations.toString() + " spots" +
                (if (engine.mutedLocations.isNotEmpty())
                    " · " + engine.mutedLocations.size.toString() + " muted" else ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        val progress: Float
        val color: Color
        val center: String
        val sub: String
        when {
            !engine.hasFix -> {
                progress = 0f; color = MaterialTheme.colorScheme.outline
                center = "…"; sub = "Getting location"
            }
            nearest == null -> {
                progress = 0f; color = MaterialTheme.colorScheme.outline
                center = "—"; sub = "All targets muted"
            }
            else -> {
                if (inRange) {
                    progress = (1f - nearest.ratio).coerceIn(0f, 1f)
                    color = UrgencyPalette[nearest.level]
                    center = formatDistance(nearest.distanceM)
                    sub = HuntEngine.urgencyLabels[nearest.level]
                } else {
                    progress = 0f; color = MaterialTheme.colorScheme.outline
                    center = formatDistance(nearest.distanceM)
                    sub = "outside " + formatDistance(engine.radiusM.toDouble()) + " radius"
                }
            }
        }
        UrgencyGauge(progress = progress, color = color, centerText = center, subText = sub)
        Spacer(Modifier.height(24.dp))

        if (nearest != null) {
            NearestCard(nearest)
            Spacer(Modifier.height(16.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { viewModel.muteNearest() },
                enabled = nearest != null,
            ) {
                Icon(Icons.Filled.VolumeOff, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Mute nearest")
            }
            Button(onClick = { viewModel.stopHunt() }) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Stop")
            }
        }

        if (engine.mutedLocations.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Muted spots",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            engine.mutedLocations.forEach { muted ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        muted.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { viewModel.unmute(muted.id) }) {
                        Text("Unmute")
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun NearestCard(nearest: HuntEngine.Nearest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                nearest.snapshot.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (nearest.snapshot.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    nearest.snapshot.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UrgencyGauge(
    progress: Float,
    color: Color,
    centerText: String,
    subText: String,
) {
    Box(
        modifier = Modifier
            .size(230.dp)
            .clip(RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val rect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)
            val startAngle = 135f
            val sweepAngle = 270f

            val backgroundArc = Path().apply {
                addArc(rect, startAngle, sweepAngle)
            }
            drawPath(
                backgroundArc,
                Color(0xFF232C38),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (progress > 0.002f) {
                val progressArc = Path().apply {
                    addArc(rect, startAngle, sweepAngle * progress)
                }
                drawPath(
                    progressArc,
                    color,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerText,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                subText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }
}
