package org.alexrust.callwhitelist.domain

import org.alexrust.callwhitelist.model.FilterSettings
import org.alexrust.callwhitelist.model.NumberRule

interface FilterRepository {
    suspend fun getRules(): List<NumberRule>
    suspend fun addRule(rule: NumberRule)
    suspend fun getSettings(): FilterSettings
}
