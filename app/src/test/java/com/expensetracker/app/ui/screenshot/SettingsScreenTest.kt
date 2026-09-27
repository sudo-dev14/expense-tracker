package com.expensetracker.app.ui.screenshot

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.expensetracker.app.AppContainer
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.app.ui.settings.SettingsScreen
import com.expensetracker.app.ui.theme.ExpenseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * `AppCard(padding = 0.dp)` hands the horizontal inset to its caller, so that a card holding
 * several rows can run its dividers to the card's edge while the rows themselves stay inset.
 * `NavRow` sets only vertical padding and relies on that caller entirely.
 *
 * The Language card forgot, and its text sat flush against the card border while every other
 * card was inset by 16dp. Comparing the two cards against each other, rather than asserting a
 * literal offset, keeps this honest if the inset is ever retuned.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the language row is inset like every other card row`() {
        val container = AppContainer(ApplicationProvider.getApplicationContext())
        compose.setContent {
            ExpenseTheme(ThemeMode.LIGHT) {
                SettingsScreen(container, onOpenExport = {}, onOpenRules = {})
            }
        }

        val export = compose.onNodeWithText("Export report").getUnclippedBoundsInRoot()
        val language = compose.onNodeWithText("Phone language").performScrollTo().getUnclippedBoundsInRoot()

        assertDpEqual(export.left, language.left, "language row left edge vs the card above it")
    }

    private fun assertDpEqual(expected: androidx.compose.ui.unit.Dp, actual: androidx.compose.ui.unit.Dp, what: String) {
        org.junit.Assert.assertTrue(
            "$what: expected $expected but was $actual",
            kotlin.math.abs((actual - expected).value) <= 1f,
        )
    }
}
