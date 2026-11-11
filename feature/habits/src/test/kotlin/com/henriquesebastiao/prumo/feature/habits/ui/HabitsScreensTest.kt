package com.henriquesebastiao.prumo.feature.habits.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.prumo.core.designsystem.theme.ModuleTheme
import com.henriquesebastiao.prumo.core.designsystem.theme.PrumoTheme
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.feature.habits.HabitsFeature
import com.henriquesebastiao.prumo.feature.habits.model.TodayItem
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The stateless screens of Hábitos with the demo server's data (HabitsPreview). */
@Config(application = Application::class, qualifiers = "w412dp-h915dp")
@RunWith(AndroidJUnit4::class)
class HabitsScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(content: @Composable () -> Unit) = compose.setContent {
        PrumoTheme(dynamicColor = false) { ModuleTheme(HabitsFeature.SEED, content) }
    }

    private fun list() = compose.onNode(
        hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
    )

    private fun home(
        tab: HabitsTab,
        today: TodayUiState = TodayUiState(),
        listState: ListUiState = ListUiState(),
        events: TodayEvents = HabitsPreview.todayEvents,
        listEvents: ListEvents = HabitsPreview.listEvents,
    ) = show {
        HabitsHomeScreen(
            tab = tab,
            onTab = {},
            today = today,
            list = listState,
            todayEvents = events,
            listEvents = listEvents,
            actions = HabitsActions({}, onNewHabit = {}),
            onBack = {},
        )
    }

    @Test
    fun todayShowsEachHabitWithItsControl() {
        var toggled: TodayItem? = null
        var stepped: Pair<TodayItem, Boolean>? = null
        val events = TodayEvents({ toggled = it }, { item, up -> stepped = item to up }, {}, {})
        home(HabitsTab.Today, today = TodayUiState(loading = false, today = HabitsPreview.today), events = events)

        compose.onNodeWithText("1 de 4 hoje").assertIsDisplayed()
        compose.onNodeWithText("1 de 3 esta semana").assertIsDisplayed()
        compose.onNodeWithText("1.250\u00A0/\u00A02.000\u00A0ml").assertIsDisplayed()
        compose.onNodeWithText("Anki").performClick()
        assertEquals("Anki", toggled?.habit?.name)
        compose.onNodeWithContentDescription("Mais Beber água").performClick()
        assertEquals("Beber água" to true, stepped?.let { it.first.habit.name to it.second })
        compose.onNodeWithContentDescription("Marcar hábitos").assertIsDisplayed()
    }

    @Test
    fun aRefusedMarkIsShownOnToday() {
        home(
            HabitsTab.Today,
            today = TodayUiState(
                loading = false,
                today = HabitsPreview.today,
                actionError = AppError.Api(422, "habits.future_date"),
            ),
        )
        compose.onNodeWithText("Essa data ainda não chegou.").assertIsDisplayed()
    }

    @Test
    fun theListShowsRateStreakAndTimeWithout() {
        var opened = ""
        home(
            HabitsTab.List,
            listState = ListUiState(loading = false, habits = HabitsPreview.habits),
            listEvents = ListEvents({ opened = it.id }, {}, {}, {}),
        )
        compose.onNodeWithText("Para praticar").assertIsDisplayed()
        compose.onNodeWithText("Todo dia · 100% cumprido · sequência de 40 dias").assertIsDisplayed()
        compose.onNodeWithText("3× por semana · 75% cumprido · sequência de 3 semanas").assertIsDisplayed()
        list().performScrollToNode(hasText("Para abandonar"))
        compose.onNodeWithText("8 dias sem · 1 recaída em 30 dias").assertIsDisplayed()
        compose.onNodeWithText("Redes sociais antes de dormir").performClick()
        assertEquals("h-social", opened)
    }

    @Test
    fun aHabitShowsItsCalendarAndAPastDayCanBeTapped() {
        var day: LocalDate? = null
        val events = HabitsPreview.detailEvents.let { base ->
            HabitDetailEvents(
                base.onEdit, base.onArchive, base.onDelete, base.onMonth, { day = it }, base.onChangeFrequency,
                base.onDeleteFrequency, base.onPause, base.onEndPause, base.onDeletePause, base.onDeleteRelapse,
                base.onRetry, base.onRefresh, base.onActionErrorShown,
            )
        }
        show {
            HabitDetailScreen(
                state = HabitDetailUiState(
                    loading = false,
                    detail = HabitsPreview.bibleDetail,
                    calendar = HabitsPreview.calendar,
                ),
                onBack = {},
                events = events,
                onRelapse = {},
            )
        }
        compose.onNodeWithText("Ler a Bíblia").assertIsDisplayed()
        compose.onNodeWithText("100%").assertIsDisplayed()
        compose.onNodeWithText("Novembro de 2026").assertIsDisplayed()
        compose.onNodeWithContentDescription("segunda-feira, 16").performClick()
        assertEquals(LocalDate.of(2026, 11, 16), day)
        list().performScrollToNode(hasText("20/10/2026 a 24/10/2026"))
        compose.onNodeWithText("20/10/2026 a 24/10/2026").assertIsDisplayed()
    }

    @Test
    fun aHabitToQuitShowsTheTimeWithoutAndTheRelapses() {
        show {
            HabitDetailScreen(
                state = HabitDetailUiState(loading = false, detail = HabitsPreview.quitDetail),
                onBack = {},
                events = HabitsPreview.detailEvents,
                onRelapse = {},
            )
        }
        compose.onNodeWithText("8 dias").assertIsDisplayed()
        compose.onNodeWithText("42 dias").assertIsDisplayed()
        compose.onNodeWithContentDescription("Registrar recaída").assertIsDisplayed()
        list().performScrollToNode(hasText("Domingo entediado."))
        compose.onNodeWithText("Domingo entediado.").assertIsDisplayed()
    }

    @Test
    fun theEditorAsksAQuantitysTargetAndUnit() {
        show {
            HabitEditorScreen(
                state = HabitEditorUiState(measure = com.henriquesebastiao.prumo.feature.habits.model.Measure.Quantity),
                fields = HabitEditorFields("Beber água", "", "", canSave = false),
                events = HabitsPreview.editorEvents,
                onClose = {},
            )
        }
        compose.onNodeWithText("Meta por dia").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Unidade").assertIsDisplayed()
        compose.onNodeWithText("Salvar").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun theFrequencySheetAsksTheDaysAndTheStart() {
        show {
            FrequencyContent(
                SheetUiState(
                    kind = com.henriquesebastiao.prumo.feature.habits.model.FrequencyKind.Weekdays,
                    weekdays = emptySet(),
                ),
                FrequencyEvents({}, {}, {}, {}, {}),
            )
        }
        compose.onNodeWithText("Nova versão da frequência").assertIsDisplayed()
        compose.onNodeWithContentDescription("segunda-feira").assertIsDisplayed()
        compose.onNodeWithText("Vale a partir de").assertIsDisplayed()
        compose.onNodeWithText("Salvar").assertIsNotEnabled()
    }
}

/** At 360 dp with the largest font the app supports, today and the calendar still fit (D23). */
@Config(application = Application::class, qualifiers = "w360dp-h740dp", fontScale = 1.3f)
@RunWith(AndroidJUnit4::class)
class HabitsSmallScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theCalendarFitsAtLargeFont() {
        compose.setContent {
            PrumoTheme(dynamicColor = false) {
                ModuleTheme(HabitsFeature.SEED) {
                    HabitDetailScreen(
                        state = HabitDetailUiState(
                            loading = false,
                            detail = HabitsPreview.bibleDetail,
                            calendar = HabitsPreview.calendar,
                        ),
                        onBack = {},
                        events = HabitsPreview.detailEvents,
                        onRelapse = {},
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("domingo, 29").assertIsDisplayed()
        compose.onAllNodesWithText("40 dias")[0].assertIsDisplayed()
    }
}
