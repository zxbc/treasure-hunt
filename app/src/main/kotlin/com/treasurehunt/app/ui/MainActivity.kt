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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasurehunt.app.LocalePrefs
import com.treasurehunt.app.R
import com.treasurehunt.app.data.BuiltInLists
import com.treasurehunt.app.hunt.HuntEngine

enum class Screen { HUNT, LISTS }

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
    var screen by rememberSaveable { mutableStateOf(Screen.HUNT) }
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

    Scaffold(
        bottomBar = {
            if (!showEditor) {
                NavigationBar {
                    val huntLabel = stringResource(R.string.nav_hunt)
                    val listsLabel = stringResource(R.string.nav_lists)
                    NavigationBarItem(
                        selected = screen == Screen.HUNT,
                        onClick = { screen = Screen.HUNT },
                        icon = { Icon(Icons.Filled.Map, contentDescription = huntLabel) },
                        label = { Text(huntLabel) },
                    )
                    NavigationBarItem(
                        selected = screen == Screen.LISTS,
                        onClick = { screen = Screen.LISTS },
                        icon = { Icon(Icons.Filled.List, contentDescription = listsLabel) },
                        label = { Text(listsLabel) },
                    )
                }
            }
        },
    ) { padding ->
        Modifier.padding(padding)
        when {
            showEditor -> ListEditorScreen(
                viewModel = viewModel,
                listId = editorListId,
                onBack = { editorListId = -1L },
            )
            screen == Screen.LISTS -> ListsScreen(
                viewModel = viewModel,
                onOpenEditor = { id -> editorListId = id },
                onRequestStart = { requestStart(it) },
            )
            else -> HuntScreen(
                viewModel = viewModel,
                engine = engine,
                onRequestStart = { requestStart(it) },
                onOpenLists = { screen = Screen.LISTS },
                onOpenSettings = { showSettings = true },
            )
        }
    }

    if (showSettings) {
        SettingsSheet(radius = engine.radiusM, onDismiss = { showSettings = false })
    }
}
