package com.expensetracker.core.parser

import com.expensetracker.core.model.SmsMessage

/**
 * Cheap first pass that throws away messages which are clearly not transactions.
 * Personal chats are rejected purely on the sender, before any look at the body.
 */
class SmsFilter {

    fun skipReason(sms: SmsMessage): SkipReason? {
        if (isPersonalSender(sms.sender)) return SkipReason.PERSONAL_SENDER

        val body = sms.body.lowercase()
        // "will be debited" is a future event, not a completed payment.
        val completed = COMPLETED_VERB.containsMatchIn(body.replace(FUTURE, " "))

        if (FAILED.containsMatchIn(body)) return SkipReason.FAILED_TRANSACTION
        if (REQUEST.containsMatchIn(body)) return SkipReason.PAYMENT_REQUEST
        if (!completed && REMINDER.containsMatchIn(body)) return SkipReason.REMINDER
        if (!completed && OTP.containsMatchIn(body)) return SkipReason.OTP
        if (!completed && PROMO.containsMatchIn(body)) return SkipReason.PROMOTIONAL
        if (Extractors.amounts(sms.body).isEmpty()) return SkipReason.NO_AMOUNT
        // No completed-payment verb: only keep it if it still reads like a transaction alert.
        if (!completed && !TXN_NOUN.containsMatchIn(body)) return SkipReason.PROMOTIONAL
        return null
    }

    companion object {
        /** Phone numbers (10+ digits) are people; banks use alphanumeric IDs or short codes. */
        fun isPersonalSender(sender: String): Boolean {
            val digits = sender.removePrefix("+").replace(" ", "").replace("-", "")
            return digits.length >= 10 && digits.all { it.isDigit() }
        }

        private val COMPLETED_VERB = Regex(
            """\b(debited|credited|spent|sent|received|paid|withdrawn|withdrawal|deducted|purchased?|transferred|deposited|refunded|refund of|reversed|charged|used for)\b"""
        )
        private val FUTURE = Regex("""will be (auto-?)?\w+""")
        private val TXN_NOUN = Regex("""\b(txn|transaction|a/c|acct|account|card)\b""")
        private val FAILED = Regex("""\b(failed|declined|unsuccessful|could not be processed|insufficient (funds|balance))\b""")
        private val REQUEST = Regex("""(has requested|is requesting|collect request|requested money|payment request)""")
        private val REMINDER = Regex(
            """(is due|due on|due date|due by|minimum amount due|total amount due|will be debited|will be auto-?debited|is scheduled|statement .*generated|bill .*generated)"""
        )
        private val OTP = Regex("""\b(otp|one[- ]time password|verification code|passcode|security code)\b""")
        private val PROMO = Regex(
            """\b(offer|cashback of up ?to|up ?to \d+% off|pre-?approved|apply now|click here|congratulations|voucher|discount|lowest rate|eligible for|limit (increase|enhance)|win\b|reward points expire)"""
        )
    }
}
