package com.expensetracker.core.analytics

import com.expensetracker.core.model.Category
import com.expensetracker.core.model.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnalyticsTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 9, 25)

    private fun at(date: LocalDate) = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun presetsAreMonthAligned() {
        assertEquals(DateRange(LocalDate.of(2026, 9, 1), today), DateRanges.resolve(RangePreset.THIS_MONTH, today))
        assertEquals(LocalDate.of(2026, 7, 1), DateRanges.resolve(RangePreset.LAST_3_MONTHS, today).start)
        assertEquals(LocalDate.of(2025, 10, 1), DateRanges.resolve(RangePreset.LAST_YEAR, today).start)
        assertEquals(LocalDate.of(2023, 10, 1), DateRanges.resolve(RangePreset.LAST_3_YEARS, today).start)
        assertEquals(LocalDate.of(2024, 1, 5), DateRanges.resolve(RangePreset.ALL_TIME, today, earliest = LocalDate.of(2024, 1, 5)).start)
    }

    @Test
    fun previousPeriodMatchesDaysOfPriorMonth() {
        val range = DateRanges.resolve(RangePreset.THIS_MONTH, today)
        assertEquals(DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 25)), DateRanges.previous(RangePreset.THIS_MONTH, range))
        val custom = DateRange(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 20))
        assertEquals(DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10)), DateRanges.previous(RangePreset.CUSTOM, custom))
    }

    @Test
    fun previousOfMarch31CoversEndOfFebruary() {
        val range = DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
        assertEquals(DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)), DateRanges.previous(RangePreset.THIS_MONTH, range))
    }

    private val txns = listOf(
        TxnPoint(40000, TransactionType.DEBIT, Category.FOOD, "Swiggy", at(LocalDate.of(2026, 9, 2))),
        TxnPoint(10000, TransactionType.DEBIT, Category.FOOD, "Zomato", at(LocalDate.of(2026, 9, 2))),
        TxnPoint(50000, TransactionType.DEBIT, Category.SHOPPING, "Amazon", at(LocalDate.of(2026, 9, 10))),
        TxnPoint(8500000, TransactionType.CREDIT, Category.INCOME, "Acme", at(LocalDate.of(2026, 9, 1))),
    )

    @Test
    fun summaryAndCategories() {
        val s = Analytics.summary(txns)
        assertEquals(100000, s.spentMinor)
        assertEquals(8500000, s.incomeMinor)
        assertEquals(8400000, s.netMinor)
        val cats = Analytics.byCategory(txns)
        assertEquals(listOf(Category.FOOD, Category.SHOPPING), cats.map { it.category })
        assertEquals(0.5f, cats[0].share)
    }

    @Test
    fun dailyBucketsFillGaps() {
        val range = DateRanges.resolve(RangePreset.THIS_MONTH, today)
        val b = Analytics.buckets(txns, range, zone)
        assertEquals(25, b.size)
        assertEquals(50000, b[1].amountMinor)
        assertEquals(0, b[0].amountMinor)
        assertEquals("2 Sep", b[1].label)
    }

    @Test
    fun granularityScalesWithRange() {
        assertEquals(Granularity.DAY, Analytics.granularityFor(DateRanges.resolve(RangePreset.THIS_MONTH, today)))
        assertEquals(Granularity.MONTH, Analytics.granularityFor(DateRanges.resolve(RangePreset.LAST_3_YEARS, today)))
        assertEquals(Granularity.YEAR, Analytics.granularityFor(DateRange(LocalDate.of(2020, 1, 1), today)))
    }

    @Test
    fun topMerchantsAndChange() {
        assertEquals("Amazon", Analytics.topMerchants(txns).first().name)
        assertEquals(12, Analytics.percentChange(11200, 10000))
        assertNull(Analytics.percentChange(100, 0))
    }
}
