package org.alexrust.callwhitelist.model

enum class CallDecision { ALLOW, BLOCK, SILENCE }

enum class OverviewPeriod(val storageValue: String) {
    TODAY("today"),
    LAST_7_DAYS("last_7_days"),
    LAST_30_DAYS("last_30_days"),
    ALL("all"),
    ;

    companion object {
        fun fromStorage(value: String): OverviewPeriod =
            entries.firstOrNull { it.storageValue == value } ?: TODAY
    }
}

const val OPEN_JOURNAL_ACTION = "org.alexrust.callwhitelist.action.OPEN_JOURNAL"

enum class MatchSource { EMERGENCY, EXPLICIT_NUMBER, SPECIAL_LIST, CONTACT, UNKNOWN, HIDDEN, DEFAULT }

enum class PolicyMatchType {
    EXACT_NUMBER,
    CONTACT,
    UNKNOWN_NUMBER,
    HIDDEN_NUMBER,
    SPECIAL_LIST,
}

data class PolicyCondition(
    val type: PolicyMatchType,
    val value: String? = null,
)

data class NumberRule(
    val id: Long = 0,
    val number: String,
    val label: String = "",
    val enabled: Boolean = true,
    val decision: CallDecision = CallDecision.ALLOW,
)

data class FilterPolicyRule(
    val id: Long = 0,
    val condition: PolicyCondition,
    val label: String = "",
    val enabled: Boolean = true,
    val decision: CallDecision = CallDecision.ALLOW,
    val priority: Int = 0,
    val timeWindow: TimeWindow? = null,
)

data class FilterProfile(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val activeWindow: TimeWindow? = null,
    val defaultDecision: CallDecision = CallDecision.BLOCK,
    val rules: List<FilterPolicyRule> = emptyList(),
)

data class FilterSnapshot(
    val version: Long,
    val profiles: List<FilterProfile>,
    val contactsAllowed: Boolean = true,
    val emergencyNumbersAlwaysAllowed: Boolean = true,
    val filteringEnabled: Boolean = true,
)

data class FilterSettings(
    val contactsAllowed: Boolean = true,
    val unknownDecision: CallDecision = CallDecision.BLOCK,
    val hiddenDecision: CallDecision = CallDecision.BLOCK,
    val emergencyNumbersAlwaysAllowed: Boolean = true,
)

data class FilterResult(
    val decision: CallDecision,
    val source: MatchSource,
    val reason: String,
)

data class TimeWindow(
    val daysOfWeek: Set<Int>,
    val startMinutes: Int,
    val endMinutes: Int,
    val enabled: Boolean = true,
)

data class CallLogEntry(
    val timestampMillis: Long,
    val number: String?,
    val result: FilterResult,
)
