package com.expensetracker.core.analytics

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class RangePreset(val label: String) {
    THIS_MONTH("This month"),
    LAST_3_MONTHS("3M"),
    LAST_6_MONTHS("6M"),
    LAST_YEAR("1Y"),
    LAST_3_YEARS("3Y"),
    ALL_TIME("All"),
    CUSTOM("Custom"),
}

/** Inclusive calendar-date range. */
data class DateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init {
        require(!endInclusive.isBefore(start)) { "end before start: $start..$endInclusive" }
    }

    val days: Long get() = ChronoUnit.DAYS.between(start, endInclusive) + 1

    fun startMillis(zone: ZoneId): Long = start.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endMillisExclusive(zone: ZoneId): Long = endInclusive.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(endInclusive)
}

object DateRanges {

    /**
     * "3M" means the current month plus the two before it, so the numbers line up with how
     * people think about months ("July to September"), not a rolling 90 days.
     */
    fun resolve(
        preset: RangePreset,
        today: LocalDate,
        custom: DateRange? = null,
        earliest: LocalDate? = null,
    ): DateRange {
        val monthStart = today.withDayOfMonth(1)
        return when (preset) {
            RangePreset.THIS_MONTH -> DateRange(monthStart, today)
            RangePreset.LAST_3_MONTHS -> DateRange(monthStart.minusMonths(2), today)
            RangePreset.LAST_6_MONTHS -> DateRange(monthStart.minusMonths(5), today)
            RangePreset.LAST_YEAR -> DateRange(monthStart.minusMonths(11), today)
            RangePreset.LAST_3_YEARS -> DateRange(monthStart.minusMonths(35), today)
            RangePreset.ALL_TIME -> DateRange(minOf(earliest ?: today, today), today)
            RangePreset.CUSTOM -> custom ?: DateRange(monthStart, today)
        }
    }

    /**
     * The period to compare against. For month-aligned presets it is the same span of days
     * in the preceding months (1-25 Sep compares with 1-25 Aug), otherwise the equal-length
     * window right before.
     */
    fun previous(preset: RangePreset, range: DateRange): DateRange = when (preset) {
        RangePreset.THIS_MONTH, RangePreset.LAST_3_MONTHS, RangePreset.LAST_6_MONTHS,
        RangePreset.LAST_YEAR, RangePreset.LAST_3_YEARS -> {
            val months = ChronoUnit.MONTHS.between(range.start, range.endInclusive.withDayOfMonth(1)) + 1
            val start = range.start.minusMonths(months)
            val end = range.endInclusive.minusMonths(months)
            DateRange(start, maxOf(start, end))
        }
        else -> DateRange(range.start.minusDays(range.days), range.start.minusDays(1))
    }

    fun comparisonLabel(preset: RangePreset): String = when (preset) {
        RangePreset.THIS_MONTH -> "vs last month"
        RangePreset.LAST_YEAR -> "vs previous year"
        else -> "vs previous period"
    }
}
