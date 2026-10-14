package com.nihongo.srs.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabDao {

    // ---------------------------------------------------------------- vocab

    /** Returns the new row id, or -1 if the word already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVocab(vocab: VocabEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllVocab(list: List<VocabEntity>): List<Long>

    @Delete
    suspend fun deleteVocab(vocab: VocabEntity)

    @Query("SELECT COUNT(*) FROM vocab")
    suspend fun vocabCount(): Int

    /** All items of one type (WORD / PHRASE), with progress, for the Manage list. */
    @Transaction
    @Query("SELECT * FROM vocab WHERE type = :type ORDER BY id DESC")
    fun observeAllWithProgress(type: String): Flow<List<VocabWithProgress>>

    @Query("SELECT * FROM vocab WHERE type = :type ORDER BY id")
    fun observeByType(type: String): Flow<List<VocabEntity>>

    // ----------------------------------------------------------- reviews

    /**
     * TODAY'S DUE REVIEWS.
     *
     * Every learned, not-yet-mastered item whose nextReview is on or before the
     * end of today. Overdue items (missed days) are included too, oldest first.
     */
    @Transaction
    @Query(
        """
        SELECT v.* FROM vocab v
        INNER JOIN user_progress p ON p.vocabId = v.id
        WHERE p.isMastered = 0 AND p.nextReview <= :endOfToday
        ORDER BY p.nextReview ASC, v.id ASC
        """
    )
    suspend fun getDueReviews(endOfToday: Long): List<VocabWithProgress>

    @Query(
        """
        SELECT COUNT(*) FROM user_progress
        WHERE isMastered = 0 AND nextReview <= :endOfToday
        """
    )
    fun observeDueCount(endOfToday: Long): Flow<Int>

    /** New words = items with no progress row yet. */
    @Query(
        """
        SELECT v.* FROM vocab v
        LEFT JOIN user_progress p ON p.vocabId = v.id
        WHERE p.vocabId IS NULL AND v.type = :type
        ORDER BY v.id ASC
        LIMIT :limit
        """
    )
    suspend fun getNewItems(type: String, limit: Int): List<VocabEntity>

    @Query(
        """
        SELECT COUNT(*) FROM vocab v
        LEFT JOIN user_progress p ON p.vocabId = v.id
        WHERE p.vocabId IS NULL AND v.type = 'WORD'
        """
    )
    fun observeNewWordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_progress WHERE isMastered = 1")
    fun observeMasteredCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_progress WHERE isMastered = 0")
    fun observeLearningCount(): Flow<Int>

    // ---------------------------------------------------------- progress

    @Upsert
    suspend fun upsertProgress(progress: UserProgress)

    @Query("DELETE FROM user_progress WHERE vocabId = :vocabId")
    suspend fun resetProgress(vocabId: Long)
}
