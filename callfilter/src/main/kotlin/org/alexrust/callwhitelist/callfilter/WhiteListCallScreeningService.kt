package org.alexrust.callwhitelist.callfilter

import android.content.Context
import android.os.CancellationSignal
import android.os.SystemClock
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Job
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.alexrust.callwhitelist.data.FilterSnapshotStore
import org.alexrust.callwhitelist.domain.EvaluateFilterSnapshot
import org.alexrust.callwhitelist.domain.PhoneNumberVariants
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.FilterProfile
import org.alexrust.callwhitelist.model.FilterResult
import org.alexrust.callwhitelist.model.FilterSnapshot
import org.alexrust.callwhitelist.model.MatchSource

class WhiteListCallScreeningService : CallScreeningService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }
        scope.launch {
            val coordinator = CallScreeningCoordinator(applicationContext)
            val rawNumber = callDetails.handle?.schemeSpecificPart
            val timestampMillis = callDetails.creationTimeMillis.takeIf { it > 0L }
                ?: Clock.System.now().toEpochMilliseconds()
            val screened = runCatching { coordinator.screen(rawNumber, timestampMillis) }
                .getOrElse { coordinator.technicalFallback(rawNumber, timestampMillis) }
            try {
                runCatching { enqueue(screened.event) }.onFailure { Log.w(TAG, "Call event enqueue failed", it) }
            } finally {
                respondToCall(callDetails, screened.response)
                Log.i(TAG, "Call stages ms: ${screened.stageSummary}")
            }
        }
    }

    private fun enqueue(event: CallEvent) {
        val request = OneTimeWorkRequestBuilder<CallEventWorker>().setInputData(event.toData()).build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(CallEventWorker.UNIQUE_WORK_PREFIX + event.eventId, ExistingWorkPolicy.KEEP, request)
    }

    override fun onDestroy() { super.onDestroy() }
    private companion object { const val TAG = "CallWhitelistFilter" }
}

private class CallScreeningCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val snapshotStore = FilterSnapshotStore(appContext)
    private val evaluator = EvaluateFilterSnapshot()
    private val contacts = ContactMatcher(appContext)

    suspend fun screen(rawNumber: String?, timestampMillis: Long): ScreenedCall {
        val deadline = TelecomDeadline.start()
        val receivedAt = Clock.System.now()
        val snapshotStarted = deadline.elapsed()
        val snapshot = runCatching { snapshotStore.read() ?: defaultSnapshot() }.getOrElse {
            return fallback(rawNumber, timestampMillis, "Snapshot unavailable", deadline, snapshotStarted)
        }
        val snapshotFinished = deadline.elapsed()
        val isContact = runCatching { withTimeoutOrNull(deadline.remainingMillis()) { contacts.matches(rawNumber) } }.getOrElse { null }
            ?: return fallback(rawNumber, timestampMillis, "Contact lookup unavailable", deadline, snapshotFinished)
        val evaluated = runCatching {
            evaluator(snapshot, rawNumber, isContact, receivedAt.toLocalDateTime(TimeZone.currentSystemDefault()), isEmergencyNumber(rawNumber), receivedAt.toEpochMilliseconds())
        }.getOrElse { return fallback(rawNumber, timestampMillis, "Filter evaluation unavailable", deadline, snapshotFinished) }
        return completed(rawNumber, timestampMillis, evaluated, deadline, snapshotStarted, snapshotFinished)
    }

    fun technicalFallback(rawNumber: String?, timestampMillis: Long) = fallback(rawNumber, timestampMillis, "Screening unavailable", TelecomDeadline.start(), 0)

    private fun fallback(rawNumber: String?, timestamp: Long, reason: String, deadline: TelecomDeadline, snapshotElapsed: Long) =
        completed(rawNumber, timestamp, FilterResult(CallDecision.ALLOW, MatchSource.TECHNICAL_FALLBACK, reason), deadline, snapshotElapsed, deadline.elapsed())

    private fun completed(rawNumber: String?, timestamp: Long, result: FilterResult, deadline: TelecomDeadline, snapshotElapsed: Long, contactElapsed: Long): ScreenedCall {
        val response = CallScreeningService.CallResponse.Builder().apply {
            if (result.decision == CallDecision.BLOCK) { setDisallowCall(true); setRejectCall(true); setSkipCallLog(false); setSkipNotification(true) }
            if (result.decision == CallDecision.SILENCE) setSilenceCall(true)
        }.build()
        val eventId = UUID.nameUUIDFromBytes("$timestamp|${rawNumber.orEmpty()}".toByteArray()).toString()
        return ScreenedCall(CallEvent(eventId, timestamp, rawNumber, result.decision, result.source, result.reason), response, "snapshot=$snapshotElapsed contact=$contactElapsed evaluate=${deadline.elapsed()}")
    }

    private fun defaultSnapshot() = FilterSnapshot(version = 0, profiles = listOf(FilterProfile(name = "Default")))
}

private data class ScreenedCall(
    val event: CallEvent,
    val response: CallScreeningService.CallResponse,
    val stageSummary: String,
)

private class TelecomDeadline private constructor(private val startedAt: Long) {
    fun elapsed(): Long = SystemClock.elapsedRealtime() - startedAt
    fun remainingMillis(): Long = (BUDGET_MILLIS - elapsed()).coerceAtLeast(1)
    companion object { private const val BUDGET_MILLIS = 4_500L; fun start() = TelecomDeadline(SystemClock.elapsedRealtime()) }
}

private fun isEmergencyNumber(rawNumber: String?): Boolean = rawNumber?.filter(Char::isDigit) in setOf("112", "911", "101", "102", "103", "104")

private class ContactMatcher(private val context: Context, private val variants: PhoneNumberVariants = PhoneNumberVariants()) {
    suspend fun matches(rawNumber: String?): Boolean = withContext(Dispatchers.IO) {
        if (rawNumber.isNullOrBlank() || ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return@withContext false
        variants(rawNumber).any { number ->
            val signal = CancellationSignal()
            currentCoroutineContext()[Job]?.invokeOnCompletion { signal.cancel() }
            val uri = android.net.Uri.withAppendedPath(ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI, android.net.Uri.encode(number))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null, signal)?.use { it.moveToFirst() } ?: false
        }
    }
}
