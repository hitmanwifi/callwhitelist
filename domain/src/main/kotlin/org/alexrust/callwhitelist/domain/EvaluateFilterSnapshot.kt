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
        nowMillis: Long? = null,
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

        if (profile == null) {
            return FilterResult(CallDecision.ALLOW, MatchSource.DEFAULT, "No active profile")
        }

        val number = rawNumber?.let { normalize(it) }
        val activeRules = profile.rules.asSequence()
            .filter { it.enabled }
            .filter { rule ->
                val timeWindow = rule.timeWindow
                timeWindow == null || isTimeWindowActive(timeWindow, now)
            }
            .filter { rule ->
                val expiresAtMillis = rule.expiresAtMillis
                expiresAtMillis == null || nowMillis == null || nowMillis < expiresAtMillis
            }
            .toList()
        val matchingRule = when {
            number != null -> activeRules.matching(PolicyMatchType.EXACT_NUMBER, number)
            else -> null
        } ?: when {
            isContact && snapshot.contactsAllowed -> null
            rawNumber == null -> activeRules.matching(PolicyMatchType.HIDDEN_NUMBER, number)
            else -> activeRules.matching(PolicyMatchType.UNKNOWN_NUMBER, number)
        }

        if (isContact && snapshot.contactsAllowed && matchingRule == null) {
            return FilterResult(CallDecision.ALLOW, MatchSource.CONTACT, "Contact")
        }

        if (matchingRule != null) {
            return FilterResult(
                decision = matchingRule.decision,
                source = matchingRule.condition.type.toMatchSource(),
                reason = matchingRule.label.ifBlank { profile.name },
            )
        }

        return FilterResult(
            decision = profile.defaultDecision,
            source = MatchSource.DEFAULT,
            reason = profile.name,
        )
    }

    private fun List<FilterPolicyRule>.matching(type: PolicyMatchType, number: String?): FilterPolicyRule? =
        asSequence().filter { matches(it, type, number) }.maxWithOrNull(
            compareBy<FilterPolicyRule> { it.priority }
                .thenBy { specificity(it.condition.type) }
                .thenBy { it.id },
        )

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
