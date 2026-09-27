package com.expensetracker.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expensetracker.app.data.Source
import com.expensetracker.app.data.Status
import com.expensetracker.app.data.TransactionEntity
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.app.ui.theme.ExpenseTheme
import com.expensetracker.core.model.Category
import com.expensetracker.core.model.Channel
import com.expensetracker.core.model.TransactionType

/**
 * Previews for the components most likely to break without anyone noticing.
 *
 * The three variants are the point. A light-only preview would not have shown that category
 * initials were unreadable on their own tint, or that the widest initial pair loses a letter once
 * the user turns their font size up — both of which shipped and were found by reading the code
 * rather than by looking at it.
 *
 * These are dev-time only: nothing references them, so R8 strips them from the release build.
 */
@Preview(name = "light", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "font 2x", showBackground = true, fontScale = 2.0f)
private annotation class AppPreviews

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    ExpenseTheme(ThemeMode.SYSTEM) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
}

/** Every category at once: the fastest way to see a contrast or clipping problem. */
@AppPreviews
@Composable
private fun MerchantAvatarPreview() = PreviewSurface {
    Category.entries.chunked(6).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { MerchantAvatar("Wonder World", it) }
        }
    }
}

@AppPreviews
@Composable
private fun TransactionRowPreview() = PreviewSurface {
    TransactionRow(previewTransaction(), onClick = {})
    TransactionRow(
        previewTransaction(
            merchant = "Kumar Brothers Provision Store and General Merchants",
            category = Category.GROCERIES,
        ),
        onClick = {},
    )
    TransactionRow(
        previewTransaction(
            amountMinor = 7_500_000,
            type = TransactionType.CREDIT,
            merchant = "Acme Software Payroll",
            category = Category.INCOME,
        ),
        onClick = {},
    )
}

@AppPreviews
@Composable
private fun OfflineBadgePreview() = PreviewSurface {
    OfflineBadge()
    OfflineBadge(onClick = {})
}

@AppPreviews
@Composable
private fun AppCardPreview() = PreviewSurface {
    AppCard { SectionLabel("Section") }
}

private fun previewTransaction(
    amountMinor: Long = 48_550,
    type: TransactionType = TransactionType.DEBIT,
    merchant: String = "Blue Tokai Coffee",
    category: Category = Category.FOOD,
) = TransactionEntity(
    id = 1,
    amountMinor = amountMinor,
    type = type,
    merchant = merchant,
    merchantKey = merchant.lowercase().replace(' ', '-'),
    category = category.key,
    account = "4821",
    bank = "HDFC",
    channel = Channel.UPI,
    timestamp = 1_741_926_000_000L,
    source = Source.SMS,
    status = Status.CONFIRMED,
)
