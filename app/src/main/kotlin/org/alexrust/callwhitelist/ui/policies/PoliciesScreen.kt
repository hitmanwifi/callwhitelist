package org.alexrust.callwhitelist.ui.policies

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.alexrust.callwhitelist.R
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.FilterPolicyRule
import org.alexrust.callwhitelist.model.FilterProfile
import org.alexrust.callwhitelist.model.NumberRule
import org.alexrust.callwhitelist.model.PolicyCondition
import org.alexrust.callwhitelist.model.PolicyMatchType
import org.alexrust.callwhitelist.model.TimeWindow
import org.alexrust.callwhitelist.system.CallScreeningAccess
import org.alexrust.callwhitelist.domain.NormalizePhoneNumber
import org.alexrust.callwhitelist.ui.components.FilteringStatusCard

@Composable
fun PoliciesScreen(
    modifier: Modifier = Modifier,
    isFilteringActive: Boolean,
    filteringEnabled: Boolean,
    rules: List<NumberRule>,
    recentNumbers: List<String>,
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
            FilteringStatusCard(
                isRoleHeld = isFilteringActive,
                filteringEnabled = filteringEnabled,
                onFilteringEnabledChanged = onFilteringEnabledChanged,
                onRoleAction = onActivateFiltering,
                roleActionLabel = R.string.make_call_handler,
            )
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
            recentNumbers = recentNumbers,
            onDismiss = { showAddDialog = false },
            onConfirm = { number, label, expiresAtMillis ->
                onAddRule(
                    NumberRule(
                        number = number,
                        label = label,
                        decision = CallDecision.ALLOW,
                        expiresAtMillis = expiresAtMillis,
                    ),
                )
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
    val expiration = rule.expiresAtMillis
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(rule.label.ifBlank { rule.number }) },
            supportingContent = {
                Column {
                    Text(rule.number)
                    expiration?.let { expiresAtMillis ->
                        Text(
                            stringResource(
                                if (expiresAtMillis <= Clock.System.now().toEpochMilliseconds()) {
                                    R.string.rule_expired
                                } else {
                                    R.string.rule_expires_format
                                },
                                formatExpiration(expiresAtMillis),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
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
private fun AddNumberDialog(
    recentNumbers: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long?) -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(TemporaryDuration.PERMANENT) }
    var durationExpanded by remember { mutableStateOf(false) }
    var countryExpanded by remember { mutableStateOf(false) }
    var recentPickerVisible by remember { mutableStateOf(false) }
    var country by remember { mutableStateOf(PhoneCountry.RUSSIA) }
    val normalizedNumber = remember(number, country) {
        NormalizePhoneNumber()(numberForStorage(number, country))
    }
    val canSave = normalizedNumber != null
    val maskResource = country.maskResource

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_number)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(modifier = Modifier.weight(0.44f)) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            onClick = { countryExpanded = true },
                        ) {
                            Icon(Icons.Outlined.Language, contentDescription = null)
                            Text(
                                stringResource(country.shortLabelResource),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        DropdownMenu(
                            expanded = countryExpanded,
                            onDismissRequest = { countryExpanded = false },
                        ) {
                            PhoneCountry.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.labelResource)) },
                                    onClick = {
                                        number = switchCountryNumber(number, country, option)
                                        country = option
                                        countryExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        modifier = Modifier.weight(0.56f),
                        value = number,
                        onValueChange = {
                            number = if (country.dialCode == null) {
                                sanitizePhoneInput(it)
                            } else {
                                it.filter(Char::isDigit)
                            }
                        },
                        placeholder = {
                            maskResource?.let { resource ->
                                Text(
                                    stringResource(resource),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        },
                        supportingText = {
                            Text(stringResource(country.formatResource))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        visualTransformation = PhoneMaskVisualTransformation(country),
                    )
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { recentPickerVisible = true },
                ) {
                    Icon(Icons.Outlined.History, contentDescription = null)
                    Text(stringResource(R.string.choose_from_journal))
                }
                if (recentNumbers.isEmpty()) {
                    Text(
                        stringResource(R.string.journal_numbers_empty_for_picker),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.rule_label)) },
                    singleLine = true,
                )
                Text(stringResource(R.string.rule_duration))
                OutlinedButton(onClick = { durationExpanded = true }) {
                    Text(stringResource(durationLabel(duration)))
                }
                DropdownMenu(
                    expanded = durationExpanded,
                    onDismissRequest = { durationExpanded = false },
                ) {
                    TemporaryDuration.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(durationLabel(option))) },
                            onClick = {
                                duration = option
                                durationExpanded = false
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    normalizedNumber?.let {
                        onConfirm(it, label.trim(), durationToExpirationMillis(duration))
                    }
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )

    if (recentPickerVisible) {
        AlertDialog(
            onDismissRequest = { recentPickerVisible = false },
            title = { Text(stringResource(R.string.recent_picker_title)) },
            text = {
                if (recentNumbers.isEmpty()) {
                    Text(stringResource(R.string.journal_numbers_empty_for_picker))
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(recentNumbers) { recentNumber ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    number = numberFromSelectedCountry(recentNumber, country)
                                    recentPickerVisible = false
                                },
                            ) {
                                Text(recentNumber)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { recentPickerVisible = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun sanitizePhoneInput(value: String): String = buildString {
    value.forEachIndexed { index, char ->
        if (char.isDigit() || (char == '+' && index == 0)) {
            append(char)
        }
    }
}

private fun numberForStorage(value: String, country: PhoneCountry): String =
    country.dialCode?.let { "$it${value.filter(Char::isDigit)}" } ?: value

private fun numberFromSelectedCountry(value: String, country: PhoneCountry): String {
    val digits = value.filter(Char::isDigit)
    val countryDigits = country.dialCode?.filter(Char::isDigit).orEmpty()
    return if (country.dialCode == null) value else digits.removePrefix(countryDigits)
}

private fun switchCountryNumber(
    value: String,
    currentCountry: PhoneCountry,
    newCountry: PhoneCountry,
): String {
    val fullNumber = numberForStorage(value, currentCountry)
    return numberFromSelectedCountry(fullNumber, newCountry)
}

private enum class PhoneCountry(
    val dialCode: String?,
    val labelResource: Int,
    val shortLabelResource: Int,
    val formatResource: Int,
    val maskResource: Int?,
) {
    WITHOUT_CODE(null, R.string.country_without_code, R.string.country_without_code_short, R.string.phone_format_any, null),
    RUSSIA("+7", R.string.country_russia, R.string.country_russia_short, R.string.phone_format_russia, R.string.phone_mask_russia),
    KAZAKHSTAN("+7", R.string.country_kazakhstan, R.string.country_kazakhstan_short, R.string.phone_format_kazakhstan, R.string.phone_mask_kazakhstan),
    BELARUS("+375", R.string.country_belarus, R.string.country_belarus_short, R.string.phone_format_belarus, R.string.phone_mask_belarus),
    UKRAINE("+380", R.string.country_ukraine, R.string.country_ukraine_short, R.string.phone_format_ukraine, R.string.phone_mask_ukraine),
    GERMANY("+49", R.string.country_germany, R.string.country_germany_short, R.string.phone_format_germany, R.string.phone_mask_germany),
    UNITED_KINGDOM("+44", R.string.country_united_kingdom, R.string.country_united_kingdom_short, R.string.phone_format_united_kingdom, R.string.phone_mask_united_kingdom),
    UNITED_STATES("+1", R.string.country_united_states, R.string.country_united_states_short, R.string.phone_format_united_states, R.string.phone_mask_united_states),
}

private class PhoneMaskVisualTransformation(
    private val country: PhoneCountry,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (country.dialCode == null) return TransformedText(text, OffsetMapping.Identity)
        val formatted = formatByMask(text.text, country)
        return TransformedText(
            text = AnnotatedString(formatted),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 0) return 0
                    var digitsSeen = 0
                    formatted.forEachIndexed { index, char ->
                        if (char.isDigit()) {
                            digitsSeen++
                            if (digitsSeen == offset) return index + 1
                        }
                    }
                    return formatted.length
                }

                override fun transformedToOriginal(offset: Int): Int =
                    formatted.take(offset.coerceIn(0, formatted.length)).count(Char::isDigit)
            },
        )
    }
}

private fun formatByMask(value: String, country: PhoneCountry): String {
    val digits = value.filter(Char::isDigit)
    val mask = when (country) {
        PhoneCountry.RUSSIA, PhoneCountry.KAZAKHSTAN -> "(###) ###-##-##"
        PhoneCountry.BELARUS, PhoneCountry.UKRAINE -> "## ###-##-##"
        PhoneCountry.GERMANY -> "### ########"
        PhoneCountry.UNITED_KINGDOM -> "## #### ####"
        PhoneCountry.UNITED_STATES -> "(###) ###-####"
        PhoneCountry.WITHOUT_CODE -> return value
    }
    val result = StringBuilder()
    var digitIndex = 0
    mask.forEachIndexed { index, char ->
        if (char == '#') {
            if (digitIndex >= digits.length) return@forEachIndexed
            result.append(digits[digitIndex++])
        } else if (digitIndex > 0 && digitIndex < digits.length &&
            mask.substring(index + 1).contains('#')
        ) {
            result.append(char)
        }
    }
    return result.toString()
}

private enum class TemporaryDuration { PERMANENT, ONE_HOUR, ONE_DAY, SEVEN_DAYS }

private fun durationLabel(duration: TemporaryDuration): Int = when (duration) {
    TemporaryDuration.PERMANENT -> R.string.duration_permanent
    TemporaryDuration.ONE_HOUR -> R.string.duration_one_hour
    TemporaryDuration.ONE_DAY -> R.string.duration_one_day
    TemporaryDuration.SEVEN_DAYS -> R.string.duration_seven_days
}

private fun durationToExpirationMillis(duration: TemporaryDuration): Long? = when (duration) {
    TemporaryDuration.PERMANENT -> null
    TemporaryDuration.ONE_HOUR -> (Clock.System.now() + 1.hours).toEpochMilliseconds()
    TemporaryDuration.ONE_DAY -> (Clock.System.now() + 1.days).toEpochMilliseconds()
    TemporaryDuration.SEVEN_DAYS -> (Clock.System.now() + 7.days).toEpochMilliseconds()
}

private fun formatExpiration(epochMillis: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "%04d-%02d-%02d %02d:%02d".format(
        local.year,
        local.month.ordinal + 1,
        local.day,
        local.hour,
        local.minute,
    )
}
