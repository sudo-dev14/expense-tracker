package com.expensetracker.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.ui.AppRoot
import com.expensetracker.app.ui.LocalizedContent
import com.expensetracker.app.ui.theme.ExpenseTheme

class MainActivity : ComponentActivity() {

    private val container get() = (application as ExpenseApp).container

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate: shows the Kharcha splash, then switches to the app theme.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by container.prefs.themeMode.collectAsStateWithLifecycle()
            val language by LanguageManager.language(this).collectAsStateWithLifecycle()
            LocalizedContent(language) {
                ExpenseTheme(themeMode) {
                    AppRoot(container)
                }
            }
        }
    }

    // The manifest routes locale changes here instead of recreating the activity, so switching
    // language just redraws the text in place.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LanguageManager.refresh(this)
    }

    override fun onResume() {
        super.onResume()
        // Catch up on anything that arrived while the app was closed (only newer messages are read).
        if (container.prefs.onboardingDone.value) container.importer.start()
    }
}
