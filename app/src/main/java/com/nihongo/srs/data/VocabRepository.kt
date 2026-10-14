package com.nihongo.srs.data

import android.content.res.AssetManager
import androidx.room.withTransaction
import com.nihongo.srs.srs.SpacedRepetitionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Single entry point the ViewModels use for data.
 * Combines the DAO with the SRS engine so the rules live in one place.
 */
class VocabRepository(
    private val db: AppDatabase,
    private val engine: SpacedRepetitionEngine,
    private val assets: AssetManager,
    private val prefs: AppPrefs,
    private val progress: ProgressRepository,
) {
    private val dao = db.vocabDao()

    /**
     * Copies the bundled N5 / N4 words and phrases into the database the first
     * time, and again whenever the bundled lists are updated (new version).
     * Existing words keep their progress; only their text is refreshed.
     */
    suspend fun syncStarterContentIfNeeded() {
        if (prefs.contentVersion >= StarterContent.VERSION) return
        reloadStarterContent()
        prefs.contentVersion = StarterContent.VERSION
    }

    /** Returns how many new items were added. */
    suspend fun reloadStarterContent(): Int {
        val items = withContext(Dispatchers.IO) {
            StarterContent.parseWords(read(StarterContent.N5_FILE), "N5") +
                StarterContent.parseWords(read(StarterContent.N4_FILE), "N4") +
                StarterContent.parsePhrases(read(StarterContent.PHRASES_FILE))
        }
        return db.withTransaction {
            var added = 0
            for (v in items) {
                val id = dao.findId(v.japanese, v.type)
                if (id == null) {
                    if (dao.insertVocab(v) != -1L) added++
                } else {
                    dao.updateContent(
                        id, v.reading, v.romaji, v.meaning, v.meaningBn, v.level,
                        v.example, v.exampleReading, v.exampleEn, v.exampleBn, v.category
                    )
                }
            }
            added
        }
    }

    private fun read(file: String): String =
        assets.open(file).bufferedReader(Charsets.UTF_8).use { it.readText() }

    suspend fun addVocab(vocab: VocabEntity): Boolean = dao.insertVocab(vocab) != -1L

    suspend fun deleteVocab(vocab: VocabEntity) = dao.deleteVocab(vocab)

    suspend fun resetProgress(vocabId: Long) = dao.resetProgress(vocabId)

    fun observeAll(type: String): Flow<List<VocabWithProgress>> = dao.observeAllWithProgress(type)

    fun observePhrases(): Flow<List<VocabEntity>> = dao.observeByType(VocabEntity.TYPE_PHRASE)

    fun observeLearned(): Flow<List<VocabWithProgress>> = dao.observeLearned()

    suspend fun getLearned(): List<VocabWithProgress> = dao.getLearned()

    suspend fun getAllWords(): List<VocabEntity> = dao.getAllWords()

    /** Learned or reviewed today. */
    fun isFromToday(item: VocabWithProgress): Boolean {
        val p = item.progress ?: return false
        val start = engine.startOfToday()
        return p.learnedAt >= start || p.lastReviewed >= start
    }

    /** Last millisecond of today; pass a fresh value after midnight to refresh counts. */
    fun endOfToday(): Long = engine.endOfToday()

    fun observeDueCount(endOfToday: Long = engine.endOfToday()): Flow<Int> =
        dao.observeDueCount(endOfToday)

    suspend fun countDueToday(): Int = dao.countDue(engine.endOfToday())

    fun observeNewWordCount(level: String): Flow<Int> = dao.observeNewWordCount(level)
    fun observeMasteredCount(): Flow<Int> = dao.observeMasteredCount()
    fun observeLearningCount(): Flow<Int> = dao.observeLearningCount()

    suspend fun getDueReviews(): List<VocabWithProgress> = dao.getDueReviews(engine.endOfToday())

    suspend fun getNewWords(level: String, limit: Int): List<VocabEntity> =
        dao.getNewWords(level, limit)

    /** Day 0: the user has just studied a new word. */
    suspend fun markLearned(vocabId: Long) {
        dao.upsertProgress(engine.startLearning(vocabId))
        progress.log(learned = 1)
    }

    /** A review answer: "I Knew It" (correct = true) or "Forgot" (correct = false). */
    suspend fun submitReview(current: UserProgress, correct: Boolean, countAsReview: Boolean = true): UserProgress {
        val updated = engine.review(current, correct)
        dao.upsertProgress(updated)
        if (countAsReview) progress.log(reviewed = 1, correct = if (correct) 1 else 0)
        return updated
    }
}
