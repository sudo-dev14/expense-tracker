package com.expensetracker.app.ui.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.app.ui.components.TransactionRow
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionRowTest : ScreenshotTest() {

    @Test
    fun light() = screenshot("TransactionRow", ThemeMode.LIGHT) { SampleRows() }

    @Test
    fun dark() = screenshot("TransactionRow", ThemeMode.DARK) { SampleRows() }

    @Test
    fun `avatar centre sits 40dp in, which is the 20dp margin plus half the 40dp square`() {
        setBareContent { TransactionRow(Fixtures.transaction(), onClick = {}, Modifier.testTag(ROW)) }

        val initials = compose.onNodeWithText("BT", useUnmergedTree = true).getUnclippedBoundsInRoot()

        assertDp(40.dp, initials.left + initials.width / 2, "avatar centre")
    }

    @Test
    fun `merchant name starts after the avatar and the 12dp gap`() {
        setBareContent { TransactionRow(Fixtures.transaction(), onClick = {}, Modifier.testTag(ROW)) }

        val merchant = compose.onNodeWithText("Blue Tokai Coffee", useUnmergedTree = true).getUnclippedBoundsInRoot()

        // 20dp margin + 40dp avatar + 12dp Arrangement.spacedBy.
        assertDp(72.dp, merchant.left, "merchant text left")
    }

    @Test
    fun `subtitle is left-aligned with the merchant name directly beneath it`() {
        setBareContent { TransactionRow(Fixtures.transaction(), onClick = {}, Modifier.testTag(ROW)) }

        val merchant = compose.onNodeWithText("Blue Tokai Coffee", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val subtitle = compose.onNodeWithText("Food & dining · HDFC ••4821 · UPI", useUnmergedTree = true).getUnclippedBoundsInRoot()

        assertDp(merchant.left, subtitle.left, "subtitle left alignment")
        assertDp(merchant.bottom, subtitle.top, "subtitle directly below the merchant name")
    }

    @Test
    fun `amount is flush to the 20dp right margin`() {
        setBareContent { TransactionRow(Fixtures.transaction(), onClick = {}, Modifier.testTag(ROW)) }

        val amount = compose.onNodeWithText("−₹485.50", useUnmergedTree = true).getUnclippedBoundsInRoot()

        assertDp(rootWidth() - 20.dp, amount.right, "amount right edge")
    }

    @Test
    fun `row is 20dp taller than its tallest child, from the 10dp vertical padding`() {
        setBareContent { TransactionRow(Fixtures.transaction(), onClick = {}, Modifier.testTag(ROW)) }

        val row = compose.onNodeWithTag(ROW).getUnclippedBoundsInRoot()
        val merchant = compose.onNodeWithText("Blue Tokai Coffee", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val subtitle = compose.onNodeWithText("Food & dining · HDFC ••4821 · UPI", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val tallest = maxOf(40.dp, subtitle.bottom - merchant.top)

        assertDp(tallest + 20.dp, row.height, "row height")
    }

    @Test
    fun `amount keeps its place when the merchant name is long enough to ellipsize`() {
        setBareContent { TransactionRow(Fixtures.longNameTransaction(), onClick = {}, Modifier.testTag(ROW)) }

        val amount = compose.onNodeWithText("−₹485.50", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val merchant = compose
            .onNodeWithText("Kumar Brothers Provision Store and General Merchants", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        assertDp(rootWidth() - 20.dp, amount.right, "amount right edge under pressure")
        // weight(1f) on the middle column must yield to the amount, not overlap it.
        assertTrue(
            "merchant text (right=${merchant.right}) overlaps the amount (left=${amount.left})",
            merchant.right <= amount.left,
        )
    }

    @Test
    fun `a credit renders a plus sign and a debit a minus sign`() {
        setBareContent {
            Column {
                TransactionRow(Fixtures.transaction(), onClick = {}, Modifier.testTag(ROW))
                TransactionRow(Fixtures.credit(), onClick = {}, Modifier.testTag("credit"))
            }
        }

        compose.onNodeWithText("−₹485.50", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("+₹75,000", useUnmergedTree = true).assertIsDisplayed()
    }

    private companion object {
        const val ROW = "row"
    }
}

@Composable
private fun SampleRows() = Column {
    TransactionRow(Fixtures.transaction(), onClick = {})
    TransactionRow(Fixtures.credit(), onClick = {})
    TransactionRow(Fixtures.longNameTransaction(), onClick = {})
    TransactionRow(Fixtures.sparseTransaction(), onClick = {})
}
