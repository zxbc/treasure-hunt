@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.treasurehunt.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.treasurehunt.app.R
import com.treasurehunt.app.data.AssetBitmaps
import com.treasurehunt.app.data.HuntListWithLocations
import com.treasurehunt.app.data.SpotImages
import com.treasurehunt.app.hunt.HuntEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    // Photos opened in the full-screen viewer, if any.
    var expandedImages by remember { mutableStateOf<SpotImages.Entry?>(null) }
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

        val spotImages = tracked?.let {
            SpotImages.forSpot(context, it.snapshot.lat, it.snapshot.lon)
        }
        if (tracked != null &&
            (tracked.snapshot.description.isNotBlank() || !spotImages?.files.isNullOrEmpty())
        ) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    tracked.snapshot.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (!spotImages?.files.isNullOrEmpty()) {
                    Spacer(Modifier.width(12.dp))
                    SpotThumbnail(
                        files = spotImages!!.files,
                        onClick = { expandedImages = spotImages },
                    )
                }
            }
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
        Spacer(Modifier.height(12.dp))

        // Muted spots are de-emphasized on purpose: they are a secondary
        // control, so they sit in a small, desaturated type below the compass.
        if (engine.mutedLocations.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.muted_spots),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            engine.mutedLocations.forEach { muted ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        muted.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = { viewModel.unmute(muted.id) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    ) {
                        Text(
                            stringResource(R.string.unmute),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        }

        expandedImages?.let { entry ->
            ImageViewerDialog(
                entry = entry,
                onDismiss = { expandedImages = null },
            )
        }
    }
}

/**
 * The small thumbnail beside a spot's description. Tapping it opens the
 * full-screen viewer with all of the spot's photos.
 */
@Composable
private fun SpotThumbnail(
    files: List<String>,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val path = files.first()
    val showLabel = stringResource(R.string.cd_show_images)
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.Default) {
            AssetBitmaps.decode(context, path, maxDim = 256)
        }
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .semantics { contentDescription = showLabel },
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Full-screen viewer for a spot's photos: a black backdrop with the photos
 * one per page (swipe between them when there are several), page dots and
 * the photo credit at the bottom. Tapping a photo — or the back button —
 * closes the viewer.
 */
@Composable
private fun ImageViewerDialog(
    entry: SpotImages.Entry,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        val pagerState = rememberPagerState { entry.files.size }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                SpotPageImage(
                    path = entry.files[page],
                    onClick = onDismiss,
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (entry.files.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        entry.files.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == pagerState.currentPage)
                                            Color.White
                                        else
                                            Color.White.copy(alpha = 0.35f),
                                    ),
                            )
                        }
                    }
                }
                val credit = entry.credits.getOrNull(pagerState.currentPage)
                if (!credit.isNullOrEmpty()) {
                    if (entry.files.size > 1) Spacer(Modifier.height(6.dp))
                    Text(
                        credit,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
            }
        }
    }
}

/** One page of the full-screen viewer: the photo scaled to fit; a tap closes. */
@Composable
private fun SpotPageImage(
    path: String,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val closeLabel = stringResource(R.string.cd_close_images)
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.Default) {
            AssetBitmaps.decode(context, path, maxDim = 1440)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = closeLabel },
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
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
 * The closest spot is tracked by default; tapping a spot tracks it
 * instead, and the first entry returns to following the nearest.
 * Each spot row carries a button on its right end that mutes the spot
 * from the hunt; muted spots stay in the list (dimmed) and the button
 * re-enables them. Rendered in the app's bottom bar while a hunt is active.
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
                SpotRow(
                    selected = trackedId != null && trackedId == spot.snapshot.id,
                    muted = false,
                    label = spot.snapshot.name + "  ·  " + formatDistance(spot.distanceM),
                    onRowClick = {
                        expanded = false
                        onSelect(spot.snapshot.id)
                    },
                    onToggleMute = {
                        onToggleMute(spot.snapshot.id, true)
                    },
                )
            }
            mutedSpots.forEach { spot ->
                // Muted spots: dimmed, the row itself is inert — the button re-enables.
                SpotRow(
                    selected = false,
                    muted = true,
                    label = spot.name,
                    onRowClick = {},
                    onToggleMute = {
                        onToggleMute(spot.id, false)
                    },
                )
            }
        }
    }
}

/**
 * One spot row in the tracking dropdown: the label area (check mark, name and
 * distance) selects the spot on tap, and the button on the right end mutes or
 * unmutes it. The two clickables are SIBLINGS, never nested: a clickable parent
 * consumes the down event before a clickable child could react, so the button
 * must live outside the row's own clickable area to keep both tappable.
 */
@Composable
private fun SpotRow(
    selected: Boolean,
    muted: Boolean,
    label: String,
    onRowClick: () -> Unit,
    onToggleMute: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minWidth = 112.dp, maxWidth = 280.dp, minHeight = 48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(enabled = !muted, onClick = onRowClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!muted) {
                // Constant 24dp slot so labels line up selected or not.
                Box(modifier = Modifier.size(24.dp)) {
                    CheckIcon(selected = selected)
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(
            onClick = onToggleMute,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = if (muted) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                contentDescription = stringResource(
                    if (muted) R.string.unmute else R.string.mute_spot,
                ),
            )
        }
    }
}

@Composable
private fun CheckIcon(selected: Boolean) {
    if (selected) {
        Icon(Icons.Filled.Check, contentDescription = null)
    }
}

