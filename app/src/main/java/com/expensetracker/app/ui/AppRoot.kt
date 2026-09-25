package com.expensetracker.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensetracker.app.AppContainer
import com.expensetracker.app.ui.export.ExportScreen
import com.expensetracker.app.ui.home.HomeScreen
import com.expensetracker.app.ui.onboarding.OnboardingFlow
import com.expensetracker.app.ui.review.ReviewScreen
import com.expensetracker.app.ui.settings.RulesScreen
import com.expensetracker.app.ui.settings.SettingsScreen
import com.expensetracker.app.ui.transactions.EditTransactionSheet
import com.expensetracker.app.ui.transactions.TransactionsScreen

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val SETTINGS = "settings"
    const val REVIEW = "review"
    const val EXPORT = "export"
    const val RULES = "rules"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Outlined.Home),
    Tab(Routes.TRANSACTIONS, "Transactions", Icons.AutoMirrored.Outlined.ReceiptLong),
    Tab(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
)

/** Id of the transaction being edited; [NEW_TRANSACTION] opens the "add expense" sheet. */
const val NEW_TRANSACTION = -1L

@Composable
fun AppRoot(container: AppContainer) {
    val onboardingDone by container.prefs.onboardingDone.collectAsStateWithLifecycle()
    // Sets the default text/icon colour for the theme. Without it, screens outside the
    // Scaffold (onboarding) fall back to black text, which is unreadable in dark mode.
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        if (!onboardingDone) {
            OnboardingFlow(container, onFinished = { container.prefs.setOnboardingDone(true) })
        } else {
            MainScaffold(container)
        }
    }
}

@Composable
private fun MainScaffold(container: AppContainer) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }

    fun openTab(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (tabs.any { it.route == currentRoute }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { openTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    container = container,
                    onOpenTransaction = { editingId = it },
                    onAddExpense = { editingId = NEW_TRANSACTION },
                    onOpenReview = { nav.navigate(Routes.REVIEW) },
                    onSeeAll = { openTab(Routes.TRANSACTIONS) },
                    onOpenPrivacy = { openTab(Routes.SETTINGS) },
                )
            }
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(
                    container = container,
                    onOpenTransaction = { editingId = it },
                    onAddExpense = { editingId = NEW_TRANSACTION },
                    onOpenPrivacy = { openTab(Routes.SETTINGS) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    container = container,
                    onOpenExport = { nav.navigate(Routes.EXPORT) },
                    onOpenRules = { nav.navigate(Routes.RULES) },
                )
            }
            composable(Routes.REVIEW) {
                ReviewScreen(container, onBack = { nav.popBackStack() }, onEdit = { editingId = it })
            }
            composable(Routes.EXPORT) {
                ExportScreen(container, onBack = { nav.popBackStack() })
            }
            composable(Routes.RULES) {
                RulesScreen(container, onBack = { nav.popBackStack() })
            }
        }
    }

    editingId?.let { id ->
        EditTransactionSheet(container, transactionId = id, onDismiss = { editingId = null })
    }
}
