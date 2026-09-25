package com.expensetracker.core.analytics

import com.expensetracker.core.model.Category
import com.expensetracker.core.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/** The minimal view of a transaction that analytics needs. */
data class TxnPoint(
    val amountMinor: Long,
    val type: TransactionType,
    val category: Category,
    val merchant: String?,
    val timestamp: Long,
)

data class Summary(val spentMinor: Long, val incomeMinor: Long, val count: Int) {
    val netMinor: Long get() = incomeMinor - spentMinor
}

data class CategoryTotal(val category: Category, val amountMinor: Long, val share: Float)

data class MerchantTotal(val name: String, val amountMinor: Long, val count: Int)

enum class Granularity { DAY, MONTH, YEAR }

data class Bucket(val start: LocalDate, val label: String, val amountMinor: Long)

object Analytics {

    fun summary(txns: List<TxnPoint>) = Summary(
        spentMinor = txns.filter { it.type == TransactionType.DEBIT }.sumOf { it.amountMinor },
        incomeMinor = txns.filter { it.type == TransactionType.CREDIT }.sumOf { it.amountMinor },
        count = txns.size,
    )

    /** Spending by category, biggest first. */
    fun byCategory(txns: List<TxnPoint>): List<CategoryTotal> {
        val debits = txns.filter { it.type == TransactionType.DEBIT }
        val total = debits.sumOf { it.amountMinor }
        if (total == 0L) return emptyList()
        return debits.groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amountMinor }
                CategoryTotal(cat, sum, sum.toFloat() / total)
            }
            .sortedByDescending { it.amountMinor }
    }

    fun topMerchants(txns: List<TxnPoint>, limit: Int = 5): List<MerchantTotal> =
        txns.filter { it.type == TransactionType.DEBIT && it.merchant != null }
            .groupBy { it.merchant!! }
            .map { (name, list) -> MerchantTotal(name, list.sumOf { it.amountMinor }, list.size) }
            .sortedByDescending { it.amountMinor }
            .take(limit)

    fun granularityFor(range: DateRange): Granularity = when {
        range.days <= 62 -> Granularity.DAY
        ChronoUnit.MONTHS.between(range.start.withDayOfMonth(1), range.endInclusive.withDayOfMonth(1)) < 36 -> Granularity.MONTH
        else -> Granularity.YEAR
    }

    /** Spending over time, with empty buckets filled in so bars line up with the calendar. */
    fun buckets(txns: List<TxnPoint>, range: DateRange, zone: ZoneId): List<Bucket> {
        val granularity = granularityFor(range)
        fun bucketStart(d: LocalDate): LocalDate = when (granularity) {
            Granularity.DAY -> d
            Granularity.MONTH -> d.withDayOfMonth(1)
            Granularity.YEAR -> d.withDayOfYear(1)
        }

        val sums = txns.asSequence()
            .filter { it.type == TransactionType.DEBIT }
            .map { bucketStart(Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()) to it.amountMinor }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.sum() }

        val out = mutableListOf<Bucket>()
        var cursor = bucketStart(range.start)
        val last = bucketStart(range.endInclusive)
        while (!cursor.isAfter(last)) {
            out += Bucket(cursor, label(cursor, granularity), sums[cursor] ?: 0L)
            cursor = when (granularity) {
                Granularity.DAY -> cursor.plusDays(1)
                Granularity.MONTH -> cursor.plusMonths(1)
                Granularity.YEAR -> cursor.plusYears(1)
            }
        }
        return out
    }

    private val DAY_FMT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val MONTH_FMT = DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH)

    private fun label(d: LocalDate, g: Granularity): String = when (g) {
        Granularity.DAY -> d.format(DAY_FMT)
        Granularity.MONTH -> d.format(MONTH_FMT)
        Granularity.YEAR -> d.year.toString()
    }

    /** Whole-number percentage change, or null when there is nothing to compare against. */
    fun percentChange(current: Long, previous: Long): Int? {
        if (previous <= 0L) return null
        return (((current - previous).toDouble() / previous) * 100).roundToInt()
    }
}
