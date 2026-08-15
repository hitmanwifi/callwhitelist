package org.alexrust.callwhitelist.domain

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.FilterPolicyRule
import org.alexrust.callwhitelist.model.FilterProfile
import org.alexrust.callwhitelist.model.FilterSnapshot
import org.alexrust.callwhitelist.model.MatchSource
import org.alexrust.callwhitelist.model.PolicyCondition
import org.alexrust.callwhitelist.model.PolicyMatchType

class EvaluateFilterSnapshotTest {
    private val now = LocalDateTime(2026, 8, 15, 12, 0)

    @Test
    fun exactNumberRuleWinsOverProfileDefault() {
        val snapshot = FilterSnapshot(
            version = 1,
            profiles = listOf(
                FilterProfile(
                    name = "Work hours",
                    defaultDecision = CallDecision.BLOCK,
                    rules = listOf(
                        FilterPolicyRule(
                            condition = PolicyCondition(PolicyMatchType.EXACT_NUMBER, "+79991234567"),
                            decision = CallDecision.ALLOW,
                        ),
                    ),
                ),
            ),
        )

        val result = EvaluateFilterSnapshot()(snapshot, "+7 (999) 123-45-67", false, now)

        assertEquals(CallDecision.ALLOW, result.decision)
        assertEquals(MatchSource.EXPLICIT_NUMBER, result.source)
    }

    @Test
    fun higherPriorityProfileWinsWhenSchedulesOverlap() {
        val snapshot = FilterSnapshot(
            version = 1,
            profiles = listOf(
                FilterProfile(name = "Default", priority = 1, defaultDecision = CallDecision.ALLOW),
                FilterProfile(name = "Night", priority = 2, defaultDecision = CallDecision.BLOCK),
            ),
        )

        val result = EvaluateFilterSnapshot()(snapshot, "+79991234567", false, now)

        assertEquals(CallDecision.BLOCK, result.decision)
        assertEquals("Night", result.reason)
    }

    @Test
    fun emergencyNumberIsAllowedByDefaultSafetyRule() {
        val result = EvaluateFilterSnapshot()(
            snapshot = FilterSnapshot(
                version = 1,
                profiles = listOf(FilterProfile(name = "Default", defaultDecision = CallDecision.BLOCK)),
            ),
            rawNumber = "112",
            isContact = false,
            now = now,
            isEmergency = true,
        )

        assertEquals(CallDecision.ALLOW, result.decision)
        assertEquals(MatchSource.EMERGENCY, result.source)
    }

    @Test
    fun disabledFilteringAllowsAnyCall() {
        val result = EvaluateFilterSnapshot()(
            snapshot = FilterSnapshot(
                version = 1,
                filteringEnabled = false,
                profiles = listOf(FilterProfile(name = "Default", defaultDecision = CallDecision.BLOCK)),
            ),
            rawNumber = "+79991234567",
            isContact = false,
            now = now,
        )

        assertEquals(CallDecision.ALLOW, result.decision)
        assertEquals(MatchSource.DEFAULT, result.source)
    }
}
