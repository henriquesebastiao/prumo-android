package com.henriquesebastiao.prumo.feature.studies.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.henriquesebastiao.prumo.feature.studies.R
import com.henriquesebastiao.prumo.feature.studies.StudiesFeature

/** What each tab of Estudos shows. */
internal data class StudiesTabs(
    val today: TodayUiState,
    val week: WeekUiState,
    val subjects: SubjectsUiState,
    val plan: PlanUiState,
)

/** What each tab does. */
internal class StudiesTabEvents(
    val today: TodayEvents,
    val week: WeekEvents,
    val subjects: SubjectsEvents,
    val plan: PlanEvents,
)

@Composable
internal fun StudiesHomeRoute(
    key: StudiesHome,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    today: TodayViewModel = hiltViewModel(),
    week: WeekViewModel = hiltViewModel(),
    subjects: SubjectsViewModel = hiltViewModel(),
    plan: PlanViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(key.tab) }
    var sheet by rememberSaveable { mutableStateOf<SheetRequest?>(null) }
    val todayState by today.state.collectAsStateWithLifecycle()
    val weekState by week.state.collectAsStateWithLifecycle()
    val subjectsState by subjects.state.collectAsStateWithLifecycle()
    val planState by plan.state.collectAsStateWithLifecycle()
    val openSubject: (String) -> Unit = { navigator.navigate(SubjectDetailKey(it)) }
    StudiesHomeScreen(
        tab = tab,
        onTab = { tab = it },
        refreshing = when (tab) {
            StudiesTab.Today -> todayState.refreshing
            StudiesTab.Week -> weekState.refreshing
            StudiesTab.Subjects -> subjectsState.refreshing
            StudiesTab.Plan -> planState.refreshing
        },
        onRefresh = {
            when (tab) {
                StudiesTab.Today -> today.pull()
                StudiesTab.Week -> week.pull()
                StudiesTab.Subjects -> subjects.pull()
                StudiesTab.Plan -> plan.pull()
            }
        },
        tabs = StudiesTabs(todayState, weekState, subjectsState, planState),
        events = StudiesTabEvents(
            today = TodayEvents(
                onOpenSubject = openSubject,
                onPlan = { navigator.navigate(PlanEditorKey()) },
                onRetry = today::pull,
            ),
            week = WeekEvents(
                onPrevious = week::previous,
                onNext = week::next,
                onSelect = week::select,
                onOpenSubject = openSubject,
                onRetry = week::pull,
            ),
            subjects = SubjectsEvents(
                onOpen = { openSubject(it.id) },
                onNewArea = { sheet = SheetRequest(SheetKind.Area) },
                onEditArea = { sheet = SheetRequest(SheetKind.Area, id = it.id) },
                onNewSubject = { sheet = SheetRequest(SheetKind.Subject, parentId = it.id) },
                onArchiveArea = subjects::setArchived,
                onDeleteArea = subjects::delete,
                onToggleArchived = subjects::toggleArchived,
                onRetry = subjects::pull,
            ),
            plan = PlanEvents(
                onSelect = plan::select,
                onNew = { navigator.navigate(PlanEditorKey()) },
                onEdit = { navigator.navigate(PlanEditorKey(it.id)) },
                onDelete = plan::delete,
                onRetry = plan::pull,
            ),
        ),
        actions = StudiesActions(
            onLog = { sheet = SheetRequest(SheetKind.Session) },
            onNewSubject = { sheet = SheetRequest(SheetKind.Subject) },
        ),
        onBack = navigator::back,
        modifier = modifier,
    )
    StudiesSheet(sheet, onDismiss = { sheet = null })
}

/** Estudos (D24): the tabs in pills under the large title, the toolbar below. */
@Composable
internal fun StudiesHomeScreen(
    tab: StudiesTab,
    onTab: (StudiesTab) -> Unit,
    tabs: StudiesTabs,
    events: StudiesTabEvents,
    actions: StudiesActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    refreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { PrumoTopBar(stringResource(R.string.studies_name), onBack, large = true, scrollBehavior = scroll) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                PillTabs(
                    options = StudiesTab.entries,
                    selected = tab,
                    label = { it.label() },
                    onSelect = onTab,
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm),
                )
                PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
                    val content = Modifier.fillMaxSize()
                    when (tab) {
                        StudiesTab.Today -> TodayTab(tabs.today, events.today, content)
                        StudiesTab.Week -> WeekTab(tabs.week, events.week, content)
                        StudiesTab.Subjects -> SubjectsTab(tabs.subjects, events.subjects, content)
                        StudiesTab.Plan -> PlanTab(tabs.plan, events.plan, content)
                    }
                }
            }
            StudiesToolbar(actions, Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.lg))
        }
    }
}

@Composable
private fun StudiesTab.label(): String = stringResource(
    when (this) {
        StudiesTab.Today -> R.string.studies_tab_today
        StudiesTab.Week -> R.string.studies_tab_week
        StudiesTab.Subjects -> R.string.studies_tab_subjects
        StudiesTab.Plan -> R.string.studies_tab_plan
    },
)

@PhonePreviews
@Composable
private fun StudiesHomePreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(StudiesFeature.SEED) {
            StudiesHomeScreen(
                tab = StudiesTab.Today,
                onTab = {},
                tabs = StudiesTabs(
                    today = TodayUiState(loading = false, day = StudiesPreview.today),
                    week = WeekUiState(),
                    subjects = SubjectsUiState(),
                    plan = PlanUiState(),
                ),
                events = StudiesPreview.events,
                actions = StudiesActions({}, {}),
                onBack = {},
            )
        }
    }
}

@PhonePreviews
@Composable
private fun StudiesWeekPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(StudiesFeature.SEED) {
            StudiesHomeScreen(
                tab = StudiesTab.Week,
                onTab = {},
                tabs = StudiesTabs(
                    today = TodayUiState(),
                    week = WeekUiState(loading = false, week = StudiesPreview.week, selected = StudiesPreview.today.on),
                    subjects = SubjectsUiState(),
                    plan = PlanUiState(),
                ),
                events = StudiesPreview.events,
                actions = StudiesActions({}, {}),
                onBack = {},
            )
        }
    }
}

@PhonePreviews
@Composable
private fun StudiesSubjectsPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(StudiesFeature.SEED) {
            StudiesHomeScreen(
                tab = StudiesTab.Subjects,
                onTab = {},
                tabs = StudiesTabs(
                    today = TodayUiState(),
                    week = WeekUiState(),
                    subjects = SubjectsUiState(
                        loading = false,
                        areas = StudiesPreview.areas,
                        subjects = StudiesPreview.subjects,
                        overview = StudiesPreview.overview,
                    ),
                    plan = PlanUiState(),
                ),
                events = StudiesPreview.events,
                actions = StudiesActions({}, {}),
                onBack = {},
            )
        }
    }
}

@PhonePreviews
@Composable
private fun StudiesPlanPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(StudiesFeature.SEED) {
            StudiesHomeScreen(
                tab = StudiesTab.Plan,
                onTab = {},
                tabs = StudiesTabs(
                    today = TodayUiState(),
                    week = WeekUiState(),
                    subjects = SubjectsUiState(),
                    plan = PlanUiState(loading = false, plans = StudiesPreview.plans),
                ),
                events = StudiesPreview.events,
                actions = StudiesActions({}, {}),
                onBack = {},
            )
        }
    }
}
