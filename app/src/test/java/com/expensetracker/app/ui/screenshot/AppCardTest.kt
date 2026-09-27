package com.expensetracker.app.ui.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.app.ui.components.AppCard
import com.expensetracker.app.ui.components.SectionLabel
import org.junit.Test

class AppCardTest : ScreenshotTest() {

    @Test
    fun light() = screenshot("AppCard", ThemeMode.LIGHT) { SampleCard() }

    @Test
    fun dark() = screenshot("AppCard", ThemeMode.DARK) { SampleCard() }

    @Test
    fun `card fills the available width`() {
        setBareContent { AppCard(Modifier.testTag(CARD)) { Text("Body") } }

        assertDp(rootWidth(), compose.onNodeWithTag(CARD).getUnclippedBoundsInRoot().width, "card width")
    }

    @Test
    fun `default padding insets the content by 18dp on every side`() {
        setBareContent {
            AppCard(Modifier.testTag(CARD)) { Text("Body", Modifier.testTag(BODY)) }
        }

        val card = compose.onNodeWithTag(CARD).getUnclippedBoundsInRoot()
        val body = compose.onNodeWithTag(BODY).getUnclippedBoundsInRoot()

        assertDp(card.left + 18.dp, body.left, "content left inset")
        assertDp(card.top + 18.dp, body.top, "content top inset")
        assertDp(card.bottom - 18.dp, body.bottom, "content bottom inset")
    }

    @Test
    fun `padding override moves the content, not the card`() {
        setBareContent {
            AppCard(Modifier.testTag(CARD), padding = 28.dp) { Text("Body", Modifier.testTag(BODY)) }
        }

        val card = compose.onNodeWithTag(CARD).getUnclippedBoundsInRoot()
        val body = compose.onNodeWithTag(BODY).getUnclippedBoundsInRoot()

        assertDp(rootWidth(), card.width, "card still fills the width")
        assertDp(card.left + 28.dp, body.left, "content left inset")
        assertDp(card.top + 28.dp, body.top, "content top inset")
    }

    private companion object {
        const val CARD = "card"
        const val BODY = "body"
    }
}

@Composable
private fun SampleCard() = AppCard {
    Column {
        SectionLabel("This month")
        Text("₹42,180", style = MaterialTheme.typography.displaySmall)
        Text("18 transactions", style = MaterialTheme.typography.bodyMedium)
    }
}
