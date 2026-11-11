package com.henriquesebastiao.prumo.feature.studies.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// The destinations of Estudos (D24). Session, area and subject forms are sheets over the current screen.

/** The tabs of Estudos: Hoje, Semana, Matérias, Plano. */
@Serializable
enum class StudiesTab { Today, Week, Subjects, Plan }

/** Where Estudos opens: the [tab]. */
@Serializable
data class StudiesHome(val tab: StudiesTab = StudiesTab.Today) : NavKey

@Serializable
data class SubjectDetailKey(val subjectId: String) : NavKey

/** A new version of the plan ([planId] null) or the edit of one not yet in force. */
@Serializable
data class PlanEditorKey(val planId: String? = null) : NavKey
