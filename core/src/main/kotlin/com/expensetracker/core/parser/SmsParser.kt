package com.expensetracker.core.parser

import com.expensetracker.core.model.ParsedTransaction
import com.expensetracker.core.model.SmsMessage

/**
 * One strategy for turning an SMS into a transaction.
 *
 * Implementations are tried in order by [ParserPipeline]; the first non-null result wins.
 * Today: bank-specific regex templates, then a generic regex parser.
 * Later: an on-device model (Gemini Nano / bundled small LLM) can implement this same
 * interface and be appended as a fallback without touching callers.
 */
interface SmsParser {
    val id: String

    /** Returns null when this parser cannot make sense of the message. */
    fun parse(sms: SmsMessage): ParsedTransaction?
}

sealed interface ParseResult {
    data class Transaction(val transaction: ParsedTransaction) : ParseResult

    /** The message was recognised as not being a transaction (OTP, promo, personal chat...). */
    data class Skipped(val reason: SkipReason) : ParseResult

    /** Looked like a payment message but no parser could extract it. */
    data object Unparsed : ParseResult
}

enum class SkipReason {
    PERSONAL_SENDER,
    OTP,
    PROMOTIONAL,
    PAYMENT_REQUEST,
    REMINDER,
    FAILED_TRANSACTION,
    NO_AMOUNT,
}

class ParserPipeline(
    private val parsers: List<SmsParser>,
    private val filter: SmsFilter = SmsFilter(),
) {
    fun parse(sms: SmsMessage): ParseResult {
        filter.skipReason(sms)?.let { return ParseResult.Skipped(it) }
        for (parser in parsers) {
            parser.parse(sms)?.let { return ParseResult.Transaction(it) }
        }
        return ParseResult.Unparsed
    }

    companion object {
        /** The default offline pipeline: bank templates first, generic rules as fallback. */
        fun default(): ParserPipeline = ParserPipeline(
            parsers = BankTemplates.all + GenericBankSmsParser(),
        )
    }
}
