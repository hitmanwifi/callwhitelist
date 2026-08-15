package org.alexrust.callwhitelist.data

import org.alexrust.callwhitelist.domain.FilterRepository
import org.alexrust.callwhitelist.model.FilterSettings
import org.alexrust.callwhitelist.model.NumberRule
import android.content.Context
import android.content.SharedPreferences

class InMemoryFilterRepository : FilterRepository {
    private val rules = mutableListOf<NumberRule>()
    override suspend fun getRules(): List<NumberRule> = rules.toList()
    override suspend fun addRule(rule: NumberRule) { rules += rule }
    override suspend fun getSettings(): FilterSettings = FilterSettings()
}

class SharedPreferencesFilterRepository(context: Context) : FilterRepository {
    private val preferences: SharedPreferences = context.getSharedPreferences("filter_rules", Context.MODE_PRIVATE)

    override suspend fun getRules(): List<NumberRule> = preferences.all.mapNotNull { (key, value) ->
        if (!key.startsWith("number:")) return@mapNotNull null
        NumberRule(number = key.removePrefix("number:"), label = value as? String ?: "")
    }

    override suspend fun addRule(rule: NumberRule) {
        preferences.edit().putString("number:${rule.number}", rule.label).apply()
    }

    override suspend fun getSettings() = org.alexrust.callwhitelist.model.FilterSettings(
        contactsAllowed = preferences.getBoolean("contacts_allowed", true),
    )
}
