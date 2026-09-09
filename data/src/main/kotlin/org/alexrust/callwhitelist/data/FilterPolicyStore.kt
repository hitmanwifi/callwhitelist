package org.alexrust.callwhitelist.data

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.FilterProfile
import org.alexrust.callwhitelist.model.FilterSnapshot
import org.alexrust.callwhitelist.preferences.UserPreferences

class FilterPolicyStore(context: Context) {
    private val coordinator = FilterSnapshotCoordinator.get(context)
    private val userPreferences = UserPreferences(context.applicationContext)
    val snapshot: StateFlow<FilterSnapshot> = coordinator.snapshot

    suspend fun updateProfile(profile: FilterProfile) {
        check(coordinator.update { current -> current.copy(
            profiles = current.profiles
                .filterNot { it.id == profile.id }
                .plus(profile),
        ) }) { "Unable to persist filter profile" }
    }

    suspend fun setFilteringEnabled(value: Boolean) {
        userPreferences.setFilteringEnabled(value)
        check(coordinator.update { current -> current.copy(
            filteringEnabled = value,
        ) }) { "Unable to persist filtering state" }
    }

}
