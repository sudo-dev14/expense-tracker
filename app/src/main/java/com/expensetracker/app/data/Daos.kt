package com.expensetracker.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query(
        """SELECT * FROM transactions
           WHERE status != 'IGNORED' AND timestamp >= :start AND timestamp < :end
           ORDER BY timestamp DESC"""
    )
    fun observeInRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query(
        """SELECT * FROM transactions
           WHERE status != 'IGNORED' AND timestamp >= :start AND timestamp < :end
           ORDER BY timestamp ASC"""
    )
    suspend fun inRange(start: Long, end: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE status = 'NEEDS_REVIEW' ORDER BY timestamp DESC")
    fun observeNeedsReview(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'NEEDS_REVIEW'")
    fun observeNeedsReviewCount(): Flow<Int>

    @Query("SELECT MIN(timestamp) FROM transactions WHERE status != 'IGNORED'")
    fun observeEarliestTimestamp(): Flow<Long?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE smsId = :smsId)")
    suspend fun existsSmsId(smsId: Long): Boolean

    /** Possible duplicates of a new transaction (see DuplicateDetector). */
    @Query(
        """SELECT * FROM transactions
           WHERE amountMinor = :amount AND type = :type
             AND ((timestamp BETWEEN :from AND :to) OR (referenceNumber IS NOT NULL AND referenceNumber = :ref))"""
    )
    suspend fun duplicateCandidates(amount: Long, type: String, from: Long, to: Long, ref: String?): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tx: TransactionEntity): Long

    @Update
    suspend fun update(tx: TransactionEntity)

    @Delete
    suspend fun delete(tx: TransactionEntity)

    @Query("UPDATE transactions SET category = :category WHERE merchantKey = :merchantKey AND type = 'DEBIT'")
    suspend fun setCategoryForMerchant(merchantKey: String, category: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE merchantKey = :merchantKey AND type = 'DEBIT' AND id != :excludeId")
    suspend fun countOtherWithMerchant(merchantKey: String, excludeId: Long): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface CategoryRuleDao {
    @Query("SELECT * FROM category_rules ORDER BY merchantName COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM category_rules WHERE merchantKey = :key")
    suspend fun get(key: String): CategoryRuleEntity?

    @Upsert
    suspend fun upsert(rule: CategoryRuleEntity)

    @Delete
    suspend fun delete(rule: CategoryRuleEntity)

    @Query("DELETE FROM category_rules")
    suspend fun deleteAll()
}
