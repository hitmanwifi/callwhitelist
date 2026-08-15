package org.alexrust.callwhitelist.model

data class FilterResult(
    val decision: CallDecision,
    val source: MatchSource,
    val reason: String,
)
