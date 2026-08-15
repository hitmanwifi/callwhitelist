package org.alexrust.callwhitelist.callfilter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.app.NotificationChannel
import android.app.NotificationManager
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.alexrust.callwhitelist.data.CallLogStore
import org.alexrust.callwhitelist.data.FilterSnapshotStore
import org.alexrust.callwhitelist.domain.EvaluateFilterSnapshot
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.CallLogEntry
import org.alexrust.callwhitelist.model.FilterProfile
import org.alexrust.callwhitelist.model.FilterResult
import org.alexrust.callwhitelist.model.FilterSnapshot
import org.alexrust.callwhitelist.model.MatchSource
import org.alexrust.callwhitelist.model.OPEN_JOURNAL_ACTION
import org.alexrust.callwhitelist.preferences.UserPreferences
import android.app.PendingIntent
import android.content.Intent

class WhiteListCallScreeningService : CallScreeningService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val snapshotStore by lazy { FilterSnapshotStore(applicationContext) }
    private val callLogStore by lazy { CallLogStore(applicationContext) }
    private val contactMatcher by lazy { ContactMatcher(applicationContext) }
    private val evaluator = EvaluateFilterSnapshot()

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart
        scope.launch {
            val receivedAt = Clock.System.now()
            val result = runCatching {
                evaluator(
                    snapshot = snapshotStore.read() ?: defaultSnapshot(),
                    rawNumber = rawNumber,
                    isContact = contactMatcher.matches(rawNumber),
                    now = receivedAt.toLocalDateTime(TimeZone.currentSystemDefault()),
                    isEmergency = isEmergencyNumber(rawNumber),
                )
            }.getOrElse {
                FilterResult(
                    decision = CallDecision.BLOCK,
                    source = MatchSource.DEFAULT,
                    reason = "Filter unavailable",
                )
            }

            val response = CallResponse.Builder().apply {
                if (result.decision == CallDecision.BLOCK) {
                    setDisallowCall(true)
                    setRejectCall(true)
                    setSkipCallLog(false)
                    setSkipNotification(true)
                } else if (result.decision == CallDecision.SILENCE) {
                    setSilenceCall(true)
                }
            }.build()

            // The system response is the deadline-critical operation. Logging follows it.
            respondToCall(callDetails, response)
            runCatching {
                callLogStore.append(
                    CallLogEntry(
                        timestampMillis = receivedAt.toEpochMilliseconds(),
                        number = rawNumber,
                        result = result,
                    ),
                )
            }
            runCatching { notifyIfEnabled(rawNumber, result) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun defaultSnapshot(): FilterSnapshot = FilterSnapshot(
        version = 0,
        profiles = listOf(FilterProfile(name = "Default")),
    )

    private suspend fun notifyIfEnabled(rawNumber: String?, result: FilterResult) {
        if (result.decision != CallDecision.BLOCK) return
        if (!UserPreferences(applicationContext).notificationsEnabled.first()) return
        if (
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (!notificationManager.areNotificationsEnabled()) return

        val channel = NotificationChannel(
            BLOCKED_CALLS_CHANNEL_ID,
            getString(R.string.blocked_call_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        applicationContext.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)

        val openJournalIntent = Intent().apply {
            action = OPEN_JOURNAL_ACTION
            setPackage(applicationContext.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            OPEN_JOURNAL_REQUEST_CODE,
            openJournalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val number = rawNumber ?: getString(R.string.blocked_call_notification_hidden_number)
        val notification = NotificationCompat.Builder(applicationContext, BLOCKED_CALLS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_missed_call)
            .setContentTitle(getString(R.string.blocked_call_notification_title))
            .setContentText(getString(R.string.blocked_call_notification_text, number))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(BLOCKED_CALLS_GROUP)
            .build()
        notificationManager.notify(BLOCKED_CALL_NOTIFICATION_ID, notification)
    }

    private companion object {
        const val BLOCKED_CALLS_CHANNEL_ID = "blocked_calls"
        const val BLOCKED_CALLS_GROUP = "blocked_calls_group"
        const val BLOCKED_CALL_NOTIFICATION_ID = 2001
        const val OPEN_JOURNAL_REQUEST_CODE = 2002
    }
}

private fun isEmergencyNumber(rawNumber: String?): Boolean {
    val digits = rawNumber?.filter(Char::isDigit) ?: return false
    return digits in setOf("112", "911", "101", "102", "103", "104")
}

private class ContactMatcher(private val context: Context) {
    fun matches(rawNumber: String?): Boolean {
        if (rawNumber.isNullOrBlank()) return false
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(rawNumber),
        )
        return context.contentResolver.query(
            lookupUri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null,
        )?.use { cursor -> cursor.moveToFirst() } ?: false
    }
}
