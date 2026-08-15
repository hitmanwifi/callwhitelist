package org.alexrust.callwhitelist.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.alexrust.callwhitelist.database.CallLogEntity
import org.alexrust.callwhitelist.database.WhiteListDatabaseProvider
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.CallLogEntry
import org.alexrust.callwhitelist.model.FilterResult
import org.alexrust.callwhitelist.model.MatchSource

class CallLogStore(context: Context) {
    private val dao = WhiteListDatabaseProvider.get(context).callLogDao()

    val entries: Flow<List<CallLogEntry>> = dao.observeAll().map { entities ->
        entities.map { it.toModel() }
    }

    fun observeBlockedCountSince(sinceMillis: Long): Flow<Int> =
        dao.observeBlockedCountSince(sinceMillis)

    suspend fun appendIfAbsent(entry: CallLogEntry) {
        dao.insertIfAbsent(entry.toEntity())
    }

    suspend fun clear() {
        dao.clear()
    }
}

private fun CallLogEntity.toModel(): CallLogEntry = CallLogEntry(
    timestampMillis = timestampMillis,
    number = number,
    result = FilterResult(
        decision = runCatching { CallDecision.valueOf(decision) }
            .getOrDefault(CallDecision.BLOCK),
        source = runCatching { MatchSource.valueOf(source) }
            .getOrDefault(MatchSource.DEFAULT),
        reason = reason,
    ),
)

private fun CallLogEntry.toEntity(): CallLogEntity = CallLogEntity(
    timestampMillis = timestampMillis,
    number = number,
    decision = result.decision.name,
    source = result.source.name,
    reason = result.reason,
)
