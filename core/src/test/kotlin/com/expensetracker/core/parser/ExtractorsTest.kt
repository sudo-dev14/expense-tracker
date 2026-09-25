package com.expensetracker.core.parser

import com.expensetracker.core.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExtractorsTest {

    @Test
    fun parsesIndianGroupedAmounts() {
        assertEquals(12345670, Extractors.parseMinor("1,23,456.7"))
        assertEquals(50000, Extractors.parseMinor("500"))
        assertEquals(50005, Extractors.parseMinor("500.05"))
        assertEquals(50000, Extractors.parseMinor("500."))
    }

    @Test
    fun firstNonBalanceAmountIsTheTransaction() {
        val body = "Avl Bal Rs.10,000 after Rs.250 debited"
        assertEquals(25000, Extractors.transactionAmount(body))
        assertEquals(1000000, Extractors.balance(body))
    }

    @Test
    fun earliestDirectionWordWins() {
        assertEquals(TransactionType.DEBIT, Extractors.type("Rs 10 debited from a/c and credited to x@upi"))
        assertEquals(TransactionType.CREDIT, Extractors.type("Rs 10 credited to your a/c"))
        assertEquals(TransactionType.DEBIT, Extractors.type("Rs 10 spent on your ICICI Credit Card"))
        assertNull(Extractors.type("Hello there"))
    }

    @Test
    fun bankFromSender() {
        assertEquals("HDFC", Extractors.bankFromSender("AX-HDFCBK"))
        assertEquals("Kotak", Extractors.bankFromSender("VM-KOTAKB-S"))
        assertNull(Extractors.bankFromSender("AX-ZOMATO"))
    }

    @Test
    fun personalSenders() {
        assertTrue(SmsFilter.isPersonalSender("+919876543210"))
        assertTrue(SmsFilter.isPersonalSender("9876543210"))
        assertFalse(SmsFilter.isPersonalSender("AX-HDFCBK"))
        assertFalse(SmsFilter.isPersonalSender("57575"))
    }
}
