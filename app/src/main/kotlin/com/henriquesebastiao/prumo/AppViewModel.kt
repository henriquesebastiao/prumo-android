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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
) : ViewModel() {
    val state: StateFlow<AppUiState> = combine(
        sessions.state,
        account.preferences,
        devicePreferences.preferences,
    ) { session, preferences, device ->
        AppUiState(session, preferences?.theme ?: ThemeMode.System, device)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    init {
        // The account's theme as soon as there is a session; signed out, nothing of it stays.
        viewModelScope.launch {
            sessions.state.collect { session ->
                when (session) {
                    is SessionState.SignedIn -> account.refreshPreferences()

                    is SessionState.SignedOut -> if (session.reason != SignOutReason.Expired) {
                        account.clear()
                        dashboard.clear()
                    }

                    SessionState.Loading -> Unit
                }
            }
        }
    }
}
