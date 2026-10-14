package com.nihongo.srs.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * What you did on one calendar day. Used for the Progress screen, the streak
 * and today's goals. Added in DB version 3.
 */
@Entity(tableName = "study_day")
data class StudyDay(
    /** Local date as yyyy-MM-dd. */
    @PrimaryKey val date: String,
    @ColumnInfo(defaultValue = "0") val reviewed: Int = 0,
    @ColumnInfo(defaultValue = "0") val reviewedCorrect: Int = 0,
    @ColumnInfo(defaultValue = "0") val learned: Int = 0,
    @ColumnInfo(defaultValue = "0") val exams: Int = 0,
    /** Best exam score of the day in percent, -1 = no exam. */
    @ColumnInfo(defaultValue = "-1") val bestExamPercent: Int = -1,
    /** Phrases checked with the microphone. */
    @ColumnInfo(defaultValue = "0") val spoken: Int = 0,
    @ColumnInfo(defaultValue = "0") val kanaQuizzes: Int = 0,
)

/** True if anything was studied that day. */
val StudyDay.isActive: Boolean
    get() = reviewed + learned + exams + spoken + kanaQuizzes > 0

/** One finished exam. Added in DB version 3. */
@Entity(tableName = "exam_result")
data class ExamResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    /** "TODAY", "ALL" or "KANA". */
    val scope: String,
    val percent: Int,
    val total: Int,
)

/** Learned / mastered / total words of one JLPT level. */
data class LevelProgress(
    val level: String,
    val total: Int,
    val learned: Int,
    val mastered: Int,
)

/** How many learning words are at each SRS stage. */
data class StageCount(val stage: Int, val count: Int)
