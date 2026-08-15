package org.alexrust.callwhitelist.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.alexrust.callwhitelist.R
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.CallLogEntry

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    isFilteringActive: Boolean,
    filteringEnabled: Boolean,
    onFilteringEnabledChanged: (Boolean) -> Unit,
    entries: List<CallLogEntry>,
    overviewPeriodLabel: String,
    onOpenPolicies: () -> Unit,
    onOpenJournal: () -> Unit,
) {
    val allowed = entries.count { it.result.decision == CallDecision.ALLOW }
    val blocked = entries.count { it.result.decision == CallDecision.BLOCK }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 24.dp),
    ) {
        item {
            Text(stringResource(R.string.overview), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Text(
                stringResource(R.string.overview_period_format, overviewPeriodLabel),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Phone, contentDescription = null)
                        Text(
                            text = stringResource(
                                when {
                                    !isFilteringActive -> R.string.filtering_inactive
                                    filteringEnabled -> R.string.filtering_active
                                    else -> R.string.filtering_disabled
                                },
                            ),
                            modifier = Modifier.padding(start = 10.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(stringResource(R.string.overview_status_description))
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
                    if (!isFilteringActive) {
                        Button(onClick = onOpenPolicies) {
                            Text(stringResource(R.string.open_call_policies))
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(stringResource(R.string.processed_calls), entries.size, Modifier.weight(1f))
                StatCard(stringResource(R.string.allowed_calls), allowed, Modifier.weight(1f))
                StatCard(stringResource(R.string.blocked_calls), blocked, Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.recent_calls), style = MaterialTheme.typography.titleLarge)
                if (entries.isNotEmpty()) {
                    OutlinedButton(onClick = onOpenJournal) {
                        Text(stringResource(R.string.view_all))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
        if (entries.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_calls_yet),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(entries.take(3)) { entry ->
                CallLogRow(entry)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CallLogRow(entry: CallLogEntry) {
    val blocked = entry.result.decision == CallDecision.BLOCK
    ListItem(
        leadingContent = {
            Icon(
                if (blocked) Icons.Outlined.Block else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (blocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text(entry.number ?: stringResource(R.string.hidden_number)) },
        supportingContent = { Text(if (blocked) stringResource(R.string.call_blocked) else stringResource(R.string.call_allowed)) },
    )
}
