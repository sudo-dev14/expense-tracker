package com.expensetracker.core.format

import kotlin.math.abs

/** Rupee formatting with Indian digit grouping (₹1,23,456), independent of device locale data. */
object Money {

    /**
     * @param showPaise always show two decimals; otherwise decimals appear only when non-zero.
     * @param signed prefix "+" or "−" (a real minus sign) for non-zero amounts.
     */
    fun format(minor: Long, showPaise: Boolean = false, signed: Boolean = false): String {
        val sign = when {
            minor < 0 -> "−"
            signed && minor > 0 -> "+"
            else -> ""
        }
        val absMinor = abs(minor)
        val rupees = absMinor / 100
        val paise = absMinor % 100
        val decimals = if (showPaise || paise != 0L) ".%02d".format(java.util.Locale.ROOT, paise) else ""
        return "$sign₹${group(rupees)}$decimals"
    }

    /** Compact form for chart axes: ₹950, ₹12.5K, ₹3.4L, ₹1.2Cr. */
    fun compact(minor: Long): String {
        val rupees = abs(minor) / 100.0
        val prefix = if (minor < 0) "−₹" else "₹"
        return prefix + when {
            rupees >= 1_00_00_000 -> trim(rupees / 1_00_00_000) + "Cr"
            rupees >= 1_00_000 -> trim(rupees / 1_00_000) + "L"
            rupees >= 1_000 -> trim(rupees / 1_000) + "K"
            else -> rupees.toLong().toString()
        }
    }

    private fun trim(v: Double): String {
        val s = "%.1f".format(java.util.Locale.ROOT, v)
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    /** 1234567 -> "12,34,567". */
    fun group(value: Long): String {
        val s = value.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        val rest = s.dropLast(3)
        val restGrouped = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$restGrouped,$last3"
    }
}
