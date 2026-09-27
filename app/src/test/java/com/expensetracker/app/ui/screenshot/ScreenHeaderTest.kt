package com.expensetracker.app.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.app.ui.components.ScreenHeader
import org.junit.Test

class ScreenHeaderTest : ScreenshotTest() {

    @Test
    fun light() = screenshot("ScreenHeader", ThemeMode.LIGHT) { ScreenHeader("Insights") }

    @Test
    fun dark() = screenshot("ScreenHeader", ThemeMode.DARK) { ScreenHeader("Insights") }

    @Test
    fun `long title keeps the badge on screen`() =
        screenshot("ScreenHeader_longTitle", ThemeMode.LIGHT) { ScreenHeader("Spending by category") }

    @Test
    fun `title is inset 20dp and the badge is flush to the opposite 20dp margin`() {
        setBareContent { Box(Modifier.testTag(HEADER)) { ScreenHeader("Insights") } }

        val header = compose.onNodeWithTag(HEADER).getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText("Insights").getUnclippedBoundsInRoot()
        val badge = compose.onNodeWithContentDescription(OFFLINE_DESCRIPTION).getUnclippedBoundsInRoot()

        assertDp(header.left + 20.dp, title.left, "title left inset")
        assertDp(header.right - 20.dp, badge.right, "badge right inset")
        // The title takes weight(1f), so the badge must never be pushed off the right edge.
        assertDp(rootWidth(), header.right - header.left, "header fills its parent's width")
    }

    @Test
    fun `title and badge share a centre line`() {
        setBareContent { Box(Modifier.testTag(HEADER)) { ScreenHeader("Insights") } }

        val title = compose.onNodeWithText("Insights").getUnclippedBoundsInRoot()
        val badge = compose.onNodeWithContentDescription(OFFLINE_DESCRIPTION).getUnclippedBoundsInRoot()

        assertDp(
            title.top + title.height / 2,
            badge.top + badge.height / 2,
            "title and badge vertical centres",
        )
        compose.onNodeWithContentDescription(OFFLINE_DESCRIPTION).assertHeightIsEqualTo(32.dp)
    }

    @Test
    fun `header height is the title plus 12dp of padding above and below`() {
        setBareContent { Box(Modifier.testTag(HEADER)) { ScreenHeader("Insights") } }

        val header = compose.onNodeWithTag(HEADER).getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText("Insights").getUnclippedBoundsInRoot()

        assertDp(title.height + 24.dp, header.height, "header height")
    }

    private companion object {
        const val HEADER = "header"
        const val OFFLINE_DESCRIPTION = "Offline. Your data stays on this phone."
    }
}
