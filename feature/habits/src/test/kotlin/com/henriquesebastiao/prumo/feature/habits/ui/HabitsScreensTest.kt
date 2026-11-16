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
import com.henriquesebastiao.prumo.feature.habits.model.DayItem
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyInput
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyKind
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun todayShowsTheSectionsOfTheDayWithTheirControls() {
        var toggled: DayItem? = null
        var stepped: Pair<DayItem, Boolean>? = null
        var day: LocalDate? = LocalDate.MIN
        val events = TodayEvents({ toggled = it }, { item, up -> stepped = item to up }, {}, {}, { day = it })
        home(HabitsTab.Today, today = TodayUiState(loading = false, day = HabitsPreview.day), events = events)

        compose.onNodeWithText("1 de 5 hoje").assertIsDisplayed()
        compose.onNodeWithText("Redes sociais antes de dormir · 8 dias sem").assertIsDisplayed()
        compose.onNodeWithText("Com horário").assertIsDisplayed()
        compose.onNodeWithText("passou do horário").assertIsDisplayed()
        compose.onNodeWithText("agora · 14:30").assertIsDisplayed()
        compose.onNodeWithContentDescription("Dia anterior").performClick()
        assertEquals(LocalDate.of(2026, 11, 16), day)
        list().performScrollToNode(hasText("Anki"))
        compose.onNodeWithText("Anki").performClick()
        assertEquals("Anki", toggled?.habit?.name)
        compose.onNodeWithText("0 de 1 neste mês · faltam 14 dias · dia preferido").assertExists()
        list().performScrollToNode(hasText("Neste período · 1"))
        compose.onNodeWithText("Neste período · 1").performClick()
        list().performScrollToNode(hasText("1 de 3 nesta semana · faltam 6 dias"))
        compose.onNodeWithContentDescription("Mais Beber água").performClick()
        assertEquals("Beber água" to true, stepped?.let { it.first.habit.name to it.second })
        compose.onNodeWithContentDescription("Marcar hábitos").assertIsDisplayed()
    }

    @Test
    fun aMarkCanBeUndoneFromTheSnackbar() {
        var undone = false
        val undo = UndoRequest("h-anki", "Anki", LocalDate.of(2026, 11, 17))
        show {
            HabitsHomeScreen(
                tab = HabitsTab.Today,
                onTab = {},
                today = TodayUiState(loading = false, day = HabitsPreview.day, undo = undo),
                list = ListUiState(),
                todayEvents = HabitsPreview.todayEvents,
                listEvents = HabitsPreview.listEvents,
                actions = HabitsActions({}, onNewHabit = {}),
                onBack = {},
                onUndo = { undone = true },
            )
        }
        compose.onNodeWithText("Anki marcado").assertIsDisplayed()
        compose.onNodeWithText("Desfazer").performClick()
        compose.waitForIdle()
        assertTrue(undone)
    }

    @Test
    fun theCheckInSheetSearchesWithoutAccents() {
        show {
            CheckInContent(
                state = TodayUiState(loading = false, day = HabitsPreview.day),
                query = "AGUA",
                onQuery = {},
                events = HabitsPreview.todayEvents,
            )
        }
        compose.onNodeWithText("Beber água").assertIsDisplayed()
        compose.onNodeWithText("Anki").assertDoesNotExist()
    }

    @Test
    fun aRefusedMarkIsShownOnToday() {
        home(
            HabitsTab.Today,
            today = TodayUiState(
                loading = false,
                day = HabitsPreview.day,
                actionError = AppError.Api(422, "habits.future_date"),
            ),
        )
        compose.onNodeWithText("Essa data ainda não chegou.").assertIsDisplayed()
    }

    @Test
    fun theListShowsFrequencyRateStreakNextDayAndTimeWithout() {
        var opened = ""
        home(
            HabitsTab.List,
            listState = ListUiState(loading = false, habits = HabitsPreview.habits),
            listEvents = ListEvents({ opened = it.id }, {}, {}, {}),
        )
        compose.onNodeWithText("Para praticar").assertIsDisplayed()
        compose.onNodeWithText("05:15").assertIsDisplayed()
        compose.onNodeWithText(
            "Todo dia · 100% cumprido · sequência de 41\u00A0dias · próxima: qua, 18/11",
        ).assertIsDisplayed()
        list().performScrollToNode(hasText("Trocar roupa de cama"))
        compose.onNodeWithText(
            "Quinzenal · sábado · 100% cumprido · sequência de 3\u00A0vezes · próxima: sáb, 21/11",
        ).assertIsDisplayed()
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
                SheetUiState(rule = FrequencyInput(kind = FrequencyKind.Weekdays, weekdays = emptySet())),
                FrequencyEvents({}, {}, {}),
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
        compose.onAllNodesWithText("41 dias")[0].assertIsDisplayed()
    }
}
