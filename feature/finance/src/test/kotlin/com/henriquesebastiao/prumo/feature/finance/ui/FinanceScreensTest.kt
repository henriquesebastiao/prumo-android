package com.henriquesebastiao.prumo.feature.finance.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.finance.model.Transaction
import java.time.YearMonth
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The stateless screens of Finanças with the e2e server's data (FinancePreview). */
@Config(application = Application::class, qualifiers = "w412dp-h915dp")
@RunWith(AndroidJUnit4::class)
class FinanceScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(content: @Composable () -> Unit) = compose.setContent {
        PrumoTheme(dynamicColor = false) { ModuleTheme(FinanceFeature.SEED, content) }
    }

    private val nbsp = ' '

    @Test
    fun theOverviewShowsTheServersNumbersAndMovesTheMonth() {
        var previous = 0
        var category = ""
        show {
            OverviewTab(
                state = OverviewUiState(loading = false, overview = FinancePreview.overview),
                events = OverviewEvents({ previous++ }, {}, {}, {}, { category = it.category.name }, {}, {}),
            )
        }
        compose.onNodeWithText("Novembro 2026").assertIsDisplayed()
        compose.onNodeWithText("R$${nbsp}7.154,27").assertIsDisplayed()
        compose.onNodeWithText("Despesas = 48 % das receitas").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Mês anterior").performClick()
        assertEquals(1, previous)
        compose.onNodeWithText("54 %").performScrollTo().performClick()
        assertEquals("Moradia", category)
    }

    @Test
    fun withoutAccountsTheOverviewInvitesToAddOne() {
        var added = 0
        show {
            OverviewTab(
                state = OverviewUiState(loading = false, overview = FinancePreview.overview.copy(accountsCount = 0)),
                events = OverviewEvents({}, {}, {}, { added++ }, {}, {}, {}),
            )
        }
        compose.onNodeWithText("Comece pelas suas contas").assertIsDisplayed()
        compose.onNodeWithText("Adicionar conta").performClick()
        assertEquals(1, added)
    }

    @Test
    fun theTransactionsAreGroupedByDayWithTheFiltersTotals() {
        var opened = ""
        show {
            val items = remember {
                flowOf(
                    PagingData.from(
                        FinancePreview.transactions.map<Transaction, TransactionItem> {
                            TransactionItem.Row(it)
                        },
                    ),
                )
            }.collectAsLazyPagingItems()
            TransactionsTab(
                state = TransactionsUiState(
                    filters = TransactionFilters(month = YearMonth.of(2026, 11)),
                    totals = TransactionTotals(65, 3_130_000, 1_999_054),
                ),
                query = "",
                items = items,
                events = TransactionsEvents({}, {}, { opened = it.id }),
            )
        }
        compose.onNodeWithText("65 transações").assertIsDisplayed()
        compose.onNodeWithText("+R$${nbsp}31.300,00").assertIsDisplayed()
        compose.onNodeWithText("Padaria").assertIsDisplayed()
        compose.onNodeWithText("−R$${nbsp}18,50").assertIsDisplayed()
        compose.onNodeWithText("Site institucional").performScrollTo().performClick()
        assertEquals("t4", opened)
    }

    @Test
    fun theAccountsShowTheirBalancesAndTheTotal() {
        show {
            AccountsTab(
                state = AccountsUiState(
                    loading = false,
                    accounts = FinancePreview.accounts,
                    totalBalanceCents = 715_427,
                ),
                events = AccountsEvents({}, {}, {}, {}),
            )
        }
        compose.onNodeWithText("Nubank").assertIsDisplayed()
        compose.onNodeWithText("R$${nbsp}4.812,37").assertIsDisplayed()
        compose.onNodeWithText("R$${nbsp}7.154,27").assertIsDisplayed()
    }

    @Test
    fun aCardShowsItsInvoiceAndLimit() {
        show {
            CardsTab(
                state = AccountsUiState(loading = false, cards = listOf(FinancePreview.card)),
                events = CardsEvents({}, {}, {}, {}),
            )
        }
        compose.onNodeWithText("Fatura de dezembro").assertIsDisplayed()
        compose.onNodeWithText("Aberta").assertIsDisplayed()
        compose.onNodeWithText("R$${nbsp}3.715,40").assertIsDisplayed()
    }

    @Test
    fun aPartialPaymentPreviewsWhatIsLeft() {
        show {
            PayInvoiceContent(
                state = PayInvoiceUiState(
                    loading = false,
                    invoices = FinancePreview.overview.openInvoices,
                    total = false,
                    accounts = FinancePreview.accounts,
                    accountId = "a2",
                ),
                amount = "50000",
                paying = 50_000,
                events = PayInvoiceEvents({}, {}, {}, {}, {}, {}, {}),
            )
        }
        compose.onNodeWithText("Restam R$${nbsp}784,60 nesta fatura").assertIsDisplayed()
        compose.onNodeWithText("Pagar R$${nbsp}500,00").performScrollTo().assertIsEnabled()
    }

    @Test
    fun theExpenseFormShowsTheCategoriesAndTheInstallments() {
        show {
            TransactionEditorScreen(
                state = TransactionEditorUiState(
                    loading = false,
                    accounts = FinancePreview.accounts,
                    cards = listOf(FinancePreview.card),
                    categories = FinancePreview.expenseCategories,
                    source = Source.OfCard("k1"),
                    installments = 3,
                ),
                fields = TransactionFields("18746", "Mercado"),
                canSave = false,
                events = TransactionEditorEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                onInstallmentsChange = {},
                onSave = {},
                onRetry = {},
                onClose = {},
            )
        }
        compose.onNodeWithText("Cartão Nubank").assertIsDisplayed()
        compose.onNodeWithText("3x").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Alimentação").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Salvar despesa").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun anInstallmentShowsItsInvoiceAndStatus() {
        show {
            TransactionDetailScreen(
                state = TransactionDetailUiState(
                    loading = false,
                    transaction = FinancePreview.installment,
                    invoice = FinancePreview.invoice,
                ),
                onBack = {},
                onEdit = {},
                onDelete = {},
                onCard = {},
                onRetry = {},
                onDeleteErrorShown = {},
            )
        }
        compose.onNodeWithText("4 de 4").assertIsDisplayed()
        compose.onNodeWithText("Fatura de dezembro").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Agendada").assertIsDisplayed()
    }
}
