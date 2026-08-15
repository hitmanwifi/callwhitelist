package org.alexrust.callwhitelist.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.alexrust.callwhitelist.model.OverviewPeriod
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class OverviewPeriodStart {
    operator fun invoke(
        period: OverviewPeriod,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Long = when (period) {
        OverviewPeriod.TODAY -> {
            val localNow = now.toLocalDateTime(timeZone)
            LocalDateTime(localNow.date, LocalTime(0, 0))
                .toInstant(timeZone)
                .toEpochMilliseconds()
        }

        OverviewPeriod.LAST_7_DAYS -> (now - 7.days).toEpochMilliseconds()
        OverviewPeriod.LAST_30_DAYS -> (now - 30.days).toEpochMilliseconds()
        OverviewPeriod.ALL -> Long.MIN_VALUE
    }
}
