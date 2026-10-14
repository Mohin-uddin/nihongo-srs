package com.nihongo.srs.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Walks a word through the full 1-4-7 schedule with a fake clock. */
class SpacedRepetitionEngineTest {

    private val zone = ZoneId.of("Asia/Dhaka")
    private val day0 = LocalDate.of(2026, 1, 1)
    private var now = at(day0, hour = 20)
    private val engine = SpacedRepetitionEngine(clock = { now }, zone = zone)

    private fun at(date: LocalDate, hour: Int = 9) =
        date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun midnight(date: LocalDate) =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun fullScheduleReachesMastered() {
        var p = engine.startLearning(1)
        assertEquals(0, p.stage)
        assertEquals(midnight(day0.plusDays(1)), p.nextReview)   // Day 1

        now = at(day0.plusDays(1)); assertTrue(engine.isDue(p))
        p = engine.review(p, correct = true)
        assertEquals(1, p.stage)
        assertEquals(midnight(day0.plusDays(4)), p.nextReview)   // Day 4

        now = at(day0.plusDays(3)); assertFalse(engine.isDue(p)) // not yet
        now = at(day0.plusDays(4)); assertTrue(engine.isDue(p))
        p = engine.review(p, correct = true)
        assertEquals(2, p.stage)
        assertEquals(midnight(day0.plusDays(7)), p.nextReview)   // Day 7

        now = at(day0.plusDays(7))
        p = engine.review(p, correct = true)
        assertEquals(3, p.stage)
        assertEquals(midnight(day0.plusDays(14)), p.nextReview)  // Day 14

        now = at(day0.plusDays(14))
        p = engine.review(p, correct = true)
        assertEquals(4, p.stage)
        assertTrue(p.isMastered)
        assertFalse(engine.isDue(p))
    }

    @Test
    fun wrongAnswerResetsToStageZeroTomorrow() {
        var p = engine.startLearning(1)
        now = at(day0.plusDays(1)); p = engine.review(p, true)
        now = at(day0.plusDays(4)); p = engine.review(p, false)
        assertEquals(0, p.stage)
        assertFalse(p.isMastered)
        assertEquals(midnight(day0.plusDays(5)), p.nextReview)
    }
}
