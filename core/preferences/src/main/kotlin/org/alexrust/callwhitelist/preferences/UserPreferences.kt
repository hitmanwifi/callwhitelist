package org.alexrust.callwhitelist.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    private val contactsAllowedKey = booleanPreferencesKey("contacts_allowed")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val lastJournalViewedAtMillisKey = longPreferencesKey("last_journal_viewed_at_millis")
    private val overviewPeriodKey = stringPreferencesKey("overview_period")
    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")

    val contactsAllowed: Flow<Boolean> = context.userPreferencesDataStore.data.map {
        it[contactsAllowedKey] ?: true
    }

    val onboardingCompleted: Flow<Boolean> = context.userPreferencesDataStore.data.map {
        it[onboardingCompletedKey] ?: false
    }

    val lastJournalViewedAtMillis: Flow<Long> = context.userPreferencesDataStore.data.map {
        it[lastJournalViewedAtMillisKey] ?: 0L
    }

    val overviewPeriod: Flow<String> = context.userPreferencesDataStore.data.map {
        it[overviewPeriodKey] ?: DEFAULT_OVERVIEW_PERIOD
    }

    val notificationsEnabled: Flow<Boolean> = context.userPreferencesDataStore.data.map {
        it[notificationsEnabledKey] ?: false
    }

    suspend fun setContactsAllowed(value: Boolean) {
        context.userPreferencesDataStore.edit { it[contactsAllowedKey] = value }
    }

    suspend fun setOnboardingCompleted(value: Boolean) {
        context.userPreferencesDataStore.edit { it[onboardingCompletedKey] = value }
    }

    suspend fun markJournalViewed(timestampMillis: Long) {
        context.userPreferencesDataStore.edit {
            it[lastJournalViewedAtMillisKey] = timestampMillis
        }
    }

    suspend fun setOverviewPeriod(value: String) {
        context.userPreferencesDataStore.edit { it[overviewPeriodKey] = value }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.userPreferencesDataStore.edit { it[notificationsEnabledKey] = value }
    }
}

private const val DEFAULT_OVERVIEW_PERIOD = "today"
