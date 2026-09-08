package org.alexrust.callwhitelist.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.alexrust.callwhitelist.R
import org.alexrust.callwhitelist.model.BLOCKED_CALLS_CHANNEL_ID

object NotificationAccess {
    fun requiresRuntimePermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun hasPermission(context: Context): Boolean =
        !requiresRuntimePermission() ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    fun ensureBlockedCallsChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(
                BLOCKED_CALLS_CHANNEL_ID,
                context.getString(R.string.blocked_calls_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    fun areAppNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun isBlockedCallsChannelEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        ensureBlockedCallsChannel(context)
        return context.getSystemService(NotificationManager::class.java)
            ?.getNotificationChannel(BLOCKED_CALLS_CHANNEL_ID)
            ?.importance != NotificationManager.IMPORTANCE_NONE
    }
}
