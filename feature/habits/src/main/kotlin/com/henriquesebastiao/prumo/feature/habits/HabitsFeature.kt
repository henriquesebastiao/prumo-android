package com.henriquesebastiao.prumo.feature.habits

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.henriquesebastiao.prumo.core.designsystem.color.Seeds
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.core.feature.QuickAction
import com.henriquesebastiao.prumo.core.network.decodeOrNull
import com.henriquesebastiao.prumo.feature.habits.model.HABITS_MODULE_ID
import com.henriquesebastiao.prumo.feature.habits.ui.HabitDetailKey
import com.henriquesebastiao.prumo.feature.habits.ui.HabitDetailRoute
import com.henriquesebastiao.prumo.feature.habits.ui.HabitEditorKey
import com.henriquesebastiao.prumo.feature.habits.ui.HabitEditorRoute
import com.henriquesebastiao.prumo.feature.habits.ui.HabitsHome
import com.henriquesebastiao.prumo.feature.habits.ui.HabitsHomeRoute
import com.henriquesebastiao.prumo.feature.habits.ui.HabitsSheet
import com.henriquesebastiao.prumo.feature.habits.ui.SheetKind
import com.henriquesebastiao.prumo.feature.habits.ui.SheetRequest
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject

/**
 * Hábitos (`habits`): habits to practice (check or quantity, a frequency in versions, pauses) and
 * to quit (relapses and the time without), on the server's `/api/v1/habits` (docs/api.md of the
 * server, "Hábitos"; D24). Nothing in "Atividade recente" nor "Vem aí" (H9).
 */
class HabitsFeature @Inject constructor() : PrumoFeature {
    override val id = ID
    override val nameRes = R.string.habits_name
    override val descriptionRes = R.string.habits_description
    override val icon = PrumoIcons.Checklist
    override val seed = SEED
    override val entryKey: NavKey = HabitsHome()

    override val quickActions = listOf(
        QuickAction(
            "habits.check_in",
            R.string.habits_action_check_in,
            R.string.habits_action_check_in_short,
            PrumoIcons.Checklist,
            HabitsHome(),
            sheet = { onDismiss -> HabitsSheet(SheetRequest(SheetKind.CheckIn), onDismiss) },
        ),
    )

    override fun EntryProviderScope<NavKey>.entries(navigator: Navigator) {
        entry<HabitsHome> { key -> ModuleTheme(SEED) { HabitsHomeRoute(key, navigator) } }
        entry<HabitDetailKey> { key -> ModuleTheme(SEED) { HabitDetailRoute(key.habitId, navigator) } }
        entry<HabitEditorKey> { key -> ModuleTheme(SEED) { HabitEditorRoute(key.habitId, navigator) } }
    }

    @Composable
    override fun HomeCard(
        summary: JsonObject?,
        hero: Boolean,
        context: HomeContext,
        onOpen: () -> Unit,
        modifier: Modifier,
    ) {
        HabitsHomeCard(decodeOrNull(HabitsSummaryDto.serializer(), summary), onOpen, modifier)
    }

    companion object {
        const val ID = HABITS_MODULE_ID
        val SEED = Seeds.HABITS
    }
}
