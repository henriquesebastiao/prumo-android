package com.henriquesebastiao.prumo.feature.finance

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.henriquesebastiao.prumo.core.designsystem.color.Seeds
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.feature.ComingSoonScreen
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.core.feature.QuickAction
import com.henriquesebastiao.prumo.core.network.decodeOrNull
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Where Finanças opens. */
@Serializable
data object FinanceHome : NavKey

/** Finanças (`finance`). In the foundation: its Home card, its actions and the "em breve" screen. */
class FinanceFeature @Inject constructor() : PrumoFeature {
    override val id = ID
    override val nameRes = R.string.finance_name
    override val descriptionRes = R.string.finance_description
    override val icon = PrumoIcons.AccountBalanceWallet
    override val seed = SEED
    override val entryKey: NavKey = FinanceHome

    override val quickActions = listOf(
        QuickAction(
            "finance.expense",
            R.string.finance_action_expense,
            R.string.finance_action_expense_short,
            PrumoIcons.Payments,
            FinanceHome,
        ),
        QuickAction(
            "finance.income",
            R.string.finance_action_income,
            R.string.finance_action_income_short,
            PrumoIcons.Savings,
            FinanceHome,
        ),
        QuickAction(
            "finance.transfer",
            R.string.finance_action_transfer,
            R.string.finance_action_transfer,
            PrumoIcons.SwapHoriz,
            FinanceHome,
        ),
        QuickAction(
            "finance.pay_invoice",
            R.string.finance_action_pay_invoice,
            R.string.finance_action_pay_invoice,
            PrumoIcons.CreditCard,
            FinanceHome,
        ),
    )

    override fun EntryProviderScope<NavKey>.entries(navigator: Navigator) {
        entry<FinanceHome> {
            ComingSoonScreen(stringResource(nameRes), icon, seed, onBack = navigator::back)
        }
    }

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
        const val ID = "finance"
        val SEED = Seeds.FINANCE
    }
}
