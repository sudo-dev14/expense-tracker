package com.expensetracker.core.parser

import com.expensetracker.core.model.Confidence
import com.expensetracker.core.model.ParsedTransaction
import com.expensetracker.core.model.SmsMessage

/** Keyword/regex heuristics that work across most Indian bank, card and UPI alerts. */
class GenericBankSmsParser : SmsParser {
    override val id = "generic"

    override fun parse(sms: SmsMessage): ParsedTransaction? {
        val body = sms.body
        val amount = Extractors.transactionAmount(body) ?: return null
        val type = Extractors.type(body) ?: return null
        val rawMerchant = Extractors.merchantCandidate(body, type)
        val merchant = Merchants.normalize(rawMerchant)
        val account = Extractors.account(body)

        return ParsedTransaction(
            amountMinor = amount,
            type = type,
            merchant = merchant,
            account = account,
            channel = Extractors.channel(body),
            referenceNumber = Extractors.reference(body),
            balanceMinor = Extractors.balance(body),
            timestamp = sms.timestamp,
            confidence = when {
                merchant != null && account != null -> Confidence.HIGH
                merchant != null || account != null -> Confidence.MEDIUM
                else -> Confidence.LOW
            },
            suggestedCategory = Merchants.defaultCategory(merchant, rawMerchant, type),
            parserId = id,
        )
    }
}
