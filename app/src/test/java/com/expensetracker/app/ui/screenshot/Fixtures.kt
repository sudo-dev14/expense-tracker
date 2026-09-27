package com.expensetracker.app.ui.screenshot

import com.expensetracker.app.data.Source
import com.expensetracker.app.data.Status
import com.expensetracker.app.data.TransactionEntity
import com.expensetracker.core.analytics.Bucket
import com.expensetracker.core.model.Category
import com.expensetracker.core.model.Channel
import com.expensetracker.core.model.TransactionType
import java.time.LocalDate

/**
 * Fixed data for the screenshot suite. Every value is a literal: no clocks, no random ids and no
 * device locale, because anything that varies between runs turns a golden into a flake.
 */
object Fixtures {

    /** 14 Mar 2025, 10:30 IST. Only used for rows that do not render a date, but kept fixed anyway. */
    const val TIMESTAMP = 1_741_926_000_000L

    fun transaction(
        amountMinor: Long = 48_550,
        type: TransactionType = TransactionType.DEBIT,
        merchant: String? = "Blue Tokai Coffee",
        category: Category = Category.FOOD,
        account: String? = "4821",
        bank: String? = "HDFC",
        channel: Channel = Channel.UPI,
    ) = TransactionEntity(
        id = 1,
        amountMinor = amountMinor,
        type = type,
        merchant = merchant,
        merchantKey = merchant?.lowercase()?.replace(' ', '-'),
        category = category.key,
        account = account,
        bank = bank,
        channel = channel,
        timestamp = TIMESTAMP,
        source = Source.SMS,
        status = Status.CONFIRMED,
    )

    /** A debit with no merchant, no account and an OTHER channel: the shortest subtitle possible. */
    fun sparseTransaction() = transaction(
        merchant = null,
        category = Category.OTHER,
        account = null,
        bank = null,
        channel = Channel.OTHER,
    )

    /** Long enough to force the merchant name and the subtitle to ellipsize. */
    fun longNameTransaction() = transaction(
        merchant = "Kumar Brothers Provision Store and General Merchants",
        category = Category.GROCERIES,
        channel = Channel.CARD,
    )

    fun credit() = transaction(
        amountMinor = 7_500_000,
        type = TransactionType.CREDIT,
        merchant = "Acme Software Payroll",
        category = Category.INCOME,
        channel = Channel.NET_BANKING,
    )

    /** Six weeks of spending with a clear peak in the middle, so the amber highlight is visible. */
    fun buckets(): List<Bucket> {
        val amounts = listOf(320_00L, 1_240_00L, 780_00L, 2_150_00L, 0L, 960_00L, 1_480_00L)
        return amounts.mapIndexed { i, amount ->
            val start = LocalDate.of(2025, 2, 3).plusWeeks(i.toLong())
            Bucket(start = start, label = "W${i + 1}", amountMinor = amount)
        }
    }
}
