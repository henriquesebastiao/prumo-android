package com.henriquesebastiao.prumo.system.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.data.repository.DashboardRepository
import com.henriquesebastiao.prumo.core.data.repository.DataChanges
import com.henriquesebastiao.prumo.core.data.repository.DevicePreferencesRepository
import com.henriquesebastiao.prumo.core.data.repository.LoadedDashboard
import com.henriquesebastiao.prumo.core.data.session.SessionRepository
import com.henriquesebastiao.prumo.core.data.session.SessionState
import com.henriquesebastiao.prumo.core.data.session.SignOutReason
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.core.feature.QuickAction
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.DayPart
import com.henriquesebastiao.prumo.core.model.DevicePreferences
import com.henriquesebastiao.prumo.core.model.Preferences
import com.henriquesebastiao.prumo.core.model.ServerAddress
import com.henriquesebastiao.prumo.core.model.errorOrNull
import com.henriquesebastiao.prumo.system.ModuleRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/** A module's card on the Home: its feature and its summary from `GET /dashboard`. */
data class HomeModuleCard(val feature: PrumoFeature, val summary: JsonObject?)

/** A module's actions in "Todas as ações". */
data class ActionGroup(val feature: PrumoFeature, val actions: List<QuickAction>)

/** How the Home stands with the server, for the cloud icon at the top. */
enum class SyncStatus { Synced, Unreachable, SessionExpired }

data class HomeUiState(
    val loading: Boolean = true,
    /** Nothing to show: the first load failed. */
    val error: AppError? = null,
    val greeting: DayPart = DayPart.Afternoon,
    val date: LocalDate? = null,
    val cards: List<HomeModuleCard> = emptyList(),
    val context: HomeContext = HomeContext("BRL", LocalDate.MIN),
    val pinned: List<QuickAction> = emptyList(),
    val groups: List<ActionGroup> = emptyList(),
    /** When the shown data was loaded, in the user's zone: shown when the server stops answering. */
    val loadedAt: LocalTime? = null,
    val server: String = "",
    val sync: SyncStatus = SyncStatus.Synced,
    val refreshing: Boolean = false,
)

private data class LoadStatus(val loading: Boolean = true, val error: AppError? = null)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val account: AccountRepository,
    private val dashboard: DashboardRepository,
    private val devicePreferences: DevicePreferencesRepository,
    private val registry: ModuleRegistry,
    sessions: SessionRepository,
    private val clock: Clock,
    changes: DataChanges,
) : ViewModel() {
    private val status = MutableStateFlow(LoadStatus())

    init {
        // A record saved in a module (a quick action's sheet over the Home) changes its summary.
        viewModelScope.launch { changes.changes.collect { dashboard.refresh() } }
    }

    val state: StateFlow<HomeUiState> = combine(
        account.preferences,
        dashboard.last,
        devicePreferences.preferences,
        sessions.state,
        status,
    ) { prefs, last, device, session, load -> build(prefs, last, device, session, load) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    /** Loads the preferences (order, zone, currency) and the Home. The last data stays on screen. */
    fun refresh() {
        status.value = status.value.copy(loading = true)
        viewModelScope.launch {
            val prefs = account.refreshPreferences()
            val home = dashboard.refresh()
            status.value = LoadStatus(loading = false, error = home.errorOrNull() ?: prefs.errorOrNull())
        }
    }

    /** Pins [id] to the toolbar, or unpins it (four at most). */
    fun togglePinned(id: String) {
        val current = state.value.pinned.map { it.id }
        val next = if (id in current) current - id else (current + id).takeLast(MAX_PINNED)
        viewModelScope.launch { devicePreferences.setPinnedActions(next) }
    }

    private fun build(
        prefs: Preferences?,
        last: LoadedDashboard?,
        device: DevicePreferences,
        session: SessionState,
        load: LoadStatus,
    ): HomeUiState {
        val now = clock.instant()
        val zone = (prefs ?: Preferences.Default).zone
        val features = registry.enabled(prefs?.enabledModules.orEmpty())
        val summaries = last?.dashboard?.summaries.orEmpty().associate { it.module to it.json }
        val groups = features.map { ActionGroup(it, it.quickActions) }.filter { it.actions.isNotEmpty() }
        val signedIn = session as? SessionState.SignedIn
        val expired = (session as? SessionState.SignedOut)?.reason == SignOutReason.Expired
        val hasData = prefs != null && last != null
        return HomeUiState(
            loading = load.loading && !hasData,
            error = load.error.takeIf { !hasData },
            greeting = DayPart.of(now.atZone(zone).hour),
            date = prefs?.today(now) ?: now.atZone(zone).toLocalDate(),
            cards = features.map { HomeModuleCard(it, summaries[it.id]) },
            context = HomeContext(prefs?.currency ?: Preferences.Default.currency, now.atZone(zone).toLocalDate()),
            pinned = pinned(groups, device.pinnedActions),
            groups = groups,
            loadedAt = last?.loadedAt?.atZone(zone)?.toLocalTime(),
            server = signedIn?.session?.baseUrl?.let(ServerAddress::display).orEmpty(),
            sync = when {
                expired || load.error == AppError.SessionExpired -> SyncStatus.SessionExpired
                load.error is AppError.Unreachable -> SyncStatus.Unreachable
                else -> SyncStatus.Synced
            },
            refreshing = load.loading && hasData,
        )
    }

    /** The device's pinned actions, or the design's four (one or two per module, in its order). */
    private fun pinned(groups: List<ActionGroup>, chosen: List<String>?): List<QuickAction> {
        val all = groups.flatMap { it.actions }.associateBy { it.id }
        val ids = chosen ?: DEFAULT_PINNED.filter { it in all }.ifEmpty { all.keys.take(MAX_PINNED) }
        return ids.mapNotNull { all[it] }.take(MAX_PINNED)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val MAX_PINNED = 4
        val DEFAULT_PINNED = listOf("finance.expense", "garage.fueling", "garage.odometer", "reading.progress")
    }
}
