package com.treasurehunt.app

import android.content.Context
import java.util.Locale

/**
 * Per-app language selection.
 *
 * The chosen locale tag is persisted in SharedPreferences and applied to the
 * base context of every component that needs resources (activity, hunt
 * service), so all strings resolve in the selected language. The default is
 * Simplified Chinese. Changing the language recreates the activity, which
 * re-runs [attachBaseContext] with the new tag.
 */
object LocalePrefs {

    const val TAG_ENGLISH = "en"
    const val TAG_CHINESE = "zh-CN"

    /** Used when the user has not picked a language yet. */
    const val DEFAULT_TAG = TAG_CHINESE

    private const val PREFS = "locale_prefs"
    private const val KEY_TAG = "language"

    /** The stored locale tag, or [DEFAULT_TAG] when nothing is stored. */
    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TAG, DEFAULT_TAG) ?: DEFAULT_TAG

    fun set(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TAG, tag).apply()
    }

    fun locale(context: Context): Locale = Locale.forLanguageTag(current(context))

    /** Returns a context whose resources resolve in the selected language. */
    fun withLocale(context: Context): Context {
        val config = context.resources.configuration
        config.setLocale(locale(context))
        return context.createConfigurationContext(config)
    }
}
