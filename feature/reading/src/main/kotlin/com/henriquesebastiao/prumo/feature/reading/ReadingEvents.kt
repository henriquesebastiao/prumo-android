package com.henriquesebastiao.prumo.feature.reading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.feature.HomeEvent
import com.henriquesebastiao.prumo.core.model.ActivityItem
import com.henriquesebastiao.prumo.core.model.long
import com.henriquesebastiao.prumo.core.model.text
import com.henriquesebastiao.prumo.core.ui.relativeDateTime
import com.henriquesebastiao.prumo.feature.reading.ui.BookDetailKey

/**
 * "Atividade recente" of Livros (docs/api.md of the server, "Itens do Início"), as the web's
 * reading/events.ts writes it: a new current page or a note, opening the book. The progress that
 * reached the last page of a book the server marked read (R4) reads as finished.
 */
@Composable
@ReadOnlyComposable
internal fun readingActivity(item: ActivityItem, context: HomeContext): HomeEvent? {
    val data = item.data
    val whenText = relativeDateTime(item.occurredAt, context.zone, context.today)
    val target = item.entityId?.let(::BookDetailKey)
    return when (item.kind) {
        "reading.progress" -> {
            val page = data.long("page") ?: 0
            val pages = data.long("pages") ?: 0
            val finished = data.text("status") == "read" && page == data.long("total_pages")
            HomeEvent(
                title = stringResource(
                    if (finished) R.string.reading_event_finished else R.string.reading_event_progress,
                    item.subject,
                ),
                subtitle = stringResource(
                    R.string.reading_event_join,
                    whenText,
                    stringResource(R.string.reading_event_until_page, page),
                ),
                value = if (pages > 0) stringResource(R.string.reading_event_pages, pages) else null,
                icon = PrumoIcons.Bookmark,
                target = target,
            )
        }

        "reading.note" -> HomeEvent(
            title = stringResource(R.string.reading_event_note, item.subject),
            subtitle = data.text("excerpt")?.let { stringResource(R.string.reading_event_join, whenText, it) }
                ?: whenText,
            icon = PrumoIcons.StickyNote2,
            target = target,
        )

        else -> null
    }
}
