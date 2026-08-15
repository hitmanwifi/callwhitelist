package org.alexrust.callwhitelist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.alexrust.callwhitelist.R
import org.alexrust.callwhitelist.preferences.UserPreferences
import org.alexrust.callwhitelist.navigation.AppNavigation
import org.alexrust.callwhitelist.system.CallScreeningAccess

@Composable
fun WhiteListApp(openJournal: Boolean = false) {
    val context = LocalContext.current
    val userPreferences = remember(context) { UserPreferences(context) }
    val scope = rememberCoroutineScope()
    var onboardingVisible by remember { mutableStateOf(false) }
    var isFilteringActive by remember {
        mutableStateOf(CallScreeningAccess.isRoleHeld(context))
    }
    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        isFilteringActive = CallScreeningAccess.isRoleHeld(context)
    }

    LaunchedEffect(Unit) {
        isFilteringActive = CallScreeningAccess.isRoleHeld(context)
        onboardingVisible = !userPreferences.onboardingCompleted.first()
    }

    AppNavigation(
        isFilteringActive = isFilteringActive,
        openJournal = openJournal,
        onActivateFiltering = {
            roleRequestLauncher.launch(CallScreeningAccess.createRoleRequestIntent(context))
        },
    )

    if (onboardingVisible) {
        AlertDialog(
            onDismissRequest = {
                onboardingVisible = false
                scope.launch { userPreferences.setOnboardingCompleted(true) }
            },
            title = { Text(stringResource(R.string.welcome_title)) },
            text = { Text(stringResource(R.string.welcome_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onboardingVisible = false
                        scope.launch { userPreferences.setOnboardingCompleted(true) }
                        roleRequestLauncher.launch(CallScreeningAccess.createRoleRequestIntent(context))
                    },
                ) {
                    Text(stringResource(R.string.open_call_policies))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onboardingVisible = false
                        scope.launch { userPreferences.setOnboardingCompleted(true) }
                    },
                ) {
                    Text(stringResource(R.string.later))
                }
            },
        )
    }
}
