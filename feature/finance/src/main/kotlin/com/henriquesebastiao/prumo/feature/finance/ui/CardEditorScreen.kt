package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.designsystem.component.GroupNote
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.getOrNull
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.core.ui.MoneyTransformation
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
import com.henriquesebastiao.prumo.feature.finance.model.Account
import com.henriquesebastiao.prumo.feature.finance.model.CardInput
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

data class CardEditorUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val editing: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val paymentAccountId: String? = null,
    val color: FinanceColor = FinanceColor.Violet,
    val saving: Boolean = false,
    val saveError: AppError? = null,
    val done: Boolean = false,
    val context: FinanceContext = FinanceContext(),
)

/** The texts typed in the card form. */
data class CardFields(
    val name: String = "",
    val limit: String = "",
    val closingDay: String = "",
    val dueDay: String = "",
)

@HiltViewModel(assistedFactory = CardEditorViewModel.Factory::class)
class CardEditorViewModel @AssistedInject constructor(
    @Assisted private val cardId: String?,
    private val repository: FinanceRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    var fields by mutableStateOf(CardFields())
        private set

    private val mutableState =
        MutableStateFlow(CardEditorUiState(context = account.preferences.value.financeContext(clock)))
    val state: StateFlow<CardEditorUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val accounts = repository.accounts().getOrNull()?.items.orEmpty()
            if (cardId == null) {
                mutableState.update { it.copy(loading = false, accounts = accounts) }
                return@launch
            }
            when (val outcome = repository.card(cardId)) {
                is Outcome.Ok -> {
                    val card = outcome.value
                    fields =
                        CardFields(card.name, Inputs.digitsOf(card.limitCents), "${card.closingDay}", "${card.dueDay}")
                    mutableState.update {
                        it.copy(
                            loading = false,
                            editing = true,
                            accounts = accounts,
                            paymentAccountId = card.paymentAccount?.id,
                            color = card.color,
                        )
                    }
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun onFieldsChange(new: CardFields) {
        fields = new
        mutableState.update { it.copy(saveError = null) }
    }

    fun onPaymentAccountChange(id: String?) = mutableState.update { it.copy(paymentAccountId = id) }

    fun onColorChange(color: FinanceColor) = mutableState.update { it.copy(color = color) }

    private fun input(): CardInput? {
        val limit = Inputs.number(fields.limit) ?: return null
        val closing = fields.closingDay.toIntOrNull()?.takeIf { it in DAYS } ?: return null
        val due = fields.dueDay.toIntOrNull()?.takeIf { it in DAYS } ?: return null
        if (fields.name.isBlank()) return null
        val current = mutableState.value
        return CardInput(fields.name.trim(), limit, closing, due, current.paymentAccountId, current.color)
    }

    val canSave: Boolean get() = input() != null

    fun save() {
        val input = input() ?: return
        if (mutableState.value.saving) return
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val outcome = if (cardId == null) repository.createCard(input) else repository.updateCard(cardId, input)
            when (outcome) {
                is Outcome.Ok -> mutableState.update { it.copy(saving = false, done = true) }
                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = outcome.error) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(cardId: String?): CardEditorViewModel
    }

    private companion object {
        val DAYS = 1..31
    }
}

@Composable
internal fun CardEditorRoute(
    cardId: String?,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CardEditorViewModel = hiltViewModel<CardEditorViewModel, CardEditorViewModel.Factory>(
        creationCallback = { it.create(cardId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) navigator.back() }
    CardEditorScreen(
        state = state,
        fields = viewModel.fields,
        canSave = viewModel.canSave,
        onFieldsChange = viewModel::onFieldsChange,
        onPaymentAccountChange = viewModel::onPaymentAccountChange,
        onColorChange = viewModel::onColorChange,
        onSave = viewModel::save,
        onRetry = viewModel::load,
        onClose = navigator::back,
        modifier = modifier,
    )
}

/** A new card or its edit (web parity): name, limit, closing and due days, payment account, color. */
@Composable
internal fun CardEditorScreen(
    state: CardEditorUiState,
    fields: CardFields,
    canSave: Boolean,
    onFieldsChange: (CardFields) -> Unit,
    onPaymentAccountChange: (String?) -> Unit,
    onColorChange: (FinanceColor) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(
                stringResource(if (state.editing) R.string.finance_card_edit else R.string.finance_card_new),
                onBack = onClose,
                close = true,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.finance_loading))

                state.error != null -> ErrorState(state.error, onRetry = onRetry)

                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.screen, vertical = Spacing.lg)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    val error = state.saveError
                    OutlinedTextField(
                        value = fields.name,
                        onValueChange = { onFieldsChange(fields.copy(name = it)) },
                        label = { Text(stringResource(R.string.finance_field_name)) },
                        placeholder = { Text(stringResource(R.string.finance_card_name_hint)) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        isError = fieldError(error, "name") != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ValueField(
                        digits = fields.limit,
                        onDigitsChange = { onFieldsChange(fields.copy(limit = it)) },
                        label = stringResource(R.string.finance_limit),
                        transformation = MoneyTransformation(state.context.currency),
                        error = fieldError(error, "limit_cents"),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        ValueField(
                            digits = fields.closingDay,
                            onDigitsChange = { onFieldsChange(fields.copy(closingDay = it)) },
                            label = stringResource(R.string.finance_closing_day),
                            transformation = VisualTransformation.None,
                            maxDigits = 2,
                            error = fieldError(error, "closing_day"),
                            modifier = Modifier.weight(1f),
                        )
                        ValueField(
                            digits = fields.dueDay,
                            onDigitsChange = { onFieldsChange(fields.copy(dueDay = it)) },
                            label = stringResource(R.string.finance_due_day),
                            transformation = VisualTransformation.None,
                            maxDigits = 2,
                            error = fieldError(error, "due_day"),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    GroupNote(stringResource(R.string.finance_closing_hint))
                    Text(stringResource(R.string.finance_payment_account), style = MaterialTheme.typography.labelLarge)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        FilterChip(
                            selected = state.paymentAccountId == null,
                            onClick = { onPaymentAccountChange(null) },
                            label = { Text(stringResource(R.string.finance_none)) },
                        )
                        state.accounts.forEach { account ->
                            FilterChip(
                                selected = account.id == state.paymentAccountId,
                                onClick = { onPaymentAccountChange(account.id) },
                                label = { Text(account.name) },
                            )
                        }
                    }
                    ColorField(state.color, onColorChange)
                    if (error != null &&
                        !error.isFieldError()
                    ) {
                        Text(financeErrorText(error), color = MaterialTheme.colorScheme.error)
                    }
                    Button(onClick = onSave, enabled = canSave && !state.saving, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (state.editing) R.string.finance_save else R.string.finance_card_create))
                    }
                }
            }
        }
    }
}

@PhonePreviews
@Composable
private fun CardEditorPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            CardEditorScreen(
                state = CardEditorUiState(loading = false, accounts = FinancePreview.accounts, paymentAccountId = "a1"),
                fields = CardFields("Nubank", "500000", "3", "10"),
                canSave = true,
                onFieldsChange = {},
                onPaymentAccountChange = {},
                onColorChange = {},
                onSave = {},
                onRetry = {},
                onClose = {},
            )
        }
    }
}
