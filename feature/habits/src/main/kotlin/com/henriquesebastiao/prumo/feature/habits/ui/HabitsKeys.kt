package com.henriquesebastiao.prumo.feature.habits.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// The destinations of Hábitos (D24). Marking, frequency, pause and relapse are sheets over the screen.

/** The tabs of Hábitos: Hoje and Hábitos. */
@Serializable
enum class HabitsTab { Today, List }

/** Where Hábitos opens: the [tab]. */
@Serializable
data class HabitsHome(val tab: HabitsTab = HabitsTab.Today) : NavKey

@Serializable
data class HabitDetailKey(val habitId: String) : NavKey

/** A new habit ([habitId] null) or the edit of one. */
@Serializable
data class HabitEditorKey(val habitId: String? = null) : NavKey
