package com.henriquesebastiao.prumo.feature.studies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.feature.studies.ui.minutesText

/**
 * Estudos on the Home: today's minutes against the plan and the week's days met, all from the
 * server's summary (E4–E6).
 */
@Composable
internal fun StudiesHomeCard(summary: StudiesSummaryDto?, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    ModuleTheme(StudiesFeature.SEED) {
        SectionCard(
            modifier = modifier.heightIn(min = TILE_MIN_HEIGHT),
            tone = SectionTone.Primary,
            icon = PrumoIcons.School,
            overline = stringResource(R.string.studies_name),
            onClick = onOpen,
        ) {
            when {
                summary == null -> Text(
                    stringResource(R.string.studies_card_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )

                summary.plannedMinutesToday > 0 -> Today(summary)

                else -> Text(
                    stringResource(R.string.studies_card_no_plan),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            summary?.let { Week(it) }
        }
    }
}

@Composable
private fun Today(summary: StudiesSummaryDto) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FitText(minutesText(summary.doneMinutesToday), style = PrumoType.dataHeadline)
        Text(
            stringResource(R.string.studies_card_today, minutesText(summary.plannedMinutesToday)),
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearWavyProgressIndicator(
            progress = {
                summary.doneMinutesToday.coerceAtMost(summary.plannedMinutesToday) /
                    summary.plannedMinutesToday.toFloat()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Week(summary: StudiesSummaryDto) {
    val week = summary.week
    if (week.daysPlanned == 0 && week.minutesDone == 0) {
        if (summary.plannedMinutesToday == 0) {
            Text(stringResource(R.string.studies_card_empty), style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    Text(
        stringResource(
            R.string.studies_card_week,
            pluralStringResource(R.plurals.studies_card_week_days, week.daysPlanned, week.daysMet, week.daysPlanned),
            minutesText(week.minutesDone),
        ),
        style = MaterialTheme.typography.bodySmall,
    )
}

private val TILE_MIN_HEIGHT = 224.dp

@PhonePreviews
@Composable
private fun StudiesHomeCardPreview() {
    PrumoTheme(dynamicColor = false) {
        Surface {
            StudiesHomeCard(
                StudiesSummaryDto(
                    blocksToday = listOf(
                        SummaryBlockDto(SummarySubjectDto("1", "Inglês", "orange"), "07:00:00", 30, 30, "met"),
                        SummaryBlockDto(SummarySubjectDto("2", "FastAPI", "green"), "20:00:00", 45, 0, "in_progress"),
                    ),
                    doneMinutesToday = 30,
                    plannedMinutesToday = 75,
                    week = SummaryWeekDto(daysMet = 1, daysPlanned = 2, minutesDone = 120, minutesPlanned = 165),
                ),
                onOpen = {},
            )
        }
    }
}
