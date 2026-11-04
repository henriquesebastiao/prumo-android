package com.henriquesebastiao.prumo.system.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.R
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.data.repository.DevicePreferencesRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ChoiceGroup
import com.henriquesebastiao.prumo.core.designsystem.component.GroupNote
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedList
import com.henriquesebastiao.prumo.core.designsystem.component.GroupedRow
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.RowAction
import com.henriquesebastiao.prumo.core.designsystem.component.SectionHeader
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.DevicePreferences
import com.henriquesebastiao.prumo.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppearanceUiState(
    val theme: ThemeMode = ThemeMode.System,
    val device: DevicePreferences = DevicePreferences(),
)

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val account: AccountRepository,
    private val device: DevicePreferencesRepository,
) : ViewModel() {
    val state: StateFlow<AppearanceUiState> = combine(account.preferences, device.preferences) { prefs, local ->
        AppearanceUiState(prefs?.theme ?: ThemeMode.System, local)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppearanceUiState())

    /** The account's theme (`PATCH /me/preferences`): it follows the user on the web and other devices. */
    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch { account.setTheme(theme) }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch { device.setPureBlack(enabled) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { device.setDynamicColor(enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@Composable
fun AppearanceRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppearanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppearanceScreen(
        state = state,
        dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        onTheme = viewModel::setTheme,
        onPureBlack = viewModel::setPureBlack,
        onDynamicColor = viewModel::setDynamicColor,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Settings › Aparência: the theme in a connected button group, pure black and dynamic color. */
@Composable
fun AppearanceScreen(
    state: AppearanceUiState,
    dynamicColorAvailable: Boolean,
    onTheme: (ThemeMode) -> Unit,
    onPureBlack: (Boolean) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier, topBar = { PrumoTopBar(stringResource(R.string.appearance_title), onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionHeader(stringResource(R.string.appearance_theme))
            ChoiceGroup(
                options = ThemeMode.entries,
                selected = state.theme,
                label = { stringResource(it.label()) },
                onSelect = onTheme,
                icon = { it.icon() },
            )
            GroupedList(
                listOf(
                    GroupedRow(
                        stringResource(R.string.appearance_pure_black),
                        RowAction.Toggle(state.device.pureBlack, onPureBlack),
                        stringResource(R.string.appearance_pure_black_summary),
                        PrumoIcons.Contrast,
                    ),
                    GroupedRow(
                        stringResource(R.string.appearance_dynamic_color),
                        RowAction.Toggle(state.device.dynamicColor && dynamicColorAvailable, onDynamicColor),
                        stringResource(
                            if (dynamicColorAvailable) {
                                R.string.appearance_dynamic_color_summary
                            } else {
                                R.string.appearance_dynamic_color_unavailable
                            },
                        ),
                        PrumoIcons.FormatColorFill,
                        enabled = dynamicColorAvailable,
                    ),
                ),
            )
            GroupNote(stringResource(R.string.appearance_note))
        }
    }
}

private fun ThemeMode.label(): Int = when (this) {
    ThemeMode.System -> R.string.appearance_theme_system
    ThemeMode.Light -> R.string.appearance_theme_light
    ThemeMode.Dark -> R.string.appearance_theme_dark
}

private fun ThemeMode.icon(): Int = when (this) {
    ThemeMode.System -> PrumoIcons.BrightnessAuto
    ThemeMode.Light -> PrumoIcons.LightMode
    ThemeMode.Dark -> PrumoIcons.DarkMode
}

@PhonePreviews
@Composable
private fun AppearancePreview() {
    PrumoTheme(dynamicColor = false) {
        AppearanceScreen(AppearanceUiState(), dynamicColorAvailable = true, onTheme = {
        }, onPureBlack = {}, onDynamicColor = {}, onBack = {})
    }
}
