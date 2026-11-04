package com.henriquesebastiao.prumo.feature.finance.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// The destinations of Finanças. Category and invoice payment are sheets over the current screen.

/** The tabs of Finanças (board 01): Resumo, Transações, Contas, Cartões. */
@Serializable
enum class FinanceTab { Overview, Transactions, Accounts, Cards }

/**
 * Where Finanças opens: the [tab], and for Transações a filter ("Ver todas" of an account, a card or
 * a category).
 */
@Serializable
data class FinanceHome(
    val tab: FinanceTab = FinanceTab.Overview,
    val accountId: String? = null,
    val cardId: String? = null,
    val categoryId: String? = null,
) : NavKey

@Serializable
data object FinanceCategories : NavKey

@Serializable
data class AccountDetail(val accountId: String) : NavKey

/** A new account ([accountId] null) or the edit of one. */
@Serializable
data class AccountEditor(val accountId: String? = null) : NavKey

@Serializable
data object ArchivedAccounts : NavKey

@Serializable
data class CardDetail(val cardId: String) : NavKey

@Serializable
data class CardEditor(val cardId: String? = null) : NavKey

@Serializable
data object ArchivedCards : NavKey

/** What a new transaction is; an edit keeps its kind (F16). */
@Serializable
enum class EntryKind { Expense, Income, Transfer }

/** A new transaction of [kind], or the edit of [transactionId]. */
@Serializable
data class TransactionEditor(val kind: EntryKind = EntryKind.Expense, val transactionId: String? = null) : NavKey

@Serializable
data class TransactionDetail(val transactionId: String) : NavKey
