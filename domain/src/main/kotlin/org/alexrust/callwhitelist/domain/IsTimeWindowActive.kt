package org.alexrust.callwhitelist.domain

import org.alexrust.callwhitelist.model.TimeWindow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus

class IsTimeWindowActive {
    operator fun invoke(window: TimeWindow, day: DayOfWeek, time: LocalTime): Boolean {
        if (!window.enabled || day.ordinal + 1 !in window.daysOfWeek) return false
        val current = time.hour * 60 + time.minute
        return if (window.startMinutes == window.endMinutes) {
            true
        } else if (window.startMinutes < window.endMinutes) {
            current >= window.startMinutes && current < window.endMinutes
        } else {
            current >= window.startMinutes
        }
    }

    operator fun invoke(window: TimeWindow, dateTime: LocalDateTime): Boolean {
        if (!window.enabled) return false

        val currentDay = dateTime.date.dayOfWeek.ordinal + 1
        val currentMinutes = dateTime.time.hour * 60 + dateTime.time.minute
        if (window.startMinutes == window.endMinutes) {
            return currentDay in window.daysOfWeek
        }

        if (window.startMinutes < window.endMinutes) {
            return currentDay in window.daysOfWeek &&
                currentMinutes >= window.startMinutes &&
                currentMinutes < window.endMinutes
        }

        val startsToday = currentDay in window.daysOfWeek && currentMinutes >= window.startMinutes
        if (startsToday) return true

        val previousDay = dateTime.date.minus(DatePeriod(days = 1)).dayOfWeek.ordinal + 1
        return previousDay in window.daysOfWeek && currentMinutes < window.endMinutes
    }
}
