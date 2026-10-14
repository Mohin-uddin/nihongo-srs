package com.nihongo.srs.data

import java.time.LocalDate

/** Streak maths, kept pure so it can be unit tested. */
object Streak {

    /**
     * Days in a row with study, ending today. If today has nothing yet, the
     * streak ending yesterday still counts (you have until midnight).
     */
    fun current(activeDates: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in activeDates) today else today.minusDays(1)
        var count = 0
        while (day in activeDates) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** Longest run of consecutive study days ever. */
    fun best(activeDates: Set<LocalDate>): Int {
        var best = 0
        for (d in activeDates) {
            if (d.minusDays(1) in activeDates) continue // not the start of a run
            var len = 0
            var x = d
            while (x in activeDates) { len++; x = x.plusDays(1) }
            best = maxOf(best, len)
        }
        return best
    }
}
