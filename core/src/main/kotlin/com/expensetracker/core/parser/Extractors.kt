package com.expensetracker.core.parser

import com.expensetracker.core.model.Channel
import com.expensetracker.core.model.TransactionType

/** Small, independently testable building blocks shared by all regex parsers. */
object Extractors {

    data class Amount(val minor: Long, val range: IntRange)

    private val CURRENCY_AMOUNT = Regex(
        """(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    /** SBI-style "debited by 500.0" with no currency marker. */
    private val VERB_AMOUNT = Regex(
        """(?:debited|credited)\s+(?:by|for|with)\s+([0-9][0-9,]*(?:\.[0-9]{1,2})?)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val BALANCE_CONTEXT = Regex(
        """(avl\.?\s*bal|available\s+bal|a/c\s+bal|bal(ance)?\s*[:\-]?|avl\.?\s*limit|available\s+limit|credit\s+limit|limit\s*[:\-])\s*$""",
        RegexOption.IGNORE_CASE,
    )

    /** All currency amounts in the message, in order of appearance. */
    fun amounts(body: String): List<Amount> {
        val found = CURRENCY_AMOUNT.findAll(body).mapNotNull { m ->
            parseMinor(m.groupValues[1])?.let { Amount(it, m.range) }
        }.toMutableList()
        if (found.isEmpty()) {
            VERB_AMOUNT.findAll(body).forEach { m ->
                val group = m.groups[1]!!
                parseMinor(group.value)?.let { found += Amount(it, group.range) }
            }
        }
        return found.filter { it.minor > 0 }
    }

    /** The transaction amount: the first amount that is not a balance or limit. */
    fun transactionAmount(body: String): Long? =
        amounts(body).firstOrNull { !isBalance(body, it) }?.minor

    fun balance(body: String): Long? = amounts(body).firstOrNull { isBalance(body, it) }?.minor

    private fun isBalance(body: String, amount: Amount): Boolean {
        val before = body.substring(maxOf(0, amount.range.first - 24), amount.range.first)
        return BALANCE_CONTEXT.containsMatchIn(before)
    }

    /** "1,23,456.7" -> 12345670 paise. */
    fun parseMinor(raw: String): Long? {
        val clean = raw.replace(",", "").trimEnd('.')
        if (clean.isEmpty()) return null
        val parts = clean.split('.')
        val rupees = parts[0].toLongOrNull() ?: return null
        val paise = when {
            parts.size < 2 || parts[1].isEmpty() -> 0L
            parts[1].length == 1 -> parts[1].toLong() * 10
            else -> parts[1].take(2).toLong()
        }
        return rupees * 100 + paise
    }

    private val DEBIT_WORDS = Regex(
        """\b(debited|debit|spent|paid|sent|withdrawn|withdrawal|deducted|purchase[d]?|charged|used for|dr)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val CREDIT_WORDS = Regex(
        """\b(credited|credit(?!\s*card)|received|deposited|refund(ed)?|reversed|reversal|cr)\b""",
        RegexOption.IGNORE_CASE,
    )

    /** Whichever direction word appears first wins ("debited from A/c ... credited to VPA" is a debit). */
    fun type(body: String): TransactionType? {
        val debit = DEBIT_WORDS.find(body)?.range?.first
        val credit = CREDIT_WORDS.find(body)?.range?.first
        return when {
            debit == null && credit == null -> null
            credit == null -> TransactionType.DEBIT
            debit == null -> TransactionType.CREDIT
            debit < credit -> TransactionType.DEBIT
            else -> TransactionType.CREDIT
        }
    }

    private val ACCOUNT = Regex(
        """(?:a/c|a/c\s*no\.?|ac|acct|account|card)\s*(?:no\.?)?\s*(?:ending\s*(?:with|in)?)?\s*[:\-]?\s*[x*•.]*\s*(\d{3,6})\b""",
        RegexOption.IGNORE_CASE,
    )
    private val MASKED_NUMBER = Regex("""\b[xX*•]{1,}(\d{3,6})\b""")

    /** Last digits of the account or card, e.g. "1234". */
    fun account(body: String): String? {
        val raw = ACCOUNT.find(body)?.groupValues?.get(1) ?: MASKED_NUMBER.find(body)?.groupValues?.get(1)
        return raw?.takeLast(4)
    }

    private val REFERENCE = Regex(
        """(?:upi\s*ref(?:erence)?\.?\s*(?:no\.?|number)?|ref(?:erence)?\.?\s*(?:no\.?|number)?|refno|txn\s*id|utr\s*(?:no\.?)?|rrn|upi)\s*[:\-]?\s*([0-9]{6,}|[a-z0-9]*[0-9][a-z0-9]{5,})""",
        RegexOption.IGNORE_CASE,
    )

    fun reference(body: String): String? = REFERENCE.find(body)?.groupValues?.get(1)

    private val VPA = Regex("""\b([a-z0-9][a-z0-9.\-_]{1,}@[a-z][a-z0-9]{1,})\b""", RegexOption.IGNORE_CASE)

    fun vpa(body: String): String? = VPA.find(body)?.groupValues?.get(1)

    /**
     * Best-effort merchant/payee text, un-normalised. Order matters: the most specific
     * patterns come first.
     */
    fun merchantCandidate(body: String, type: TransactionType?): String? {
        vpa(body)?.let { return it }
        val patterns = buildList {
            add(UPI_PATH)
            add(INFO)
            add(AT)
            if (type == TransactionType.CREDIT) add(FROM) else add(TO)
            add(if (type == TransactionType.CREDIT) TO else FROM)
        }
        for (p in patterns) {
            for (match in p.findAll(body)) {
                val candidate = cleanCandidate(match.groupValues[1])
                if (candidate.isNotBlank() && !isNoise(candidate)) return candidate
            }
        }
        return null
    }

    private const val STOP = """(?=\s+(?:on|for|via|using|ref|refno|txn|dated|avl|avl\.|from|upi|thru|through|with|is|has)\b|\s*[.,;(\n]|\s*$)"""
    private val AT = Regex("""\b(?:at|on)\s+([A-Za-z][A-Za-z0-9&'*.\- ]{1,40}?)$STOP""", RegexOption.IGNORE_CASE)
    private val TO = Regex("""\b(?:to|towards|trf to|paid to|sent to)\s+([A-Za-z][A-Za-z0-9&'*.\- ]{1,40}?)$STOP""", RegexOption.IGNORE_CASE)
    private val FROM = Regex("""\b(?:from|by)\s+([A-Za-z][A-Za-z0-9&'*.\- ]{1,40}?)$STOP""", RegexOption.IGNORE_CASE)
    private val INFO = Regex("""\binfo\s*[:\-]\s*(?:upi/)?(?:[a-z0-9]*/)*([A-Za-z][A-Za-z0-9&'. ]{1,40})""", RegexOption.IGNORE_CASE)
    private val UPI_PATH = Regex("""\bupi/(?:p2[am]/)?\d{6,}/([A-Za-z][A-Za-z0-9&'. ]{1,40})""", RegexOption.IGNORE_CASE)

    private fun cleanCandidate(raw: String): String =
        raw.trim().removeSuffix(".").replace(Regex("""^(mr\.?|ms\.?|mrs\.?|m/s\.?)\s+""", RegexOption.IGNORE_CASE), "").trim()

    private val NOISE = Regex(
        """^(your|you|a/c|ac|acct|account|card|bank|xx|x+\d*|\*+\d*|the|beneficiary|upi|date|rs\.?|inr|linked|self|\d.*)(\s|$)""",
        RegexOption.IGNORE_CASE,
    )

    private fun isNoise(candidate: String): Boolean =
        NOISE.containsMatchIn(candidate) || BANK_WORDS.containsMatchIn(candidate)

    private val BANK_WORDS = Regex("""\b(hdfc|icici|sbi|axis|kotak|yes|idfc|indusind|federal|pnb|canara|bob)\s+bank\b""", RegexOption.IGNORE_CASE)

    fun channel(body: String): Channel {
        val b = body.lowercase()
        return when {
            Regex("""auto-?debit|autopay|auto pay|mandate|\bsi\b|standing instruction|nach|ecs""").containsMatchIn(b) -> Channel.AUTO_DEBIT
            "upi" in b || vpa(body) != null -> Channel.UPI
            Regex("""\batm\b|withdrawn|withdrawal""").containsMatchIn(b) -> Channel.ATM
            "card" in b -> Channel.CARD
            Regex("""\b(neft|imps|rtgs|net ?banking|netbanking)\b""").containsMatchIn(b) -> Channel.NET_BANKING
            "wallet" in b -> Channel.WALLET
            else -> Channel.OTHER
        }
    }

    private val BANKS = linkedMapOf(
        "HDFC" to "HDFC", "ICICI" to "ICICI", "SBI" to "SBI", "AXIS" to "Axis", "KOTAK" to "Kotak",
        "KOTAKB" to "Kotak", "YESB" to "Yes Bank", "IDFC" to "IDFC", "INDUS" to "IndusInd", "PNB" to "PNB",
        "CANBNK" to "Canara", "BOB" to "Bank of Baroda", "FEDBNK" to "Federal", "PAYTM" to "Paytm",
        "AMEX" to "Amex", "SCB" to "StanChart", "CITI" to "Citi", "AUBANK" to "AU Bank",
    )

    /** "AX-HDFCBK" -> "HDFC". */
    fun bankFromSender(sender: String): String? {
        val code = sender.uppercase().substringAfter('-').substringBefore('-')
        return BANKS.entries.firstOrNull { code.contains(it.key) }?.value
    }
}
