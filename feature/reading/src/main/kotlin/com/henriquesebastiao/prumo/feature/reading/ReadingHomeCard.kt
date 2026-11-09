package com.henriquesebastiao.prumo.feature.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing

/**
 * Livros on the Home: the book read most recently, with its progress (from the API, R5 of the
 * server). Covers come with the module's phase; the card draws the book's place for now.
 */
@Composable
internal fun ReadingHomeCard(summary: ReadingSummaryDto?, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val current = summary?.current
    ModuleTheme(ReadingFeature.SEED) {
        SectionCard(
            modifier = modifier.heightIn(min = TILE_MIN_HEIGHT),
            tone = SectionTone.Primary,
            icon = PrumoIcons.MenuBook,
            overline = stringResource(R.string.reading_name),
            onClick = onOpen,
        ) {
            if (current != null) {
                CurrentBook(current)
            } else {
                Text(stringResource(R.string.reading_card_nothing), style = MaterialTheme.typography.titleMedium)
                val books = summary?.booksCount ?: 0
                if (books > 0) {
                    Text(
                        pluralStringResource(R.plurals.reading_card_books, books, books),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentBook(current: CurrentBookDto) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Box(
                Modifier
                    .size(width = 36.dp, height = 52.dp)
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(PrumoIcons.MenuBook),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(stringResource(R.string.reading_card_percent, current.progressPercent), style = PrumoType.dataHeadline)
        }
        Column {
            Text(
                current.book.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.reading_card_pages, current.currentPage, current.totalPages),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        LinearWavyProgressIndicator(progress = {
            current.progressPercent / PERCENT
        }, modifier = Modifier.fillMaxWidth())
    }
}

private const val PERCENT = 100f
private val TILE_MIN_HEIGHT = 224.dp

@PhonePreviews
@Composable
private fun ReadingHomeCardPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            ReadingHomeCard(
                ReadingSummaryDto(
                    booksCount = 3,
                    current = CurrentBookDto(BookRefDto("1", "Código Limpo", "Robert C. Martin"), 212, 425, 50),
                ),
                onOpen = {},
            )
        }
    }
}
