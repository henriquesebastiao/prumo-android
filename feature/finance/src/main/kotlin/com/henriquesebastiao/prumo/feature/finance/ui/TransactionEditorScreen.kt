package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henriquesebastiao.prumo.core.designsystem.component.ChoiceGroup
import com.henriquesebastiao.prumo.core.designsystem.component.GroupNote
import com.henriquesebastiao.prumo.core.designsystem.component.IconAvatar
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoTopBar
import com.henriquesebastiao.prumo.core.designsystem.component.SeededAvatar
import com.henriquesebastiao.prumo.core.designsystem.component.ServerIcons
import com.henriquesebastiao.prumo.core.designsystem.component.ValueField
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.ui.DateField
import com.henriquesebastiao.prumo.core.ui.ErrorState
import com.henriquesebastiao.prumo.core.ui.LoadingState
import com.henriquesebastiao.prumo.core.ui.MoneyTransformation
import com.henriquesebastiao.prumo.core.ui.TimeField
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.R
import com.henriquesebastiao.prumo.feature.finance.model.Account
import com.henriquesebastiao.prumo.feature.finance.model.TransactionKind
import java.time.LocalDate
import java.time.LocalTime

/** What the transaction form does when something changes. */
internal class TransactionEditorEvents(
    val onKindChange: (EntryKind) -> Unit,
    val onFieldsChange: (TransactionFields) -> Unit,
    val onSourceChange: (Source) -> Unit,
    val onFromChange: (String) -> Unit,
    val onToChange: (String) -> Unit,
    val onCategoryChange: (String) -> Unit,
    val onNewCategory: () -> Unit,
    val onDateChange: (LocalDate) -> Unit,
    val onTimeChange: (LocalTime?) -> Unit,
)

@Composable
internal fun TransactionEditorRoute(
    key: TransactionEditor,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: TransactionEditorViewModel =
        hiltViewModel<TransactionEditorViewModel, TransactionEditorViewModel.Factory>(
            creationCallback = { it.create(key) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<FinanceSheet?>(null) }
    LaunchedEffect(state.done) { if (state.done) navigator.back() }
    TransactionEditorScreen(
        state = state,
        fields = viewModel.fields,
        canSave = viewModel.canSave,
        events = TransactionEditorEvents(
            onKindChange = viewModel::onKindChange,
            onFieldsChange = viewModel::onFieldsChange,
            onSourceChange = viewModel::onSourceChange,
            onFromChange = viewModel::onFromChange,
            onToChange = viewModel::onToChange,
            onCategoryChange = viewModel::onCategoryChange,
            onNewCategory = { sheet = FinanceSheet.Category(expense = state.kind != EntryKind.Income) },
            onDateChange = viewModel::onDateChange,
            onTimeChange = viewModel::onTimeChange,
        ),
        onInstallmentsChange = viewModel::onInstallmentsChange,
        onSave = viewModel::save,
        onRetry = viewModel::load,
        onClose = navigator::back,
        modifier = modifier,
    )
    FinanceSheetHost(sheet, onDismiss = { sheet = null }, onCategorySaved = viewModel::onCategoryCreated)
}

/** A new expense, income or transfer (boards 08 and 09), or the edit of a transaction. */
@Composable
internal fun TransactionEditorScreen(
    state: TransactionEditorUiState,
    fields: TransactionFields,
    canSave: Boolean,
    events: TransactionEditorEvents,
    onInstallmentsChange: (Int) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier,
        topBar = {
            PrumoTopBar(
                stringResource(
                    if (state.editing ==
                        null
                    ) {
                        R.string.finance_transaction_new
                    } else {
                        R.string.finance_transaction_edit
                    },
                ),
                onBack = onClose,
                close = true,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingState(stringResource(R.string.finance_loading))

                state.error != null -> ErrorState(state.error, onRetry = onRetry)

                state.accounts.isEmpty() && state.editing == null -> GroupNote(
                    stringResource(R.string.finance_no_accounts_for_record),
                    Modifier.padding(Spacing.screen),
                )

                else -> TransactionForm(state, fields, canSave, events, onInstallmentsChange, onSave)
            }
        }
    }
}

@Composable
private fun TransactionForm(
    state: TransactionEditorUiState,
    fields: TransactionFields,
    canSave: Boolean,
    events: TransactionEditorEvents,
    onInstallmentsChange: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val context = state.context
    val editingKind = state.editing?.kind
    val plain = editingKind == TransactionKind.CardPayment || editingKind == TransactionKind.Adjustment
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (state.editing == null) {
            ChoiceGroup(
                options = EntryKind.entries,
                selected = state.kind,
                label = { it.label() },
                onSelect = events.onKindChange,
            )
        }
        ValueField(
            digits = fields.amount,
            onDigitsChange = { events.onFieldsChange(fields.copy(amount = it)) },
            label = stringResource(R.string.finance_amount),
            transformation = MoneyTransformation(context.currency),
            large = true,
            error = fieldError(state.saveError, "amount_cents"),
        )
        when {
            plain -> Unit
            state.kind == EntryKind.Transfer -> TransferAccounts(state, events)
            else -> SourceAndCategory(state, events, onInstallmentsChange)
        }
        OutlinedTextField(
            value = fields.description,
            onValueChange = { events.onFieldsChange(fields.copy(description = it)) },
            label = {
                Text(
                    stringResource(
                        if (state.kind == EntryKind.Transfer ||
                            plain
                        ) {
                            R.string.finance_description_optional
                        } else {
                            R.string.finance_field_description
                        },
                    ),
                )
            },
            placeholder = { Text(stringResource(R.string.finance_description_hint)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            isError = fieldError(state.saveError, "description") != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            DateField(state.date, events.onDateChange, stringResource(R.string.finance_date), Modifier.weight(1f))
            TimeField(
                state.time,
                events.onTimeChange,
                onClear = { events.onTimeChange(null) },
                label = stringResource(R.string.finance_time),
                modifier = Modifier.width(140.dp),
            )
        }
        OutlinedTextField(
            value = fields.notes,
            onValueChange = { events.onFieldsChange(fields.copy(notes = it)) },
            label = { Text(stringResource(R.string.finance_notes)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.kind == EntryKind.Transfer && !plain) GroupNote(stringResource(R.string.finance_transfer_hint))
        state.saveError?.takeIf {
            !it.isFieldError()
        }?.let { Text(financeErrorText(it), color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSave, enabled = canSave && !state.saving, modifier = Modifier.fillMaxWidth()) {
            Text(saveLabel(state, fields))
        }
    }
}

@Composable
private fun saveLabel(state: TransactionEditorUiState, fields: TransactionFields): String = when {
    state.editing != null -> stringResource(R.string.finance_save)

    state.kind == EntryKind.Income -> stringResource(R.string.finance_save_income)

    state.kind == EntryKind.Transfer -> Inputs.number(fields.amount)?.takeIf { it > 0 }
        ?.let { stringResource(R.string.finance_transfer_amount, state.context.money(it)) }
        ?: stringResource(R.string.finance_save_transfer)

    else -> stringResource(R.string.finance_save_expense)
}

@Composable
internal fun EntryKind.label(): String = stringResource(
    when (this) {
        EntryKind.Expense -> R.string.finance_kind_expense
        EntryKind.Income -> R.string.finance_kind_income
        EntryKind.Transfer -> R.string.finance_kind_transfer
    },
)

/** "Conta ou cartão", the installments of a card purchase and the category tiles (board 08). */
@Composable
private fun ColumnScope.SourceAndCategory(
    state: TransactionEditorUiState,
    events: TransactionEditorEvents,
    onInstallmentsChange: (Int) -> Unit,
) {
    Text(
        stringResource(if (state.kind == EntryKind.Expense) R.string.finance_source else R.string.finance_account),
        style = MaterialTheme.typography.labelLarge,
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (state.kind == EntryKind.Expense) {
            state.cards.forEach { card ->
                FilterChip(
                    selected = state.source == Source.OfCard(card.id),
                    onClick = { events.onSourceChange(Source.OfCard(card.id)) },
                    label = { Text(stringResource(R.string.finance_card_named, card.name)) },
                    leadingIcon = { Icon(painterResource(PrumoIcons.CreditCard), contentDescription = null) },
                )
            }
        }
        state.accounts.forEach { account ->
            FilterChip(
                selected = state.source == Source.OfAccount(account.id),
                onClick = { events.onSourceChange(Source.OfAccount(account.id)) },
                label = { Text(account.name) },
            )
        }
    }
    if (state.onCard && state.editing == null) {
        MenuChip(
            label = if (state.installments > 1) {
                stringResource(R.string.finance_installments_times, state.installments)
            } else {
                stringResource(R.string.finance_installments_single)
            },
            selected = state.installments > 1,
            choices = (1..MAX_INSTALLMENTS).map { count ->
                Choice(
                    if (count ==
                        1
                    ) {
                        stringResource(R.string.finance_installments_single)
                    } else {
                        stringResource(R.string.finance_installments_times, count)
                    },
                ) { onInstallmentsChange(count) }
            },
        )
        GroupNote(stringResource(R.string.finance_installments_hint))
    }
    Text(stringResource(R.string.finance_category), style = MaterialTheme.typography.labelLarge)
    val categories = state.categories.filter { it.kind == state.categoryKind }
    if (categories.isEmpty()) GroupNote(stringResource(R.string.finance_no_categories))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        categories.forEach { category ->
            val selected = category.id == state.categoryId
            CategoryTile(category.name, selected, onClick = { events.onCategoryChange(category.id) }) {
                SeededAvatar(
                    category.color.seed,
                    ServerIcons.of(category.icon),
                    size = 56.dp,
                    cookie = selected,
                )
            }
        }
        CategoryTile(
            stringResource(R.string.finance_new_category_short),
            selected = false,
            onClick = events.onNewCategory,
        ) {
            IconAvatar(
                PrumoIcons.Add,
                size = 56.dp,
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A category to pick: its avatar (cookie-shaped when chosen) and its name. */
@Composable
private fun CategoryTile(label: String, selected: Boolean, onClick: () -> Unit, avatar: @Composable () -> Unit) {
    Column(
        Modifier
            .width(76.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        avatar()
        Text(
            label,
            style = if (selected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** De and Para with each account's balance, and the button that swaps them (board 09). */
@Composable
private fun ColumnScope.TransferAccounts(state: TransactionEditorUiState, events: TransactionEditorEvents) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AccountDropdown(
                stringResource(R.string.finance_from),
                state.accounts,
                state.fromId,
                events.onFromChange,
                state.context,
            )
            AccountDropdown(
                stringResource(R.string.finance_to),
                state.accounts,
                state.toId,
                events.onToChange,
                state.context,
            )
        }
        FilledTonalIconButton(
            onClick = {
                val from = state.fromId
                val to = state.toId
                if (to != null) events.onFromChange(to)
                if (from != null) events.onToChange(from)
            },
        ) {
            Icon(painterResource(PrumoIcons.SwapVert), contentDescription = stringResource(R.string.finance_swap))
        }
    }
    if (state.fromId != null && state.fromId == state.toId) {
        Text(stringResource(R.string.finance_error_same_account), color = MaterialTheme.colorScheme.error)
    }
}

/** An account to choose, shown with its balance: "Inter · R$ 2.156,90". */
@Composable
private fun AccountDropdown(
    label: String,
    accounts: List<Account>,
    selected: String?,
    onSelect: (String) -> Unit,
    context: FinanceContext,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val account = accounts.firstOrNull { it.id == selected }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = account?.let { it.name + SEPARATOR + context.money(it.balanceCents) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = account?.let {
                { Icon(painterResource(ServerIcons.of(it.icon)), contentDescription = null) }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name + SEPARATOR + context.money(option.balanceCents)) },
                    onClick = {
                        onSelect(option.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

private const val MAX_INSTALLMENTS = 24

@PhonePreviews
@Composable
private fun ExpensePreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            TransactionEditorScreen(
                state = TransactionEditorUiState(
                    loading = false,
                    accounts = FinancePreview.accounts,
                    cards = listOf(FinancePreview.card),
                    categories = FinancePreview.expenseCategories,
                    source = Source.OfCard("k1"),
                    categoryId = "c1",
                    installments = 3,
                ),
                fields = TransactionFields("18746", "Mercado Bom Preço"),
                canSave = true,
                events = TransactionEditorEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                onInstallmentsChange = {},
                onSave = {},
                onRetry = {},
                onClose = {},
            )
        }
    }
}

@PhonePreviews
@Composable
private fun TransferPreview() {
    PrumoTheme(dynamicColor = false) {
        ModuleTheme(FinanceFeature.SEED) {
            TransactionEditorScreen(
                state = TransactionEditorUiState(
                    loading = false,
                    kind = EntryKind.Transfer,
                    accounts = FinancePreview.accounts,
                    fromId = "a2",
                    toId = "a1",
                ),
                fields = TransactionFields("50000"),
                canSave = true,
                events = TransactionEditorEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                onInstallmentsChange = {},
                onSave = {},
                onRetry = {},
                onClose = {},
            )
        }
    }
}
