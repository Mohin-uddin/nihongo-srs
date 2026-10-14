package com.nihongo.srs.data

import com.nihongo.srs.srs.SpacedRepetitionEngine
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point the ViewModels use for data.
 * Combines the DAO with the SRS engine so the rules live in one place.
 */
class VocabRepository(
    private val dao: VocabDao,
    private val engine: SpacedRepetitionEngine,
) {

    /** Preloads the JLPT N5 words and speaking phrases the first time the app opens. */
    suspend fun seedIfEmpty() {
        if (dao.vocabCount() == 0) preloadStarterData()
    }

    /** Safe to call any time: existing words are skipped thanks to the unique index. */
    suspend fun preloadStarterData(): Int =
        dao.insertAllVocab(N5Seed.words + N5Seed.phrases).count { it != -1L }

    suspend fun addVocab(vocab: VocabEntity): Boolean = dao.insertVocab(vocab) != -1L

    suspend fun deleteVocab(vocab: VocabEntity) = dao.deleteVocab(vocab)

    suspend fun resetProgress(vocabId: Long) = dao.resetProgress(vocabId)

    fun observeAll(type: String): Flow<List<VocabWithProgress>> = dao.observeAllWithProgress(type)

    fun observePhrases(): Flow<List<VocabEntity>> = dao.observeByType(VocabEntity.TYPE_PHRASE)

    /** Last millisecond of today; pass a fresh value after midnight to refresh counts. */
    fun endOfToday(): Long = engine.endOfToday()

    fun observeDueCount(endOfToday: Long = engine.endOfToday()): Flow<Int> =
        dao.observeDueCount(endOfToday)
    fun observeNewWordCount(): Flow<Int> = dao.observeNewWordCount()
    fun observeMasteredCount(): Flow<Int> = dao.observeMasteredCount()
    fun observeLearningCount(): Flow<Int> = dao.observeLearningCount()

    suspend fun getDueReviews(): List<VocabWithProgress> = dao.getDueReviews(engine.endOfToday())

    suspend fun getNewWords(limit: Int): List<VocabEntity> =
        dao.getNewItems(VocabEntity.TYPE_WORD, limit)

    /** Day 0: the user has just studied a new word. */
    suspend fun markLearned(vocabId: Long) {
        dao.upsertProgress(engine.startLearning(vocabId))
    }

    /** A review answer: "I Knew It" (correct = true) or "Forgot" (correct = false). */
    suspend fun submitReview(current: UserProgress, correct: Boolean): UserProgress {
        val updated = engine.review(current, correct)
        dao.upsertProgress(updated)
        return updated
    }
}
