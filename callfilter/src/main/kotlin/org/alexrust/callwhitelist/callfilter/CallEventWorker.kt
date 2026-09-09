package org.alexrust.callwhitelist.callfilter

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import org.alexrust.callwhitelist.data.CallLogStore
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.CallLogEntry
import org.alexrust.callwhitelist.model.FilterResult
import org.alexrust.callwhitelist.model.MatchSource

class CallEventWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val event = CallEvent.from(inputData)
        if (CallLogStore(applicationContext).appendIfAbsent(event.toEntry())) {
            BlockedCallNotifier(applicationContext).notifyIfEnabled(event)
        }
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        const val UNIQUE_WORK_PREFIX = "call-event-"
    }
}

data class CallEvent(
    val eventId: String,
    val timestampMillis: Long,
    val number: String?,
    val decision: CallDecision,
    val source: MatchSource,
    val reason: String,
) {
    fun toData(): Data = Data.Builder()
        .putString("eventId", eventId)
        .putLong("timestamp", timestampMillis)
        .putString("number", number)
        .putString("decision", decision.name)
        .putString("source", source.name)
        .putString("reason", reason)
        .build()

    fun toEntry() = CallLogEntry(
        eventId = eventId,
        timestampMillis = timestampMillis,
        number = number,
        result = FilterResult(decision, source, reason),
    )

    companion object {
        fun from(data: Data) = CallEvent(
            eventId = requireNotNull(data.getString("eventId")),
            timestampMillis = data.getLong("timestamp", 0),
            number = data.getString("number"),
            decision = CallDecision.valueOf(requireNotNull(data.getString("decision"))),
            source = MatchSource.valueOf(requireNotNull(data.getString("source"))),
            reason = requireNotNull(data.getString("reason")),
        )
    }
}
