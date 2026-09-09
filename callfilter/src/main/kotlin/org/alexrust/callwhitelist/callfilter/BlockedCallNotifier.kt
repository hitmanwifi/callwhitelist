package org.alexrust.callwhitelist.callfilter

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import org.alexrust.callwhitelist.callfilter.R
import org.alexrust.callwhitelist.model.BLOCKED_CALLS_CHANNEL_ID
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.OPEN_JOURNAL_ACTION
import org.alexrust.callwhitelist.preferences.UserPreferences

class BlockedCallNotifier(private val context: Context) {
    suspend fun notifyIfEnabled(event: CallEvent) {
        if (event.decision != CallDecision.BLOCK) return
        if (!UserPreferences(context).notificationsEnabled.first()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Blocked-call notification skipped: POST_NOTIFICATIONS is denied")
            return
        }
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "Blocked-call notification skipped: app notifications are disabled")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(BLOCKED_CALLS_CHANNEL_ID, context.getString(R.string.blocked_call_notification_channel), NotificationManager.IMPORTANCE_DEFAULT),
            )
            if (context.getSystemService(NotificationManager::class.java)?.getNotificationChannel(BLOCKED_CALLS_CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return
        }
        val intent = Intent().apply { action = OPEN_JOURNAL_ACTION; setPackage(context.packageName); flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pendingIntent = PendingIntent.getActivity(context, OPEN_JOURNAL_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val number = event.number ?: context.getString(R.string.blocked_call_notification_hidden_number)
        manager.notify(event.eventId.hashCode(), NotificationCompat.Builder(context, BLOCKED_CALLS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_missed_call)
            .setContentTitle(context.getString(R.string.blocked_call_notification_title))
            .setContentText(context.getString(R.string.blocked_call_notification_text, number))
            .setContentIntent(pendingIntent).setAutoCancel(true).setGroup(BLOCKED_CALLS_GROUP).build())
    }

    private companion object { const val TAG = "CallWhitelistFilter"; const val BLOCKED_CALLS_GROUP = "blocked_calls_group"; const val OPEN_JOURNAL_REQUEST_CODE = 2002 }
}
