package com.henriquesebastiao.prumo.system.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.henriquesebastiao.prumo.R
import com.henriquesebastiao.prumo.core.designsystem.component.DateBadge
import com.henriquesebastiao.prumo.core.designsystem.component.EmptyState
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedListItem
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.HeroHeader
import com.henriquesebastiao.prumo.core.designsystem.component.IconAvatar
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.component.Tag
import com.henriquesebastiao.prumo.core.designsystem.theme.ListStyle
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.HomeEvent
import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.core.feature.QuickAction
import com.henriquesebastiao.prumo.core.model.DayPart
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.core.ui.ServerUnreachableCard
import com.henriquesebastiao.prumo.ui.PreviewData
import java.time.YearMonth

@Composable
fun HomeRoute(
    onOpenModule: (PrumoFeature) -> Unit,
    onOpen: (NavKey) -> Unit,
    onAction: (QuickAction) -> Unit,
    onSettings: () -> Unit,
    onOrganizeModules: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Every time the Home is shown again (back from a module or Settings), it reloads.
    LaunchedEffect(Unit) { viewModel.refresh() }
    HomeScreen(
        state = state,
        onRefresh = viewModel::pull,
        onOpenModule = onOpenModule,
        onOpen = onOpen,
        onAction = onAction,
        onTogglePinned = viewModel::togglePinned,
        onSettings = onSettings,
        onOrganizeModules = onOrganizeModules,
        onSignIn = onSignIn,
        modifier = modifier,
    )
}

/**
 * The root of the app on phones: the greeting, one card per enabled module in the user's order
 * (the first one is the hero, D08), "Vem aí" and "Atividade recente" (D20) and the floating toolbar
 * with the four pinned actions; its FAB opens "Todas as ações". [onOpen] opens an item of the lists.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onOpenModule: (PrumoFeature) -> Unit,
    onOpen: (NavKey) -> Unit,
    onAction: (QuickAction) -> Unit,
    onTogglePinned: (String) -> Unit,
    onSettings: () -> Unit,
    onOrganizeModules: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAllActions by rememberSaveable { mutableStateOf(false) }
    // A module's sheet open over the Home ("Registrar abastecimento"), by the action's id.
    var sheetAction by rememberSaveable { mutableStateOf<String?>(null) }
    val act: (QuickAction) -> Unit = { action ->
        if (action.sheet != null) sheetAction = action.id else onAction(action)
    }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                TopRow(state.sync, onSettings, onSignIn, onRefresh)
                HeroHeader(
                    title = greeting(state.greeting),
                    subtitle = state.date?.let(Formats::weekdayAndDate),
                    small = true,
                )
                when {
                    state.loading -> LoadingState(stringResource(R.string.home_loading))

                    state.error != null -> ErrorState(state.error, onRetry = onRefresh, server = state.server)

                    else -> PullToRefreshBox(
                        isRefreshing = state.refreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.weight(1f),
                    ) {
                        Cards(state, onOpenModule, onOpen, onOrganizeModules, onRefresh)
                    }
                }
            }
            if (state.pinned.isNotEmpty() && state.error == null && !state.loading) {
                Toolbar(
                    state.pinned,
                    act,
                    onAll = { showAllActions = true },
                    modifier = Modifier.align(
                        Alignment.BottomCenter,
                    ).navigationBarsPadding().padding(bottom = Spacing.lg),
                )
            }
        }
    }
    if (showAllActions) {
        AllActionsSheet(
            groups = state.groups,
            pinned = state.pinned.map { it.id }.toSet(),
            onAction = {
                showAllActions = false
                act(it)
            },
            onTogglePinned = onTogglePinned,
            onDismiss = { showAllActions = false },
        )
    }
    val sheet = sheetAction?.let { id -> state.groups.flatMap { it.actions }.firstOrNull { it.id == id }?.sheet }
    sheet?.invoke { sheetAction = null }
}

@Composable
private fun TopRow(sync: SyncStatus, onSettings: () -> Unit, onSignIn: () -> Unit, onRefresh: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(start = Spacing.xl, end = Spacing.md, top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMediumEmphasized,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        val (icon, label, action) = when (sync) {
            SyncStatus.Synced -> Triple(PrumoIcons.CloudDone, R.string.home_synced, onRefresh)
            SyncStatus.Unreachable -> Triple(PrumoIcons.CloudAlert, R.string.home_unreachable, onRefresh)
            SyncStatus.SessionExpired -> Triple(PrumoIcons.LockClock, R.string.home_session_expired, onSignIn)
        }
        val tonal = sync != SyncStatus.Synced
        FilledTonalIconButton(
            onClick = action,
            colors = if (tonal) {
                IconButtonDefaults.filledTonalIconButtonColors()
            } else {
                IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            },
        ) {
            Icon(painterResource(icon), contentDescription = stringResource(label))
        }
        FilledTonalIconButton(
            onClick = onSettings,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Icon(painterResource(PrumoIcons.Settings), contentDescription = stringResource(R.string.home_settings))
        }
    }
}

@Composable
private fun Cards(
    state: HomeUiState,
    onOpenModule: (PrumoFeature) -> Unit,
    onOpen: (NavKey) -> Unit,
    onOrganizeModules: () -> Unit,
    onRetry: () -> Unit,
) {
    val list = rememberLazyListState()
    // The card goes in above the hero, and the keyed list keeps the hero where it was: with the list at
    // its top (the device's copy shown first, D21), bring the card into view.
    val unreachable = state.sync == SyncStatus.Unreachable
    LaunchedEffect(unreachable) {
        if (unreachable && list.firstVisibleItemIndex <= 1) list.animateScrollToItem(0)
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        state = list,
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = Spacing.md,
            bottom = TOOLBAR_CLEARANCE,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (state.sync == SyncStatus.Unreachable) {
            item(key = "unreachable") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    ServerUnreachableCard(state.server, onRetry)
                    state.loadedAt?.let {
                        Tag(
                            stringResource(R.string.home_cached_at, Formats.time(it)),
                            icon = PrumoIcons.History,
                            small = true,
                        )
                    }
                }
            }
        }
        if (state.cards.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    PrumoIcons.Widgets,
                    stringResource(R.string.home_no_modules_title),
                    body = stringResource(R.string.home_no_modules_body),
                )
            }
        }
        state.cards.firstOrNull()?.let { hero ->
            item(key = hero.feature.id) {
                hero.feature.HomeCard(hero.summary, true, state.context, {
                    onOpenModule(hero.feature)
                }, Modifier.fillMaxWidth())
            }
        }
        items(state.cards.drop(1).chunked(2), key = { row -> row.joinToString { it.feature.id } }) { row ->
            // Cards side by side take the height of the taller one.
            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                row.forEach { card ->
                    card.feature.HomeCard(card.summary, false, state.context, {
                        onOpenModule(card.feature)
                    }, Modifier.weight(1f).fillMaxHeight())
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        // One item: a list whose kinds no module knows draws nothing, and leaves no gap.
        item(key = "events") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Upcoming(state.upcoming, state.context, onOpen)
                Activity(state.activity, state.context, onOpen)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = onOrganizeModules) {
                        Icon(painterResource(PrumoIcons.Tune), contentDescription = null)
                        Spacer(Modifier.padding(start = Spacing.sm))
                        Text(stringResource(R.string.home_organize_modules))
                    }
                }
            }
        }
    }
}

/** "Vem aí": the first items the modules describe, by date (D20), each with its day. */
@Composable
private fun Upcoming(entries: List<HomeUpcoming>, context: HomeContext, onOpen: (NavKey) -> Unit) {
    val rows = entries.mapNotNull { entry ->
        entry.feature.upcoming(entry.item, context)?.let { event -> entry to event }
    }.take(EVENTS_SHOWN)
    if (rows.isEmpty()) return
    EventList(stringResource(R.string.home_upcoming)) {
        rows.forEachIndexed { index, (entry, event) ->
            val on = entry.item.on
            EventRow(event, index, rows.size, onOpen) {
                ModuleTheme(entry.feature.seed) {
                    DateBadge(on.dayOfMonth.toString(), Formats.shortMonth(YearMonth.from(on)))
                }
            }
        }
    }
}

/** "Atividade recente": the newest records the modules describe (D20), each in its module's colors. */
@Composable
private fun Activity(entries: List<HomeActivity>, context: HomeContext, onOpen: (NavKey) -> Unit) {
    val rows = entries.mapNotNull { entry ->
        entry.feature.activity(entry.item, context)?.let { event -> entry to event }
    }.take(EVENTS_SHOWN)
    if (rows.isEmpty()) return
    EventList(stringResource(R.string.home_activity)) {
        rows.forEachIndexed { index, (entry, event) ->
            EventRow(event, index, rows.size, onOpen) {
                ModuleTheme(entry.feature.seed) { IconAvatar(event.icon ?: entry.feature.icon) }
            }
        }
    }
}

@Composable
private fun EventList(title: String, rows: @Composable () -> Unit) {
    Column {
        SectionHeader(title)
        Column(verticalArrangement = Arrangement.spacedBy(ListStyle.Segmented.gap)) { rows() }
    }
}

/** A line of the Home's lists: tapping it opens its [HomeEvent.target], when it has one. */
@Composable
private fun EventRow(
    event: HomeEvent,
    index: Int,
    count: Int,
    onOpen: (NavKey) -> Unit,
    leading: @Composable () -> Unit,
) {
    val value: (@Composable () -> Unit)? = event.value?.let { text ->
        {
            Text(
                text,
                style = PrumoType.dataSmall,
                color = if (event.positive) {
                    PrumoTheme.extendedColors.success
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
    val target = event.target
    GroupedListItem(
        GroupedRow(
            title = event.title,
            summary = event.subtitle,
            action = if (target != null) {
                RowAction.Click({ onOpen(target) }, chevron = false, trailing = value)
            } else {
                RowAction.Static(value)
            },
            leading = leading,
        ),
        index,
        count,
    )
}

@Composable
private fun Toolbar(
    pinned: List<QuickAction>,
    onAction: (QuickAction) -> Unit,
    onAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
            // The vibrant toolbar's FAB is in tertiary container, as in the design system.
            FloatingToolbarDefaults.VibrantFloatingActionButton(
                onClick = onAll,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Icon(painterResource(PrumoIcons.Add), contentDescription = stringResource(R.string.home_all_actions))
            }
        },
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = modifier,
    ) {
        pinned.forEach { action ->
            IconButton(onClick = { onAction(action) }) {
                Icon(painterResource(action.icon), contentDescription = stringResource(action.labelRes))
            }
        }
    }
}

@Composable
private fun greeting(part: DayPart): String = stringResource(
    when (part) {
        DayPart.Morning -> R.string.home_greeting_morning
        DayPart.Afternoon -> R.string.home_greeting_afternoon
        DayPart.Night -> R.string.home_greeting_night
    },
)

// The list scrolls clear of the floating toolbar (64 dp + its margin).
private val TOOLBAR_CLEARANCE = 112.dp

/** Items shown in each of the Home's lists, as on the web (the server sends up to ten). */
private const val EVENTS_SHOWN = 4

@PhonePreviews
@Composable
private fun HomePreview() {
    PrumoTheme(dynamicColor = false) {
        HomeScreen(
            state = PreviewData.home,
            onRefresh = {},
            onOpenModule = {},
            onOpen = {},
            onAction = {},
            onTogglePinned = {},
            onSettings = {},
            onOrganizeModules = {},
            onSignIn = {},
        )
    }
}
