package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.ColorOption
import com.henriquesebastiao.prumo.core.designsystem.component.ColorPicker
import com.henriquesebastiao.prumo.core.designsystem.component.IconOption
import com.henriquesebastiao.prumo.core.designsystem.component.IconPicker
import com.henriquesebastiao.prumo.core.designsystem.component.MenuOption
import com.henriquesebastiao.prumo.core.designsystem.component.OverflowMenu
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.ServerIcons
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.core.ui.MoneyTransformation
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
import com.henriquesebastiao.prumo.feature.finance.model.Account
import com.henriquesebastiao.prumo.feature.finance.model.AccountInput
import com.henriquesebastiao.prumo.feature.finance.model.AccountKind
import com.henriquesebastiao.prumo.feature.finance.model.FinanceColor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountEditorUiState(
    val loading: Boolean = false,
    val error: AppError? = null,
    val editing: Account? = null,
    val kind: AccountKind = AccountKind.Checking,
    val color: FinanceColor = FinanceColor.Blue,
    val icon: String = AccountIcons.first(),
    val negative: Boolean = false,
    val includeInTotal: Boolean = true,
    val saving: Boolean = false,
    val saveError: AppError? = null,
    val done: Boolean = false,
    val context: FinanceContext = FinanceContext(),
)

@HiltViewModel(assistedFactory = AccountEditorViewModel.Factory::class)
class AccountEditorViewModel @AssistedInject constructor(
    @Assisted private val accountId: String?,
    private val repository: FinanceRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    var name by mutableStateOf("")
        private set

    /** The balance as digits of cents; [AccountEditorUiState.negative] gives its sign. */
    var balance by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(
        AccountEditorUiState(loading = accountId != null, context = account.preferences.value.financeContext(clock)),
    )
    val state: StateFlow<AccountEditorUiState> = mutableState.asStateFlow()

    init {
        if (accountId != null) load()
    }

    fun load() {
        val id = accountId ?: return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.account(id)) {
                is Outcome.Ok -> {
                    val account = outcome.value
                    name = account.name
                    balance = Inputs.digitsOf(kotlin.math.abs(account.initialBalanceCents))
                    mutableState.update {
                        it.copy(
                            loading = false,
                            editing = account,
                            kind = account.kind,
                            color = account.color,
                            icon = account.icon,
                            negative = account.initialBalanceCents < 0,
                            includeInTotal = account.includeInTotal,
                        )
                    }
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun onNameChange(text: String) {
        name = text
        mutableState.update { it.copy(saveError = null) }
    }

    fun onBalanceChange(digits: String) {
        balance = digits
    }

    fun onKindChange(kind: AccountKind) = mutableState.update { it.copy(kind = kind) }

    fun onColorChange(color: FinanceColor) = mutableState.update { it.copy(color = color) }

    fun onIconChange(icon: String) = mutableState.update { it.copy(icon = icon) }

    fun onNegativeChange(negative: Boolean) = mutableState.update { it.copy(negative = negative) }

    fun onIncludeChange(include: Boolean) = mutableState.update { it.copy(includeInTotal = include) }

    val canSave: Boolean get() = name.isNotBlank()

    fun save() {
        if (!canSave) return
        val current = mutableState.value
        val cents = Inputs.number(balance) ?: 0L
        val input = AccountInput(
            name = name.trim(),
            kind = current.kind,
            initialBalanceCents = if (current.negative) -cents else cents,
            color = current.color,
            icon = current.icon,
            includeInTotal = current.includeInTotal,
        )
        submit {
            if (accountId ==
                null
            ) {
                repository.createAccount(input)
            } else {
                repository.updateAccount(accountId, input)
            }
        }
    }

    fun setArchived(archived: Boolean) {
        val id = accountId ?: return
        submit { repository.setAccountArchived(id, archived) }
    }

    private fun submit(call: suspend () -> Outcome<*>) {
        if (mutableState.value.saving) return
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val outcome = call()) {
                is Outcome.Ok -> mutableState.update { it.copy(saving = false, done = true) }
                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = outcome.error) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(accountId: String?): AccountEditorViewModel
    }
}

/** What the account form does when something changes. */
internal class AccountEditorEvents(
    val onNameChange: (String) -> Unit,
    val onBalanceChange: (String) -> Unit,
    val onKindChange: (AccountKind) -> Unit,
    val onColorChange: (FinanceColor) -> Unit,
    val onIconChange: (String) -> Unit,
    val onNegativeChange: (Boolean) -> Unit,
    val onIncludeChange: (Boolean) -> Unit,
    val onSave: () -> Unit,
    val onArchive: (Boolean) -> Unit,
)

@Composable
internal fun AccountEditorRoute(
    accountId: String?,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: AccountEditorViewModel = hiltViewModel<AccountEditorViewModel, AccountEditorViewModel.Factory>(
        creationCallback = { it.create(accountId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) navigator.back() }
    AccountEditorScreen(
        state = state,
        name = viewModel.name,
        balance = viewModel.balance,
        canSave = viewModel.canSave,
        events = AccountEditorEvents(
            onNameChange = viewModel::onNameChange,
            onBalanceChange = viewModel::onBalanceChange,
            onKindChange = viewModel::onKindChange,
            onColorChange = viewModel::onColorChange,
            onIconChange = viewModel::onIconChange,
            onNegativeChange = viewModel::onNegativeChange,
            onIncludeChange = viewModel::onIncludeChange,
            onSave = viewModel::save,
            onArchive = viewModel::setArchived,
        ),
        onRetry = viewModel::load,
        onClose = navigator::back,
        modifier = modifier,
    )
}

/** A new account or its edit (board 06). */
@Composable
internal fun AccountEditorScreen(
    state: AccountEditorUiState,
    name: String,
    balance: String,
    canSave: Boolean,
    events: AccountEditorEvents,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editing = state.editing
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(
                title = stringResource(
                    if (editing ==
                        null
                    ) {
                        R.string.finance_account_new
                    } else {
                        R.string.finance_account_edit
                    },
                ),
                onBack = onClose,
                close = true,
                actions = {
                    if (editing != null) {
                        OverflowMenu(
                            listOf(
                                if (editing.archived) {
                                    MenuOption(stringResource(R.string.finance_unarchive), PrumoIcons.Unarchive) {
                                        events.onArchive(false)
                                    }
                                } else {
                                    MenuOption(stringResource(R.string.finance_archive), PrumoIcons.Archive) {
                                        events.onArchive(true)
                                    }
                                },
                            ),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.finance_loading))
                state.error != null -> ErrorState(state.error, onRetry = onRetry)
                else -> AccountForm(state, name, balance, canSave, events)
            }
        }
    }
}

@Composable
private fun AccountForm(
    state: AccountEditorUiState,
    name: String,
    balance: String,
    canSave: Boolean,
    events: AccountEditorEvents,
) {
    val error = state.saveError
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = events.onNameChange,
            label = { Text(stringResource(R.string.finance_field_name)) },
            placeholder = { Text(stringResource(R.string.finance_account_name_hint)) },
            isError = fieldError(error, "name") != null,
            supportingText = fieldError(error, "name")?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.finance_kind), style = MaterialTheme.typography.labelLarge)
        // Board 06: the kinds wrap onto a second line instead of scrolling out of sight.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AccountKind.entries.forEach { kind ->
                FilterChip(
                    selected = kind == state.kind,
                    onClick = { events.onKindChange(kind) },
                    label = { Text(kind.shortLabel()) },
                )
            }
        }
        ValueField(
            digits = balance,
            onDigitsChange = events.onBalanceChange,
            label = stringResource(
                if (state.editing ==
                    null
                ) {
                    R.string.finance_current_balance
                } else {
                    R.string.finance_initial_balance
                },
            ),
            transformation = MoneyTransformation(state.context.currency),
            large = true,
            supportingText = stringResource(
                if (state.editing ==
                    null
                ) {
                    R.string.finance_current_balance_hint
                } else {
                    R.string.finance_initial_balance_hint
                },
            ),
            error = fieldError(error, "initial_balance_cents"),
        )
        FilterChip(
            selected = state.negative,
            onClick = { events.onNegativeChange(!state.negative) },
            label = { Text(stringResource(R.string.finance_negative)) },
        )
        ColorField(state.color, events.onColorChange)
        Text(stringResource(R.string.finance_icon), style = MaterialTheme.typography.labelLarge)
        val icons = if (state.icon in AccountIcons) AccountIcons else AccountIcons + state.icon
        IconPicker(
            options = icons.map { IconOption(ServerIcons.of(it), iconLabel(it)) },
            selected = icons.indexOf(state.icon),
            onSelect = { events.onIconChange(icons[it]) },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.finance_include_in_total),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = state.includeInTotal, onCheckedChange = events.onIncludeChange)
        }
        if (error != null &&
            !error.isFieldError()
        ) {
            Text(financeErrorText(error), color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = events.onSave, enabled = canSave && !state.saving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.finance_account_save))
        }
    }
}

/** The color of an account, card or category (C10, D16): the web's seven and the current one. */
@Composable
internal fun ColorField(color: FinanceColor, onChange: (FinanceColor) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(R.string.finance_color), style = MaterialTheme.typography.labelLarge)
        val colors = if (color in FinanceColor.Picker) FinanceColor.Picker else FinanceColor.Picker + color
        ColorPicker(
            options = colors.map { ColorOption(it.seed, it.label()) },
            selected = colors.indexOf(color),
            onSelect = { onChange(colors[it]) },
        )
    }
}

@PhonePreviews
@Composable
private fun AccountEditorPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            AccountEditorScreen(
                state = AccountEditorUiState(kind = AccountKind.Cash, color = FinanceColor.Green, icon = "wallet"),
                name = "Carteira",
                balance = "25000",
                canSave = true,
                events = AccountEditorEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                onRetry = {},
                onClose = {},
            )
        }
    }
}
