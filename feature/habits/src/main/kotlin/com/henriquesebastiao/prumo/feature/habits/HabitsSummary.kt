package com.henriquesebastiao.prumo.feature.habits

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The module's Home summary (schema HabitsSummary of the server), with the fields the card uses.

@Serializable
data class HabitsSummaryDto(
    val module: String = "habits",
    @SerialName("done_today") val doneToday: Int,
    @SerialName("scheduled_today") val scheduledToday: Int,
    /** Up to 3 habits still open today. */
    val pending: List<SummaryHabitDto>,
    @SerialName("pending_more") val pendingMore: Int,
    val quit: List<SummaryQuitDto>,
)

/** Schema HabitRef. */
@Serializable
data class SummaryHabitDto(val id: String, val name: String, val color: String, val icon: String)

/** Schema HabitsSummaryQuit: a habit to quit and its days without (H7). */
@Serializable
data class SummaryQuitDto(val id: String, val name: String, @SerialName("clean_days") val cleanDays: Int)
