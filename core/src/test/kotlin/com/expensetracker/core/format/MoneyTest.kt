package com.expensetracker.core.format

import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyTest {
    @Test
    fun indianGrouping() {
        assertEquals("₹0", Money.format(0))
        assertEquals("₹999", Money.format(99900))
        assertEquals("₹1,23,456", Money.format(12345600))
        assertEquals("₹12,34,56,789.50", Money.format(123456789_50))
        assertEquals("₹349.00", Money.format(34900, showPaise = true))
    }

    @Test
    fun signs() {
        assertEquals("+₹85,000", Money.format(8500000, signed = true))
        assertEquals("−₹349", Money.format(-34900, signed = true))
        assertEquals("−₹349", Money.format(-34900))
    }

    @Test
    fun compact() {
        assertEquals("₹950", Money.compact(95000))
        assertEquals("₹12.5K", Money.compact(1250000))
        assertEquals("₹3.4L", Money.compact(34000000))
        assertEquals("₹1.2Cr", Money.compact(1_20_00_000_00))
    }
}
