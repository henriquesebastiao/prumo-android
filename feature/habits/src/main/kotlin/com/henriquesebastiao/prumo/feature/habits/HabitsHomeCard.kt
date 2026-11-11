package com.henriquesebastiao.prumo.feature.habits

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.feature.habits.ui.daysText

/**
 * Hábitos on the Home (H9): today done over scheduled, the first open habits and the habits to
 * quit with their days without, all from the server's summary.
 */
@Composable
internal fun HabitsHomeCard(summary: HabitsSummaryDto?, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    ModuleTheme(HabitsFeature.SEED) {
        SectionCard(
            modifier = modifier.heightIn(min = TILE_MIN_HEIGHT),
            tone = SectionTone.Primary,
            icon = PrumoIcons.Checklist,
            overline = stringResource(R.string.habits_name),
            onClick = onOpen,
        ) {
            when {
                summary == null || (summary.scheduledToday == 0 && summary.quit.isEmpty()) -> Text(
                    stringResource(R.string.habits_card_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )

                summary.scheduledToday == 0 -> Text(
                    stringResource(R.string.habits_card_none),
                    style = MaterialTheme.typography.titleMedium,
                )

                else -> {
                    FitText(
                        stringResource(R.string.habits_card_today, summary.doneToday, summary.scheduledToday),
                        style = PrumoType.dataHeadline,
                    )
                    LinearWavyProgressIndicator(progress = { summary.doneToday / summary.scheduledToday.toFloat() })
                    val names = summary.pending.map { it.name } +
                        listOfNotNull(
                            stringResource(R.string.habits_card_more, summary.pendingMore).takeIf {
                                summary.pendingMore >
                                    0
                            },
                        )
                    if (names.isNotEmpty()) {
                        Text(
                            names.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            summary?.quit?.firstOrNull()?.let { quit ->
                Text(
                    stringResource(
                        R.string.habits_join,
                        quit.name,
                        stringResource(R.string.habits_clean, daysText(quit.cleanDays)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val TILE_MIN_HEIGHT = 224.dp

@PhonePreviews
@Composable
private fun HabitsHomeCardPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            HabitsHomeCard(
                HabitsSummaryDto(
                    doneToday = 1,
                    scheduledToday = 4,
                    pending = listOf(
                        SummaryHabitDto("1", "Academia", "orange", "dumbbell"),
                        SummaryHabitDto("2", "Anki", "blue", "graduation-cap"),
                        SummaryHabitDto("3", "Beber água", "cyan", "droplet"),
                    ),
                    pendingMore = 0,
                    quit = listOf(SummaryQuitDto("4", "Redes sociais antes de dormir", 8)),
                ),
                onOpen = {},
            )
        }
    }
}
