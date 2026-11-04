package com.henriquesebastiao.prumo.feature.finance.data

import com.henriquesebastiao.prumo.core.model.Wire
import com.henriquesebastiao.prumo.core.network.testing.OpenApiContract
import com.henriquesebastiao.prumo.feature.finance.model.AccountKind
import com.henriquesebastiao.prumo.feature.finance.model.CategoryKind
import com.henriquesebastiao.prumo.feature.finance.model.FinanceColor
import com.henriquesebastiao.prumo.feature.finance.model.InvoiceStatus
import com.henriquesebastiao.prumo.feature.finance.model.TransactionKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** The hand-written DTOs of Finanças against the server's openapi.json (D04). */
class FinanceContractTest {
    @Test
    fun theReadDtosFollowTheContract() {
        OpenApiContract.assertReads(AccountRefDto.serializer().descriptor, "AccountRef")
        OpenApiContract.assertReads(CategoryRefDto.serializer().descriptor, "CategoryRef")
        OpenApiContract.assertReads(CardRefDto.serializer().descriptor, "CardRef")
        OpenApiContract.assertReads(AccountDto.serializer().descriptor, "AccountRead")
        OpenApiContract.assertReads(AccountListDto.serializer().descriptor, "AccountList")
        OpenApiContract.assertReads(CategoryDto.serializer().descriptor, "CategoryRead")
        OpenApiContract.assertReads(CategoryListDto.serializer().descriptor, "CategoryList")
        OpenApiContract.assertReads(InvoiceDto.serializer().descriptor, "InvoiceRead")
        OpenApiContract.assertReads(InvoiceListDto.serializer().descriptor, "InvoiceList")
        OpenApiContract.assertReads(InvoiceDetailDto.serializer().descriptor, "InvoiceDetail")
        OpenApiContract.assertReads(CreditCardDto.serializer().descriptor, "CreditCardRead")
        OpenApiContract.assertReads(CreditCardListDto.serializer().descriptor, "CreditCardList")
        OpenApiContract.assertReads(TransactionDto.serializer().descriptor, "TransactionRead")
        OpenApiContract.assertReads(TransactionPageDto.serializer().descriptor, "TransactionPage")
        OpenApiContract.assertReads(AccountBalanceDto.serializer().descriptor, "AccountBalance")
        OpenApiContract.assertReads(MonthPointDto.serializer().descriptor, "MonthPoint")
        OpenApiContract.assertReads(CategoryShareDto.serializer().descriptor, "CategoryShare")
        OpenApiContract.assertReads(OpenInvoiceDto.serializer().descriptor, "OpenInvoice")
        OpenApiContract.assertReads(OverviewDto.serializer().descriptor, "FinanceOverview")
    }

    @Test
    fun theWriteDtosFollowTheContract() {
        OpenApiContract.assertWrites(AccountWriteDto.serializer().descriptor, "AccountCreate")
        OpenApiContract.assertWrites(AccountWriteDto.serializer().descriptor, "AccountUpdate")
        OpenApiContract.assertWrites(AdjustmentCreateDto.serializer().descriptor, "AdjustmentCreate")
        OpenApiContract.assertWrites(CategoryWriteDto.serializer().descriptor, "CategoryCreate")
        OpenApiContract.assertWrites(CategoryWriteDto.serializer().descriptor, "CategoryUpdate")
        OpenApiContract.assertWrites(CreditCardWriteDto.serializer().descriptor, "CreditCardCreate")
        OpenApiContract.assertWrites(CreditCardWriteDto.serializer().descriptor, "CreditCardUpdate")
        OpenApiContract.assertWrites(TransactionCreateDto.serializer().descriptor, "TransactionCreate")
        OpenApiContract.assertWrites(TransactionUpdateDto.serializer().descriptor, "TransactionUpdate")
        OpenApiContract.assertWrites(PaymentCreateDto.serializer().descriptor, "InvoicePaymentCreate")
    }

    @Test
    fun theEnumsAndColorsAreTheServers() {
        assertEquals(OpenApiContract.enumValues("AccountKind"), wires<AccountKind>())
        assertEquals(OpenApiContract.enumValues("CategoryKind"), wires<CategoryKind>())
        assertEquals(OpenApiContract.enumValues("TransactionKind"), wires<TransactionKind>())
        assertEquals(OpenApiContract.enumValues("InvoiceStatus"), wires<InvoiceStatus>())
        val palette = OpenApiContract.schema("AccountCreate")["properties"]!!.jsonObject["color"]!!.jsonObject["enum"]
        assertEquals((palette as JsonArray).map { (it as JsonPrimitive).content }.toSet(), wires<FinanceColor>())
    }

    private inline fun <reified T> wires(): Set<String> where T : Enum<T>, T : Wire =
        enumValues<T>().map { it.wire }.toSet()
}
