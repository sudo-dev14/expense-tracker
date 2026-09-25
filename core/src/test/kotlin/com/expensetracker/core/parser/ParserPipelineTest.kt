package com.expensetracker.core.parser

import com.expensetracker.core.model.Category
import com.expensetracker.core.model.Channel
import com.expensetracker.core.model.Confidence
import com.expensetracker.core.model.ParsedTransaction
import com.expensetracker.core.model.SmsMessage
import com.expensetracker.core.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.fail

/**
 * Sample messages below imitate common Indian bank formats with made-up numbers.
 * Add real (masked) samples here whenever a format is missed.
 */
class ParserPipelineTest {

    private val pipeline = ParserPipeline.default()

    private fun parse(sender: String, body: String): ParsedTransaction =
        when (val r = pipeline.parse(SmsMessage(1, sender, body, 1_000L))) {
            is ParseResult.Transaction -> r.transaction
            else -> fail("Expected a transaction but got $r for: $body")
        }

    private fun skip(sender: String, body: String): SkipReason {
        val r = pipeline.parse(SmsMessage(1, sender, body, 1_000L))
        assertIs<ParseResult.Skipped>(r, "Expected skip for: $body")
        return r.reason
    }

    @Test
    fun hdfcUpiDebitWithVpa() {
        val t = parse(
            "AX-HDFCBK",
            "Rs.349.00 debited from a/c **1234 on 25-09-26 to VPA zomato@hdfcbank (UPI Ref No 426812345678). Not you? Call 18002586161",
        )
        assertEquals(34900, t.amountMinor)
        assertEquals(TransactionType.DEBIT, t.type)
        assertEquals("Zomato", t.merchant)
        assertEquals("1234", t.account)
        assertEquals("426812345678", t.referenceNumber)
        assertEquals(Channel.UPI, t.channel)
        assertEquals(Category.FOOD, t.suggestedCategory)
        assertEquals(Confidence.HIGH, t.confidence)
    }

    @Test
    fun hdfcMultilineSent() {
        val t = parse("VM-HDFCBK", "Sent Rs.412.00\nFrom HDFC Bank A/C *1234\nTo SWIGGY\nOn 12/03/25\nRef 507112345678\nNot You?\nCall 18002586161")
        assertEquals(41200, t.amountMinor)
        assertEquals("Swiggy", t.merchant)
        assertEquals("1234", t.account)
        assertEquals(TransactionType.DEBIT, t.type)
    }

    @Test
    fun hdfcCardSpend() {
        val t = parse("AD-HDFCBK", "Spent Rs.900 From HDFC Bank Card x9021 At AMAZON PAY INDIA On 2026-09-25:10:12:30 Bal Rs.12000 Not You? Call 18002586161")
        assertEquals("hdfc-card-spend", t.parserId)
        assertEquals(90000, t.amountMinor)
        assertEquals("Amazon", t.merchant)
        assertEquals("9021", t.account)
        assertEquals(1200000, t.balanceMinor)
        assertEquals(Category.SHOPPING, t.suggestedCategory)
    }

    @Test
    fun iciciUpiDebitTemplate() {
        val t = parse("JD-ICICIB", "ICICI Bank Acct XX123 debited for Rs 500.00 on 25-Sep-26; Rahul S credited. UPI:426812345678. Call 18002662 for dispute.")
        assertEquals("icici-upi-debit", t.parserId)
        assertEquals(50000, t.amountMinor)
        assertEquals("Rahul S", t.merchant)
        assertEquals("123", t.account)
        assertEquals(TransactionType.DEBIT, t.type)
    }

    @Test
    fun iciciCardSpendIgnoresAvailableLimit() {
        val t = parse("VK-ICICIB", "INR 2,000.00 spent using ICICI Bank Card XX9021 on 24-Sep-26 on INDIAN OIL. Avl Limit: INR 1,20,000.00. If not you, call 1800 2662.")
        assertEquals(200000, t.amountMinor)
        assertEquals("Indian Oil", t.merchant)
        assertEquals(Category.FUEL, t.suggestedCategory)
    }

    @Test
    fun sbiUpiDebitWithoutCurrencySymbol() {
        val t = parse("BZ-SBIUPI", "Dear UPI user A/C X1234 debited by 500.0 on date 25Sep26 trf to RAHUL S Refno 426812345678. If not u? call 1800111109. -SBI")
        assertEquals(50000, t.amountMinor)
        assertEquals("RAHUL S", t.merchant?.uppercase())
        assertEquals("426812345678", t.referenceNumber)
    }

    @Test
    fun kotakSentToVpa() {
        val t = parse("AX-KOTAKB", "Sent Rs.1,850.00 from Kotak Bank AC X5566 to decathlonsports@ybl on 23-09-26.UPI Ref 426698765432. Not you, kotak.com/fraud")
        assertEquals(185000, t.amountMinor)
        assertEquals("Decathlon", t.merchant)
        assertEquals("5566", t.account)
        assertEquals(Category.SHOPPING, t.suggestedCategory)
    }

    @Test
    fun axisUpiPath() {
        val t = parse("AD-AXISBK", "INR 500.00 debited\nA/c no. XX1234\n25-09-26, 10:12:30\nUPI/P2M/426812345678/ZOMATO LTD\nNot you? SMS BLOCKUPI Cust ID to 919951860002")
        assertEquals(50000, t.amountMinor)
        assertEquals("Zomato", t.merchant)
        assertEquals("426812345678", t.referenceNumber)
    }

    @Test
    fun salaryCredit() {
        val t = parse("AX-HDFCBK", "Update! INR 85,000.00 deposited in HDFC Bank A/c XX1234 on 01-SEP-26 for NEFT Cr-ACME TECHNOLOGIES PVT LTD. Avl bal INR 1,02,340.50.")
        assertEquals(8500000, t.amountMinor)
        assertEquals(TransactionType.CREDIT, t.type)
        assertEquals(10234050, t.balanceMinor)
        assertEquals(Category.INCOME, t.suggestedCategory)
    }

    @Test
    fun upiCreditFromPerson() {
        val t = parse("VM-SBIUPI", "Dear SBI UPI User, ur A/cX1234 credited by Rs500 on 25Sep26 by (Ref no 426812345678)")
        assertEquals(TransactionType.CREDIT, t.type)
        assertEquals(50000, t.amountMinor)
        assertEquals("1234", t.account)
    }

    @Test
    fun atmWithdrawal() {
        val t = parse("AX-SBIINB", "Rs.5,000.00 withdrawn at SBI ATM S1BW000123 from A/c X1234 on 25Sep26. Avl Bal Rs.20,000.00")
        assertEquals(500000, t.amountMinor)
        assertEquals(Channel.ATM, t.channel)
        assertEquals(TransactionType.DEBIT, t.type)
    }

    @Test
    fun skipsPersonalChats() {
        assertEquals(SkipReason.PERSONAL_SENDER, skip("+919876543210", "Sent you Rs 500 for dinner, thanks!"))
    }

    @Test
    fun skipsOtp() {
        assertEquals(SkipReason.OTP, skip("AX-HDFCBK", "123456 is your OTP for txn of Rs 2,000.00 at AMAZON on card ending 9021. Valid for 5 mins. Do not share."))
    }

    @Test
    fun skipsPromotions() {
        assertEquals(SkipReason.PROMOTIONAL, skip("VM-ICICIB", "Congratulations! You are pre-approved for a Personal Loan of Rs 5,00,000. Apply now: http://icici.co/x"))
    }

    @Test
    fun skipsCollectRequests() {
        assertEquals(SkipReason.PAYMENT_REQUEST, skip("VM-HDFCBK", "Rahul has requested money from you on Google Pay. On approving, Rs 500 will be debited from your A/c."))
    }

    @Test
    fun skipsDueReminders() {
        assertEquals(SkipReason.REMINDER, skip("AX-ICICIB", "Your ICICI Bank Credit Card XX9021 statement is generated. Total amount due Rs 12,340; minimum amount due Rs 620, due by 05-Oct-26."))
    }

    @Test
    fun skipsFailedTransactions() {
        assertEquals(SkipReason.FAILED_TRANSACTION, skip("AX-HDFCBK", "Your UPI transaction of Rs 349.00 to zomato@hdfcbank has failed. Any amount debited will be reversed."))
    }
}
