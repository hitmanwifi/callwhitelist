package org.alexrust.callwhitelist.model

data class CallLogEntry(
    val timestampMillis: Long,
    val number: String?,
    val result: FilterResult,
)
