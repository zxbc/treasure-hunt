package com.treasurehunt.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Hunt lists bundled with the APK as assets.
 *
 * Each asset is imported exactly once, on first launch, through the same
 * [JsonImport] parser used by manual file imports. A per-asset flag in
 * SharedPreferences records the import, so if the user deliberately deletes
 * a built-in list it stays deleted on subsequent launches (and a failed
 * import is retried on the next launch).
 */
object BuiltInLists {

    /** Built-in list assets to import on first launch. */
    val ASSETS = listOf("kolodko-mini-statues.json")

    private const val PREFS = "built_in_lists"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Import every built-in asset that has not been imported yet. */
    fun importAllIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        for (asset in ASSETS) {
            val flag = "imported_" + asset
            if (prefs.getBoolean(flag, false)) continue
            scope.launch {
                try {
                    val text = context.assets.open(asset).bufferedReader().use { it.readText() }
                    val parsed = JsonImport.parse(text, defaultListName = asset.removeSuffix(".json"))
                    val repo = Repository(HuntDatabase.get(context))
                    repo.insert(parsed.name, parsed.locations)
                } catch (e: Exception) {
                    // A broken asset must never crash the app; stay silent and
                    // retry on the next launch instead of marking it imported.
                    return@launch
                }
                prefs.edit().putBoolean(flag, true).apply()
            }
        }
    }
}
