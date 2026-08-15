package org.alexrust.callwhitelist.model

data class FilterSettings(
    val contactsAllowed: Boolean = true,
    val unknownDecision: CallDecision = CallDecision.BLOCK,
    val hiddenDecision: CallDecision = CallDecision.BLOCK,
    val emergencyNumbersAlwaysAllowed: Boolean = true,
)
