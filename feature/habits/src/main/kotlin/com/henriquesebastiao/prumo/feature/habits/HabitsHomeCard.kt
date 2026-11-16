package com.henriquesebastiao.prumo.feature.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.designsystem.component.FitText
import com.henriquesebastiao.prumo.core.designsystem.component.PhonePreviews
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.component.SectionCard
import com.henriquesebastiao.prumo.core.designsystem.component.SectionTone
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoType
import com.henriquesebastiao.prumo.core.designsystem.theme.Spacing
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.feature.habits.data.DayItemDto
import com.henriquesebastiao.prumo.feature.habits.data.HabitRefDto
import com.henriquesebastiao.prumo.feature.habits.data.HabitsRepository
import com.henriquesebastiao.prumo.feature.habits.data.toModel
import com.henriquesebastiao.prumo.feature.habits.model.DayItem
import com.henriquesebastiao.prumo.feature.habits.model.Measure
import com.henriquesebastiao.prumo.feature.habits.ui.HabitAvatar
import com.henriquesebastiao.prumo.feature.habits.ui.SEPARATOR
import com.henriquesebastiao.prumo.feature.habits.ui.TodayViewModel
import com.henriquesebastiao.prumo.feature.habits.ui.amountText
import com.henriquesebastiao.prumo.feature.habits.ui.daysText
import com.henriquesebastiao.prumo.feature.habits.ui.habitsErrorText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Marks from the Home's card: a write tells the Home (through the repository's DataChanges), which
 * reloads the summary; nothing is computed here.
 */
@HiltViewModel
class HabitsCardViewModel @Inject constructor(private val repository: HabitsRepository) : ViewModel() {
    private val mutableError = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = mutableError.asStateFlow()

    fun toggle(item: DayItem, on: LocalDate) = send {
        if (item.done) repository.unmark(item.habit.id, on) else repository.mark(item.habit.id, on, 1)
    }

    /** One step more of a quantity. */
    fun step(item: DayItem, on: LocalDate) =
        send { repository.mark(item.habit.id, on, (item.value ?: 0) + TodayViewModel.stepOf(item)) }

    private fun send(block: suspend () -> Outcome<*>) {
        mutableError.value = null
        viewModelScope.launch {
            val outcome = block()
            if (outcome is Outcome.Failed) mutableError.value = outcome.error
        }
    }
}

/** What the card does with a habit of the day. */
internal class CardEvents(val onToggle: (DayItem, LocalDate) -> Unit, val onStep: (DayItem, LocalDate) -> Unit)

@Composable
internal fun HabitsHomeCardRoute(
    summary: HabitsSummaryDto?,
    hero: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitsCardViewModel = hiltViewModel(),
) {
    val error by viewModel.error.collectAsStateWithLifecycle()
    HabitsHomeCard(
        summary,
        hero,
        onOpen,
        modifier,
        CardEvents(viewModel::toggle, viewModel::step),
        error?.let { habitsErrorText(it) },
    )
}

/**
 * Hábitos on the Home (H9): today done over scheduled, the next open habits to mark right here
 * (5 on the hero, 3 on a tile) and the habits to quit with their days without, all from the
 * server's summary.
 */
@Composable
internal fun HabitsHomeCard(
    summary: HabitsSummaryDto?,
    hero: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    events: CardEvents = CardEvents({ _, _ -> }, { _, _ -> }),
    error: String? = null,
) {
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
                    val today = summary.today?.let(LocalDate::parse)
                    val items = summary.upcoming.take(if (hero) HERO_ITEMS else TILE_ITEMS).map { it.toModel() }
                    if (today != null && items.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            items.forEach { item -> CardRow(item, today, events) }
                        }
                    }
                    val more = summary.scheduledToday - summary.doneToday - items.size
                    if (more > 0) {
                        Text(
                            stringResource(R.string.habits_card_more, more),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

/** A habit to mark from the card: the whole row toggles a `check`; a quantity takes one step more. */
@Composable
private fun CardRow(item: DayItem, today: LocalDate, events: CardEvents) {
    val quantity = item.habit.measure == Measure.Quantity
    // The name first: a tile is narrow; the time and the amount go under it.
    val detail = listOfNotNull(
        item.habit.timeOfDay?.let(Formats::time),
        amountText(item.habit, item.value).takeIf { quantity },
    ).joinToString(SEPARATOR)
    val rowModifier = if (quantity) {
        Modifier
    } else {
        Modifier.toggleable(value = item.done, role = Role.Checkbox, onValueChange = { events.onToggle(item, today) })
    }
    Row(
        rowModifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        HabitAvatar(item.habit.color, item.habit.icon, size = 28)
        Column(Modifier.weight(1f)) {
            Text(
                item.habit.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isNotEmpty()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (quantity) {
            IconButton(onClick = { events.onStep(item, today) }) {
                Icon(
                    painterResource(PrumoIcons.Add),
                    contentDescription = stringResource(R.string.habits_increase, item.habit.name),
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Checkbox(checked = item.done, onCheckedChange = null)
        }
    }
}

private val TILE_MIN_HEIGHT = 224.dp
private const val HERO_ITEMS = 5
private const val TILE_ITEMS = 3

@PhonePreviews
@Composable
private fun HabitsHomeCardPreview() {
    fun item(id: String, name: String, color: String, icon: String, time: String? = null, measure: String = "check") =
        DayItemDto(
            HabitRefDto(id, name, color, icon, measure, 2000.takeIf { measure == "quantity" }, "ml", time),
            done = false,
            value = 1250.takeIf { measure == "quantity" },
            settled = false,
        )
    PrumoTheme(dynamicColor = false) {
        Surface {
            HabitsHomeCard(
                HabitsSummaryDto(
                    today = "2026-11-17",
                    doneToday = 1,
                    scheduledToday = 6,
                    pending = emptyList(),
                    pendingMore = 0,
                    upcoming = listOf(
                        item("1", "Alongar", "teal", "heart-pulse", "07:00"),
                        item("2", "Regar as plantas", "green", "sun", "18:00"),
                        item("3", "Tarefa do mês", "green", "sparkles"),
                        item("4", "Anki", "blue", "graduation-cap"),
                        item("5", "Beber água", "cyan", "droplet", measure = "quantity"),
                    ),
                    quit = listOf(SummaryQuitDto("6", "Redes sociais antes de dormir", 8)),
                ),
                hero = true,
                onOpen = {},
            )
        }
    }
}
