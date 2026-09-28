@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.treasurehunt.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasurehunt.app.LocalePrefs
import com.treasurehunt.app.data.BuiltInLists
import com.treasurehunt.app.hunt.HuntEngine

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocalePrefs.withLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HuntEngine.init(this)
        BuiltInLists.importAllIfNeeded(this)
        setContent {
            TreasureHuntTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot(viewModel: HuntViewModel = viewModel()) {
    val context = LocalContext.current
    var showLists by rememberSaveable { mutableStateOf(false) }
    var editorListId by rememberSaveable { mutableStateOf(-1L) } // -1 = editor hidden, 0 = new list
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var pendingListId by rememberSaveable { mutableStateOf(0L) }

    val engine by viewModel.engineState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        val locationOk = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        if (locationOk) {
            viewModel.startHunt(pendingListId)
        }
    }

    fun requestStart(listId: Long) {
        pendingListId = listId
        val needed = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val locationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (needed.size == 1 && locationGranted) {
            viewModel.startHunt(listId)
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    val showEditor = editorListId != -1L
    // The tracked spot for the bottom picker: the user's choice when one was
    // made, otherwise the closest active spot (top of the sorted list).
    val tracked = engine.spots.firstOrNull { it.snapshot.id == engine.trackedId }
        ?: engine.spots.firstOrNull()

    Scaffold(
        bottomBar = {
            // The tracking dropdown lives at the very bottom of the screen,
            // where the old hunt/lists navigation used to sit, shown while a
            // hunt is active.
            if (engine.isActive && !showEditor && !showLists) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                ) {
                    TrackingPicker(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        spots = engine.spots,
                        mutedSpots = engine.mutedLocations,
                        trackedId = engine.trackedId,
                        tracked = tracked,
                        onSelect = { viewModel.setTracked(it) },
                        onToggleMute = { id, muted -> viewModel.toggleMuted(id, muted) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                showEditor -> ListEditorScreen(
                    viewModel = viewModel,
                    listId = editorListId,
                    onBack = { editorListId = -1L },
                )
                showLists -> ListsScreen(
                    viewModel = viewModel,
                    onOpenEditor = { id -> editorListId = id },
                    onRequestStart = { requestStart(it) },
                    onBack = { showLists = false },
                )
                else -> HuntScreen(
                    viewModel = viewModel,
                    engine = engine,
                    onRequestStart = { requestStart(it) },
                    onOpenSettings = { showSettings = true },
                )
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            radius = engine.radiusM,
            huntActive = engine.isActive,
            onManageLists = {
                showSettings = false
                showLists = true
            },
            onStopHunt = { viewModel.stopHunt() },
            onDismiss = { showSettings = false },
        )
    }
}
