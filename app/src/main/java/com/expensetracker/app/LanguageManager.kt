package com.expensetracker.app

import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * In-app language choice, applied without restarting the screen:
 * - Android 13+: per-app languages built into the system (also reachable from system Settings,
 *   via locales_config.xml). MainActivity handles the locale change itself (configChanges in the
 *   manifest), so Compose just redraws the text.
 * - Older versions: the choice is saved here and applied through [localizedContext], which the
 *   UI provides as LocalContext. [wrap] applies it on a cold start.
 */
object LanguageManager {
    private const val PREFS = "settings"
    private const val KEY = "app_language"

    private val _language = MutableStateFlow(AppLanguage.SYSTEM)
    private var loaded = false

    /** The current choice, updated as soon as it changes. */
    fun language(context: Context): StateFlow<AppLanguage> {
        if (!loaded) {
            _language.value = readCurrent(context)
            loaded = true
        }
        return _language.asStateFlow()
    }

    /** Re-reads the system setting, e.g. after the user changed it in Android's app settings. */
    fun refresh(context: Context) {
        _language.value = readCurrent(context)
        loaded = true
    }

    fun set(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                if (language == AppLanguage.SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(language.tag)
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.tag).apply()
            Locale.setDefault(localeFor(language))
        }
        _language.value = language
    }

    /** Below Android 13: returns [base] with the saved language applied (used on a cold start). */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val language = readCurrent(base)
        val locale = localeFor(language)
        // Date formatting reads the default locale, so keep it in step with the chosen language.
        Locale.setDefault(locale)
        if (language == AppLanguage.SYSTEM) return base
        return base.createConfigurationContext(configFor(base, locale))
    }

    /**
     * Below Android 13: [base] with its resources switched to [language]. Stays a ContextWrapper
     * around [base], so code that looks for the Activity still finds it.
     */
    fun localizedContext(base: Context, language: AppLanguage): Context {
        val resources = base.createConfigurationContext(configFor(base, localeFor(language))).resources
        return object : ContextWrapper(base) {
            override fun getResources(): Resources = resources
        }
    }

    private fun readCurrent(context: Context): AppLanguage =
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) AppLanguage.SYSTEM else AppLanguage.fromTag(locales[0].language)
        } else {
            AppLanguage.fromTag(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))
        }

    private fun localeFor(language: AppLanguage): Locale =
        if (language == AppLanguage.SYSTEM) Resources.getSystem().configuration.locales[0] else Locale.forLanguageTag(language.tag)

    private fun configFor(base: Context, locale: Locale) =
        Configuration(base.resources.configuration).apply { setLocale(locale) }
}
