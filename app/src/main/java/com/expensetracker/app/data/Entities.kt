package com.expensetracker.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expensetracker.core.analytics.TxnPoint
import com.expensetracker.core.model.Category
import com.expensetracker.core.model.Channel
import com.expensetracker.core.model.TransactionType

enum class Source { SMS, MANUAL }

enum class Status {
    CONFIRMED,

    /** Parser was unsure; shown in the "Needs review" inbox. Still counted in totals. */
    NEEDS_REVIEW,

    /** User said "Not an expense". Hidden and excluded from totals. */
    IGNORED,
}

@Entity(
    tableName = "transactions",
    indices = [
        Index("timestamp"),
        Index("status"),
        Index("merchantKey"),
        Index(value = ["smsId"], unique = true),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val type: TransactionType,
    val merchant: String?,
    /** [com.expensetracker.core.parser.Merchants.key] of [merchant], used by category rules. */
    val merchantKey: String?,
    /** [Category.key]. */
    val category: String,
    val account: String?,
    val bank: String?,
    val channel: Channel,
    val timestamp: Long,
    val note: String? = null,
    val sender: String? = null,
    val rawSms: String? = null,
    val smsId: Long? = null,
    val referenceNumber: String? = null,
    val source: Source,
    val status: Status,
    val parserId: String? = null,
)

// Derived values live outside the entity so Room never tries to map them to columns.

val TransactionEntity.categoryEnum: Category get() = Category.fromKey(category)

val TransactionEntity.displayMerchant: String
    get() = merchant ?: if (type == TransactionType.CREDIT) "Money received" else "Payment"

/** "HDFC ••1234", "••1234", or null. */
val TransactionEntity.accountLabel: String?
    get() = when {
        account != null && bank != null -> "$bank ••$account"
        account != null -> "••$account"
        else -> bank
    }

fun TransactionEntity.toPoint() = TxnPoint(amountMinor, type, categoryEnum, merchant, timestamp)

@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
    @PrimaryKey val merchantKey: String,
    val merchantName: String,
    val category: String,
)
