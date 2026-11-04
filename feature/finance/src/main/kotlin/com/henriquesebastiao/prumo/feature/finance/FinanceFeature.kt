package com.henriquesebastiao.prumo.feature.finance

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.henriquesebastiao.prumo.core.designsystem.color.Seeds
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.core.feature.QuickAction
import com.henriquesebastiao.prumo.core.network.decodeOrNull
import com.henriquesebastiao.prumo.feature.finance.model.FINANCE_MODULE_ID
import com.henriquesebastiao.prumo.feature.finance.ui.AccountDetail
import com.henriquesebastiao.prumo.feature.finance.ui.AccountDetailRoute
import com.henriquesebastiao.prumo.feature.finance.ui.AccountEditor
import com.henriquesebastiao.prumo.feature.finance.ui.AccountEditorRoute
import com.henriquesebastiao.prumo.feature.finance.ui.ArchivedAccounts
import com.henriquesebastiao.prumo.feature.finance.ui.ArchivedRoute
import com.henriquesebastiao.prumo.feature.finance.ui.CardDetail
import com.henriquesebastiao.prumo.feature.finance.ui.CardDetailRoute
import com.henriquesebastiao.prumo.feature.finance.ui.CardEditor
import com.henriquesebastiao.prumo.feature.finance.ui.CardEditorRoute
import com.henriquesebastiao.prumo.feature.finance.ui.CategoriesRoute
import com.henriquesebastiao.prumo.feature.finance.ui.EntryKind
import com.henriquesebastiao.prumo.feature.finance.ui.FinanceCategories
import com.henriquesebastiao.prumo.feature.finance.ui.FinanceHome
import com.henriquesebastiao.prumo.feature.finance.ui.FinanceHomeRoute
import com.henriquesebastiao.prumo.feature.finance.ui.FinanceSheet
import com.henriquesebastiao.prumo.feature.finance.ui.FinanceSheetHost
import com.henriquesebastiao.prumo.feature.finance.ui.FinanceTab
import com.henriquesebastiao.prumo.feature.finance.ui.TransactionDetail
import com.henriquesebastiao.prumo.feature.finance.ui.TransactionDetailRoute
import com.henriquesebastiao.prumo.feature.finance.ui.TransactionEditor
import com.henriquesebastiao.prumo.feature.finance.ui.TransactionEditorRoute
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject

/**
 * Finanças (`finance`): accounts, cards and invoices, transactions, categories and the month's
 * overview, on the server's `/api/v1/finance` (docs/api.md of the server, "Finanças").
 */
class FinanceFeature @Inject constructor() : PrumoFeature {
    override val id = ID
    override val nameRes = R.string.finance_name
    override val descriptionRes = R.string.finance_description
    override val icon = PrumoIcons.AccountBalanceWallet
    override val seed = SEED
    override val entryKey: NavKey = FinanceHome()

    override val quickActions = listOf(
        QuickAction(
            "finance.expense",
            R.string.finance_action_expense,
            R.string.finance_action_expense_short,
            PrumoIcons.Payments,
            TransactionEditor(EntryKind.Expense),
        ),
        QuickAction(
            "finance.income",
            R.string.finance_action_income,
            R.string.finance_action_income_short,
            PrumoIcons.Savings,
            TransactionEditor(EntryKind.Income),
        ),
        QuickAction(
            "finance.transfer",
            R.string.finance_action_transfer,
            R.string.finance_action_transfer,
            PrumoIcons.SwapHoriz,
            TransactionEditor(EntryKind.Transfer),
        ),
        QuickAction(
            "finance.pay_invoice",
            R.string.finance_action_pay_invoice,
            R.string.finance_action_pay_invoice,
            PrumoIcons.CreditCard,
            FinanceHome(FinanceTab.Cards),
            sheet = { onDismiss -> FinanceSheetHost(FinanceSheet.PayInvoice(), onDismiss) },
        ),
    )

    override fun EntryProviderScope<NavKey>.entries(navigator: Navigator) {
        entry<FinanceHome> { key -> Module { FinanceHomeRoute(key, navigator) } }
        entry<FinanceCategories> { Module { CategoriesRoute(onBack = navigator::back) } }
        entry<AccountDetail> { key -> Module { AccountDetailRoute(key.accountId, navigator) } }
        entry<AccountEditor> { key -> Module { AccountEditorRoute(key.accountId, navigator) } }
        entry<ArchivedAccounts> {
            Module {
                ArchivedRoute(
                    onBack = navigator::back,
                    onAccount = { navigator.navigate(AccountDetail(it.id)) },
                    onCard = { navigator.navigate(CardDetail(it.id)) },
                )
            }
        }
        entry<CardDetail> { key -> Module { CardDetailRoute(key.cardId, navigator) } }
        entry<CardEditor> { key -> Module { CardEditorRoute(key.cardId, navigator) } }
        entry<TransactionEditor> { key -> Module { TransactionEditorRoute(key, navigator) } }
        entry<TransactionDetail> { key -> Module { TransactionDetailRoute(key.transactionId, navigator) } }
    }

    /** A destination of the module, in its colors. */
    @Composable
    private fun Module(content: @Composable () -> Unit) = ModuleTheme(SEED, content)

    @Composable
    override fun HomeCard(
        summary: JsonObject?,
        hero: Boolean,
        context: HomeContext,
        onOpen: () -> Unit,
        modifier: Modifier,
    ) {
        FinanceHomeCard(decodeOrNull(FinanceSummaryDto.serializer(), summary), hero, context.currency, onOpen, modifier)
    }

    companion object {
        const val ID = FINANCE_MODULE_ID
        val SEED = Seeds.FINANCE
    }
}
