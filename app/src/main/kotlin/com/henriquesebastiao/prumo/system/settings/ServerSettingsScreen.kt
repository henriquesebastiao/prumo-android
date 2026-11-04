package com.henriquesebastiao.prumo.system.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.R
import com.henriquesebastiao.prumo.core.data.repository.ServerRepository
import com.henriquesebastiao.prumo.core.data.session.SessionRepository
import com.henriquesebastiao.prumo.core.data.session.SessionState
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.ErrorCard
import com.henriquesebastiao.prumo.core.designsystem.component.GroupNote
import com.henriquesebastiao.prumo.core.designsystem.component.KpiTile
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.StatusChip
import com.henriquesebastiao.prumo.core.designsystem.component.StatusTone
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.ServerAddress
import com.henriquesebastiao.prumo.core.model.ServerCheck
import com.henriquesebastiao.prumo.core.ui.R as UiR
import com.henriquesebastiao.prumo.core.ui.unreachableText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerSettingsUiState(val server: String = "", val checking: Boolean = true, val check: ServerCheck? = null)

@HiltViewModel
class ServerSettingsViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val servers: ServerRepository,
) : ViewModel() {
    private val baseUrl = (sessions.state.value as? SessionState.SignedIn)?.session?.baseUrl
    private val mutableState =
        MutableStateFlow(ServerSettingsUiState(server = baseUrl?.let(ServerAddress::display).orEmpty()))
    val state: StateFlow<ServerSettingsUiState> = mutableState.asStateFlow()

    init {
        test()
    }

    /** "Testar conexão": the health of the session's server, with its version and response time. */
    fun test() {
        val url = baseUrl ?: return
        mutableState.update { it.copy(checking = true) }
        viewModelScope.launch {
            val check = servers.check(url)
            mutableState.update { it.copy(checking = false, check = check) }
        }
    }

    /** "Trocar servidor": signs out here and starts the first run again. */
    fun changeServer() {
        viewModelScope.launch { sessions.forgetServer() }
    }
}

@Composable
fun ServerSettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServerSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ServerSettingsScreen(state, viewModel::test, viewModel::changeServer, onBack, modifier)
}

/** Settings › Servidor: the address, whether it answers, its version and response time. */
@Composable
fun ServerSettingsScreen(
    state: ServerSettingsUiState,
    onTest: () -> Unit,
    onChangeServer: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmChange by rememberSaveable { mutableStateOf(false) }
    Scaffold(modifier, topBar = { PrumoTopBar(stringResource(R.string.server_settings_title), onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SectionCard(icon = PrumoIcons.Dns, title = state.server) {
                when (val check = state.check) {
                    is ServerCheck.Found -> {
                        StatusChip(stringResource(R.string.server_settings_connected), StatusTone.Success)
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            KpiTile(
                                stringResource(R.string.server_version_value, check.version),
                                stringResource(R.string.server_version),
                                Modifier.weight(1f),
                            )
                            KpiTile(
                                stringResource(R.string.server_latency_value, check.latencyMillis),
                                stringResource(R.string.server_latency),
                                Modifier.weight(1f),
                            )
                        }
                    }

                    is ServerCheck.Unreachable -> ErrorCard(
                        stringResource(UiR.string.ui_server_unreachable_title),
                        unreachableText(check.cause, state.server),
                        icon = PrumoIcons.CloudAlert,
                    )

                    is ServerCheck.DatabaseDown -> ErrorCard(
                        stringResource(R.string.server_database_down_title),
                        stringResource(R.string.server_database_down_body, check.version),
                    )

                    is ServerCheck.NotPrumo -> ErrorCard(
                        stringResource(R.string.server_not_prumo_title),
                        stringResource(R.string.server_not_prumo_body, state.server),
                    )

                    ServerCheck.InvalidAddress, null -> if (state.checking) LoadingIndicator()
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(onClick = onTest, enabled = !state.checking) {
                    Icon(painterResource(PrumoIcons.NetworkCheck), contentDescription = null)
                    Text(stringResource(R.string.server_settings_test), Modifier.padding(start = Spacing.sm))
                }
                OutlinedButton(onClick = {
                    confirmChange = true
                }) { Text(stringResource(R.string.server_settings_change)) }
            }
            GroupNote(stringResource(R.string.server_settings_change_note))
        }
    }
    if (confirmChange) {
        ConfirmDialog(
            icon = PrumoIcons.Dns,
            title = stringResource(R.string.server_settings_change_title),
            body = stringResource(R.string.server_settings_change_body),
            confirmLabel = stringResource(R.string.server_settings_change),
            onConfirm = {
                confirmChange = false
                onChangeServer()
            },
            onDismiss = { confirmChange = false },
        )
    }
}

@PhonePreviews
@Composable
private fun ServerSettingsPreview() {
    PrumoTheme(dynamicColor = false) {
        ServerSettingsScreen(
            ServerSettingsUiState(
                server = "prumo.minhacasa.net",
                checking = false,
                check = ServerCheck.Found("https://prumo.minhacasa.net", "1.0.2", 84),
            ),
            onTest = {},
            onChangeServer = {},
            onBack = {},
        )
    }
}
