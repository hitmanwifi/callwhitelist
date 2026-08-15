package org.alexrust.callwhitelist.domain

import kotlinx.datetime.LocalDateTime
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.FilterPolicyRule
import org.alexrust.callwhitelist.model.FilterResult
import org.alexrust.callwhitelist.model.FilterSnapshot
import org.alexrust.callwhitelist.model.MatchSource
import org.alexrust.callwhitelist.model.PolicyMatchType

class EvaluateFilterSnapshot(
    private val normalize: NormalizePhoneNumber = NormalizePhoneNumber(),
    private val isTimeWindowActive: IsTimeWindowActive = IsTimeWindowActive(),
) {
    operator fun invoke(
        snapshot: FilterSnapshot,
        rawNumber: String?,
        isContact: Boolean,
        now: LocalDateTime,
        isEmergency: Boolean = false,
    ): FilterResult {
        if (!snapshot.filteringEnabled) {
            return FilterResult(
                decision = CallDecision.ALLOW,
                source = MatchSource.DEFAULT,
                reason = "Filtering disabled",
            )
        }

        if (isEmergency && snapshot.emergencyNumbersAlwaysAllowed) {
            return FilterResult(
                decision = CallDecision.ALLOW,
                source = MatchSource.EMERGENCY,
                reason = "Emergency number",
            )
        }

        val profile = snapshot.profiles
            .asSequence()
            .filter { it.enabled }
            .filter {
                val activeWindow = it.activeWindow
                activeWindow == null || isTimeWindowActive(activeWindow, now)
            }
            .maxWithOrNull(compareBy<org.alexrust.callwhitelist.model.FilterProfile> { it.priority }.thenBy { it.id })

        val number = rawNumber?.let { normalize(it) }
        val matchType = when {
            rawNumber == null -> PolicyMatchType.HIDDEN_NUMBER
            number == null -> PolicyMatchType.UNKNOWN_NUMBER
            isContact -> PolicyMatchType.CONTACT
            else -> PolicyMatchType.EXACT_NUMBER
        }

        val matchingRule = profile?.rules
            ?.asSequence()
            ?.filter { it.enabled }
            ?.filter {
                val timeWindow = it.timeWindow
                timeWindow == null || isTimeWindowActive(timeWindow, now)
            }
            ?.filter { matches(it, matchType, number) }
            ?.maxWithOrNull(
                compareBy<FilterPolicyRule> { it.priority }
                    .thenBy { specificity(it.condition.type) }
                    .thenBy { it.id },
            )

        if (matchingRule != null) {
            return FilterResult(
                decision = matchingRule.decision,
                source = matchingRule.condition.type.toMatchSource(),
                reason = matchingRule.label.ifBlank { profile.name },
            )
        }

        return FilterResult(
            decision = profile?.defaultDecision ?: CallDecision.BLOCK,
            source = MatchSource.DEFAULT,
            reason = profile?.name ?: "No active profile",
        )
    }

    private fun matches(
        rule: FilterPolicyRule,
        matchType: PolicyMatchType,
        normalizedNumber: String?,
    ): Boolean {
        if (rule.condition.type != matchType) return false
        return when (matchType) {
            PolicyMatchType.EXACT_NUMBER ->
                normalizedNumber != null && normalize(rule.condition.value.orEmpty()) == normalizedNumber

            PolicyMatchType.CONTACT,
            PolicyMatchType.UNKNOWN_NUMBER,
            PolicyMatchType.HIDDEN_NUMBER,
            PolicyMatchType.SPECIAL_LIST,
            -> true
        }
    }

    private fun specificity(type: PolicyMatchType): Int = when (type) {
        PolicyMatchType.EXACT_NUMBER -> 5
        PolicyMatchType.CONTACT -> 4
        PolicyMatchType.SPECIAL_LIST -> 3
        PolicyMatchType.UNKNOWN_NUMBER -> 2
        PolicyMatchType.HIDDEN_NUMBER -> 1
    }

    private fun PolicyMatchType.toMatchSource(): MatchSource = when (this) {
        PolicyMatchType.EXACT_NUMBER -> MatchSource.EXPLICIT_NUMBER
        PolicyMatchType.CONTACT -> MatchSource.CONTACT
        PolicyMatchType.UNKNOWN_NUMBER -> MatchSource.UNKNOWN
        PolicyMatchType.HIDDEN_NUMBER -> MatchSource.HIDDEN
        PolicyMatchType.SPECIAL_LIST -> MatchSource.SPECIAL_LIST
    }
}
