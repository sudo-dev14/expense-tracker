package com.expensetracker.core.model

/** A raw SMS as read from the device inbox or a live broadcast. */
data class SmsMessage(
    /** Inbox row id, or null for messages that arrived via broadcast before being stored. */
    val id: Long?,
    val sender: String,
    val body: String,
    /** Epoch millis. */
    val timestamp: Long,
)

enum class TransactionType { DEBIT, CREDIT }

enum class Channel(val label: String) {
    UPI("UPI"),
    CARD("Card"),
    NET_BANKING("Net banking"),
    ATM("ATM"),
    WALLET("Wallet"),
    AUTO_DEBIT("Auto-pay"),
    OTHER("Other"),
}

/** How sure the parser is. LOW results go to the "Needs review" inbox. */
enum class Confidence { HIGH, MEDIUM, LOW }

/**
 * Built-in categories. [key] is what gets persisted, so never change an existing key.
 * [colorArgb] is used by charts and the PDF export.
 */
enum class Category(val key: String, val label: String, val colorArgb: Long) {
    FOOD("food", "Food & dining", 0xFF0E5A52),
    GROCERIES("groceries", "Groceries", 0xFF3E8E5E),
    SHOPPING("shopping", "Shopping", 0xFFD08A2E),
    TRANSPORT("transport", "Transport", 0xFF4A7BC0),
    FUEL("fuel", "Fuel", 0xFF2F5D99),
    BILLS("bills", "Bills & utilities", 0xFF8B6BB8),
    ENTERTAINMENT("entertainment", "Entertainment", 0xFFC0567A),
    HEALTH("health", "Health", 0xFF2E9AA0),
    TRAVEL("travel", "Travel", 0xFF6C8A2E),
    TRANSFERS("transfers", "Transfers", 0xFF7A7F80),
    INCOME("income", "Income", 0xFF0E7A52),
    OTHER("other", "Other", 0xFFB9B6AC);

    companion object {
        fun fromKey(key: String?): Category = entries.firstOrNull { it.key == key } ?: OTHER

        /** Categories a user can pick for spending (income is implied by credit type). */
        val spendCategories: List<Category> = entries.filter { it != INCOME }
    }
}

/** What a parser extracted from one SMS. Amounts are in minor units (paise). */
data class ParsedTransaction(
    val amountMinor: Long,
    val type: TransactionType,
    val merchant: String?,
    val account: String?,
    val channel: Channel,
    val referenceNumber: String?,
    val balanceMinor: Long?,
    val timestamp: Long,
    val confidence: Confidence,
    /** Default category from the built-in merchant dictionary; user rules override it. */
    val suggestedCategory: Category,
    /** Which [com.expensetracker.core.parser.SmsParser] produced this, for debugging and metrics. */
    val parserId: String,
)
