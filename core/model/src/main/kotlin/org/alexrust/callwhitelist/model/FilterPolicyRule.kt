package org.alexrust.callwhitelist.model

data class FilterPolicyRule(
    val id: Long = 0,
    val condition: PolicyCondition,
    val label: String = "",
    val enabled: Boolean = true,
    val decision: CallDecision = CallDecision.ALLOW,
    val priority: Int = 0,
    val timeWindow: TimeWindow? = null,
    val expiresAtMillis: Long? = null,
)
