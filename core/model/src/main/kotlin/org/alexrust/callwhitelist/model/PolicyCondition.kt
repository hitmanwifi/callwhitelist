package org.alexrust.callwhitelist.model

data class PolicyCondition(
    val type: PolicyMatchType,
    val value: String? = null,
)
