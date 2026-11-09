package com.henriquesebastiao.prumo.feature.reading.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.ImageRequest
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Preferences
import com.henriquesebastiao.prumo.core.network.di.ApiClient
import com.henriquesebastiao.prumo.core.ui.errorText
import com.henriquesebastiao.prumo.feature.reading.R
import com.henriquesebastiao.prumo.feature.reading.ReadingFeature
import com.henriquesebastiao.prumo.feature.reading.data.ReadingRepository
import com.henriquesebastiao.prumo.feature.reading.model.Book
import com.henriquesebastiao.prumo.feature.reading.model.BookStatus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.Serializable
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient

/** The user's zone and "today" in it (S3 of the server): the dates of the forms and of the screens. */
data class ReadingContext(
    val zone: ZoneId = Preferences.Default.zone,
    val today: LocalDate = LocalDate.of(2026, 11, 17),
)

internal fun Preferences?.readingContext(clock: Clock): ReadingContext {
    val prefs = this ?: Preferences.Default
    return ReadingContext(prefs.zone, prefs.today(clock.instant()))
}

/**
 * The covers of the books: fetched through the session's client (the cover route needs the bearer,
 * R6), cached by Coil in memory and on disk. Given to the screens by [LocalCovers].
 */
@Singleton
class Covers @Inject constructor(
    @ApplicationContext context: Context,
    @ApiClient client: OkHttpClient,
    private val repository: ReadingRepository,
) {
    val loader: ImageLoader by lazy {
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .build()
    }

    fun url(book: Book): String? = repository.coverUrl(book)
}

/** The covers of the module's screens (none in previews and tests: they draw the placeholder). */
val LocalCovers = staticCompositionLocalOf<Covers?> { null }

/** How a composable reaches the [Covers] singleton (the feature is built outside Hilt in tests). */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface CoversEntryPoint {
    fun covers(): Covers
}

/** The app's [Covers], for [LocalCovers] at the module's destinations. */
@Composable
internal fun rememberCovers(): Covers {
    val context = LocalContext.current.applicationContext
    return remember(context) { EntryPointAccessors.fromApplication(context, CoversEntryPoint::class.java).covers() }
}

/**
 * A book's cover, or, without one (R6), a tonal one with the title and the author on it (just the
 * icon when it is too small for text). [container] colors the placeholder.
 */
@Composable
internal fun BookCover(
    book: Book?,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    val covers = LocalCovers.current
    val url = book?.let { covers?.url(it) }
    val placeholder: @Composable () -> Unit = { CoverPlaceholder(book, width, container, content) }
    Box(modifier.size(width, height).clip(shape).clearAndSetSemantics {}) {
        if (url != null && covers != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).build(),
                imageLoader = covers.loader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { placeholder() },
                error = { placeholder() },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            placeholder()
        }
    }
}

@Composable
private fun CoverPlaceholder(book: Book?, width: Dp, container: Color, content: Color) {
    Box(Modifier.fillMaxSize().background(container)) {
        if (book == null || width < TEXT_COVER_MIN) {
            Icon(
                painterResource(PrumoIcons.MenuBook),
                contentDescription = null,
                tint = content,
                modifier = Modifier.align(Alignment.Center).size(if (width < TEXT_COVER_MIN) 20.dp else 36.dp),
            )
        } else {
            Column(Modifier.padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(painterResource(PrumoIcons.MenuBook), contentDescription = null, tint = content)
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = content,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    book.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val TEXT_COVER_MIN = 72.dp

/** "Lendo", "Quero ler", "Lido". */
@Composable
internal fun BookStatus.label(): String = stringResource(
    when (this) {
        BookStatus.WantToRead -> R.string.reading_status_want_to_read
        BookStatus.Reading -> R.string.reading_status_reading
        BookStatus.Read -> R.string.reading_status_read
    },
)

/** Which sheet of Livros is open over a screen, and for what. Kept across a rotation. */
data class SheetRequest(val kind: SheetKind, val bookId: String? = null, val noteId: String? = null) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

enum class SheetKind { Progress, Note }

/** The sheet of [request], in the module's colors (also over the Home). */
@Composable
internal fun ReadingSheet(request: SheetRequest?, onDismiss: () -> Unit) {
    if (request == null) return
    ModuleTheme(ReadingFeature.SEED) {
        when (request.kind) {
            SheetKind.Progress -> ProgressSheet(request.bookId, onDismiss)
            SheetKind.Note -> NoteSheet(request.bookId, request.noteId, onDismiss)
        }
    }
}

/** What the toolbar of Livros records. [onAddBook] null leaves it out (a book's own toolbar). */
internal class ReadingActions(val onProgress: () -> Unit, val onNote: () -> Unit, val onAddBook: (() -> Unit)? = null)

/**
 * The floating toolbar of Livros (boards 01 and 05): a note and, on the shelf, a new book; the FAB
 * updates the page.
 */
@Composable
internal fun ReadingToolbar(actions: ReadingActions, modifier: Modifier = Modifier) {
    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
            FloatingToolbarDefaults.StandardFloatingActionButton(
                onClick = actions.onProgress,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    painterResource(PrumoIcons.AutoStories),
                    contentDescription = stringResource(R.string.reading_action_progress_short),
                )
            }
        },
        // A module's toolbar is the standard one (the Home's is vibrant), its FAB in primary container.
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        modifier = modifier,
    ) {
        IconButton(onClick = actions.onNote) {
            Icon(
                painterResource(PrumoIcons.EditNote),
                contentDescription = stringResource(R.string.reading_action_note),
            )
        }
        actions.onAddBook?.let { onAdd ->
            IconButton(onClick = onAdd) {
                Icon(
                    painterResource(PrumoIcons.LibraryAdd),
                    contentDescription = stringResource(R.string.reading_action_add_book),
                )
            }
        }
    }
}

/** Room under a list for the floating toolbar. */
internal val TOOLBAR_SPACE = 112.dp

/**
 * The text of an error of Livros: the module's codes (`docs/api.md` of the server, "Livros"), then
 * the app's.
 */
@Composable
@ReadOnlyComposable
internal fun readingErrorText(error: AppError): String = when ((error as? AppError.Api)?.code) {
    PAGE_OUT_OF_RANGE -> stringResource(R.string.reading_error_page_out_of_range)
    INVALID_COVER -> stringResource(R.string.reading_error_invalid_cover)
    COVER_TOO_LARGE -> stringResource(R.string.reading_error_cover_too_large)
    INVALID_DATES -> stringResource(R.string.reading_error_invalid_dates)
    else -> errorText(error)
}

/** "Confira este valor." when the server refused the field at [path] (`validation_error`). */
@Composable
@ReadOnlyComposable
internal fun fieldError(error: AppError?, path: String): String? {
    val api = error as? AppError.Api ?: return null
    if (api.code == PAGE_OUT_OF_RANGE &&
        path in PAGE_FIELDS
    ) {
        return stringResource(R.string.reading_error_page_out_of_range)
    }
    if (api.code == INVALID_DATES && path in DATE_FIELDS) return stringResource(R.string.reading_error_invalid_dates)
    val refused = api.fields.any { it.path == path || it.path.startsWith("$path.") }
    return if (refused) stringResource(R.string.reading_field_invalid) else null
}

/** Whether [error] belongs to a field (shown there) rather than to the whole form. */
internal fun AppError.isFieldError(): Boolean =
    this is AppError.Api && (code == PAGE_OUT_OF_RANGE || code == INVALID_DATES || fields.isNotEmpty())

private const val PAGE_OUT_OF_RANGE = "reading.page_out_of_range"
private const val INVALID_COVER = "reading.invalid_cover"
private const val COVER_TOO_LARGE = "reading.cover_too_large"
private const val INVALID_DATES = "reading.invalid_dates"
private val PAGE_FIELDS = setOf("page", "current_page")
private val DATE_FIELDS = setOf("finished_on")
