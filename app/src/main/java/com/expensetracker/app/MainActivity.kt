package com.expensetracker.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.ui.AppRoot
import com.expensetracker.app.ui.LocalizedContent
import com.expensetracker.app.ui.theme.ExpenseTheme
import com.expensetracker.app.ui.theme.isDarkTheme

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
            val dark = themeMode.isDarkTheme()

            // The window is not part of the composition, so it has to be told. Without this the
            // bar icons and the background behind Compose follow the system night setting: a user
            // on a light phone who picks Dark gets a white flash on every cold start and dark
            // icons on a dark status bar for the whole session.
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = systemBarStyle(dark),
                    navigationBarStyle = systemBarStyle(dark),
                )
                val background = ContextCompat.getColor(
                    this,
                    if (dark) R.color.window_background_dark else R.color.window_background_light,
                )
                window.setBackgroundDrawable(ColorDrawable(background))
            }

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

    /** Transparent bars; only the icon contrast changes with the theme. */
    private fun systemBarStyle(dark: Boolean) =
        if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)

    override fun onResume() {
        super.onResume()
        // Catch up on anything that arrived while the app was closed (only newer messages are read).
        if (container.prefs.onboardingDone.value) container.importer.start()
    }
}
