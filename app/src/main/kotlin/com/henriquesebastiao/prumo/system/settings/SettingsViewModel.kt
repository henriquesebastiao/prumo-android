package com.henriquesebastiao.prumo.system.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.data.repository.DevicePreferencesRepository
import com.henriquesebastiao.prumo.core.data.repository.ServerRepository
import com.henriquesebastiao.prumo.core.data.session.SessionRepository
import com.henriquesebastiao.prumo.core.data.session.SessionState
import com.henriquesebastiao.prumo.core.model.DevicePreferences
import com.henriquesebastiao.prumo.core.model.Preferences
import com.henriquesebastiao.prumo.core.model.ServerAddress
import com.henriquesebastiao.prumo.core.model.ServerCheck
import com.henriquesebastiao.prumo.core.model.User
import com.henriquesebastiao.prumo.core.model.getOrNull
import com.henriquesebastiao.prumo.system.ModuleRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The Settings list: a line of summary under each entry. */
data class SettingsUiState(
    val user: User? = null,
    val preferences: Preferences? = null,
    val device: DevicePreferences = DevicePreferences(),
    /** The names of the enabled modules the app knows, in order. */
    val enabledModules: List<Int> = emptyList(),
    val sessionsCount: Int? = null,
    val server: String = "",
    val serverReachable: Boolean? = null,
)

private data class Extra(val sessionsCount: Int? = null, val serverReachable: Boolean? = null)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val account: AccountRepository,
    private val sessions: SessionRepository,
    private val servers: ServerRepository,
    private val registry: ModuleRegistry,
    devicePreferences: DevicePreferencesRepository,
) : ViewModel() {
    private val extra = MutableStateFlow(Extra())

    val state: StateFlow<SettingsUiState> = combine(
        account.user,
        account.preferences,
        devicePreferences.preferences,
        sessions.state,
        extra,
    ) { user, prefs, device, session, more ->
        SettingsUiState(
            user = user,
            preferences = prefs,
            device = device,
            enabledModules = registry.enabled(prefs?.enabledModules.orEmpty()).map { it.nameRes },
            sessionsCount = more.sessionsCount,
            server = (session as? SessionState.SignedIn)?.session?.baseUrl?.let(ServerAddress::display).orEmpty(),
            serverReachable = more.serverReachable,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    fun refresh() {
        viewModelScope.launch {
            account.refreshUser()
            account.refreshPreferences()
            val count = account.sessions().getOrNull()?.size
            val baseUrl = (sessions.state.value as? SessionState.SignedIn)?.session?.baseUrl
            val reachable = baseUrl?.let { servers.check(it) is ServerCheck.Found }
            extra.value = Extra(count, reachable)
        }
    }

    fun signOut() {
        viewModelScope.launch { sessions.signOut() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
