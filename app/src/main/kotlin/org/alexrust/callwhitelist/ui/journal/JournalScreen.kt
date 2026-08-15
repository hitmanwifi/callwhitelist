package org.alexrust.callwhitelist.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.alexrust.callwhitelist.R
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.CallLogEntry

@Composable
fun JournalScreen(
    modifier: Modifier = Modifier,
    entries: List<CallLogEntry>,
    onClear: () -> Unit,
) {
    var filter by remember { mutableStateOf<CallDecision?>(null) }
    val visibleEntries = entries.filter { filter == null || it.result.decision == filter }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
    ) {
        item {
            Row {
                Text(
                    stringResource(R.string.journal),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClear, enabled = entries.isNotEmpty()) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.clear_journal))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter == null,
                    onClick = { filter = null },
                    label = { Text(stringResource(R.string.all_calls)) },
                )
                FilterChip(
                    selected = filter == CallDecision.ALLOW,
                    onClick = { filter = CallDecision.ALLOW },
                    label = { Text(stringResource(R.string.allowed_calls)) },
                )
                FilterChip(
                    selected = filter == CallDecision.BLOCK,
                    onClick = { filter = CallDecision.BLOCK },
                    label = { Text(stringResource(R.string.blocked_calls)) },
                )
            }
        }
        if (visibleEntries.isEmpty()) {
            item {
                Card {
                    Text(
                        stringResource(if (entries.isEmpty()) R.string.empty_journal else R.string.no_matching_calls),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            items(visibleEntries) { entry -> JournalRow(entry) }
        }
    }
}

@Composable
private fun JournalRow(entry: CallLogEntry) {
    val isBlocked = entry.result.decision == CallDecision.BLOCK
    Card {
        ListItem(
            leadingContent = {
                Icon(
                    if (isBlocked) Icons.Outlined.Block else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                )
            },
            headlineContent = { Text(entry.number ?: stringResource(R.string.hidden_number)) },
            supportingContent = {
                Text(if (isBlocked) stringResource(R.string.call_blocked) else stringResource(R.string.call_allowed))
            },
        )
    }
}
