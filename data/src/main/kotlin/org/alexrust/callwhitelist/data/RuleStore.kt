package org.alexrust.callwhitelist.data

import android.content.Context
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.alexrust.callwhitelist.database.NumberRuleEntity
import org.alexrust.callwhitelist.database.WhiteListDatabaseProvider
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.FilterPolicyRule
import org.alexrust.callwhitelist.model.FilterProfile
import org.alexrust.callwhitelist.model.FilterSnapshot
import org.alexrust.callwhitelist.model.NumberRule
import org.alexrust.callwhitelist.model.PolicyCondition
import org.alexrust.callwhitelist.model.PolicyMatchType
import org.alexrust.callwhitelist.preferences.UserPreferences

interface RuleStore {
    val rules: Flow<List<NumberRule>>

    suspend fun add(rule: NumberRule): Long

    suspend fun update(rule: NumberRule)

    suspend fun delete(rule: NumberRule)

    val contactsAllowed: Flow<Boolean>

    suspend fun setContactsAllowed(value: Boolean)
}

class RoomRuleStore(context: Context) : RuleStore {
    private val appContext = context.applicationContext
    private val dao = WhiteListDatabaseProvider.get(context).numberRuleDao()
    private val coordinator = FilterSnapshotCoordinator.get(appContext)
    private val userPreferences = UserPreferences(appContext)
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val rules: Flow<List<NumberRule>> = dao.observeAll().map { entities ->
        entities.map { it.toModel() }
    }

    override val contactsAllowed: Flow<Boolean> = userPreferences.contactsAllowed

    init {
        refreshScope.launch { publishSnapshot() }
    }

    override suspend fun add(rule: NumberRule): Long {
        return dao.insert(rule.toEntity()).also { publishSnapshot() }
    }

    override suspend fun update(rule: NumberRule) {
        dao.update(rule.toEntity())
        publishSnapshot()
    }

    override suspend fun delete(rule: NumberRule) {
        dao.delete(rule.toEntity())
        publishSnapshot()
    }

    override suspend fun setContactsAllowed(value: Boolean) {
        userPreferences.setContactsAllowed(value)
        publishSnapshot()
    }

    private suspend fun publishSnapshot() {
        val contactsAreAllowed = userPreferences.contactsAllowed.first()
        val filteringIsEnabled = userPreferences.filteringEnabled.first()
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val numberRules = dao.getAll()
            .filter { it.enabled }
            .map { it.toModel() }
            .filter {
                val expiresAtMillis = it.expiresAtMillis
                expiresAtMillis == null || expiresAtMillis > nowMillis
            }
        val generatedRules = buildList {
            numberRules.forEach { rule ->
                add(
                    FilterPolicyRule(
                        id = rule.id,
                        condition = PolicyCondition(PolicyMatchType.EXACT_NUMBER, rule.number),
                        label = rule.label,
                        decision = rule.decision,
                        priority = EXPLICIT_NUMBER_PRIORITY,
                        expiresAtMillis = rule.expiresAtMillis,
                    ),
                )
            }
        }
        check(coordinator.update {
            val existingProfile = it.profiles.firstOrNull { profile -> profile.id == DEFAULT_PROFILE_ID }
            val preservedRules = existingProfile?.rules.orEmpty().filterNot { rule ->
                rule.condition.type == PolicyMatchType.EXACT_NUMBER
            }
            val profile = (existingProfile ?: FilterProfile(id = DEFAULT_PROFILE_ID, name = "Default"))
                .copy(rules = preservedRules + generatedRules)
            val profiles = it.profiles.filterNot { profile -> profile.id == DEFAULT_PROFILE_ID }.plus(profile)
            FilterSnapshot(
                version = it.version,
                profiles = profiles,
                contactsAllowed = contactsAreAllowed,
                emergencyNumbersAlwaysAllowed = it.emergencyNumbersAlwaysAllowed,
                filteringEnabled = filteringIsEnabled,
            )
        }) { "Unable to persist filter rules" }
    }

    private companion object {
        const val DEFAULT_PROFILE_ID = 1L
        const val EXPLICIT_NUMBER_PRIORITY = 20
    }
}

private fun NumberRuleEntity.toModel(): NumberRule = NumberRule(
    id = id,
    number = number,
    label = label,
    enabled = enabled,
    decision = runCatching { CallDecision.valueOf(decision) }
        .getOrDefault(CallDecision.ALLOW),
    expiresAtMillis = expiresAtMillis,
)

private fun NumberRule.toEntity(): NumberRuleEntity = NumberRuleEntity(
    id = id,
    number = number,
    label = label,
    enabled = enabled,
    decision = decision.name,
    expiresAtMillis = expiresAtMillis,
)
