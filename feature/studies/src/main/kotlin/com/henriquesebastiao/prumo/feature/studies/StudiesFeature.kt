package com.henriquesebastiao.prumo.feature.studies

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.henriquesebastiao.prumo.core.designsystem.color.Seeds
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.HomeEvent
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.core.feature.QuickAction
import com.henriquesebastiao.prumo.core.model.ActivityItem
import com.henriquesebastiao.prumo.core.network.decodeOrNull
import com.henriquesebastiao.prumo.feature.studies.model.STUDIES_MODULE_ID
import com.henriquesebastiao.prumo.feature.studies.ui.PlanEditorKey
import com.henriquesebastiao.prumo.feature.studies.ui.PlanEditorRoute
import com.henriquesebastiao.prumo.feature.studies.ui.SheetKind
import com.henriquesebastiao.prumo.feature.studies.ui.SheetRequest
import com.henriquesebastiao.prumo.feature.studies.ui.StudiesHome
import com.henriquesebastiao.prumo.feature.studies.ui.StudiesHomeRoute
import com.henriquesebastiao.prumo.feature.studies.ui.StudiesSheet
import com.henriquesebastiao.prumo.feature.studies.ui.SubjectDetailKey
import com.henriquesebastiao.prumo.feature.studies.ui.SubjectDetailRoute
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject

/**
 * Estudos (`studies`): the weekly plan in versions, the sessions, the areas, subjects and topics,
 * and the link to each subject's note in Obsidian, on the server's `/api/v1/studies` (docs/api.md
 * of the server, "Estudos"; D24).
 */
class StudiesFeature @Inject constructor() : PrumoFeature {
    override val id = ID
    override val nameRes = R.string.studies_name
    override val descriptionRes = R.string.studies_description
    override val icon = PrumoIcons.School
    override val seed = SEED
    override val entryKey: NavKey = StudiesHome()

    override val quickActions = listOf(
        QuickAction(
            "studies.log_session",
            R.string.studies_action_log,
            R.string.studies_action_log_short,
            PrumoIcons.Timer,
            StudiesHome(),
            sheet = { onDismiss -> StudiesSheet(SheetRequest(SheetKind.Session), onDismiss) },
        ),
    )

    override fun EntryProviderScope<NavKey>.entries(navigator: Navigator) {
        entry<StudiesHome> { key -> ModuleTheme(SEED) { StudiesHomeRoute(key, navigator) } }
        entry<SubjectDetailKey> { key -> ModuleTheme(SEED) { SubjectDetailRoute(key.subjectId, navigator) } }
        entry<PlanEditorKey> { key -> ModuleTheme(SEED) { PlanEditorRoute(key.planId, navigator) } }
    }

    @Composable
    override fun HomeCard(
        summary: JsonObject?,
        hero: Boolean,
        context: HomeContext,
        onOpen: () -> Unit,
        modifier: Modifier,
    ) {
        StudiesHomeCard(decodeOrNull(StudiesSummaryDto.serializer(), summary), onOpen, modifier)
    }

    @Composable
    override fun activity(item: ActivityItem, context: HomeContext): HomeEvent? = studiesActivity(item, context)

    companion object {
        const val ID = STUDIES_MODULE_ID
        val SEED = Seeds.STUDIES
    }
}
