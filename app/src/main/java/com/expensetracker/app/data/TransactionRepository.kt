package com.expensetracker.app.data

import androidx.room.withTransaction
import com.expensetracker.core.model.Category
import com.expensetracker.core.model.Channel
import com.expensetracker.core.model.Confidence
import com.expensetracker.core.model.SmsMessage
import com.expensetracker.core.model.TransactionType
import com.expensetracker.core.parser.DuplicateDetector
import com.expensetracker.core.parser.Extractors
import com.expensetracker.core.parser.Merchants
import com.expensetracker.core.parser.ParseResult
import com.expensetracker.core.parser.ParserPipeline
import kotlinx.coroutines.flow.Flow

sealed interface IngestResult {
    data class Added(val transaction: TransactionEntity) : IngestResult
    data object Duplicate : IngestResult
    data object NotATransaction : IngestResult
}

class TransactionRepository(
    private val db: AppDatabase,
    private val pipeline: ParserPipeline = ParserPipeline.default(),
) {
    private val dao = db.transactions()
    private val rules = db.rules()

    fun observeInRange(start: Long, end: Long): Flow<List<TransactionEntity>> = dao.observeInRange(start, end)
    fun observeNeedsReview(): Flow<List<TransactionEntity>> = dao.observeNeedsReview()
    fun observeNeedsReviewCount(): Flow<Int> = dao.observeNeedsReviewCount()
    fun observeEarliestTimestamp(): Flow<Long?> = dao.observeEarliestTimestamp()
    fun observeById(id: Long): Flow<TransactionEntity?> = dao.observeById(id)
    fun observeRules(): Flow<List<CategoryRuleEntity>> = rules.observeAll()
    suspend fun inRange(start: Long, end: Long): List<TransactionEntity> = dao.inRange(start, end)

    /** Parse one SMS and store it if it is a new transaction. Safe to call repeatedly. */
    suspend fun ingest(sms: SmsMessage): IngestResult {
        if (sms.id != null && dao.existsSmsId(sms.id!!)) return IngestResult.Duplicate

        val entity = when (val result = pipeline.parse(sms)) {
            is ParseResult.Skipped -> return IngestResult.NotATransaction
            is ParseResult.Transaction -> {
                val p = result.transaction
                val key = p.merchant?.let(Merchants::key)
                val ruleCategory = key?.let { rules.get(it) }?.category
                    ?.takeIf { p.type == TransactionType.DEBIT }
                TransactionEntity(
                    amountMinor = p.amountMinor,
                    type = p.type,
                    merchant = p.merchant,
                    merchantKey = key,
                    category = ruleCategory ?: p.suggestedCategory.key,
                    account = p.account,
                    bank = Extractors.bankFromSender(sms.sender),
                    channel = p.channel,
                    timestamp = p.timestamp,
                    sender = sms.sender,
                    rawSms = sms.body,
                    smsId = sms.id,
                    referenceNumber = p.referenceNumber,
                    source = Source.SMS,
                    status = if (p.confidence == Confidence.LOW || p.merchant == null) Status.NEEDS_REVIEW else Status.CONFIRMED,
                    parserId = p.parserId,
                )
            }
            // Looks like a payment but no parser understood it: keep it for the user to check.
            ParseResult.Unparsed -> {
                val amount = Extractors.transactionAmount(sms.body) ?: return IngestResult.NotATransaction
                TransactionEntity(
                    amountMinor = amount,
                    type = TransactionType.DEBIT,
                    merchant = null,
                    merchantKey = null,
                    category = Category.OTHER.key,
                    account = Extractors.account(sms.body),
                    bank = Extractors.bankFromSender(sms.sender),
                    channel = Extractors.channel(sms.body),
                    timestamp = sms.timestamp,
                    sender = sms.sender,
                    rawSms = sms.body,
                    smsId = sms.id,
                    source = Source.SMS,
                    status = Status.NEEDS_REVIEW,
                    parserId = "unparsed",
                )
            }
        }

        if (isDuplicate(entity)) return IngestResult.Duplicate
        val id = dao.insert(entity)
        return if (id == -1L) IngestResult.Duplicate else IngestResult.Added(entity.copy(id = id))
    }

    private suspend fun isDuplicate(new: TransactionEntity): Boolean {
        val window = DuplicateDetector.WINDOW_MILLIS
        val candidates = dao.duplicateCandidates(
            amount = new.amountMinor,
            type = new.type.name,
            from = new.timestamp - window,
            to = new.timestamp + window,
            ref = new.referenceNumber,
        )
        val me = new.toCandidate()
        return candidates.any { DuplicateDetector.isLikelyDuplicate(me, it.toCandidate()) }
    }

    private fun TransactionEntity.toCandidate() = DuplicateDetector.Candidate(
        amountMinor, type, timestamp, sender, account, referenceNumber, rawSms,
    )

    /** How many other transactions a "always use this category" rule would change. */
    suspend fun countOthersWithMerchant(tx: TransactionEntity): Int =
        tx.merchantKey?.let { dao.countOtherWithMerchant(it, tx.id) } ?: 0

    /**
     * Save user edits. When [applyToMerchant] is set, remembers the category for this merchant
     * and re-categorises its past transactions too.
     */
    suspend fun saveEdits(tx: TransactionEntity, applyToMerchant: Boolean) {
        val merchantKey = tx.merchant?.let(Merchants::key)
        val updated = tx.copy(merchantKey = merchantKey, status = if (tx.status == Status.NEEDS_REVIEW) Status.CONFIRMED else tx.status)
        db.withTransaction {
            dao.update(updated)
            if (applyToMerchant && merchantKey != null && tx.type == TransactionType.DEBIT) {
                rules.upsert(CategoryRuleEntity(merchantKey, tx.merchant!!, tx.category))
                dao.setCategoryForMerchant(merchantKey, tx.category)
            }
        }
    }

    suspend fun addManual(
        amountMinor: Long,
        type: TransactionType,
        merchant: String?,
        category: Category,
        timestamp: Long,
        note: String?,
    ): Long {
        val name = merchant?.trim()?.takeIf { it.isNotEmpty() }
        return dao.insert(
            TransactionEntity(
                amountMinor = amountMinor,
                type = type,
                merchant = name,
                merchantKey = name?.let(Merchants::key),
                category = if (type == TransactionType.CREDIT) Category.INCOME.key else category.key,
                account = null,
                bank = null,
                channel = Channel.OTHER,
                timestamp = timestamp,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                source = Source.MANUAL,
                status = Status.CONFIRMED,
            )
        )
    }

    suspend fun setStatus(id: Long, status: Status) {
        dao.getById(id)?.let { dao.update(it.copy(status = status)) }
    }

    suspend fun delete(tx: TransactionEntity) = dao.delete(tx)

    suspend fun deleteRule(rule: CategoryRuleEntity) = rules.delete(rule)

    suspend fun deleteEverything() {
        db.withTransaction {
            dao.deleteAll()
            rules.deleteAll()
        }
    }
}
