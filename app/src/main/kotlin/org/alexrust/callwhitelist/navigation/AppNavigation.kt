package org.alexrust.callwhitelist.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import kotlin.time.Clock
import kotlinx.coroutines.launch
import org.alexrust.callwhitelist.R
import org.alexrust.callwhitelist.data.CallLogStore
import org.alexrust.callwhitelist.data.FilterPolicyStore
import org.alexrust.callwhitelist.data.RoomRuleStore
import org.alexrust.callwhitelist.preferences.UserPreferences
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.NumberRule
import org.alexrust.callwhitelist.model.OverviewPeriod
import org.alexrust.callwhitelist.domain.OverviewPeriodStart
import org.alexrust.callwhitelist.ui.home.HomeScreen
import org.alexrust.callwhitelist.ui.journal.JournalScreen
import org.alexrust.callwhitelist.ui.policies.PoliciesScreen
import org.alexrust.callwhitelist.ui.settings.SettingsScreen

@Composable
fun AppNavigation(
    isFilteringActive: Boolean,
    onActivateFiltering: () -> Unit,
    openJournal: Boolean = false,
) {
    val context = LocalContext.current
    val ruleStore = remember(context) { RoomRuleStore(context) }
    val callLogStore = remember(context) { CallLogStore(context) }
    val policyStore = remember(context) { FilterPolicyStore(context) }
    val userPreferences = remember(context) { UserPreferences(context) }
    val rules by ruleStore.rules.collectAsStateWithLifecycleCompat(emptyList())
    val contactsAllowed by ruleStore.contactsAllowed.collectAsStateWithLifecycleCompat(true)
    val entries by callLogStore.entries.collectAsStateWithLifecycleCompat(emptyList())
    val lastJournalViewedAtMillis by userPreferences.lastJournalViewedAtMillis
        .collectAsStateWithLifecycleCompat(0L)
    val overviewPeriodStorage by userPreferences.overviewPeriod
        .collectAsStateWithLifecycleCompat(OverviewPeriod.TODAY.storageValue)
    val notificationsEnabled by userPreferences.notificationsEnabled
        .collectAsStateWithLifecycleCompat(false)
    val filteringEnabled by userPreferences.filteringEnabled
        .collectAsStateWithLifecycleCompat(true)
    val overviewPeriod = OverviewPeriod.fromStorage(overviewPeriodStorage)
    val overviewStartMillis = OverviewPeriodStart()(overviewPeriod)
    val overviewEntries = remember(entries, overviewStartMillis) {
        entries.filter { it.timestampMillis >= overviewStartMillis }
    }
    val unreadBlockedCount by remember(lastJournalViewedAtMillis) {
        callLogStore.observeBlockedCountSince(lastJournalViewedAtMillis)
    }.collectAsStateWithLifecycleCompat(0)
    val snapshot by policyStore.snapshot.collectAsStateWithLifecycleCompat(policyStore.snapshot.collectAsState().value)
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(if (openJournal) JOURNAL_TAB else 0) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == JOURNAL_TAB) {
            userPreferences.markJournalViewed(Clock.System.now().toEpochMilliseconds())
        }
    }

    val tabs = listOf(
        R.string.overview,
        R.string.policies,
        R.string.journal,
        R.string.app_settings,
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, labelResource ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Outlined.Home, contentDescription = null)
                                1 -> Icon(Icons.Outlined.Tune, contentDescription = null)
                                2 -> {
                                    BadgedBox(
                                        badge = {
                                            if (unreadBlockedCount > 0) {
                                                Badge { Text(unreadBlockedCount.toString()) }
                                            }
                                        },
                                    ) {
                                        Icon(Icons.Outlined.History, contentDescription = null)
                                    }
                                }
                                else -> Icon(Icons.Outlined.Settings, contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(labelResource)) },
                    )
                }
            }
        },
    ) { paddingValues ->
        when (selectedTab) {
            0 -> HomeScreen(
                modifier = Modifier.padding(paddingValues),
                isFilteringActive = isFilteringActive,
                filteringEnabled = filteringEnabled,
                onFilteringEnabledChanged = { value ->
                    scope.launch { policyStore.setFilteringEnabled(value) }
                },
                entries = overviewEntries,
                overviewPeriodLabel = stringResource(periodLabel(overviewPeriod)),
                onOpenPolicies = { selectedTab = 1 },
                onOpenJournal = { selectedTab = 2 },
            )

            1 -> PoliciesScreen(
                modifier = Modifier.padding(paddingValues),
                isFilteringActive = isFilteringActive,
                filteringEnabled = filteringEnabled,
                rules = rules,
                contactsAllowed = contactsAllowed,
                profile = snapshot.profiles.firstOrNull { it.id == 1L },
                onActivateFiltering = onActivateFiltering,
                onFilteringEnabledChanged = { value ->
                    scope.launch { policyStore.setFilteringEnabled(value) }
                },
                onContactsChanged = { value -> scope.launch { ruleStore.setContactsAllowed(value) } },
                onProfileChanged = { profile -> scope.launch { policyStore.updateProfile(profile) } },
                onAddRule = { rule -> scope.launch { ruleStore.add(rule) } },
                onUpdateRule = { rule -> scope.launch { ruleStore.update(rule) } },
                onDeleteRule = { rule -> scope.launch { ruleStore.delete(rule) } },
            )

            2 -> JournalScreen(
                modifier = Modifier.padding(paddingValues),
                entries = entries,
                onClear = { scope.launch { callLogStore.clear() } },
            )

            else -> SettingsScreen(
                modifier = Modifier.padding(paddingValues),
                overviewPeriod = overviewPeriod,
                notificationsEnabled = notificationsEnabled,
                onNotificationsEnabledChanged = { value ->
                    scope.launch { userPreferences.setNotificationsEnabled(value) }
                },
                onOverviewPeriodChanged = { period ->
                    scope.launch { userPreferences.setOverviewPeriod(period.storageValue) }
                },
            )
        }
    }
}

private const val JOURNAL_TAB = 2

private fun periodLabel(period: OverviewPeriod): Int = when (period) {
    OverviewPeriod.TODAY -> R.string.period_today
    OverviewPeriod.LAST_7_DAYS -> R.string.period_last_7_days
    OverviewPeriod.LAST_30_DAYS -> R.string.period_last_30_days
    OverviewPeriod.ALL -> R.string.period_all_time
}

@Composable
private fun <T> kotlinx.coroutines.flow.Flow<T>.collectAsStateWithLifecycleCompat(initial: T): androidx.compose.runtime.State<T> =
    this.collectAsState(initial)
