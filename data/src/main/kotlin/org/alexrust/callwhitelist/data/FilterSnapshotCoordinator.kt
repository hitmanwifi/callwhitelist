package org.alexrust.callwhitelist.data

import android.content.Context
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.FilterProfile
import org.alexrust.callwhitelist.model.FilterSnapshot

/** The sole application-scoped writer for the screening snapshot. */
class FilterSnapshotCoordinator private constructor(context: Context) {
    private val snapshotStore = FilterSnapshotStore(context.applicationContext)
    private val mutex = Mutex()
    private val mutableSnapshot = MutableStateFlow(snapshotStore.read() ?: defaultSnapshot())
    val snapshot: StateFlow<FilterSnapshot> = mutableSnapshot.asStateFlow()

    suspend fun update(transform: (FilterSnapshot) -> FilterSnapshot): Boolean = mutex.withLock {
        val current = mutableSnapshot.value
        val updated = transform(current).copy(version = Clock.System.now().toEpochMilliseconds())
        if (!snapshotStore.write(updated)) return@withLock false
        mutableSnapshot.value = updated
        true
    }

    private fun defaultSnapshot() = FilterSnapshot(
        version = 0,
        profiles = listOf(FilterProfile(id = DEFAULT_PROFILE_ID, name = "Default", defaultDecision = CallDecision.BLOCK)),
    )

    companion object {
        private const val DEFAULT_PROFILE_ID = 1L
        @Volatile private var instance: FilterSnapshotCoordinator? = null

        fun get(context: Context): FilterSnapshotCoordinator = instance ?: synchronized(this) {
            instance ?: FilterSnapshotCoordinator(context.applicationContext).also { instance = it }
        }
    }
}
