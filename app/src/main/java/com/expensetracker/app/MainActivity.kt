package com.expensetracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.ui.AppRoot
import com.expensetracker.app.ui.theme.ExpenseTheme

class MainActivity : ComponentActivity() {

    private val container get() = (application as ExpenseApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by container.prefs.themeMode.collectAsStateWithLifecycle()
            ExpenseTheme(themeMode) {
                AppRoot(container)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Catch up on anything that arrived while the app was closed (only newer messages are read).
        if (container.prefs.onboardingDone.value) container.importer.start()
    }
}
