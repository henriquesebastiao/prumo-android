package com.henriquesebastiao.prumo.feature.finance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The module's Home summary (schema FinanceSummary of the server), with the fields the card uses.

@Serializable
data class FinanceSummaryDto(
    val module: String = "finance",
    /** "2026-11": the month of [expenseCents], in the user's time zone. */
    val month: String,
    @SerialName("total_balance_cents") val totalBalanceCents: Long,
    @SerialName("expense_cents") val expenseCents: Long,
    @SerialName("next_invoice") val nextInvoice: OpenInvoiceDto? = null,
)

/** Schema OpenInvoice: the card and its invoice. */
@Serializable
data class OpenInvoiceDto(val card: CardRefDto, val invoice: InvoiceDto)

/** Schema CardRef. */
@Serializable
data class CardRefDto(val id: String, val name: String)

/** Schema InvoiceRead, the fields the card shows. */
@Serializable
data class InvoiceDto(
    @SerialName("closing_on") val closingOn: String,
    @SerialName("amount_cents") val amountCents: Long,
    val status: String,
)
