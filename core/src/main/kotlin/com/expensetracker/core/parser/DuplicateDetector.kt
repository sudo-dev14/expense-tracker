package com.expensetracker.core.parser

import com.expensetracker.core.model.TransactionType
import kotlin.math.abs

/**
 * The same payment often produces two messages (bank + UPI app, or card + wallet).
 * This decides whether a new transaction is the same one as an existing one.
 */
object DuplicateDetector {

    data class Candidate(
        val amountMinor: Long,
        val type: TransactionType,
        val timestamp: Long,
        val sender: String?,
        val account: String?,
        val referenceNumber: String?,
        val body: String?,
    )

    const val WINDOW_MILLIS: Long = 10 * 60 * 1000

    fun isLikelyDuplicate(new: Candidate, existing: Candidate): Boolean {
        if (new.amountMinor != existing.amountMinor || new.type != existing.type) return false
        if (new.body != null && new.body == existing.body) return true
        if (new.referenceNumber != null && existing.referenceNumber != null) {
            return new.referenceNumber == existing.referenceNumber
        }
        if (abs(new.timestamp - existing.timestamp) > WINDOW_MILLIS) return false
        if (new.account != null && existing.account != null && new.account != existing.account) return false
        // Same amount within minutes from a *different* sender: almost always one payment reported twice.
        // From the same sender it is more likely two real purchases, so keep both.
        return new.sender != null && existing.sender != null && !sameSender(new.sender, existing.sender)
    }

    /** "AX-HDFCBK" and "VM-HDFCBK" are the same bank with different operator prefixes. */
    private fun sameSender(a: String, b: String): Boolean = core(a) == core(b)

    private fun core(sender: String) = sender.uppercase().substringAfter('-').substringBefore('-')
}
