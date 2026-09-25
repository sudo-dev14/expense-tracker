package com.expensetracker.core.parser

import com.expensetracker.core.model.Category
import com.expensetracker.core.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MerchantsAndDuplicatesTest {

    @Test
    fun normalizesVpasAndNames() {
        assertEquals("Zomato", Merchants.normalize("zomato@hdfcbank"))
        assertEquals("Rahul S", Merchants.normalize("rahul.s@okaxis"))
        assertEquals("UPI transfer", Merchants.normalize("9876543210@ybl"))
        assertEquals("Indian Oil", Merchants.normalize("INDIAN OIL CORP"))
        assertEquals("Blue Tokai Coffee", Merchants.normalize("BLUE TOKAI COFFEE"))
    }

    @Test
    fun shortKeywordsMatchWholeWordsOnly() {
        assertEquals(Category.OTHER, Merchants.defaultCategory("Coca Cola Store", null, TransactionType.DEBIT))
        assertEquals(Category.TRANSPORT, Merchants.defaultCategory("Uber", "uber india", TransactionType.DEBIT))
    }

    @Test
    fun creditsAreIncome() {
        assertEquals(Category.INCOME, Merchants.defaultCategory("Acme", null, TransactionType.CREDIT))
    }

    private fun c(amount: Long = 34900, t: Long = 0, sender: String = "AX-HDFCBK", account: String? = "1234", ref: String? = null, body: String? = null) =
        DuplicateDetector.Candidate(amount, TransactionType.DEBIT, t, sender, account, ref, body)

    @Test
    fun sameReferenceIsDuplicate() {
        assertTrue(DuplicateDetector.isLikelyDuplicate(c(ref = "42681"), c(ref = "42681", t = 99_999_999)))
        assertFalse(DuplicateDetector.isLikelyDuplicate(c(ref = "42681"), c(ref = "99999")))
    }

    @Test
    fun differentSenderWithinWindowIsDuplicate() {
        assertTrue(DuplicateDetector.isLikelyDuplicate(c(sender = "VM-PAYTM"), c(t = 60_000)))
        assertFalse(DuplicateDetector.isLikelyDuplicate(c(sender = "VM-PAYTM"), c(t = 60 * 60_000)))
    }

    @Test
    fun sameSenderSameAmountIsTwoPurchases() {
        assertFalse(DuplicateDetector.isLikelyDuplicate(c(sender = "VM-HDFCBK"), c(sender = "AX-HDFCBK", t = 60_000)))
    }

    @Test
    fun identicalBodyIsDuplicate() {
        assertTrue(DuplicateDetector.isLikelyDuplicate(c(body = "x"), c(body = "x", t = 5_000_000)))
    }
}
