package com.expensetracker.core.parser

import com.expensetracker.core.model.Confidence
import com.expensetracker.core.model.ParsedTransaction
import com.expensetracker.core.model.SmsMessage
import com.expensetracker.core.model.TransactionType

/**
 * An exact template for one bank's message format. Named groups:
 * `amount` (required), and optionally `merchant`, `account`, `ref`.
 * The shared [Extractors] fill in anything the template does not capture.
 *
 * Templates are plain data, so formats learned later (from user corrections or an
 * on-device model) can be stored and loaded the same way.
 */
class TemplateSmsParser(
    override val id: String,
    private val senderPattern: Regex,
    private val bodyPattern: Regex,
    private val type: TransactionType,
) : SmsParser {

    override fun parse(sms: SmsMessage): ParsedTransaction? {
        if (!senderPattern.containsMatchIn(sms.sender)) return null
        val match = bodyPattern.find(sms.body) ?: return null
        val groups = match.groups
        val amount = groups.value("amount")?.let(Extractors::parseMinor) ?: return null
        val rawMerchant = groups.value("merchant") ?: Extractors.merchantCandidate(sms.body, type)
        val merchant = Merchants.normalize(rawMerchant)
        val account = groups.value("account")?.takeLast(4) ?: Extractors.account(sms.body)

        return ParsedTransaction(
            amountMinor = amount,
            type = type,
            merchant = merchant,
            account = account,
            channel = Extractors.channel(sms.body),
            referenceNumber = groups.value("ref") ?: Extractors.reference(sms.body),
            balanceMinor = Extractors.balance(sms.body),
            timestamp = sms.timestamp,
            confidence = if (merchant != null) Confidence.HIGH else Confidence.MEDIUM,
            suggestedCategory = Merchants.defaultCategory(merchant, rawMerchant, type),
            parserId = id,
        )
    }

    private fun MatchGroupCollection.value(name: String): String? =
        runCatching { this[name]?.value }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}

/**
 * Known formats for major Indian banks. These are based on commonly seen message shapes and
 * must be validated against real (masked) samples; anything they miss falls through to
 * [GenericBankSmsParser].
 */
object BankTemplates {
    private val I = setOf(RegexOption.IGNORE_CASE)
    private const val AMT = """(?<amount>[0-9][0-9,]*(?:\.[0-9]{1,2})?)"""

    val all: List<SmsParser> = listOf(
        // "ICICI Bank Acct XX123 debited for Rs 500.00 on 25-Sep-26; Rahul S credited. UPI:426812345678."
        TemplateSmsParser(
            id = "icici-upi-debit",
            senderPattern = Regex("ICICI", I),
            bodyPattern = Regex(
                """acct\s+[x*]*(?<account>\d{3,6})\s+debited\s+for\s+(?:rs\.?|inr)\s*$AMT.*?;\s*(?<merchant>[A-Za-z][A-Za-z0-9 .&']{1,40}?)\s+credited""",
                I,
            ),
            type = TransactionType.DEBIT,
        ),
        // "INR 2,000.00 spent using ICICI Bank Card XX9021 on 24-Sep-26 on INDIAN OIL. Avl Limit: ..."
        TemplateSmsParser(
            id = "icici-card-spend",
            senderPattern = Regex("ICICI", I),
            bodyPattern = Regex(
                """(?:inr|rs\.?)\s*$AMT\s+spent\s+using\s+icici\s+bank\s+card\s+[x*]*(?<account>\d{3,6})\s+on\s+\S+\s+on\s+(?<merchant>[^.]{2,40})\.""",
                I,
            ),
            type = TransactionType.DEBIT,
        ),
        // "Dear UPI user A/C X1234 debited by 500.0 on date 25Sep26 trf to RAHUL S Refno 426812345678"
        TemplateSmsParser(
            id = "sbi-upi-debit",
            senderPattern = Regex("SBI", I),
            bodyPattern = Regex(
                """a/c\s+[x*]*(?<account>\d{3,6})\s+debited\s+by\s+$AMT.*?trf\s+to\s+(?<merchant>[A-Za-z][A-Za-z0-9 .&']{1,40}?)\s+ref\s*no\s*(?<ref>\d{6,})""",
                I,
            ),
            type = TransactionType.DEBIT,
        ),
        // "Dear SBI UPI User, ur A/cX1234 credited by Rs500 on 25Sep26 by (Ref no 426812345678)"
        TemplateSmsParser(
            id = "sbi-upi-credit",
            senderPattern = Regex("SBI", I),
            bodyPattern = Regex(
                """a/c\s*[x*]*(?<account>\d{3,6})\s+credited\s+by\s+(?:rs\.?)?\s*$AMT""",
                I,
            ),
            type = TransactionType.CREDIT,
        ),
        // "Spent Rs.900 From HDFC Bank Card x9021 At AMAZON On 2026-09-25:10:12:30"
        TemplateSmsParser(
            id = "hdfc-card-spend",
            senderPattern = Regex("HDFC", I),
            bodyPattern = Regex(
                """spent\s+(?:rs\.?|inr)\s*$AMT\s+from\s+hdfc\s+bank\s+card\s+[x*]*(?<account>\d{3,6})\s+at\s+(?<merchant>.+?)\s+on\s""",
                I,
            ),
            type = TransactionType.DEBIT,
        ),
        // "INR 500.00 debited\nA/c no. XX1234\n25-09-26, 10:12:30\nUPI/P2M/426812345678/ZOMATO"
        TemplateSmsParser(
            id = "axis-upi-debit",
            senderPattern = Regex("AXIS", I),
            bodyPattern = Regex(
                """(?:inr|rs\.?)\s*$AMT\s+debited\s+a/c\s+no\.?\s+[x*]*(?<account>\d{3,6}).*?upi/p2[am]/(?<ref>\d{6,})/(?<merchant>[^\n/]{2,40})""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ),
            type = TransactionType.DEBIT,
        ),
    )
}
