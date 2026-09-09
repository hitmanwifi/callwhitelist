package org.alexrust.callwhitelist.model

data class CallLogEntry(
    val eventId: String = "",
    val timestampMillis: Long,
    val number: String?,
    val result: FilterResult,
)
