package com.expensetracker.app.ui.screenshot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.expensetracker.app.AppContainer
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.app.ui.NEW_TRANSACTION
import com.expensetracker.app.ui.theme.ExpenseTheme
import com.expensetracker.app.ui.transactions.EditTransactionSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The add/edit sheet, driven as the real screen rather than as an extracted component.
 *
 * Worth knowing for anything built on top of this: a real [AppContainer] constructs fine under
 * Robolectric. It only needs a Context, and the Room database, preferences, SMS reader and PDF
 * exporter it builds all work on the JVM so long as nothing queries the SMS provider. Screens do
 * not have to be refactored into stateless overloads to be testable.
 *
 * What is *not* reachable here is the sheet's dismissal. `ModalBottomSheet` renders in its own
 * window and its scrim never receives a synthetic click under Robolectric, so a tap on it does
 * nothing — the sheet is still displayed afterwards. Every other exit either needs the async
 * save, which would make a timing assertion pass whether or not the sheet animates out, or a
 * back press, which `createComposeRule` has no activity to deliver. The exit animation is
 * therefore verified on a device, not here; see the note in app/src/test/README.md.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditTransactionSheetTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * "Add" has nothing to fetch, so its content exists on the frame the sheet first appears.
     * If it did not, the sheet would measure an empty body, begin rising to that height, and
     * start over when the real content arrived — the jump this screen used to have.
     */
    @Test
    fun `a new transaction has its full content on the first frame`() {
        val container = AppContainer(ApplicationProvider.getApplicationContext())
        compose.setContent {
            ExpenseTheme(ThemeMode.LIGHT) {
                EditTransactionSheet(container, NEW_TRANSACTION, onDismiss = {})
            }
        }

        compose.onNodeWithText("Add a transaction").assertIsDisplayed()
        compose.onNodeWithText("Spent").assertIsDisplayed()
        compose.onNodeWithText("Category").assertIsDisplayed()
        compose.onNodeWithText("Add").assertIsDisplayed()
    }
}
