package org.alexrust.callwhitelist.model

data class TimeWindow(
    val daysOfWeek: Set<Int>,
    val startMinutes: Int,
    val endMinutes: Int,
    val enabled: Boolean = true,
)
