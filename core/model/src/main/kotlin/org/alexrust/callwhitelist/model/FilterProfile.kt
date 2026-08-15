package org.alexrust.callwhitelist.model

data class FilterProfile(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val activeWindow: TimeWindow? = null,
    val defaultDecision: CallDecision = CallDecision.BLOCK,
    val rules: List<FilterPolicyRule> = emptyList(),
)
