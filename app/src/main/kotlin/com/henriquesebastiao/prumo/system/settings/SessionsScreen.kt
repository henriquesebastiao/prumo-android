package com.henriquesebastiao.prumo.system.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.R
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedList
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.DeviceSession
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.Preferences
import com.henriquesebastiao.prumo.core.model.SessionClient
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.ui.PreviewData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionsUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    /** This device's session first, then the others by last use. */
    val sessions: List<DeviceSession> = emptyList(),
    val zone: ZoneId = ZoneId.systemDefault(),
)

@HiltViewModel
class SessionsViewModel @Inject constructor(private val account: AccountRepository, private val clock: Clock) :
    ViewModel() {
    private val mutableState = MutableStateFlow(SessionsUiState())
    val state: StateFlow<SessionsUiState> = mutableState.asStateFlow()

    /** Today in the user's zone, for "hoje, 09:14". */
    val today get() = clock.instant().atZone(state.value.zone).toLocalDate()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val zone = (account.preferences.value ?: Preferences.Default).zone
            when (val outcome = account.sessions()) {
                is Outcome.Ok -> mutableState.value = SessionsUiState(
                    loading = false,
                    sessions = outcome.value.sortedWith(
                        compareByDescending<DeviceSession> {
                            it.current
                        }.thenByDescending { it.lastUsedAt },
                    ),
                    zone = zone,
                )

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun revoke(id: String) {
        viewModelScope.launch {
            account.revokeSession(id)
            load()
        }
    }

    /** "Encerrar as outras sessões": one by one, there is no batch route (D13). */
    fun revokeOthers() {
        viewModelScope.launch {
            state.value.sessions.filterNot { it.current }.forEach { account.revokeSession(it.id) }
            load()
        }
    }
}

@Composable
fun SessionsRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SessionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SessionsScreen(
        state,
        viewModel.today,
        viewModel::revoke,
        viewModel::revokeOthers,
        viewModel::load,
        onBack,
        modifier,
    )
}

/** Settings › Sessões ativas: each device and browser, its client and last use; end one or all others. */
@Composable
fun SessionsScreen(
    state: SessionsUiState,
    today: LocalDate,
    onRevoke: (String) -> Unit,
    onRevokeOthers: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var revoking by rememberSaveable { mutableStateOf<String?>(null) }
    var revokingOthers by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier,
        topBar = { PrumoTopBar(stringResource(R.string.sessions_title), onBack) },
    ) { padding ->
        when {
            state.loading -> LoadingState(stringResource(R.string.sessions_title), Modifier.padding(padding))

            state.error != null -> ErrorState(state.error, onRetry, Modifier.padding(padding))

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                GroupedList(
                    state.sessions.map { session ->
                        sessionRow(session, state.zone, today) {
                            revoking =
                                session.id
                        }
                    },
                )
                if (state.sessions.any { !it.current }) {
                    OutlinedButton(onClick = { revokingOthers = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(painterResource(PrumoIcons.Logout), contentDescription = null)
                        Text(stringResource(R.string.sessions_revoke_others), Modifier.padding(start = Spacing.sm))
                    }
                }
            }
        }
    }
    revoking?.let { id ->
        val name =
            state.sessions.firstOrNull { it.id == id }?.deviceName ?: stringResource(R.string.sessions_unknown_device)
        ConfirmDialog(
            icon = PrumoIcons.Logout,
            title = stringResource(R.string.sessions_revoke_title),
            body = stringResource(R.string.sessions_revoke_body, name),
            confirmLabel = stringResource(R.string.sessions_revoke),
            onConfirm = {
                revoking = null
                onRevoke(id)
            },
            onDismiss = { revoking = null },
            destructive = true,
        )
    }
    if (revokingOthers) {
        ConfirmDialog(
            icon = PrumoIcons.Devices,
            title = stringResource(R.string.sessions_revoke_others_title),
            body = stringResource(R.string.sessions_revoke_others_body),
            confirmLabel = stringResource(R.string.sessions_revoke_others),
            onConfirm = {
                revokingOthers = false
                onRevokeOthers()
            },
            onDismiss = { revokingOthers = false },
            destructive = true,
        )
    }
}

@Composable
private fun sessionRow(session: DeviceSession, zone: ZoneId, today: LocalDate, onRevoke: () -> Unit): GroupedRow {
    val name = session.deviceName ?: stringResource(R.string.sessions_unknown_device)
    val client = stringResource(
        if (session.client ==
            SessionClient.App
        ) {
            R.string.sessions_app
        } else {
            R.string.sessions_web
        },
    )
    val used = session.lastUsedAt.atZone(zone)
    val whenUsed = if (used.toLocalDate() == today) {
        stringResource(R.string.sessions_today, Formats.time(used.toLocalTime()))
    } else {
        "${Formats.date(used.toLocalDate())}, ${Formats.time(used.toLocalTime())}"
    }
    val revokeLabel = stringResource(R.string.sessions_revoke)
    return GroupedRow(
        title = if (session.current) stringResource(R.string.sessions_this_device, name) else name,
        action = if (session.current) {
            RowAction.Static()
        } else {
            RowAction.Static {
                IconButton(onClick = onRevoke) {
                    Icon(painterResource(PrumoIcons.Logout), contentDescription = revokeLabel)
                }
            }
        },
        summary = if (session.current) {
            stringResource(R.string.sessions_active_now, client)
        } else {
            stringResource(R.string.sessions_last_used, client, whenUsed)
        },
        icon = if (session.client == SessionClient.App) PrumoIcons.Smartphone else PrumoIcons.Computer,
        emphasized = session.current,
    )
}

@PhonePreviews
@Composable
private fun SessionsPreview() {
    PrumoTheme(dynamicColor = false) {
        SessionsScreen(
            PreviewData.sessions,
            today = LocalDate.of(2026, 11, 17),
            onRevoke = {},
            onRevokeOthers = {},
            onRetry = {},
            onBack = {},
        )
    }
}
