package org.alexrust.callwhitelist.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.alexrust.callwhitelist.R

@Composable
fun FilteringStatusCard(
    modifier: Modifier = Modifier,
    isRoleHeld: Boolean,
    filteringEnabled: Boolean,
    onFilteringEnabledChanged: (Boolean) -> Unit,
    onRoleAction: () -> Unit,
    roleActionLabel: Int,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Security,
                    contentDescription = stringResource(R.string.filtering_status_icon),
                )
                Text(
                    text = stringResource(
                        when {
                            !isRoleHeld -> R.string.filtering_inactive
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
                    enabled = isRoleHeld,
                    onCheckedChange = onFilteringEnabledChanged,
                )
            }
            if (!isRoleHeld) {
                Text(stringResource(R.string.call_processing_role_description))
                Button(onClick = onRoleAction) {
                    Text(stringResource(roleActionLabel))
                }
            }
        }
    }
}
