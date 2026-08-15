package org.alexrust.callwhitelist.ui.policies

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.alexrust.callwhitelist.R
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.FilterPolicyRule
import org.alexrust.callwhitelist.model.FilterProfile
import org.alexrust.callwhitelist.model.NumberRule
import org.alexrust.callwhitelist.model.PolicyCondition
import org.alexrust.callwhitelist.model.PolicyMatchType
import org.alexrust.callwhitelist.model.TimeWindow
import org.alexrust.callwhitelist.system.CallScreeningAccess

@Composable
fun PoliciesScreen(
    modifier: Modifier = Modifier,
    isFilteringActive: Boolean,
    filteringEnabled: Boolean,
    rules: List<NumberRule>,
    contactsAllowed: Boolean,
    profile: FilterProfile?,
    onActivateFiltering: () -> Unit,
    onFilteringEnabledChanged: (Boolean) -> Unit,
    onContactsChanged: (Boolean) -> Unit,
    onProfileChanged: (FilterProfile) -> Unit,
    onAddRule: (NumberRule) -> Unit,
    onUpdateRule: (NumberRule) -> Unit,
    onDeleteRule: (NumberRule) -> Unit,
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var contactPermissionGranted by remember(context) {
        mutableStateOf(CallScreeningAccess.hasContactsPermission(context))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                contactPermissionGranted = CallScreeningAccess.hasContactsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val contactsEnabled = contactsAllowed && contactPermissionGranted
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        contactPermissionGranted = granted
        onContactsChanged(granted)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
    ) {
        item {
            Text(stringResource(R.string.policies), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Security, contentDescription = null)
                        Text(
                            stringResource(if (isFilteringActive) R.string.filtering_active else R.string.filtering_inactive),
                            modifier = Modifier.padding(start = 10.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    if (!isFilteringActive) {
                        Text(stringResource(R.string.call_processing_role_description))
                        Button(onClick = onActivateFiltering) {
                            Text(stringResource(R.string.make_call_handler))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.enable_filtering),
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = filteringEnabled,
                            enabled = isFilteringActive,
                            onCheckedChange = onFilteringEnabledChanged,
                        )
                    }
                }
            }
        }
        item {
            ProfilePolicyCard(
                profile = profile ?: FilterProfile(id = 1L, name = "Default"),
                onChanged = onProfileChanged,
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Contacts, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.allow_contacts)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                if (contactsEnabled) R.string.contacts_permission_granted
                                else R.string.contacts_permission_required,
                            ),
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = contactsEnabled,
                            onCheckedChange = { enabled ->
                                if (!enabled) {
                                    onContactsChanged(false)
                                } else if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_CONTACTS,
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    onContactsChanged(true)
                                } else {
                                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            },
                        )
                    },
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.allowed_numbers), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_number))
                }
            }
        }
        if (rules.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_allowed_numbers),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(rules, key = { it.id }) { rule ->
                NumberRuleRow(
                    rule = rule,
                    onToggle = { onUpdateRule(rule.copy(enabled = it)) },
                    onDelete = { onDeleteRule(rule) },
                )
            }
        }
        item {
            OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text(stringResource(R.string.add_number))
            }
        }
    }

    if (showAddDialog) {
        AddNumberDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { number, label ->
                onAddRule(NumberRule(number = number, label = label, decision = CallDecision.ALLOW))
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun ProfilePolicyCard(profile: FilterProfile, onChanged: (FilterProfile) -> Unit) {
    var startText by remember(profile) {
        mutableStateOf(formatMinutes(profile.activeWindow?.startMinutes ?: 22 * 60))
    }
    var endText by remember(profile) {
        mutableStateOf(formatMinutes(profile.activeWindow?.endMinutes ?: 7 * 60))
    }
    var selectedDays by remember(profile) {
        mutableStateOf(profile.activeWindow?.daysOfWeek ?: (1..7).toSet())
    }
    val schedule = profile.activeWindow

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.default_profile), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.default_action))
            DecisionChips(
                selected = profile.defaultDecision,
                onSelected = { onChanged(profile.copy(defaultDecision = it)) },
            )
            Text(stringResource(R.string.unknown_numbers_action))
            DecisionChips(
                selected = profile.decisionFor(PolicyMatchType.UNKNOWN_NUMBER),
                onSelected = { onChanged(profile.withDecision(PolicyMatchType.UNKNOWN_NUMBER, it)) },
            )
            Text(stringResource(R.string.hidden_numbers_action))
            DecisionChips(
                selected = profile.decisionFor(PolicyMatchType.HIDDEN_NUMBER),
                onSelected = { onChanged(profile.withDecision(PolicyMatchType.HIDDEN_NUMBER, it)) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.schedule_filtering), modifier = Modifier.weight(1f))
                Switch(
                    checked = schedule != null,
                    onCheckedChange = { enabled ->
                        onChanged(
                            profile.copy(
                                activeWindow = if (enabled) {
                                    TimeWindow(
                                        daysOfWeek = selectedDays,
                                        startMinutes = parseMinutes(startText) ?: 22 * 60,
                                        endMinutes = parseMinutes(endText) ?: 7 * 60,
                                    )
                                } else {
                                    null
                                },
                            ),
                        )
                    },
                )
            }
            if (schedule != null) {
                Text(stringResource(R.string.schedule_description))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = { startText = it },
                        label = { Text(stringResource(R.string.schedule_start)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = endText,
                        onValueChange = { endText = it },
                        label = { Text(stringResource(R.string.schedule_end)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (1..7).forEach { day ->
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = {
                                selectedDays = if (day in selectedDays) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                            label = { Text(dayLabel(day)) },
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        val start = parseMinutes(startText)
                        val end = parseMinutes(endText)
                        if (start != null && end != null && selectedDays.isNotEmpty()) {
                            onChanged(
                                profile.copy(
                                    activeWindow = TimeWindow(
                                        daysOfWeek = selectedDays,
                                        startMinutes = start,
                                        endMinutes = end,
                                    ),
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.save_schedule))
                }
            }
        }
    }
}

@Composable
private fun DecisionChips(selected: CallDecision, onSelected: (CallDecision) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CallDecision.values().forEach { decision ->
            FilterChip(
                selected = decision == selected,
                onClick = { onSelected(decision) },
                label = { Text(decisionLabel(decision)) },
            )
        }
    }
}

@Composable
private fun decisionLabel(decision: CallDecision): String = stringResource(
    when (decision) {
        CallDecision.ALLOW -> R.string.decision_allow
        CallDecision.BLOCK -> R.string.decision_block
        CallDecision.SILENCE -> R.string.decision_silence
    },
)

@Composable
private fun dayLabel(day: Int): String = stringResource(
    when (day) {
        1 -> R.string.day_monday
        2 -> R.string.day_tuesday
        3 -> R.string.day_wednesday
        4 -> R.string.day_thursday
        5 -> R.string.day_friday
        6 -> R.string.day_saturday
        else -> R.string.day_sunday
    },
)

private fun formatMinutes(value: Int): String = "%02d:%02d".format(value / 60, value % 60)

private fun parseMinutes(value: String): Int? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun FilterProfile.decisionFor(type: PolicyMatchType): CallDecision = rules
    .firstOrNull { it.condition.type == type }
    ?.decision
    ?: CallDecision.BLOCK

private fun FilterProfile.withDecision(type: PolicyMatchType, decision: CallDecision): FilterProfile {
    val existing = rules.firstOrNull { it.condition.type == type }
    val updated = existing?.copy(decision = decision) ?: FilterPolicyRule(
        id = if (type == PolicyMatchType.UNKNOWN_NUMBER) -2L else -3L,
        condition = PolicyCondition(type),
        label = "",
        decision = decision,
        priority = 5,
    )
    return copy(rules = rules.filterNot { it.condition.type == type } + updated)
}

@Composable
private fun NumberRuleRow(rule: NumberRule, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(rule.label.ifBlank { rule.number }) },
            supportingContent = { Text(rule.number) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = rule.enabled, onCheckedChange = onToggle)
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete_number))
                    }
                }
            },
        )
    }
}

@Composable
private fun AddNumberDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var number by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    val canSave = number.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_number)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(stringResource(R.string.phone_number)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.rule_label)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = canSave, onClick = { onConfirm(number.trim(), label.trim()) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
