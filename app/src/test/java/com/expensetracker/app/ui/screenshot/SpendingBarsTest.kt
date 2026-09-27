package com.expensetracker.app.ui.screenshot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.app.ui.components.SpendingBars
import com.expensetracker.core.analytics.Bucket
import org.junit.Test
import java.time.LocalDate

/**
 * `SpendingBars` is pure `Canvas` drawing: the baseline, the 0.7-of-slot bar width capped at
 * 28dp, the 2dp minimum height for tiny buckets and the amber highlight on the tallest bar all
 * live in the golden. The assertions here pin down the layout contract around that drawing.
 */
class SpendingBarsTest : ScreenshotTest() {

    @Test
    fun light() = screenshot("SpendingBars", ThemeMode.LIGHT) { Bars() }

    @Test
    fun dark() = screenshot("SpendingBars", ThemeMode.DARK) { Bars() }

    /** No buckets: the baseline is drawn and nothing else. */
    @Test
    fun empty() = screenshot("SpendingBars_empty", ThemeMode.LIGHT) {
        SpendingBars(emptyList(), Modifier.fillMaxWidth().height(120.dp))
    }

    /** Every bucket zero: `max == 0` takes the same early return as the empty list. */
    @Test
    fun allZero() = screenshot("SpendingBars_allZero", ThemeMode.LIGHT) {
        SpendingBars(
            List(5) { Bucket(LocalDate.of(2025, 2, 3).plusWeeks(it.toLong()), "W${it + 1}", 0L) },
            Modifier.fillMaxWidth().height(120.dp),
        )
    }

    /**
     * Few buckets means a wide slot, so the 28dp cap on bar width is what decides how they look.
     * Many buckets means a narrow slot, where 0.7 of the slot wins instead.
     */
    @Test
    fun fewBuckets() = screenshot("SpendingBars_fewBuckets", ThemeMode.LIGHT) {
        SpendingBars(Fixtures.buckets().take(2), Modifier.fillMaxWidth().height(120.dp))
    }

    @Test
    fun manyBuckets() = screenshot("SpendingBars_manyBuckets", ThemeMode.LIGHT) {
        val many = (0 until 30).map {
            Bucket(
                LocalDate.of(2025, 2, 1).plusDays(it.toLong()),
                "D${it + 1}",
                (200_00L + (it * 137_00L) % 1_800_00L),
            )
        }
        SpendingBars(many, Modifier.fillMaxWidth().height(120.dp))
    }

    /** A single tiny bucket next to a huge one exercises the 2dp minimum bar height. */
    @Test
    fun tinyBucketBesideHuge() = screenshot("SpendingBars_tinyBesideHuge", ThemeMode.LIGHT) {
        SpendingBars(
            listOf(
                Bucket(LocalDate.of(2025, 2, 3), "W1", 500_000_00L),
                Bucket(LocalDate.of(2025, 2, 10), "W2", 1_00L),
            ),
            Modifier.fillMaxWidth().height(120.dp),
        )
    }

    @Test
    fun `chart occupies exactly the box it is given`() {
        setBareContent {
            SpendingBars(Fixtures.buckets(), Modifier.fillMaxWidth().height(120.dp).testTag(BARS))
        }

        compose.onNodeWithTag(BARS).assertHeightIsEqualTo(120.dp)
        assertDp(rootWidth(), compose.onNodeWithTag(BARS).getUnclippedBoundsInRoot().width, "chart width")
    }

    @Test
    fun `an empty chart still reserves its full box`() {
        setBareContent { SpendingBars(emptyList(), Modifier.fillMaxWidth().height(120.dp).testTag(BARS)) }

        compose.onNodeWithTag(BARS).assertHeightIsEqualTo(120.dp)
        assertDp(rootWidth(), compose.onNodeWithTag(BARS).getUnclippedBoundsInRoot().width, "chart width")
    }

    @Test
    fun `bucket count does not change the measured bounds`() {
        setBareContent {
            SpendingBars(Fixtures.buckets().take(1), Modifier.size(width = 200.dp, height = 80.dp).testTag(BARS))
        }

        compose.onNodeWithTag(BARS)
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(80.dp)
    }

    private companion object {
        const val BARS = "bars"
    }
}

@Composable
private fun Bars() = SpendingBars(Fixtures.buckets(), Modifier.fillMaxWidth().height(120.dp))
