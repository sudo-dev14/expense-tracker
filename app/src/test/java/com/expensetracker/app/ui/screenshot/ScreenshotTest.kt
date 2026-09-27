package com.expensetracker.app.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.app.ui.theme.ExpenseTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Base class for the component screenshot + placement suite.
 *
 * Compose is rendered to a real bitmap on the JVM by Robolectric's native graphics backend, so
 * these run as ordinary unit tests with no emulator: `./gradlew :app:verifyRoborazziDebug`.
 *
 * Two kinds of assertion live side by side, and both matter:
 *  - [screenshot] records or verifies a golden PNG for a component in one theme.
 *  - [setBareContent] plus the `assert*Position*`/`assert*IsEqualTo` helpers check that a
 *    component is laid out where it is supposed to be, which a pixel diff alone cannot tell you
 *    apart from a deliberate redesign.
 *
 * Goldens are recorded on Linux CI only — see `app/src/test/README.md`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * Renders [content] on the theme background inside a small margin and captures it.
     * The margin keeps card borders and shadows off the image edge; placement tests use
     * [setBareContent] instead so their expected offsets are the component's own.
     */
    protected fun screenshot(name: String, mode: ThemeMode, content: @Composable () -> Unit) {
        compose.setContent {
            ExpenseTheme(mode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxWidth().padding(FRAME)) { content() }
                }
            }
        }
        compose.onRoot().captureRoboImage("$GOLDEN_DIR/${name}_${mode.name.lowercase()}.png")
    }

    /** Renders [content] with the root at the component's own bounds, for placement assertions. */
    protected fun setBareContent(mode: ThemeMode = ThemeMode.LIGHT, content: @Composable () -> Unit) {
        compose.setContent {
            ExpenseTheme(mode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxWidth()) { content() }
                }
            }
        }
    }

    protected fun rootWidth(): Dp = compose.onRoot().getUnclippedBoundsInRoot().width

    /**
     * Dp equality with a tolerance, for distances derived from two `getBoundsInRoot()` reads.
     * Rounding to whole pixels at xhdpi (2dp granularity is 0.5dp) means derived gaps land
     * within half a dp of the value in the source.
     */
    protected fun assertDp(expected: Dp, actual: Dp, what: String, tolerance: Dp = 1.dp) {
        assertTrue(
            "$what: expected $expected (±$tolerance) but was $actual",
            abs((actual - expected).value) <= tolerance.value,
        )
    }

    private companion object {
        /** Relative to the module directory, which is the working directory for unit tests. */
        const val GOLDEN_DIR = "src/test/screenshots"
        val FRAME = 12.dp
    }
}
