package com.expensetracker.app.ui.screenshot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.annotation.Config

/**
 * Guards the font-scale mechanism itself.
 *
 * `@Config(fontScale = …)` sets `Configuration.fontScale`, which Compose reads into
 * `LocalDensity`. If a Robolectric upgrade ever stops propagating it, every large-font test below
 * would keep passing while silently rendering at 1.0 — testing nothing. These two assertions
 * fail loudly instead.
 */
class FontScaleHarnessTest : ScreenshotTest() {

    @Test
    fun `the default font scale reaches Compose as 1x`() {
        assertEquals(1.0f, observedFontScale(), 0.001f)
    }

    @Test
    @Config(fontScale = 2.0f)
    fun `a 2x font scale reaches Compose`() {
        assertEquals(2.0f, observedFontScale(), 0.001f)
    }

    private fun observedFontScale(): Float {
        var scale by mutableFloatStateOf(0f)
        compose.setContent { scale = LocalDensity.current.fontScale }
        compose.waitForIdle()
        return scale
    }
}
