package com.expensetracker.app

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import java.util.Locale

enum class AppLanguage(val tag: String, @StringRes val labelRes: Int) {
    SYSTEM("", R.string.lang_system),
    ENGLISH("en", R.string.lang_english),
    HINDI("hi", R.string.lang_hindi);

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it != SYSTEM && it.tag == tag?.substringBefore('-') } ?: SYSTEM
    }
}

/**
 * In-app language choice. Android 13+ has per-app languages built in (also reachable from
 * system Settings, via locales_config.xml); older versions store the choice here and apply it
 * in [MainActivity.attachBaseContext].
 */
object LanguageManager {
    private const val PREFS = "settings"
    private const val KEY = "app_language"

    fun current(context: Context): AppLanguage =
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) AppLanguage.SYSTEM else AppLanguage.fromTag(locales[0].language)
        } else {
            AppLanguage.fromTag(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))
        }

    fun set(activity: Activity, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= 33) {
            // The system saves the choice and recreates the activity itself.
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (language == AppLanguage.SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(language.tag)
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.tag).apply()
            activity.recreate()
        }
    }

    /** Below Android 13: returns [base] with the saved language applied. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val language = AppLanguage.fromTag(base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))
        // Date formatting reads the default locale, so keep it in step with the chosen language.
        if (language == AppLanguage.SYSTEM) {
            Locale.setDefault(Resources.getSystem().configuration.locales[0])
            return base
        }
        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }
}
