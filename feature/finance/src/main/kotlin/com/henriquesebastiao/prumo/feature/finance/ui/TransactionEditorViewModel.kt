package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Inputs
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.getOrNull
import com.henriquesebastiao.prumo.feature.finance.data.FinanceRepository
import com.henriquesebastiao.prumo.feature.finance.model.Account
import com.henriquesebastiao.prumo.feature.finance.model.Category
import com.henriquesebastiao.prumo.feature.finance.model.CategoryKind
import com.henriquesebastiao.prumo.feature.finance.model.CreditCard
import com.henriquesebastiao.prumo.feature.finance.model.Transaction
import com.henriquesebastiao.prumo.feature.finance.model.TransactionInput
import com.henriquesebastiao.prumo.feature.finance.model.TransactionKind
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where an income or expense goes: an account or, for an expense, a card. */
sealed interface Source {
    val id: String

    data class OfAccount(override val id: String) : Source

    data class OfCard(override val id: String) : Source
}

data class TransactionEditorUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val kind: EntryKind = EntryKind.Expense,
    /** The transaction being edited; null for a new one. */
    val editing: Transaction? = null,
    val accounts: List<Account> = emptyList(),
    val cards: List<CreditCard> = emptyList(),
    val categories: List<Category> = emptyList(),
    val source: Source? = null,
    val fromId: String? = null,
    val toId: String? = null,
    val categoryId: String? = null,
    val date: LocalDate = LocalDate.of(2026, 11, 17),
    val time: LocalTime? = null,
    /** F10: 1 is a single payment; only for a new card expense. */
    val installments: Int = 1,
    val saving: Boolean = false,
    val saveError: AppError? = null,
    val done: Boolean = false,
    val context: FinanceContext = FinanceContext(),
) {
    val categoryKind: CategoryKind get() = if (kind == EntryKind.Income) CategoryKind.Income else CategoryKind.Expense
    val onCard: Boolean get() = source is Source.OfCard
}

/** The texts typed in the transaction form. */
data class TransactionFields(val amount: String = "", val description: String = "", val notes: String = "")

@Suppress("TooManyFunctions")
@HiltViewModel(assistedFactory = TransactionEditorViewModel.Factory::class)
class TransactionEditorViewModel @AssistedInject constructor(
    @Assisted private val key: TransactionEditor,
    private val repository: FinanceRepository,
    account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    var fields by mutableStateOf(TransactionFields())
        private set

    private val context = account.preferences.value.financeContext(clock)
    private val mutableState = MutableStateFlow(
        TransactionEditorUiState(kind = key.kind, date = context.today, context = context),
    )
    val state: StateFlow<TransactionEditorUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val accounts = repository.accounts()
            val editing = key.transactionId?.let { repository.transaction(it) }
            val error = (accounts as? Outcome.Failed)?.error ?: (editing as? Outcome.Failed)?.error
            if (error != null) {
                mutableState.update { it.copy(loading = false, error = error) }
                return@launch
            }
            val cards = repository.cards().getOrNull().orEmpty()
            val categories = repository.categories().getOrNull().orEmpty()
            mutableState.update {
                it.copy(
                    loading = false,
                    accounts = accounts.getOrNull()?.items.orEmpty(),
                    cards = cards,
                    categories = categories,
                )
            }
            val transaction = editing?.getOrNull()
            if (transaction != null) fill(transaction) else defaults()
        }
    }

    /** A9 (D16): the account used last in this run, else the first; the transfer goes to another one. */
    private fun defaults() {
        val accounts = mutableState.value.accounts
        val first = accounts.firstOrNull { it.id == repository.lastAccountId } ?: accounts.firstOrNull()
        mutableState.update {
            it.copy(
                source = first?.let { account -> Source.OfAccount(account.id) },
                fromId = first?.id,
                toId = accounts.firstOrNull { account -> account.id != first?.id }?.id,
            )
        }
    }

    private fun fill(transaction: Transaction) {
        fields =
            TransactionFields(
                Inputs.digitsOf(kotlin.math.abs(transaction.amountCents)),
                transaction.description,
                transaction.notes.orEmpty(),
            )
        val kind = when (transaction.kind) {
            TransactionKind.Income -> EntryKind.Income
            TransactionKind.Transfer -> EntryKind.Transfer
            else -> EntryKind.Expense
        }
        mutableState.update {
            it.copy(
                kind = kind,
                editing = transaction,
                source = transaction.creditCard?.let { card -> Source.OfCard(card.id) }
                    ?: transaction.account?.let { account -> Source.OfAccount(account.id) },
                fromId = transaction.account?.id,
                toId = transaction.destinationAccount?.id,
                categoryId = transaction.category?.id,
                date = transaction.occurredOn,
                time = transaction.occurredTime,
            )
        }
    }

    /** Another kind (only on a new one): a category of the other kind is dropped; income never goes to a card. */
    fun onKindChange(kind: EntryKind) {
        if (mutableState.value.editing != null) return
        mutableState.update { state ->
            val categoryKind = if (kind == EntryKind.Income) CategoryKind.Income else CategoryKind.Expense
            val category = state.categories.firstOrNull {
                it.id == state.categoryId
            }?.takeIf { it.kind == categoryKind }
            val source = state.source.takeIf { kind == EntryKind.Expense || it !is Source.OfCard }
                ?: state.accounts.firstOrNull()?.let { Source.OfAccount(it.id) }
            state.copy(kind = kind, categoryId = category?.id, source = source, installments = 1, saveError = null)
        }
    }

    fun onFieldsChange(new: TransactionFields) {
        fields = new
        mutableState.update { it.copy(saveError = null) }
    }

    fun onSourceChange(source: Source) = mutableState.update {
        it.copy(source = source, installments = if (source is Source.OfCard) it.installments else 1, saveError = null)
    }

    fun onFromChange(id: String) = mutableState.update { it.copy(fromId = id, saveError = null) }

    fun onToChange(id: String) = mutableState.update { it.copy(toId = id, saveError = null) }

    fun onCategoryChange(id: String) = mutableState.update { it.copy(categoryId = id, saveError = null) }

    fun onDateChange(date: LocalDate) = mutableState.update { it.copy(date = date) }

    fun onTimeChange(time: LocalTime?) = mutableState.update { it.copy(time = time) }

    fun onInstallmentsChange(count: Int) = mutableState.update { it.copy(installments = count) }

    /** A category created from the form: loaded again and chosen. */
    fun onCategoryCreated(id: String) {
        viewModelScope.launch {
            val categories = repository.categories().getOrNull() ?: return@launch
            mutableState.update { it.copy(categories = categories, categoryId = id) }
        }
    }

    /** What the form sends, or null while something the server requires is missing. */
    private fun input(): TransactionInput? {
        val current = mutableState.value
        val amount = Inputs.number(fields.amount)?.takeIf { it > 0 } ?: return null
        val base = TransactionInput(
            kind = TransactionKind.Expense,
            amountCents = amount,
            occurredOn = current.date,
            occurredTime = current.time,
            description = fields.description.trim(),
            notes = fields.notes.trim().ifEmpty { null },
            accountId = null,
            destinationAccountId = null,
            creditCardId = null,
            categoryId = null,
        )
        val editing = current.editing
        return when {
            // A payment or an adjustment keeps its references and sign; only its value, day and text change.
            editing != null && editing.kind in PLAIN -> base.copy(
                kind = editing.kind,
                amountCents = if (editing.amountCents < 0) -amount else amount,
                accountId = editing.account?.id,
                destinationAccountId = editing.destinationAccount?.id,
                creditCardId = editing.creditCard?.id,
                categoryId = editing.category?.id,
            )

            current.kind == EntryKind.Transfer -> transfer(current, base)

            else -> entry(current, base)
        }
    }

    private fun transfer(current: TransactionEditorUiState, base: TransactionInput): TransactionInput? {
        val from = current.fromId ?: return null
        val to = current.toId?.takeIf { it != from } ?: return null
        return base.copy(kind = TransactionKind.Transfer, accountId = from, destinationAccountId = to)
    }

    /** F16: income and expenses need a description and a category. */
    private fun entry(current: TransactionEditorUiState, base: TransactionInput): TransactionInput? {
        val category = current.categoryId
        val source = current.source
        if (base.description.isEmpty() || category == null || source == null) return null
        return base.copy(
            kind = if (current.kind == EntryKind.Income) TransactionKind.Income else TransactionKind.Expense,
            accountId = (source as? Source.OfAccount)?.id,
            creditCardId = (source as? Source.OfCard)?.id,
            categoryId = category,
            installments = current.installments.takeIf { source is Source.OfCard && current.editing == null && it > 1 },
        )
    }

    val canSave: Boolean get() = input() != null

    fun save() {
        val input = input() ?: return
        val current = mutableState.value
        if (current.saving) return
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val editing = current.editing
            val outcome = if (editing ==
                null
            ) {
                repository.createTransaction(input)
            } else {
                repository.updateTransaction(editing.id, input)
            }
            when (outcome) {
                is Outcome.Ok -> mutableState.update { it.copy(saving = false, done = true) }
                is Outcome.Failed -> mutableState.update { it.copy(saving = false, saveError = outcome.error) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(key: TransactionEditor): TransactionEditorViewModel
    }

    private companion object {
        val PLAIN = setOf(TransactionKind.CardPayment, TransactionKind.Adjustment)
    }
}
