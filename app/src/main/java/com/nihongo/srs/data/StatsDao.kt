package com.nihongo.srs.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Query("INSERT OR IGNORE INTO study_day (date) VALUES (:date)")
    suspend fun ensureDay(date: String)

    @Query(
        """
        UPDATE study_day SET
            reviewed = reviewed + :reviewed,
            reviewedCorrect = reviewedCorrect + :correct,
            learned = learned + :learned,
            exams = exams + :exams,
            bestExamPercent = MAX(bestExamPercent, :examPercent),
            spoken = spoken + :spoken,
            kanaQuizzes = kanaQuizzes + :kana
        WHERE date = :date
        """
    )
    suspend fun addToDay(
        date: String, reviewed: Int, correct: Int, learned: Int,
        exams: Int, examPercent: Int, spoken: Int, kana: Int,
    )

    @Query("SELECT * FROM study_day WHERE date = :date")
    fun observeDay(date: String): Flow<StudyDay?>

    @Query("SELECT * FROM study_day ORDER BY date DESC")
    fun observeAllDays(): Flow<List<StudyDay>>

    @Insert
    suspend fun insertExam(result: ExamResult)

    @Query("SELECT * FROM exam_result ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentExams(limit: Int): Flow<List<ExamResult>>

    @Query(
        """
        SELECT v.level AS level,
               COUNT(*) AS total,
               SUM(CASE WHEN p.vocabId IS NOT NULL THEN 1 ELSE 0 END) AS learned,
               SUM(CASE WHEN p.isMastered = 1 THEN 1 ELSE 0 END) AS mastered
        FROM vocab v LEFT JOIN user_progress p ON p.vocabId = v.id
        WHERE v.type = 'WORD'
        GROUP BY v.level
        """
    )
    fun observeLevelProgress(): Flow<List<LevelProgress>>

    @Query(
        """
        SELECT stage, COUNT(*) AS count FROM user_progress
        WHERE isMastered = 0 GROUP BY stage
        """
    )
    fun observeStageCounts(): Flow<List<StageCount>>
}
