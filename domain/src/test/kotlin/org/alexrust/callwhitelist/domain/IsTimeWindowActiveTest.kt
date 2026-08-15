package org.alexrust.callwhitelist.domain

import org.alexrust.callwhitelist.model.TimeWindow
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsTimeWindowActiveTest {
    @Test fun supportsOvernightWindow() {
        val window = TimeWindow(setOf(DayOfWeek.FRIDAY.ordinal + 1), 22 * 60, 7 * 60)
        assertTrue(IsTimeWindowActive()(window, DayOfWeek.FRIDAY, LocalTime(23, 0)))
    }

    @Test fun overnightWindowContinuesIntoNextDay() {
        val window = TimeWindow(setOf(DayOfWeek.FRIDAY.ordinal + 1), 22 * 60, 7 * 60)
        val saturdayMorning = LocalDateTime(2026, 8, 15, 6, 59)

        assertTrue(IsTimeWindowActive()(window, saturdayMorning))
        assertFalse(IsTimeWindowActive()(window, LocalDateTime(2026, 8, 15, 7, 0)))
    }
}
