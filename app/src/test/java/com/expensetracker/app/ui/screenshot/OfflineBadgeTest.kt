package com.expensetracker.app.ui.screenshot

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.app.ui.components.OfflineBadge
import org.junit.Test

class OfflineBadgeTest : ScreenshotTest() {

    @Test
    fun light() = screenshot("OfflineBadge", ThemeMode.LIGHT) { OfflineBadge() }

    @Test
    fun dark() = screenshot("OfflineBadge", ThemeMode.DARK) { OfflineBadge() }

    @Test
    fun `pill is 32dp tall and labelled for screen readers`() {
        setBareContent { OfflineBadge() }

        compose.onNodeWithContentDescription(OFFLINE_DESCRIPTION)
            .assertIsDisplayed()
            .assertHeightIsEqualTo(32.dp)
    }

    @Test
    fun `label sits inside the pill with 12dp of horizontal padding after the icon`() {
        setBareContent { OfflineBadge() }

        val pill = compose.onNodeWithContentDescription(OFFLINE_DESCRIPTION).getUnclippedBoundsInRoot()
        val label = compose.onNodeWithText("Offline").getUnclippedBoundsInRoot()

        // Icon (14dp) then a 6dp gap, all inside 12dp of padding: label.left = pill.left + 12 + 14 + 6.
        assertDp(pill.left + 32.dp, label.left, "label left edge")
        assertDp(pill.right - 12.dp, label.right, "label right edge")
        // Vertically centred in the 32dp pill.
        assertDp(
            pill.top + (pill.height - label.height) / 2,
            label.top,
            "label vertical centring",
        )
    }

    private companion object {
        const val OFFLINE_DESCRIPTION = "Offline. Your data stays on this phone."
    }
}
