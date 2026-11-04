package com.henriquesebastiao.prumo.feature.finance.data

import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The routes of Finanças (`docs/api.md` of the server, "Finanças"), relative to the server's base
 * URL. Bodies go as [JsonObject] written by `RequestJson` (:core:network), so a `PATCH` can clear a
 * field with `null`.
 */
@Suppress("TooManyFunctions")
interface FinanceApi {
    @GET("api/v1/finance/overview")
    suspend fun overview(@Query("month") month: String?): OverviewDto

    @GET("api/v1/finance/accounts")
    suspend fun accounts(@Query("archived") archived: Boolean): AccountListDto

    @GET("api/v1/finance/accounts/{id}")
    suspend fun account(@Path("id") id: String): AccountDto

    @POST("api/v1/finance/accounts")
    suspend fun createAccount(@Body body: JsonObject): AccountDto

    @PATCH("api/v1/finance/accounts/{id}")
    suspend fun updateAccount(@Path("id") id: String, @Body body: JsonObject): AccountDto

    @DELETE("api/v1/finance/accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: String)

    @POST("api/v1/finance/accounts/{id}/archive")
    suspend fun archiveAccount(@Path("id") id: String): AccountDto

    @POST("api/v1/finance/accounts/{id}/unarchive")
    suspend fun unarchiveAccount(@Path("id") id: String): AccountDto

    @POST("api/v1/finance/accounts/{id}/adjustments")
    suspend fun adjustAccount(@Path("id") id: String, @Body body: JsonObject): TransactionDto

    @GET("api/v1/finance/categories")
    suspend fun categories(
        @Query("kind") kind: String?,
        @Query("archived") archived: Boolean,
        @Query("month") month: String?,
    ): CategoryListDto

    @POST("api/v1/finance/categories")
    suspend fun createCategory(@Body body: JsonObject): CategoryDto

    @PATCH("api/v1/finance/categories/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Body body: JsonObject): CategoryDto

    @DELETE("api/v1/finance/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String)

    @POST("api/v1/finance/categories/{id}/archive")
    suspend fun archiveCategory(@Path("id") id: String): CategoryDto

    @POST("api/v1/finance/categories/{id}/unarchive")
    suspend fun unarchiveCategory(@Path("id") id: String): CategoryDto

    @Suppress("LongParameterList")
    @GET("api/v1/finance/transactions")
    suspend fun transactions(
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("kind") kind: String?,
        @Query("account_id") accountId: String?,
        @Query("credit_card_id") creditCardId: String?,
        @Query("category_id") categoryId: String?,
        @Query("q") query: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): TransactionPageDto

    @GET("api/v1/finance/transactions/{id}")
    suspend fun transaction(@Path("id") id: String): TransactionDto

    @POST("api/v1/finance/transactions")
    suspend fun createTransaction(@Body body: JsonObject): TransactionDto

    @PATCH("api/v1/finance/transactions/{id}")
    suspend fun updateTransaction(@Path("id") id: String, @Body body: JsonObject): TransactionDto

    /** [installments] `all` removes the whole installment purchase (F10); `one`, only this one. */
    @DELETE("api/v1/finance/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String, @Query("installments") installments: String)

    @GET("api/v1/finance/credit-cards")
    suspend fun cards(@Query("archived") archived: Boolean): CreditCardListDto

    @GET("api/v1/finance/credit-cards/{id}")
    suspend fun card(@Path("id") id: String): CreditCardDto

    @POST("api/v1/finance/credit-cards")
    suspend fun createCard(@Body body: JsonObject): CreditCardDto

    @PATCH("api/v1/finance/credit-cards/{id}")
    suspend fun updateCard(@Path("id") id: String, @Body body: JsonObject): CreditCardDto

    @DELETE("api/v1/finance/credit-cards/{id}")
    suspend fun deleteCard(@Path("id") id: String)

    @POST("api/v1/finance/credit-cards/{id}/archive")
    suspend fun archiveCard(@Path("id") id: String): CreditCardDto

    @POST("api/v1/finance/credit-cards/{id}/unarchive")
    suspend fun unarchiveCard(@Path("id") id: String): CreditCardDto

    @GET("api/v1/finance/credit-cards/{id}/invoices")
    suspend fun invoices(@Path("id") cardId: String): InvoiceListDto

    @GET("api/v1/finance/credit-cards/{id}/invoices/{closing}")
    suspend fun invoice(@Path("id") cardId: String, @Path("closing") closingOn: String): InvoiceDetailDto

    @POST("api/v1/finance/credit-cards/{id}/invoices/{closing}/payments")
    suspend fun payInvoice(
        @Path("id") cardId: String,
        @Path("closing") closingOn: String,
        @Body body: JsonObject,
    ): TransactionDto
}
