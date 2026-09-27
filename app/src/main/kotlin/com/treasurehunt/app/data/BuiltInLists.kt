package com.treasurehunt.app.data

import android.content.Context
import com.treasurehunt.app.LocalePrefs
import com.treasurehunt.app.hunt.HuntEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Hunt lists bundled with the APK as assets.
 *
 * Each asset is imported exactly once, on first launch, through the same
 * [JsonImport] parser used by manual file imports. A per-asset flag in
 * SharedPreferences records the import, so if the user deliberately deletes
 * a built-in list it stays deleted on subsequent launches (and a failed
 * import is retried on the next launch).
 *
 * The kolodko hunt ships in two languages — the original English list and a
 * Chinese translation with identical coordinates. After the imports, the
 * remembered default hunt (what the hunt picker pre-selects) is pointed at
 * the list matching the app's current language, and re-applied whenever the
 * user switches language.
 */
object BuiltInLists {

    /** List names exactly as declared in the assets' "name" fields. */
    const val KOLODKO_EN_NAME = "Kolodko Mini Statues — Budapest"
    const val KOLODKO_ZH_NAME = "科尔杜科迷你雕像 — 布达佩斯"

    private data class Asset(val file: String, val flag: String, val name: String)

    private val ASSETS = listOf(
        Asset("kolodko-mini-statues.json", "imported_kolodko_en", KOLODKO_EN_NAME),
        Asset("kolodko-mini-statues-zh.json", "imported_kolodko_zh", KOLODKO_ZH_NAME),
    )

    /**
     * Flag from the single-asset era, before the Chinese asset existed. Its
     * meaning depends on when the device first launched: devices from that
     * era already hold one of the two lists under this flag, so the database
     * itself decides which asset still needs importing.
     */
    private const val LEGACY_FLAG = "imported_kolodko-mini-statues.json"

    private const val PREFS = "built_in_lists"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Import every built-in asset not yet on this device, then align the remembered default hunt with the app language. */
    fun importAllIfNeeded(context: Context) {
        scope.launch {
            try {
                val repo = Repository(HuntDatabase.get(context))
                importMissing(context, repo)
                applyLanguageDefault(context, repo)
            } catch (e: Exception) {
                // A broken asset must never crash the app; stay silent and
                // retry on the next launch instead of marking it imported.
            }
        }
    }

    /** Re-align the remembered default hunt with the app language (called on language switch). */
    fun applyLanguageDefault(context: Context) {
        scope.launch {
            try {
                applyLanguageDefault(context, Repository(HuntDatabase.get(context)))
            } catch (e: Exception) {
                // Never let a settings action fail.
            }
        }
    }

    private suspend fun importMissing(context: Context, repo: Repository) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        for (asset in ASSETS) {
            if (isImported(prefs, repo, asset)) continue
            try {
                val text = context.assets.open(asset.file).bufferedReader().use { it.readText() }
                val parsed = JsonImport.parse(context, text, defaultListName = asset.file.removeSuffix(".json"))
                repo.insert(parsed.name, parsed.locations)
            } catch (e: Exception) {
                // Stay silent; retry on the next launch without marking it imported.
                continue
            }
            prefs.edit().putBoolean(asset.flag, true).apply()
        }
    }

    private suspend fun isImported(
        prefs: android.content.SharedPreferences,
        repo: Repository,
        asset: Asset,
    ): Boolean {
        if (prefs.getBoolean(asset.flag, false)) return true
        if (!prefs.getBoolean(LEGACY_FLAG, false)) return false
        // Single-asset era: a list with this asset's name already exists.
        return repo.lists.first().any { it.name == asset.name }
    }

    /**
     * Point the remembered default hunt (what the hunt picker pre-selects) at
     * the kolodko list matching the app language. Only overrides a default
     * that is unset or itself a kolodko list, so a hunt the user explicitly
     * chose is kept.
     */
    private suspend fun applyLanguageDefault(context: Context, repo: Repository) {
        val lists = repo.lists.first()
        val (wanted, other) = if (LocalePrefs.current(context) == LocalePrefs.TAG_CHINESE)
            KOLODKO_ZH_NAME to KOLODKO_EN_NAME
        else
            KOLODKO_EN_NAME to KOLODKO_ZH_NAME
        val desired = lists.firstOrNull { it.name == wanted } ?: return
        val remembered = HuntEngine.activeListFromPrefs()
        if (remembered == desired.id) return
        val otherId = lists.firstOrNull { it.name == other }?.id
        if (remembered != 0L && remembered != otherId) return
        HuntEngine.saveActiveList(desired.id)
    }
}
