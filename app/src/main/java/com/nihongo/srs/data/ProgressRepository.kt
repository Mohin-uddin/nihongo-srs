package com.nihongo.srs.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Records what you study each day and serves the numbers for Progress and Home. */
class ProgressRepository(private val db: AppDatabase) {

    private val dao = db.statsDao()

    private fun today(): String = LocalDate.now().toString()

    /** Adds to today's counters. examPercent = -1 means "no exam". */
    suspend fun log(
        reviewed: Int = 0, correct: Int = 0, learned: Int = 0,
        exams: Int = 0, examPercent: Int = -1, spoken: Int = 0, kana: Int = 0,
    ) {
        val date = today()
        db.withTransaction {
            dao.ensureDay(date)
            dao.addToDay(date, reviewed, correct, learned, exams, examPercent, spoken, kana)
        }
    }

    suspend fun saveExam(scope: String, percent: Int, total: Int) {
        dao.insertExam(ExamResult(timestamp = System.currentTimeMillis(), scope = scope, percent = percent, total = total))
        if (scope == "KANA") log(kana = 1) else log(exams = 1, examPercent = percent)
    }

    fun observeToday(): Flow<StudyDay> =
        dao.observeDay(today()).map { it ?: StudyDay(today()) }

    fun observeAllDays(): Flow<List<StudyDay>> = dao.observeAllDays()

    /** Current streak in days. */
    fun observeStreak(): Flow<Int> = dao.observeAllDays().map { days ->
        Streak.current(activeDates(days), LocalDate.now())
    }

    fun activeDates(days: List<StudyDay>): Set<LocalDate> =
        days.filter { it.isActive }.map { LocalDate.parse(it.date) }.toSet()

    fun observeRecentExams(limit: Int = 10): Flow<List<ExamResult>> = dao.observeRecentExams(limit)

    fun observeLevelProgress(): Flow<List<LevelProgress>> = dao.observeLevelProgress()

    fun observeStageCounts(): Flow<List<StageCount>> = dao.observeStageCounts()
}
