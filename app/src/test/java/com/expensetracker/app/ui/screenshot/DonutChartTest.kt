package com.expensetracker.app.ui.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.app.ui.components.DonutChart
import com.expensetracker.app.ui.components.DonutSlice
import com.expensetracker.app.ui.components.color
import com.expensetracker.core.model.Category
import org.junit.Test

/**
 * `DonutChart` draws straight onto a `Canvas`, so a layout assertion can only prove the chart
 * occupies the space it was given. The arc geometry — the inset by half the stroke, the -90°
 * start, the 1.5° gap between slices — is only visible in the golden, which is exactly why the
 * golden exists.
 */
class DonutChartTest : ScreenshotTest() {

    @Test
    fun light() = screenshot("DonutChart", ThemeMode.LIGHT) { Donut() }

    @Test
    fun dark() = screenshot("DonutChart", ThemeMode.DARK) { Donut() }

    /** No slices: only the track ring should be drawn, not an empty box. */
    @Test
    fun empty() = screenshot("DonutChart_empty", ThemeMode.LIGHT) {
        DonutChart(emptyList(), Modifier.size(180.dp))
    }

    /** One slice takes the no-gap branch, so the ring must close completely. */
    @Test
    fun singleSlice() = screenshot("DonutChart_singleSlice", ThemeMode.LIGHT) {
        DonutChart(listOf(DonutSlice(Category.FOOD.color, 1f)), Modifier.size(180.dp))
    }

    @Test
    fun thinStroke() = screenshot("DonutChart_thinStroke", ThemeMode.LIGHT) {
        DonutChart(SLICES, Modifier.size(180.dp), strokeWidth = 6.dp)
    }

    @Test
    fun `chart occupies exactly the size it is given`() {
        setBareContent { DonutChart(SLICES, Modifier.size(180.dp).testTag(DONUT)) }

        compose.onNodeWithTag(DONUT)
            .assertWidthIsEqualTo(180.dp)
            .assertHeightIsEqualTo(180.dp)
    }

    /** A wider-than-tall box must not stretch the ring into an ellipse or grow the layout. */
    @Test
    fun `a non-square box does not change the chart's measured bounds`() {
        setBareContent {
            DonutChart(SLICES, Modifier.size(width = 240.dp, height = 120.dp).testTag(DONUT))
        }

        compose.onNodeWithTag(DONUT)
            .assertWidthIsEqualTo(240.dp)
            .assertHeightIsEqualTo(120.dp)
    }

    @Test
    fun `an empty chart still reserves its full size`() {
        setBareContent { DonutChart(emptyList(), Modifier.size(180.dp).testTag(DONUT)) }

        compose.onNodeWithTag(DONUT)
            .assertWidthIsEqualTo(180.dp)
            .assertHeightIsEqualTo(180.dp)
    }

    private companion object {
        const val DONUT = "donut"
    }
}

/** Five shares that add to 1.0, ordered biggest first the way `Analytics.byCategory` returns them. */
private val SLICES = listOf(
    DonutSlice(Category.FOOD.color, 0.38f),
    DonutSlice(Category.SHOPPING.color, 0.24f),
    DonutSlice(Category.TRANSPORT.color, 0.18f),
    DonutSlice(Category.BILLS.color, 0.12f),
    DonutSlice(Category.OTHER.color, 0.08f),
)

@Composable
private fun Donut() = Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    DonutChart(SLICES, Modifier.size(180.dp))
    DonutChart(SLICES, Modifier.size(96.dp), strokeWidth = 10.dp)
}
