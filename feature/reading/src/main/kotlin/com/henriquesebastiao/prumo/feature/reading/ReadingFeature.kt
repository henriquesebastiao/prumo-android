package com.henriquesebastiao.prumo.feature.reading

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.henriquesebastiao.prumo.core.designsystem.color.Seeds
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.feature.ComingSoonScreen
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.Navigator
import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.core.feature.QuickAction
import com.henriquesebastiao.prumo.core.network.decodeOrNull
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Where Livros opens. */
@Serializable
data object ReadingHome : NavKey

/** Livros (`reading`). In the foundation: its Home card, its actions and the "em breve" screen. */
class ReadingFeature @Inject constructor() : PrumoFeature {
    override val id = ID
    override val nameRes = R.string.reading_name
    override val descriptionRes = R.string.reading_description
    override val icon = PrumoIcons.MenuBook
    override val seed = SEED
    override val entryKey: NavKey = ReadingHome

    override val quickActions = listOf(
        QuickAction(
            "reading.progress",
            R.string.reading_action_progress,
            R.string.reading_action_progress_short,
            PrumoIcons.AutoStories,
            ReadingHome,
        ),
        QuickAction(
            "reading.note",
            R.string.reading_action_note,
            R.string.reading_action_note,
            PrumoIcons.EditNote,
            ReadingHome,
        ),
        QuickAction(
            "reading.add_book",
            R.string.reading_action_add_book,
            R.string.reading_action_add_book,
            PrumoIcons.LibraryAdd,
            ReadingHome,
        ),
    )

    override fun EntryProviderScope<NavKey>.entries(navigator: Navigator) {
        entry<ReadingHome> {
            ComingSoonScreen(stringResource(nameRes), icon, seed, onBack = navigator::back)
        }
    }

    @Composable
    override fun HomeCard(
        summary: JsonObject?,
        hero: Boolean,
        context: HomeContext,
        onOpen: () -> Unit,
        modifier: Modifier,
    ) {
        ReadingHomeCard(decodeOrNull(ReadingSummaryDto.serializer(), summary), onOpen, modifier)
    }

    companion object {
        const val ID = "reading"
        val SEED = Seeds.READING
    }
}
