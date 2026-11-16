package com.henriquesebastiao.prumo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.data.repository.DashboardRepository
import com.henriquesebastiao.prumo.core.data.repository.DevicePreferencesRepository
import com.henriquesebastiao.prumo.core.data.session.SessionRepository
import com.henriquesebastiao.prumo.core.data.session.SessionState
import com.henriquesebastiao.prumo.core.data.session.SignOutReason
import com.henriquesebastiao.prumo.core.model.DevicePreferences
import com.henriquesebastiao.prumo.core.model.ThemeMode
import com.henriquesebastiao.prumo.system.LauncherShortcuts
import com.henriquesebastiao.prumo.system.ModuleRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the whole app needs to draw: the session and the look (theme, pure black, dynamic color). */
data class AppUiState(
    val session: SessionState = SessionState.Loading,
    val theme: ThemeMode = ThemeMode.System,
    val device: DevicePreferences = DevicePreferences(),
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val account: AccountRepository,
    private val dashboard: DashboardRepository,
    devicePreferences: DevicePreferencesRepository,
    private val shortcuts: LauncherShortcuts,
    registry: ModuleRegistry,
) : ViewModel() {
    private val mutableLaunchAction = MutableStateFlow<String?>(null)

    /** A quick action asked by a shortcut of the app's icon, until the Home opens it. */
    val launchAction: StateFlow<String?> = mutableLaunchAction.asStateFlow()

    fun onLaunchAction(id: String?) {
        if (id != null) mutableLaunchAction.value = id
    }

    fun onLaunchActionOpened() {
        mutableLaunchAction.value = null
    }

    val state: StateFlow<AppUiState> = combine(
        sessions.state,
        account.preferences,
        devicePreferences.preferences,
    ) { session, preferences, device ->
        AppUiState(session, preferences?.theme ?: ThemeMode.System, device)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    init {
        // The shortcuts of the app's icon follow the enabled modules ("Marcar hábitos").
        viewModelScope.launch {
            account.preferences.filterNotNull().map { registry.enabled(it.enabledModules) }.distinctUntilChanged()
                .collect { features -> shortcuts.publish(features.flatMap { it.quickActions }) }
        }
        // The account's theme as soon as there is a session; signed out, nothing of it stays.
        viewModelScope.launch {
            sessions.state.collect { session ->
                when (session) {
                    is SessionState.SignedIn -> account.refreshPreferences()

                    is SessionState.SignedOut -> if (session.reason != SignOutReason.Expired) {
                        account.clear()
                        dashboard.clear()
                        shortcuts.clear()
                    }

                    SessionState.Loading -> Unit
                }
            }
        }
    }
}
