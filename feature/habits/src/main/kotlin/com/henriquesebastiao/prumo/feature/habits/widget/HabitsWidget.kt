package com.henriquesebastiao.prumo.feature.habits.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.henriquesebastiao.prumo.core.data.di.ApplicationScope
import com.henriquesebastiao.prumo.core.data.session.SessionRepository
import com.henriquesebastiao.prumo.core.data.session.SessionState
import com.henriquesebastiao.prumo.core.designsystem.component.PrumoIcons
import com.henriquesebastiao.prumo.core.designsystem.theme.seedColorScheme
import com.henriquesebastiao.prumo.core.feature.QuickAction
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Formats
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.feature.habits.HabitsFeature
import com.henriquesebastiao.prumo.feature.habits.R
import com.henriquesebastiao.prumo.feature.habits.data.HabitsRepository
import com.henriquesebastiao.prumo.feature.habits.model.DayItem
import com.henriquesebastiao.prumo.feature.habits.model.HabitsDay
import com.henriquesebastiao.prumo.feature.habits.model.Measure
import com.henriquesebastiao.prumo.feature.habits.ui.TodayViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the widget shows: the day from the server, or why it couldn't load (D27). */
sealed interface WidgetState {
    data class Loaded(val day: HabitsDay) : WidgetState

    data class Failed(val error: AppError) : WidgetState
}

/**
 * The widget of the home screen (D27): today done over scheduled and the habits still open in
 * the order of the day (H9, H10), each marked right there (a check, or one step more of a
 * quantity). The header opens "Marcar hábitos" over the Home. Everything comes from
 * `GET /habits/day`; the widget computes nothing.
 */
class HabitsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val first = load(context)
        provideContent {
            // While the widget's session lives, update() only recomposes: a new [Refresh] value
            // (written by [refreshHabitsWidgets]) loads the day again.
            val refresh = currentState(Refresh)
            var state by remember { mutableStateOf(first) }
            LaunchedEffect(refresh) { if (refresh != null) state = load(context) }
            HabitsWidgetTheme { HabitsWidgetContent(state) }
        }
    }

    companion object {
        /** Bumped to load the day again. */
        val Refresh = longPreferencesKey("refresh")

        private suspend fun load(context: Context): WidgetState {
            val entry = context.widgetEntryPoint()
            // A widget may start the app's process: the encrypted session is still being read.
            entry.sessions().state.first { it != SessionState.Loading }
            return when (val outcome = entry.repository().day()) {
                is Outcome.Ok -> WidgetState.Loaded(outcome.value)
                is Outcome.Failed -> WidgetState.Failed(outcome.error)
            }
        }
    }
}

/** Every Hábitos widget loads the day again (after a write here or in the app). */
suspend fun refreshHabitsWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    manager.getGlanceIds(HabitsWidget::class.java).forEach { id ->
        updateAppWidgetState(context, id) { it[HabitsWidget.Refresh] = System.nanoTime() }
        HabitsWidget().update(context, id)
    }
}

class HabitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitsWidget()
}

/** The module's colors (its seed, D24), light and dark. */
@Composable
internal fun HabitsWidgetTheme(content: @Composable () -> Unit) {
    val seed = HabitsFeature.SEED
    GlanceTheme(
        colors = ColorProviders(light = seedColorScheme(seed, dark = false), dark = seedColorScheme(seed, dark = true)),
        content = content,
    )
}

@Composable
internal fun HabitsWidgetContent(state: WidgetState) {
    val context = LocalContext.current
    Column(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Header(state)
        Spacer(GlanceModifier.height(8.dp))
        when (state) {
            is WidgetState.Failed -> Note(context.getString(failureText(state.error)))

            is WidgetState.Loaded -> {
                val open = state.day.timeline + state.day.untimed
                when {
                    open.isNotEmpty() -> LazyColumn(GlanceModifier.fillMaxWidth()) {
                        items(open, itemId = { it.habit.id.hashCode().toLong() }) { item ->
                            ItemRow(item, state.day.on)
                        }
                    }

                    state.day.scheduled > 0 -> Note(context.getString(R.string.habits_widget_all_done))

                    else -> Note(context.getString(R.string.habits_widget_nothing))
                }
            }
        }
    }
}

@Composable
private fun Header(state: WidgetState) {
    val context = LocalContext.current
    val day = (state as? WidgetState.Loaded)?.day
    Row(
        GlanceModifier.fillMaxWidth().clickable(actionStartActivity(checkInIntent(context))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            ImageProvider(PrumoIcons.Checklist),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier.size(20.dp),
        )
        Spacer(GlanceModifier.size(8.dp))
        Text(
            context.getString(R.string.habits_name),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.defaultWeight(),
        )
        if (day != null && day.scheduled > 0) {
            Text(
                context.getString(R.string.habits_today_progress, day.done, day.scheduled),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}

@Composable
private fun ItemRow(item: DayItem, on: LocalDate) {
    val context = LocalContext.current
    val quantity = item.habit.measure == Measure.Quantity
    val detail = listOfNotNull(
        item.habit.timeOfDay?.let(Formats::time),
        context.getString(R.string.habits_late).takeIf { item.late },
        item.value?.takeIf { quantity }?.let {
            context.getString(
                R.string.habits_amount,
                Formats.integer(it.toLong()),
                Formats.integer((item.habit.targetValue ?: 0).toLong()),
                item.habit.unit.orEmpty(),
            ).trim()
        },
    ).joinToString(" · ")
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(GlanceModifier.defaultWeight()) {
            Text(
                item.habit.name,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
                maxLines = 1,
            )
            if (detail.isNotEmpty()) {
                Text(
                    detail,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
        if (quantity) {
            val next = (item.value ?: 0) + TodayViewModel.stepOf(item)
            Image(
                ImageProvider(PrumoIcons.Add),
                contentDescription = context.getString(R.string.habits_increase, item.habit.name),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(40.dp).padding(8.dp).clickable(markAction(item, on, next)),
            )
        } else {
            CheckBox(
                checked = item.done,
                onCheckedChange = markAction(item, on, if (item.done) 0 else 1),
                modifier = GlanceModifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
        modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity(checkInIntent(LocalContext.current))),
    )
}

private fun failureText(error: AppError): Int = when (error) {
    AppError.SessionExpired -> R.string.habits_widget_signed_out
    is AppError.Unreachable -> R.string.habits_widget_unreachable
    else -> R.string.habits_widget_failed
}

/** "Marcar hábitos" over the Home: the app's launch intent with the quick action (as the shortcut). */
private fun checkInIntent(context: Context): Intent {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
    return launch
        .setAction(QuickAction.LAUNCH_ACTION)
        .putExtra(QuickAction.EXTRA_ID, HabitsFeature.CHECK_IN)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}

private fun markAction(item: DayItem, on: LocalDate, value: Int) = actionRunCallback<MarkHabitAction>(
    actionParametersOf(
        MarkHabitAction.HabitId to item.habit.id,
        MarkHabitAction.Day to on.toString(),
        MarkHabitAction.Value to value,
    ),
)

/** Marks (or, with [Value] 0, unmarks) a habit on a day from the widget, then shows the new day. */
class MarkHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[HabitId] ?: return
        val on = parameters[Day]?.let(LocalDate::parse) ?: return
        val value = parameters[Value] ?: 1
        val repository = context.habitsRepository()
        // A write refreshes every widget through HabitsWidgetUpdater; a refused one leaves the day.
        if (value == 0) repository.unmark(id, on) else repository.mark(id, on, value)
    }

    companion object {
        val HabitId = ActionParameters.Key<String>("habit_id")
        val Day = ActionParameters.Key<String>("day")
        val Value = ActionParameters.Key<Int>("value")
    }
}

/** Refreshes the widgets after a write of Hábitos from anywhere in the app. */
fun interface HabitsWidgetUpdater {
    fun refresh()
}

@Singleton
class GlanceHabitsWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) : HabitsWidgetUpdater {
    override fun refresh() {
        scope.launch { refreshHabitsWidgets(context) }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HabitsWidgetEntryPoint {
    fun repository(): HabitsRepository

    fun sessions(): SessionRepository
}

private fun Context.widgetEntryPoint(): HabitsWidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, HabitsWidgetEntryPoint::class.java)

private fun Context.habitsRepository(): HabitsRepository = widgetEntryPoint().repository()
