package com.expensetracker.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/** Small local settings. Stored in app-private storage and excluded from backups. */
class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _onboardingDone = MutableStateFlow(sp.getBoolean(KEY_ONBOARDING, false))
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    private val _autoTracking = MutableStateFlow(sp.getBoolean(KEY_AUTO_TRACKING, true))
    val autoTracking: StateFlow<Boolean> = _autoTracking.asStateFlow()

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(sp.getString(KEY_THEME, null) ?: "") }.getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setOnboardingDone(done: Boolean) {
        sp.edit().putBoolean(KEY_ONBOARDING, done).apply()
        _onboardingDone.value = done
    }

    fun setAutoTracking(enabled: Boolean) {
        sp.edit().putBoolean(KEY_AUTO_TRACKING, enabled).apply()
        _autoTracking.value = enabled
    }

    fun setThemeMode(mode: ThemeMode) {
        sp.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    /** Date (epoch millis) of the newest inbox SMS already processed. */
    var lastImportedSmsDate: Long
        get() = sp.getLong(KEY_LAST_SMS, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_SMS, value).apply()

    fun clear() {
        sp.edit().clear().apply()
        _onboardingDone.value = false
        _autoTracking.value = true
        _themeMode.value = ThemeMode.SYSTEM
    }

    private companion object {
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_AUTO_TRACKING = "auto_tracking"
        const val KEY_THEME = "theme_mode"
        const val KEY_LAST_SMS = "last_imported_sms_date"
    }
}
