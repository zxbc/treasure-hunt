@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.treasurehunt.app.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.treasurehunt.app.R
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
    onOpenSettings: () -> Unit,
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (engine.isActive) {
            ActiveHuntContent(viewModel, engine, onOpenSettings)
        } else {
            InactiveHuntContent(lists, engine.radiusM, onRequestStart, onOpenSettings)
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun InactiveHuntContent(
    lists: List<HuntListWithLocations>,
    radiusM: Int,
    onRequestStart: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (lists.isEmpty()) {
                    Text(
                        stringResource(R.string.hunt_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.hunt_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        stringResource(R.string.pick_hunt),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(16.dp))

                    val rememberedId = HuntEngine.activeListFromPrefs()
                    var selected by remember {
                        mutableStateOf(lists.firstOrNull { it.id == rememberedId } ?: lists.first())
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    Column(Modifier.fillMaxWidth()) {
                        MenuSelectField(
                            value = selected.name,
                            expanded = menuExpanded,
                            onOpen = { menuExpanded = true },
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
                        stringResource(R.string.hunt_summary, selected.locations.size, radiusM),
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
                        Text(stringResource(R.string.start_hunt))
                    }
                }
            }
            SettingsGear(onOpenSettings)
        }
    }
}

@Composable
private fun ActiveHuntContent(
    viewModel: HuntViewModel,
    engine: HuntEngine.State,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    // The tracked spot: the user's choice when one was made, otherwise the
    // closest active spot (top of the sorted list).
    val tracked = engine.spots.firstOrNull { it.snapshot.id == engine.trackedId } ?: engine.spots.firstOrNull()
    val inRange = tracked != null && tracked.distanceM <= engine.radiusM

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (tracked != null) {
                    Text(
                        tracked.snapshot.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    engine.listName + " · " +
                        stringResource(R.string.spot_count, engine.totalLocations) +
                        (if (engine.mutedLocations.isNotEmpty())
                            " · " + stringResource(R.string.muted_count, engine.mutedLocations.size) else ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SettingsGear(onOpenSettings)
        }

        if (tracked != null && tracked.snapshot.description.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                tracked.snapshot.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))

        val progress: Float
        val color: Color
        val needleColor: Color
        val center: String
        val sub: String
        when {
            !engine.hasFix -> {
                progress = 0f
                color = MaterialTheme.colorScheme.outline
                needleColor = MaterialTheme.colorScheme.outline
                center = "…"
                sub = stringResource(R.string.getting_location)
            }
            tracked == null -> {
                progress = 0f
                color = MaterialTheme.colorScheme.outline
                needleColor = MaterialTheme.colorScheme.outline
                center = "—"
                sub = stringResource(R.string.all_targets_muted)
            }
            inRange -> {
                progress = (1f - tracked.ratio).coerceIn(0f, 1f)
                color = UrgencyPalette[tracked.level]
                needleColor = UrgencyPalette[tracked.level]
                center = formatDistance(tracked.distanceM)
                sub = HuntEngine.urgencyLabel(context, tracked.level)
            }
            else -> {
                progress = 0f
                color = MaterialTheme.colorScheme.outline
                needleColor = UrgencyPalette[tracked.level]
                center = formatDistance(tracked.distanceM)
                sub = stringResource(
                    R.string.outside_radius,
                    formatDistance(engine.radiusM.toDouble()),
                )
            }
        }
        DistanceCompass(
            targetBearing = tracked?.bearing ?: 0f,
            progress = progress,
            color = color,
            needleColor = needleColor,
            centerText = center,
            subText = sub,
            centerContent = {
                Button(
                    onClick = {
                        tracked?.snapshot?.id?.let { viewModel.toggleMuted(it, true) }
                    },
                    enabled = tracked != null,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.found))
                }
            },
        )
        Spacer(Modifier.height(20.dp))

        if (engine.mutedLocations.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.muted_spots),
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
                        Text(stringResource(R.string.unmute))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/** Top-right settings gear for the hunt pages. */
@Composable
private fun SettingsGear(onOpenSettings: () -> Unit) {
    IconButton(onClick = onOpenSettings) {
        Icon(
            Icons.Filled.Settings,
            contentDescription = stringResource(R.string.cd_settings),
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * Dropdown menu of all spots of the current hunt, sorted by distance.
 * The closest spot is tracked by default; picking another spot tracks it
 * instead, and the first entry returns to following the nearest.
 * Long-pressing a spot disables it from the hunt; disabled spots stay in
 * the list (dimmed) and long-pressing them again re-enables them.
 * Rendered in the app's bottom bar while a hunt is active.
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun TrackingPicker(
    spots: List<HuntEngine.Nearest>,
    mutedSpots: List<HuntEngine.LocationSnapshot>,
    trackedId: Long?,
    tracked: HuntEngine.Nearest?,
    onSelect: (Long?) -> Unit,
    onToggleMute: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        MenuSelectField(
            label = stringResource(R.string.tracking_label),
            value = tracked?.let { it.snapshot.name }
                ?: if (mutedSpots.isNotEmpty()) stringResource(R.string.all_targets_muted)
                else stringResource(R.string.getting_location),
            enabled = spots.isNotEmpty() || mutedSpots.isNotEmpty(),
            expanded = expanded,
            onOpen = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(),
        ) {
            DropdownMenuItem(
                leadingIcon = {
                    CheckIcon(selected = trackedId == null)
                },
                text = { Text(stringResource(R.string.track_nearest_auto)) },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            spots.forEach { spot ->
                val source = remember(spot.snapshot.id) { MutableInteractionSource() }
                DropdownMenuItem(
                    leadingIcon = {
                        CheckIcon(selected = trackedId != null && trackedId == spot.snapshot.id)
                    },
                    text = {
                        Text(spot.snapshot.name + "  ·  " + formatDistance(spot.distanceM))
                    },
                    // The outer combinedClickable owns both gestures, so the
                    // item's own click stays empty.
                    onClick = {},
                    modifier = Modifier.combinedClickable(
                        interactionSource = source,
                        indication = null,
                        onClick = {
                            expanded = false
                            onSelect(spot.snapshot.id)
                        },
                        onLongClick = { onToggleMute(spot.snapshot.id, true) },
                    ),
                )
            }
            mutedSpots.forEach { spot ->
                // Disabled spots: dimmed, taps are inert — long-press re-enables.
                val source = remember(spot.id) { MutableInteractionSource() }
                DropdownMenuItem(
                    enabled = false,
                    text = {
                        Text(
                            spot.name,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = {},
                    modifier = Modifier.combinedClickable(
                        interactionSource = source,
                        indication = null,
                        onClick = {},
                        onLongClick = { onToggleMute(spot.id, false) },
                    ),
                )
            }
        }
    }
}

@Composable
private fun CheckIcon(selected: Boolean) {
    if (selected) {
        Icon(Icons.Filled.Check, contentDescription = null)
    }
}

