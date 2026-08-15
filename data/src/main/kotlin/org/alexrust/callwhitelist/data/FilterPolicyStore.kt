package org.alexrust.callwhitelist.data

import android.content.Context
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.FilterProfile
import org.alexrust.callwhitelist.model.FilterSnapshot
import org.alexrust.callwhitelist.preferences.UserPreferences

class FilterPolicyStore(context: Context) {
    private val snapshotStore = FilterSnapshotStore(context.applicationContext)
    private val userPreferences = UserPreferences(context.applicationContext)
    private val state = MutableStateFlow(snapshotStore.read() ?: defaultSnapshot())

    val snapshot: StateFlow<FilterSnapshot> = state.asStateFlow()

    suspend fun updateProfile(profile: FilterProfile) {
        val current = snapshotStore.read() ?: state.value
        val updated = current.copy(
            version = Clock.System.now().toEpochMilliseconds(),
            profiles = current.profiles
                .filterNot { it.id == profile.id }
                .plus(profile),
        )
        snapshotStore.write(updated)
        state.value = updated
    }

    suspend fun setFilteringEnabled(value: Boolean) {
        userPreferences.setFilteringEnabled(value)
        val current = snapshotStore.read() ?: state.value
        val updated = current.copy(
            version = Clock.System.now().toEpochMilliseconds(),
            filteringEnabled = value,
        )
        snapshotStore.write(updated)
        state.value = updated
    }

    private fun defaultSnapshot(): FilterSnapshot = FilterSnapshot(
        version = 0,
        profiles = listOf(
            FilterProfile(
                id = DEFAULT_PROFILE_ID,
                name = "Default",
                defaultDecision = CallDecision.BLOCK,
            ),
        ),
        filteringEnabled = true,
    )

    private companion object {
        const val DEFAULT_PROFILE_ID = 1L
    }
}
