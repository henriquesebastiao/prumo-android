package com.henriquesebastiao.prumo.feature.garage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The module's Home summary (schema GarageSummary of the server), with the fields the card uses.

@Serializable
data class GarageSummaryDto(
    val module: String = "garage",
    @SerialName("vehicles_count") val vehiclesCount: Int,
    @SerialName("next_due") val nextDue: NextDueDto? = null,
)

/** The oil change that comes first (schema NextDue); the API says which criterion leads. */
@Serializable
data class NextDueDto(
    val vehicle: VehicleRefDto,
    val status: String,
    val primary: String,
    @SerialName("remaining_km") val remainingKm: Long? = null,
    @SerialName("remaining_days") val remainingDays: Int? = null,
    @SerialName("used_percent") val usedPercent: Int? = null,
)

/** Schema VehicleRef. */
@Serializable
data class VehicleRefDto(val id: String, val name: String, val kind: String)
