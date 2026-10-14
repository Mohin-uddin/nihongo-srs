package com.nihongo.srs.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakTest {
    private val today = LocalDate.of(2026, 10, 14)
    private fun days(vararg back: Long) = back.map { today.minusDays(it) }.toSet()

    @Test fun countsConsecutiveDaysEndingToday() = assertEquals(3, Streak.current(days(0, 1, 2, 4), today))
    @Test fun todayNotStudiedYetKeepsYesterdaysStreak() = assertEquals(2, Streak.current(days(1, 2), today))
    @Test fun brokenStreakIsZero() = assertEquals(0, Streak.current(days(2, 3), today))
    @Test fun bestRun() = assertEquals(4, Streak.best(days(0, 1, 5, 6, 7, 8, 10)))
}
