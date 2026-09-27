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

    @Test
    @Config(fontScale = 2.0f)
    fun `EXPLORE containment at 2x`() {
        setBareContent {
            Row {
                Box(Modifier.testTag("bt")) { MerchantAvatar("Blue Tokai", Category.FOOD) }
                Box(Modifier.testTag("ww")) { MerchantAvatar("Wonder World", Category.FOOD) }
            }
        }
        listOf("bt" to "BT", "ww" to "WW").forEach { (tag, initials) ->
            val box = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            val text = compose.onNodeWithText(initials, useUnmergedTree = true).getUnclippedBoundsInRoot()
            println("MEASURE $initials: box=${box.width}x${box.height} text=${text.width}x${text.height}")
        }
    }

    private companion object {
        const val AVATAR = "avatar"
    }
}

@Composable
private fun AvatarRow() = Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    MerchantAvatar("Blue Tokai", Category.FOOD)
    MerchantAvatar("Swiggy", Category.GROCERIES)
    MerchantAvatar("Indian Oil", Category.FUEL)
    MerchantAvatar("", Category.OTHER)
    MerchantAvatar("Netflix", Category.ENTERTAINMENT, size = 56.dp)
}
