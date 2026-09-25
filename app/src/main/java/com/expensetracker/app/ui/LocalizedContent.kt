package com.expensetracker.app.ui

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.expensetracker.app.AppLanguage
import com.expensetracker.app.LanguageManager

/**
 * Shows [content] in [language] without recreating the activity. Android 13+ already hands the
 * activity the new locale; older versions get resources switched to the chosen language.
 */
@Composable
fun LocalizedContent(language: AppLanguage, content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT >= 33) {
        content()
        return
    }
    val base = LocalContext.current
    val systemConfig = LocalConfiguration.current
    val localized = remember(language, base, systemConfig) { LanguageManager.localizedContext(base, language) }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        content = content,
    )
}
