package com.henriquesebastiao.prumo.system.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henriquesebastiao.prumo.BuildConfig
import com.henriquesebastiao.prumo.R
import com.henriquesebastiao.prumo.core.designsystem.component.ConfirmDialog
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedList
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.ThemeMode
import com.henriquesebastiao.prumo.ui.PreviewData

/** Where each entry of Settings leads. */
data class SettingsActions(
    val onBack: () -> Unit,
    val onAccount: () -> Unit,
    val onAppearance: () -> Unit,
    val onModules: () -> Unit,
    val onSessions: () -> Unit,
    val onServer: () -> Unit,
)

@Composable
fun SettingsRoute(
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    SettingsScreen(state, actions, onSignOut = viewModel::signOut, modifier = modifier)
}

/** Settings, opened by the gear on the Home: the account on top, then the app, then "Sair". */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            PrumoTopBar(stringResource(R.string.settings_title), actions.onBack, large = true, scrollBehavior = scroll)
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.screen,
                end = Spacing.screen,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + Spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item {
                val user = state.user
                GroupedList(
                    listOf(
                        GroupedRow(
                            title = user?.displayName.orEmpty(),
                            action = RowAction.Click(actions.onAccount),
                            summary = user?.let { stringResource(R.string.settings_account_summary, it.username) },
                            icon = PrumoIcons.Person,
                            emphasized = true,
                        ),
                    ),
                )
            }
            item { AppGroup(state, actions) }
            item {
                GroupedList(
                    listOf(
                        GroupedRow(
                            title = stringResource(R.string.settings_sign_out),
                            action = RowAction.Click({ confirmSignOut = true }, chevron = false),
                            summary = stringResource(R.string.settings_sign_out_summary),
                            icon = PrumoIcons.Logout,
                            neutral = true,
                        ),
                    ),
                )
            }
            item {
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                )
            }
        }
    }
    if (confirmSignOut) {
        ConfirmDialog(
            icon = PrumoIcons.Logout,
            title = stringResource(R.string.sign_out_title),
            body = stringResource(R.string.sign_out_body),
            confirmLabel = stringResource(R.string.settings_sign_out),
            onConfirm = {
                confirmSignOut = false
                onSignOut()
            },
            onDismiss = { confirmSignOut = false },
        )
    }
}

@Composable
private fun AppGroup(state: SettingsUiState, actions: SettingsActions) {
    val theme = stringResource(
        when (state.preferences?.theme ?: ThemeMode.System) {
            ThemeMode.System -> R.string.settings_theme_system_summary
            ThemeMode.Light -> R.string.settings_theme_light_summary
            ThemeMode.Dark -> R.string.settings_theme_dark_summary
        },
    )
    val names = state.enabledModules.map { stringResource(it) }
    val modules = if (names.isEmpty()) {
        stringResource(R.string.settings_modules_none)
    } else {
        pluralStringResource(R.plurals.settings_modules_summary, names.size, names.size, names.joinToString(", "))
    }
    val server = when (state.serverReachable) {
        false -> stringResource(R.string.settings_server_unreachable, state.server)
        else -> stringResource(R.string.settings_server_connected, state.server)
    }
    GroupedList(
        listOfNotNull(
            GroupedRow(
                stringResource(R.string.settings_appearance),
                RowAction.Click(actions.onAppearance),
                if (state.device.dynamicColor) stringResource(R.string.settings_appearance_dynamic, theme) else theme,
                PrumoIcons.Palette,
            ),
            GroupedRow(
                stringResource(R.string.settings_modules),
                RowAction.Click(actions.onModules),
                modules,
                PrumoIcons.Widgets,
            ),
            GroupedRow(
                stringResource(R.string.settings_sessions),
                RowAction.Click(actions.onSessions),
                state.sessionsCount?.let { pluralStringResource(R.plurals.settings_sessions_summary, it, it) },
                PrumoIcons.Devices,
            ),
            GroupedRow(
                stringResource(R.string.settings_server),
                RowAction.Click(actions.onServer),
                server,
                PrumoIcons.Dns,
            ),
        ),
    )
}

@PhonePreviews
@Composable
private fun SettingsPreview() {
    PrumoTheme(dynamicColor = false) {
        SettingsScreen(PreviewData.settings, SettingsActions({}, {}, {}, {}, {}, {}), onSignOut = {})
    }
}
