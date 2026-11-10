package com.henriquesebastiao.prumo.system.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.data.repository.DataExportRepository
import com.henriquesebastiao.prumo.core.data.repository.DevicePreferencesRepository
import com.henriquesebastiao.prumo.core.data.repository.ExportResult
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
import java.time.Clock
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    /** "Exportar dados" is downloading the archive. */
    val exporting: Boolean = false,
    /** How the last export ended, until its message is shown. */
    val exportResult: ExportResult? = null,
)

private data class Extra(
    val sessionsCount: Int? = null,
    val serverReachable: Boolean? = null,
    val exporting: Boolean = false,
    val exportResult: ExportResult? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val account: AccountRepository,
    private val sessions: SessionRepository,
    private val servers: ServerRepository,
    private val registry: ModuleRegistry,
    private val export: DataExportRepository,
    private val clock: Clock,
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
            exporting = more.exporting,
            exportResult = more.exportResult,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    fun refresh() {
        viewModelScope.launch {
            account.refreshUser()
            account.refreshPreferences()
            val count = account.sessions().getOrNull()?.size
            val baseUrl = (sessions.state.value as? SessionState.SignedIn)?.session?.baseUrl
            val reachable = baseUrl?.let { servers.check(it) is ServerCheck.Found }
            extra.update { it.copy(sessionsCount = count, serverReachable = reachable) }
        }
    }

    /** The name the file picker suggests, like the server's: `prumo-marina-20261117-1430.zip`. */
    fun exportFileName(): String {
        val zone = (account.preferences.value ?: Preferences.Default).zone
        val stamp = FILE_STAMP.format(clock.instant().atZone(zone))
        val user = account.user.value?.username
        return if (user != null) "prumo-$user-$stamp.zip" else "prumo-$stamp.zip"
    }

    /** "Exportar dados": the account's data (D38 of the server) into the file the user chose. */
    fun export(target: Uri) {
        if (extra.value.exporting) return
        extra.update { it.copy(exporting = true, exportResult = null) }
        viewModelScope.launch {
            val result = export.export(target)
            extra.update { it.copy(exporting = false, exportResult = result) }
        }
    }

    fun exportResultShown() {
        extra.update { it.copy(exportResult = null) }
    }

    fun signOut() {
        viewModelScope.launch { sessions.signOut() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        val FILE_STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
    }
}
