package org.alexrust.callwhitelist.domain

import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.FilterResult
import org.alexrust.callwhitelist.model.MatchSource

class EvaluateIncomingCall(
    private val repository: FilterRepository,
    private val normalize: NormalizePhoneNumber = NormalizePhoneNumber(),
) {
    suspend operator fun invoke(rawNumber: String?, isContact: Boolean = false): FilterResult {
        val settings = repository.getSettings()
        if (rawNumber.isNullOrBlank()) {
            return FilterResult(settings.hiddenDecision, MatchSource.HIDDEN, "Hidden number")
        }
        val number = normalize(rawNumber)
            ?: return FilterResult(settings.unknownDecision, MatchSource.UNKNOWN, "Unknown number")
        val rule = repository.getRules().firstOrNull { it.enabled && normalize(it.number) == number }
        if (rule != null) return FilterResult(rule.decision, MatchSource.EXPLICIT_NUMBER, rule.label.ifBlank { "Explicit rule" })
        if (isContact && settings.contactsAllowed) return FilterResult(CallDecision.ALLOW, MatchSource.CONTACT, "Contact")
        return FilterResult(settings.unknownDecision, MatchSource.DEFAULT, "No matching rule")
    }
}
