package com.henriquesebastiao.prumo.feature.reading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.henriquesebastiao.prumo.feature.reading.model.READING_MODULE_ID
import com.henriquesebastiao.prumo.feature.reading.ui.BookDetailKey
import com.henriquesebastiao.prumo.feature.reading.ui.BookDetailRoute
import com.henriquesebastiao.prumo.feature.reading.ui.BookEditorKey
import com.henriquesebastiao.prumo.feature.reading.ui.BookEditorRoute
import com.henriquesebastiao.prumo.feature.reading.ui.LocalCovers
import com.henriquesebastiao.prumo.feature.reading.ui.NotesRoute
import com.henriquesebastiao.prumo.feature.reading.ui.ReadingHome
import com.henriquesebastiao.prumo.feature.reading.ui.ReadingNotes
import com.henriquesebastiao.prumo.feature.reading.ui.ReadingSheet
import com.henriquesebastiao.prumo.feature.reading.ui.SheetKind
import com.henriquesebastiao.prumo.feature.reading.ui.SheetRequest
import com.henriquesebastiao.prumo.feature.reading.ui.ShelfRoute
import com.henriquesebastiao.prumo.feature.reading.ui.rememberCovers
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject

/**
 * Livros (`reading`): the shelf, the progress, the notes and the covers, on the server's
 * `/api/v1/reading` (docs/api.md of the server, "Livros").
 */
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
            sheet = { onDismiss -> ReadingSheet(SheetRequest(SheetKind.Progress), onDismiss) },
        ),
        QuickAction(
            "reading.note",
            R.string.reading_action_note,
            R.string.reading_action_note,
            PrumoIcons.EditNote,
            ReadingHome,
            sheet = { onDismiss -> ReadingSheet(SheetRequest(SheetKind.Note), onDismiss) },
        ),
        QuickAction(
            "reading.add_book",
            R.string.reading_action_add_book,
            R.string.reading_action_add_book,
            PrumoIcons.LibraryAdd,
            BookEditorKey(),
        ),
    )

    override fun EntryProviderScope<NavKey>.entries(navigator: Navigator) {
        entry<ReadingHome> { Module { ShelfRoute(navigator) } }
        entry<BookDetailKey> { key -> Module { BookDetailRoute(key.bookId, navigator) } }
        entry<BookEditorKey> { key -> Module { BookEditorRoute(key.bookId, navigator) } }
        entry<ReadingNotes> { Module { NotesRoute(navigator) } }
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

    @Composable
    override fun activity(item: ActivityItem, context: HomeContext): HomeEvent? = readingActivity(item, context)

    /** A destination of the module, in its colors and with the covers at hand. */
    @Composable
    private fun Module(content: @Composable () -> Unit) = ModuleTheme(SEED) {
        CompositionLocalProvider(LocalCovers provides rememberCovers(), content = content)
    }

    companion object {
        const val ID = READING_MODULE_ID
        val SEED = Seeds.READING
    }
}
