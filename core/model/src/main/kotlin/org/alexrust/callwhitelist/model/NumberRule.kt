package org.alexrust.callwhitelist.model

data class NumberRule(
    val id: Long = 0,
    val number: String,
    val label: String = "",
    val enabled: Boolean = true,
    val decision: CallDecision = CallDecision.ALLOW,
    val expiresAtMillis: Long? = null,
)
