package com.expensetracker.app.ui.screenshot

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.app.ui.components.RangeChips
import com.expensetracker.core.analytics.DateRange
import com.expensetracker.core.analytics.RangePreset
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.Config
import java.time.LocalDate

class RangeChipsTest : ScreenshotTest() {

    @Test
    fun light() = screenshot("RangeChips", ThemeMode.LIGHT) { Chips() }

    @Test
    fun dark() = screenshot("RangeChips", ThemeMode.DARK) { Chips() }

    /** With CUSTOM selected the last chip's label becomes the formatted range, widening it. */
    @Test
    fun customSelected() = screenshot("RangeChips_customSelected", ThemeMode.LIGHT) {
        RangeChips(
            selected = RangePreset.CUSTOM,
            customRange = CUSTOM_RANGE,
            onSelect = {},
            onCustomRange = {},
        )
    }

    @Test
    fun `first chip starts at the 20dp content padding`() {
        setBareContent { Chips() }

        assertDp(20.dp, compose.onNodeWithText("This month").getUnclippedBoundsInRoot().left, "first chip left")
    }

    @Test
    fun `chips are separated by an 8dp gap`() {
        setBareContent { Chips() }

        val first = compose.onNodeWithText("This month").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText("3M").getUnclippedBoundsInRoot()
        val third = compose.onNodeWithText("6M").getUnclippedBoundsInRoot()

        assertDp(8.dp, second.left - first.right, "gap between chip 1 and 2")
        assertDp(8.dp, third.left - second.right, "gap between chip 2 and 3")
    }

    @Test
    fun `chips share one 32dp baseline height`() {
        setBareContent { Chips() }

        listOf("This month", "3M", "6M", "1Y").forEach {
            compose.onNodeWithText(it).assertHeightIsEqualTo(32.dp)
        }
    }

    @Test
    fun `chips sit on a single row with matching tops`() {
        setBareContent { Chips() }

        val first = compose.onNodeWithText("This month").getUnclippedBoundsInRoot()
        val fourth = compose.onNodeWithText("1Y").getUnclippedBoundsInRoot()

        assertDp(first.top, fourth.top, "chip top alignment")
    }

    @Test
    fun `only the selected preset reports itself as selected`() {
        setBareContent { Chips() }

        compose.onNodeWithText("This month").assertIsSelected()
        compose.onNodeWithText("3M").assertIsNotSelected()
        compose.onNodeWithText("6M").assertIsNotSelected()
    }

    @Test
    fun `a custom content padding moves the first chip`() {
        setBareContent {
            RangeChips(
                selected = RangePreset.THIS_MONTH,
                customRange = null,
                onSelect = {},
                onCustomRange = {},
                contentPadding = PaddingValues(horizontal = 4.dp),
            )
        }

        assertDp(4.dp, compose.onNodeWithText("This month").getUnclippedBoundsInRoot().left, "first chip left")
    }

    // ---- Large font scale -------------------------------------------------------------------
    //
    // The chips sit in a LazyRow, so at a large font scale they grow and push the later presets
    // off-screen rather than wrapping. The horizontal rhythm must survive that.

    @Test
    @Config(fontScale = 2.0f)
    fun largeFontLight() = screenshot("RangeChips_fontScale2", ThemeMode.LIGHT) { Chips() }

    @Test
    @Config(fontScale = 2.0f)
    fun largeFontDark() = screenshot("RangeChips_fontScale2", ThemeMode.DARK) { Chips() }

    @Test
    @Config(fontScale = 2.0f)
    fun `at 2x the chips keep their 20dp inset and 8dp gaps`() {
        setBareContent { Chips() }

        val first = compose.onNodeWithText("This month").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText("3M").getUnclippedBoundsInRoot()
        val third = compose.onNodeWithText("6M").getUnclippedBoundsInRoot()

        assertDp(20.dp, first.left, "first chip left at 2x")
        assertDp(8.dp, second.left - first.right, "gap between chip 1 and 2 at 2x")
        assertDp(8.dp, third.left - second.right, "gap between chip 2 and 3 at 2x")
    }

    /**
     * `FilterChip`'s 32dp default height is a minimum, not a cap: the chip must grow with its
     * label rather than clipping it. If a future Material version starts enforcing a fixed
     * height, this catches it.
     */
    @Test
    fun `at 2x the chips grow taller than the 32dp default`() {
        setBareContent { Chips() }

        val first = compose.onNodeWithText("This month").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText("3M").getUnclippedBoundsInRoot()

        assertTrue("chip height ${first.height} should exceed the 32dp default at 2x", first.height > 32.dp)
        assertDp(first.height, second.height, "all chips share one height at 2x")
    }

    @Test
    @Config(fontScale = 2.0f)
    fun `at 2x the chips still sit on a single row`() {
        setBareContent { Chips() }

        val first = compose.onNodeWithText("This month").getUnclippedBoundsInRoot()
        val third = compose.onNodeWithText("6M").getUnclippedBoundsInRoot()

        assertDp(first.top, third.top, "chip top alignment at 2x")
    }

    private companion object {
        val CUSTOM_RANGE = DateRange(LocalDate.of(2025, 2, 3), LocalDate.of(2025, 3, 14))
    }
}

@androidx.compose.runtime.Composable
private fun Chips() = RangeChips(
    selected = RangePreset.THIS_MONTH,
    customRange = null,
    onSelect = {},
    onCustomRange = {},
)
