package com.expensetracker.app.ui.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.app.ui.components.MerchantAvatar
import com.expensetracker.core.model.Category
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.Config

class MerchantAvatarTest : ScreenshotTest() {

    @Test
    fun light() = screenshot("MerchantAvatar", ThemeMode.LIGHT) { AvatarRow() }

    /** The tint alpha differs by theme (0.28 dark, 0.14 light), so both goldens earn their keep. */
    @Test
    fun dark() = screenshot("MerchantAvatar", ThemeMode.DARK) { AvatarRow() }

    @Test
    fun `default avatar is a 40dp square`() {
        setBareContent { Box(Modifier.testTag(AVATAR)) { MerchantAvatar("Blue Tokai", Category.FOOD) } }

        compose.onNodeWithTag(AVATAR)
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun `size override scales the square`() {
        setBareContent {
            Box(Modifier.testTag(AVATAR)) { MerchantAvatar("Blue Tokai", Category.FOOD, size = 56.dp) }
        }

        compose.onNodeWithTag(AVATAR)
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun `initials sit at the centre of the square`() {
        setBareContent { Box(Modifier.testTag(AVATAR)) { MerchantAvatar("Blue Tokai", Category.FOOD) } }

        val box = compose.onNodeWithTag(AVATAR).getUnclippedBoundsInRoot()
        val text = compose.onNodeWithText("BT").getUnclippedBoundsInRoot()

        assertDp(box.left + box.width / 2, text.left + text.width / 2, "initials horizontal centre")
        assertDp(box.top + box.height / 2, text.top + text.height / 2, "initials vertical centre")
    }

    /**
     * The initials rule has three branches: two words take one letter each, a single long word
     * takes its first two letters, and an empty name falls back to "?".
     */
    @Test
    fun `initials cover the two-word, one-word and empty cases`() {
        setBareContent {
            Row {
                Box(Modifier.testTag("two")) { MerchantAvatar("Blue Tokai", Category.FOOD) }
                Box(Modifier.testTag("one")) { MerchantAvatar("Swiggy", Category.FOOD) }
                Box(Modifier.testTag("none")) { MerchantAvatar("", Category.OTHER) }
            }
        }

        compose.onNodeWithText("BT").assertIsDisplayed()
        compose.onNodeWithText("SW").assertIsDisplayed()
        compose.onNodeWithText("?").assertIsDisplayed()

        // Whatever the initials, the square keeps its size.
        listOf("two", "one", "none").forEach { tag ->
            compose.onNodeWithTag(tag).assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
        }
    }

    // ---- Large font scale -------------------------------------------------------------------
    //
    // MerchantAvatar sizes its text as (size.value * 0.33f).sp inside a *fixed* size box, so the
    // glyphs grow with the user's font scale while their container does not.

    @Test
    @Config(fontScale = 2.0f)
    fun largeFontLight() = screenshot("MerchantAvatar_fontScale2", ThemeMode.LIGHT) { WideInitialsRow() }

    @Test
    @Config(fontScale = 2.0f)
    fun largeFontDark() = screenshot("MerchantAvatar_fontScale2", ThemeMode.DARK) { WideInitialsRow() }

    @Test
    @Config(fontScale = 2.0f)
    fun `the square keeps its size at 2x, whatever the initials`() {
        setBareContent {
            Row {
                Box(Modifier.testTag("bt")) { MerchantAvatar("Blue Tokai", Category.FOOD) }
                Box(Modifier.testTag("ww")) { MerchantAvatar("Wonder World", Category.FOOD) }
                Box(Modifier.testTag("big")) { MerchantAvatar("Wonder World", Category.FOOD, size = 56.dp) }
            }
        }

        compose.onNodeWithTag("bt").assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
        compose.onNodeWithTag("ww").assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
        compose.onNodeWithTag("big").assertWidthIsEqualTo(56.dp).assertHeightIsEqualTo(56.dp)
    }

    /**
     * The real invariant: however big the glyphs get, they are constrained by the avatar and
     * never spill onto the neighbouring one. This is what stops a large font scale from turning
     * a transaction list into overlapping text.
     */
    @Test
    @Config(fontScale = 2.0f)
    fun `initials never escape the avatar at 2x`() {
        setBareContent { Box(Modifier.testTag(AVATAR)) { MerchantAvatar("Wonder World", Category.FOOD) } }

        val box = compose.onNodeWithTag(AVATAR).getUnclippedBoundsInRoot()
        val text = compose.onNodeWithText("WW", useUnmergedTree = true).getUnclippedBoundsInRoot()

        assertTrue("initials left ${text.left} escapes box left ${box.left}", text.left >= box.left)
        assertTrue("initials right ${text.right} escapes box right ${box.right}", text.right <= box.right)
        assertTrue("initials top ${text.top} escapes box top ${box.top}", text.top >= box.top)
        assertTrue("initials bottom ${text.bottom} escapes box bottom ${box.bottom}", text.bottom <= box.bottom)
    }

    /**
     * Both initials keep rendering at a large font scale.
     *
     * This replaces a characterisation test that pinned the opposite behaviour: the initials were
     * sized in raw sp inside a fixed-size box, so at 2x "WW" saturated its 40dp container and only
     * the first letter was drawn — "Wonder World" and "Mega Mart" both rendered as one letter and
     * became indistinguishable. They are now sized from the avatar's dp, so they hold a constant
     * physical size whatever the user's font setting.
     *
     * Width is the proxy for "both glyphs actually drew". The semantics tree reports the string
     * handed to `Text`, so `onNodeWithText("WW")` matches even when only "W" is visible, and
     * `getUnclippedBoundsInRoot` returns the *constrained* bounds — which is why the containment
     * check above passes either way and cannot detect this on its own.
     */
    @Test
    @Config(fontScale = 2.0f)
    fun `both initials still fit inside the avatar at 2x`() {
        setBareContent {
            Row {
                Box(Modifier.testTag("bt")) { MerchantAvatar("Blue Tokai", Category.FOOD) }
                Box(Modifier.testTag("ww")) { MerchantAvatar("Wonder World", Category.FOOD) }
            }
        }

        val narrow = compose.onNodeWithText("BT", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val wide = compose.onNodeWithText("WW", useUnmergedTree = true).getUnclippedBoundsInRoot()

        assertTrue("\"WW\" (${wide.width}) should fit the 40dp avatar with room to spare", wide.width < 40.dp)
        assertTrue(
            "\"WW\" (${wide.width}) should measure wider than \"BT\" (${narrow.width}); if they match, " +
                "the second glyph was dropped",
            wide.width > narrow.width,
        )
    }

    private companion object {
        const val AVATAR = "avatar"
    }
}

/** Initial pairs from the widest glyphs down to the narrowest, to show the clipping threshold. */
@Composable
private fun WideInitialsRow() = Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    MerchantAvatar("Wonder World", Category.FOOD)
    MerchantAvatar("Mega Mart", Category.SHOPPING)
    MerchantAvatar("Blue Tokai", Category.TRANSPORT)
    MerchantAvatar("Indian Oil", Category.FUEL)
    MerchantAvatar("", Category.OTHER)
}

@Composable
private fun AvatarRow() = Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    MerchantAvatar("Blue Tokai", Category.FOOD)
    MerchantAvatar("Swiggy", Category.GROCERIES)
    MerchantAvatar("Indian Oil", Category.FUEL)
    MerchantAvatar("", Category.OTHER)
    MerchantAvatar("Netflix", Category.ENTERTAINMENT, size = 56.dp)
}
