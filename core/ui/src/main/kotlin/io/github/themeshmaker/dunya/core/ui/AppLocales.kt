/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import io.github.themeshmaker.dunya.core.model.AppLanguage
import java.util.Locale

/**
 * The app's own language, independent of the phone's.
 *
 * Android 13+ has per-app languages built in (also reachable from system Settings → Apps →
 * Deen → Language), so we use [LocaleManager]. Older versions store the choice here and the
 * Application and Activity wrap their context with it ([wrap]).
 */
object AppLocales {
    private const val PREFS = "deen_locale"
    private const val KEY_TAG = "tag"

    fun current(context: Context): AppLanguage {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales.takeIf { !it.isEmpty }?.get(0)
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, null)?.let { Locale.forLanguageTag(it) }
        } ?: return AppLanguage.SYSTEM
        return AppLanguage.entries.firstOrNull { language ->
            language.tag?.let { languageCode(Locale.forLanguageTag(it)) == languageCode(locale) } == true
        } ?: AppLanguage.SYSTEM
    }

    /** Java still reports a few languages by their old codes (Indonesian "in", Hebrew "iw"). */
    private fun languageCode(locale: Locale): String = when (val code = locale.language) {
        "in" -> "id"
        "iw" -> "he"
        "ji" -> "yi"
        else -> code
    }

    /** Applies [language]; on older Android the activity restarts to pick it up. */
    @SuppressLint("ApplySharedPref") // The restarted activity must read the new value: write it now.
    fun set(activity: Activity, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The system recreates the activities itself.
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TAG, language.tag).commit()
            activity.recreate()
        }
    }

    /** Android 12 and older: a context in the chosen language (unchanged if it follows the phone). */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        // Before the first unlock (direct boot, e.g. to play Fajr) credential storage is locked:
        // fall back to the phone's language rather than crash.
        val tag = try {
            base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, null)
        } catch (_: IllegalStateException) {
            null
        } ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }
}

/** The activity behind a (possibly wrapped) context, e.g. to change the app's language. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
