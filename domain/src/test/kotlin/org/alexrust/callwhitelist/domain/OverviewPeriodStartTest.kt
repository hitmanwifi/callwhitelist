package org.alexrust.callwhitelist.domain

import kotlinx.datetime.TimeZone
import org.alexrust.callwhitelist.model.OverviewPeriod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class OverviewPeriodStartTest {
    private val now = Instant.parse("2026-08-15T12:00:00Z")
    private val start = OverviewPeriodStart()

    @Test
    fun todayUsesLocalMidnight() {
        assertEquals(
            Instant.parse("2026-08-15T00:00:00Z").toEpochMilliseconds(),
            start(OverviewPeriod.TODAY, now, TimeZone.UTC),
        )
    }

    @Test
    fun relativePeriodsUseExplicitDurations() {
        assertEquals(
            Instant.parse("2026-08-08T12:00:00Z").toEpochMilliseconds(),
            start(OverviewPeriod.LAST_7_DAYS, now, TimeZone.UTC),
        )
        assertEquals(
            Instant.parse("2026-07-16T12:00:00Z").toEpochMilliseconds(),
            start(OverviewPeriod.LAST_30_DAYS, now, TimeZone.UTC),
        )
    }

    @Test
    fun allTimeHasNoLowerBound() {
        assertEquals(Long.MIN_VALUE, start(OverviewPeriod.ALL, now, TimeZone.UTC))
    }
}
