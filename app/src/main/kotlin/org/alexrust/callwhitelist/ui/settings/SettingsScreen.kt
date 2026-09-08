package org.alexrust.callwhitelist.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import org.alexrust.callwhitelist.R
import org.alexrust.callwhitelist.model.OverviewPeriod
import org.alexrust.callwhitelist.model.ThemeMode
import org.alexrust.callwhitelist.system.NotificationAccess

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    overviewPeriod: OverviewPeriod,
    themeMode: ThemeMode,
    onOverviewPeriodChanged: (OverviewPeriod) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationPermissionGranted by remember(context) {
        mutableStateOf(NotificationAccess.hasPermission(context))
    }
    var appNotificationsEnabled by remember(context) {
        mutableStateOf(NotificationAccess.areAppNotificationsEnabled(context))
    }
    var blockedCallsChannelEnabled by remember(context) {
        mutableStateOf(NotificationAccess.isBlockedCallsChannelEnabled(context))
    }
    var pendingNotificationEnable by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionGranted = granted
        pendingNotificationEnable = false
        onNotificationsEnabledChanged(granted)
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationPermissionGranted = NotificationAccess.hasPermission(context)
                appNotificationsEnabled = NotificationAccess.areAppNotificationsEnabled(context)
                blockedCallsChannelEnabled = NotificationAccess.isBlockedCallsChannelEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var languageExpanded by remember { mutableStateOf(false) }
    var periodExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    val notificationsReady = notificationPermissionGranted &&
        appNotificationsEnabled && blockedCallsChannelEnabled
    val notificationStatus = when {
        !notificationPermissionGranted -> R.string.notifications_permission_required
        !appNotificationsEnabled -> R.string.notifications_app_disabled
        !blockedCallsChannelEnabled -> R.string.notifications_channel_disabled
        else -> R.string.notifications_permission_granted
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.app_settings), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.app_settings_description))
        Text(stringResource(R.string.language))
        OutlinedButton(onClick = { languageExpanded = true }) {
            Text(stringResource(R.string.choose_language))
        }
        DropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.language_russian)) },
                onClick = { setLocale("ru"); languageExpanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.language_english)) },
                onClick = { setLocale("en"); languageExpanded = false },
            )
        }
        Text(stringResource(R.string.overview_period))
        OutlinedButton(onClick = { periodExpanded = true }) {
            Text(stringResource(periodLabel(overviewPeriod)))
        }
        DropdownMenu(expanded = periodExpanded, onDismissRequest = { periodExpanded = false }) {
            OverviewPeriod.entries.forEach { period ->
                DropdownMenuItem(
                    text = { Text(stringResource(periodLabel(period))) },
                    onClick = {
                        onOverviewPeriodChanged(period)
                        periodExpanded = false
                    },
                )
            }
        }
        Text(stringResource(R.string.theme))
        OutlinedButton(onClick = { themeExpanded = true }) {
            Text(stringResource(themeLabel(themeMode)))
        }
        DropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(stringResource(themeLabel(mode))) },
                    onClick = {
                        onThemeModeChanged(mode)
                        themeExpanded = false
                    },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Notifications, contentDescription = null)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(stringResource(R.string.blocked_call_notifications))
                Text(
                    stringResource(notificationStatus),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        pendingNotificationEnable = false
                        onNotificationsEnabledChanged(false)
                    } else if (NotificationAccess.requiresRuntimePermission() && !notificationPermissionGranted) {
                        pendingNotificationEnable = true
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onNotificationsEnabledChanged(true)
                    }
                },
            )
        }
        if (!notificationsReady || pendingNotificationEnable) {
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        },
                    )
                },
            ) {
                Text(stringResource(R.string.open_notification_settings))
            }
        }
        Text(stringResource(R.string.app_version), style = MaterialTheme.typography.bodySmall)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.legal_information),
                    style = MaterialTheme.typography.titleMedium,
                )
                LegalInfoRow(
                    icon = Icons.Outlined.Copyright,
                    title = stringResource(R.string.copyright_holder),
                    value = stringResource(R.string.copyright_holder_value),
                )
                LegalInfoRow(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.project_license),
                    value = stringResource(R.string.project_license_value),
                )
                Text(
                    stringResource(R.string.license_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LegalInfoRow(
    icon: ImageVector,
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun periodLabel(period: OverviewPeriod): Int = when (period) {
    OverviewPeriod.TODAY -> R.string.period_today
    OverviewPeriod.LAST_7_DAYS -> R.string.period_last_7_days
    OverviewPeriod.LAST_30_DAYS -> R.string.period_last_30_days
    OverviewPeriod.ALL -> R.string.period_all_time
}

private fun themeLabel(themeMode: ThemeMode): Int = when (themeMode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun setLocale(languageTag: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
}
