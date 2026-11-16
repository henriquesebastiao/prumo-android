package com.henriquesebastiao.prumo.feature.habits.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PillTabs
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.feature.habits.HabitsFeature
import com.henriquesebastiao.prumo.feature.habits.R

@Composable
internal fun HabitsHomeRoute(
    key: HabitsHome,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    today: TodayViewModel = hiltViewModel(),
    list: ListViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(key.tab) }
    var sheet by rememberSaveable { mutableStateOf<SheetRequest?>(null) }
    val todayState by today.state.collectAsStateWithLifecycle()
    val listState by list.state.collectAsStateWithLifecycle()
    val open: (String) -> Unit = { navigator.navigate(HabitDetailKey(it)) }
    val newHabit = { navigator.navigate(HabitEditorKey()) }
    HabitsHomeScreen(
        tab = tab,
        onTab = { tab = it },
        today = todayState,
        list = listState,
        todayEvents = TodayEvents(today::toggle, today::step, open, today::pull, today::onDay, today::reorder),
        listEvents = ListEvents(
            onOpen = { open(it.id) },
            onNew = newHabit,
            onToggleArchived = list::toggleArchived,
            onRetry = list::pull,
            onQuery = list::onQuery,
            onKind = list::onKind,
            onCadence = list::onCadence,
            onReorder = list::reorder,
        ),
        query = list.query,
        onUndo = today::undo,
        onUndoShown = today::onUndoShown,
        actions = HabitsActions(onPrimary = { sheet = SheetRequest(SheetKind.CheckIn) }, onNewHabit = newHabit),
        refreshing = if (tab == HabitsTab.Today) todayState.refreshing else listState.refreshing,
        onRefresh = { if (tab == HabitsTab.Today) today.pull() else list.pull() },
        onBack = navigator::back,
        modifier = modifier,
    )
    HabitsSheet(sheet, onDismiss = { sheet = null })
}

/** Hábitos (D24): the tabs in pills under the large title, the toolbar below. */
@Composable
internal fun HabitsHomeScreen(
    tab: HabitsTab,
    onTab: (HabitsTab) -> Unit,
    today: TodayUiState,
    list: ListUiState,
    todayEvents: TodayEvents,
    listEvents: ListEvents,
    actions: HabitsActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    query: String = "",
    refreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onUndo: () -> Unit = {},
    onUndoShown: () -> Unit = {},
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbar = remember { SnackbarHostState() }
    val undo = today.undo
    val marked = undo?.let { stringResource(R.string.habits_marked, it.name) }
    val undoLabel = stringResource(R.string.habits_undo)
    LaunchedEffect(undo) {
        if (marked == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(marked, actionLabel = undoLabel, duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) onUndo() else onUndoShown()
    }
    Scaffold(
        modifier.nestedScroll(scroll.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = TOOLBAR_SPACE)) },
        topBar = { PrumoTopBar(stringResource(R.string.habits_name), onBack, large = true, scrollBehavior = scroll) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                PillTabs(
                    options = HabitsTab.entries,
                    selected = tab,
                    label = {
                        stringResource(
                            if (it ==
                                HabitsTab.Today
                            ) {
                                R.string.habits_tab_today
                            } else {
                                R.string.habits_tab_list
                            },
                        )
                    },
                    onSelect = onTab,
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm),
                )
                PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
                    when (tab) {
                        HabitsTab.Today -> TodayTab(today, todayEvents, Modifier.fillMaxSize())
                        HabitsTab.List -> ListTab(list, query, listEvents, Modifier.fillMaxSize())
                    }
                }
            }
            HabitsToolbar(actions, Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.lg))
        }
    }
}

@PhonePreviews
@Composable
private fun HabitsTodayPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(HabitsFeature.SEED) {
            HabitsHomeScreen(
                tab = HabitsTab.Today,
                onTab = {},
                today = TodayUiState(loading = false, day = HabitsPreview.day),
                list = ListUiState(),
                todayEvents = HabitsPreview.todayEvents,
                listEvents = HabitsPreview.listEvents,
                actions = HabitsActions({}, onNewHabit = {}),
                onBack = {},
            )
        }
    }
}

@PhonePreviews
@Composable
private fun HabitsListPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(HabitsFeature.SEED) {
            HabitsHomeScreen(
                tab = HabitsTab.List,
                onTab = {},
                today = TodayUiState(),
                list = ListUiState(loading = false, habits = HabitsPreview.habits),
                todayEvents = HabitsPreview.todayEvents,
                listEvents = HabitsPreview.listEvents,
                actions = HabitsActions({}, onNewHabit = {}),
                onBack = {},
            )
        }
    }
}
