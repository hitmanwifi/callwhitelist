package org.alexrust.callwhitelist.model

data class FilterSnapshot(
    val version: Long,
    val profiles: List<FilterProfile>,
    val contactsAllowed: Boolean = true,
    val emergencyNumbersAlwaysAllowed: Boolean = true,
    val filteringEnabled: Boolean = true,
)
