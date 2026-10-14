package com.nihongo.srs.srs

import com.nihongo.srs.data.UserProgress
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The 1-4-7 spaced repetition engine.
 *
 * Schedule (exactly as specified):
 *
 * | When          | Answer  | New stage | Next review          |
 * |---------------|---------|-----------|----------------------|
 * | Day 0 (learn) |   -     | 0         | +1 day  -> Day 1     |
 * | Day 1         | correct | 1         | +3 days -> Day 4     |
 * | Day 4         | correct | 2         | +3 days -> Day 7     |
 * | Day 7         | correct | 3         | +7 days -> Day 14    |
 * | Day 14        | correct | 4         | Mastered (no more reviews) |
 * | any review    | wrong   | 0         | +1 day (tomorrow)    |
 *
 * All dates are whole calendar days in the phone's time zone. `nextReview`
 * is stored as local midnight of the due day, so a word due "tomorrow" shows up
 * as soon as the day starts, whatever time it was learned.
 *
 * Reviewing a word late (overdue) still counts: the next interval is measured
 * from the day you actually reviewed it.
 *
 * `clock` and `zone` are injectable so the logic can be unit tested.
 */
class SpacedRepetitionEngine(
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    companion object {
        /** Stage at which a word is considered mastered. */
        const val MASTERED_STAGE = 4

        /**
         * Days to wait after reaching each stage.
         * index = stage the word has just reached (0..3).
         */
        private val INTERVAL_DAYS = intArrayOf(
            1, // stage 0: learned / reset  -> review tomorrow (Day 1)
            3, // stage 1: Day 1 correct    -> Day 4
            3, // stage 2: Day 4 correct    -> Day 7
            7, // stage 3: Day 7 correct    -> Day 14
        )

        /** Human-friendly label for a stage, in English + Bengali. */
        fun stageLabel(stage: Int, mastered: Boolean): String = when {
            mastered || stage >= MASTERED_STAGE -> "Mastered • আয়ত্ত"
            stage == 0 -> "Stage 0 • নতুন"
            else -> "Stage $stage • ধাপ $stage"
        }
    }

    /** Day 0: create the progress row for a word the user just learned. */
    fun startLearning(vocabId: Long): UserProgress {
        val now = clock()
        return UserProgress(
            vocabId = vocabId,
            stage = 0,
            lastReviewed = now,
            nextReview = startOfDayPlus(now, INTERVAL_DAYS[0]),
            isMastered = false,
        )
    }

    /**
     * Applies one review answer and returns the updated progress.
     *
     * @param correct true for "I Knew It", false for "Forgot".
     */
    fun review(current: UserProgress, correct: Boolean): UserProgress {
        val now = clock()

        if (!correct) {
            // Wrong at any stage -> back to Stage 0, review again tomorrow.
            return current.copy(
                stage = 0,
                lastReviewed = now,
                nextReview = startOfDayPlus(now, INTERVAL_DAYS[0]),
                isMastered = false,
            )
        }

        val newStage = (current.stage + 1).coerceAtMost(MASTERED_STAGE)
        return if (newStage >= MASTERED_STAGE) {
            // Stage 4+: mastered. nextReview is kept far in the future so it never shows as due.
            current.copy(
                stage = MASTERED_STAGE,
                lastReviewed = now,
                nextReview = Long.MAX_VALUE,
                isMastered = true,
            )
        } else {
            current.copy(
                stage = newStage,
                lastReviewed = now,
                nextReview = startOfDayPlus(now, INTERVAL_DAYS[newStage]),
                isMastered = false,
            )
        }
    }

    /** Is this word due on or before today? */
    fun isDue(progress: UserProgress): Boolean =
        !progress.isMastered && progress.nextReview <= endOfToday()

    /** Last millisecond of today in the phone's time zone (used by the DAO due query). */
    fun endOfToday(): Long = startOfDayPlus(clock(), 1) - 1

    /** Whole days from today until the next review (0 = today, negative = overdue). */
    fun daysUntilNextReview(progress: UserProgress): Long {
        if (progress.isMastered) return Long.MAX_VALUE
        val today = toLocalDate(clock())
        val due = toLocalDate(progress.nextReview)
        return due.toEpochDay() - today.toEpochDay()
    }

    private fun toLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** Local midnight of (the day of [millis] + [days]). */
    private fun startOfDayPlus(millis: Long, days: Int): Long =
        toLocalDate(millis).plusDays(days.toLong())
            .atStartOfDay(zone).toInstant().toEpochMilli()
}
