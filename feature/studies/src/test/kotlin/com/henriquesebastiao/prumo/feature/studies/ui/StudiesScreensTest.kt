package com.henriquesebastiao.prumo.feature.studies.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
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
import com.henriquesebastiao.prumo.feature.studies.StudiesFeature
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The stateless screens of Estudos with the demo server's data (StudiesPreview). */
@Config(application = Application::class, qualifiers = "w412dp-h915dp")
@RunWith(AndroidJUnit4::class)
class StudiesScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(content: @Composable () -> Unit) = compose.setContent {
        PrumoTheme(dynamicColor = false) { ModuleTheme(StudiesFeature.SEED, content) }
    }

    private fun home(tab: StudiesTab, tabs: StudiesTabs, events: StudiesTabEvents = StudiesPreview.events) = show {
        StudiesHomeScreen(
            tab = tab,
            onTab = {},
            tabs = tabs,
            events = events,
            actions = StudiesActions({}, {}),
            onBack = {},
        )
    }

    /** The screen's vertical lazy list (rows of chips scroll sideways). */
    private fun list() = compose.onNode(
        hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
    )

    private val empty = StudiesTabs(TodayUiState(), WeekUiState(), SubjectsUiState(), PlanUiState())

    @Test
    fun todayShowsTheMinutesAndEachSubjectsStatus() {
        var opened = ""
        val events = StudiesTabEvents(
            today = TodayEvents({ opened = it }, {}, {}),
            week = StudiesPreview.events.week,
            subjects = StudiesPreview.events.subjects,
            plan = StudiesPreview.events.plan,
        )
        home(
            StudiesTab.Today,
            StudiesTabs(
                TodayUiState(loading = false, day = StudiesPreview.today),
                WeekUiState(),
                SubjectsUiState(),
                PlanUiState(),
            ),
            events,
        )

        compose.onNodeWithText("30\u00A0min").assertIsDisplayed()
        compose.onNodeWithText("de 1h\u00A015 planejados para hoje").assertIsDisplayed()
        compose.onNodeWithText("às 07:00 · meta 30\u00A0min · 30\u00A0min feitos").assertIsDisplayed()
        compose.onNodeWithText("às 20:00 · meta 45\u00A0min · nada ainda").assertIsDisplayed()
        compose.onNodeWithText("Cumprido").assertIsDisplayed()
        compose.onNodeWithText("Em andamento").assertIsDisplayed()
        compose.onNodeWithContentDescription("Registrar estudo").assertIsDisplayed()
        compose.onNodeWithText("FastAPI").performClick()
        assertEquals("s-api", opened)
    }

    @Test
    fun aDayWithoutPlanInvitesToMakeOne() {
        var planned = 0
        val events = StudiesTabEvents(
            today = TodayEvents({}, { planned++ }, {}),
            week = StudiesPreview.events.week,
            subjects = StudiesPreview.events.subjects,
            plan = StudiesPreview.events.plan,
        )
        val free = StudiesPreview.today.copy(rows = emptyList(), doneMinutes = 0, plannedMinutes = 0, met = null)
        home(StudiesTab.Today, empty.copy(today = TodayUiState(loading = false, day = free)), events)

        compose.onNodeWithText("Nenhum bloco planejado para hoje.").assertIsDisplayed()
        compose.onNodeWithText("Montar plano").performClick()
        assertEquals(1, planned)
    }

    @Test
    fun theWeekShowsItsTotalsTheStripAndThePickedDay() {
        var picked: LocalDate? = null
        val events = StudiesTabEvents(
            today = StudiesPreview.events.today,
            week = WeekEvents({}, {}, { picked = it }, {}, {}),
            subjects = StudiesPreview.events.subjects,
            plan = StudiesPreview.events.plan,
        )
        val state = WeekUiState(loading = false, week = StudiesPreview.week, selected = StudiesPreview.today.on)
        home(StudiesTab.Week, empty.copy(week = state), events)

        compose.onNodeWithText("Esta semana").assertIsDisplayed()
        compose.onNodeWithText("1 de 2").assertIsDisplayed()
        compose.onNodeWithText("2h de 2h\u00A045").assertIsDisplayed()
        compose.onNodeWithText("terça-feira, 17/11").assertIsDisplayed()
        compose.onNodeWithContentDescription("segunda-feira, 16").performClick()
        assertEquals(LocalDate.of(2026, 11, 16), picked)
        compose.onNodeWithText("Por matéria").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theSubjectsAreGroupedByArea() {
        var newSubjectIn = ""
        val events = StudiesTabEvents(
            today = StudiesPreview.events.today,
            week = StudiesPreview.events.week,
            subjects = SubjectsEvents({}, {}, {}, { newSubjectIn = it.id }, { _, _ -> }, {}, {}, {}),
            plan = StudiesPreview.events.plan,
        )
        val state = SubjectsUiState(
            loading = false,
            areas = StudiesPreview.areas,
            subjects = StudiesPreview.subjects,
            overview = StudiesPreview.overview,
            actionError = AppError.Api(409, "studies.area_in_use"),
        )
        home(StudiesTab.Subjects, empty.copy(subjects = state), events)

        compose.onNodeWithText("5 matérias em 3 áreas · 14h\u00A020 este mês").assertIsDisplayed()
        compose.onNodeWithText("A área ainda tem matérias. Mova ou exclua as matérias antes.").assertIsDisplayed()
        compose.onNodeWithText("Faculdade").assertIsDisplayed()
        compose.onNodeWithText("3 tópicos · 1h esta semana").assertIsDisplayed()
        compose.onNodeWithText("Kotlin e Compose").performScrollTo()
        compose.onNodeWithText("sem sessões ainda").assertIsDisplayed()
        list().performScrollToNode(hasContentDescription("Ações de Inglês"))
        compose.onNodeWithContentDescription("Ações de Inglês").performClick()
        compose.onNodeWithText("Nova matéria").performClick()
        assertEquals("a-en", newSubjectIn)
    }

    @Test
    fun theVersionInForceShowsItsDaysAndCannotBeEdited() {
        home(StudiesTab.Plan, empty.copy(plan = PlanUiState(loading = false, plans = StudiesPreview.plans)))

        compose.onNodeWithText("Vigente desde 02/11/2026").assertIsDisplayed()
        compose.onNodeWithText("Começa em 23/11/2026").assertIsDisplayed()
        compose.onNodeWithText(
            "A versão em vigor não muda: mudar o plano cria uma versão a partir de hoje ou de uma data futura.",
        ).assertIsDisplayed()
        compose.onAllNodesWithText("Editar esta versão").assertCountEquals(0)
        compose.onNodeWithText("Segunda-feira").assertIsDisplayed()
        list().performScrollToNode(hasText("Domingo"))
        compose.onAllNodesWithText("Livre").assertCountEquals(2)
    }

    @Test
    fun aFutureVersionCanBeEditedOrDeleted() {
        var edited = ""
        val events = StudiesTabEvents(
            today = StudiesPreview.events.today,
            week = StudiesPreview.events.week,
            subjects = StudiesPreview.events.subjects,
            plan = PlanEvents({}, {}, { edited = it.id }, {}, {}),
        )
        val state = PlanUiState(loading = false, plans = StudiesPreview.plans, selectedId = "p3")
        home(StudiesTab.Plan, empty.copy(plan = state), events)

        compose.onNodeWithText("Editar esta versão").performClick()
        assertEquals("p3", edited)
        compose.onNodeWithText("Excluir").assertIsDisplayed()
    }

    @Test
    fun theSubjectOpensItsNoteAndListsTopicsAndSessions() {
        var opened = ""
        var session = ""
        show {
            SubjectDetailScreen(
                state = SubjectDetailUiState(loading = false, detail = StudiesPreview.detail),
                onBack = {},
                events = SubjectDetailEvents({ opened = it }, {}, {}, {}, {}, { session = it.id }, {}, {}, {}),
                actions = StudiesActions({}),
            )
        }
        compose.onNodeWithText("Abrir no Obsidian").performClick()
        assertEquals("obsidian://open?vault=Estudos&file=Dev%2FFastAPI", opened)
        compose.onNodeWithText("Minutos por semana").assertIsDisplayed()
        compose.onNodeWithText("Pydantic").performScrollTo().assertIsDisplayed()
        list().performScrollToNode(hasText("12/11, 20:00"))
        compose.onNodeWithText("12/11, 20:00").performClick()
        assertEquals("ss1", session)
    }

    @Test
    fun aSubjectWithoutLinkSaysHowToAddIt() {
        val detail = StudiesPreview.detail.copy(subject = StudiesPreview.detail.subject.copy(obsidianUri = null))
        show {
            SubjectDetailScreen(
                state = SubjectDetailUiState(loading = false, detail = detail, noObsidianApp = false),
                onBack = {},
                events = SubjectDetailEvents({}, {}, {}, {}, {}, {}, {}, {}, {}),
                actions = StudiesActions({}),
            )
        }
        compose.onNodeWithText("Sem link do Obsidian. Edite a matéria para adicionar.").assertIsDisplayed()
    }

    @Test
    fun theSessionSheetOffersTheChipsAndANewTopic() {
        var chip: Int? = -1
        show {
            SessionSheetContent(
                state = SessionUiState(
                    loading = false,
                    subjects = listOf(StudiesPreview.fastApi, StudiesPreview.english),
                    subjectId = StudiesPreview.fastApi.id,
                    topics = StudiesPreview.detail.topics,
                ),
                fields = SessionFields("Docker", "", canSave = true),
                events = SessionEvents({}, {}, { chip = it }, {}, {}, {}, {}, {}),
            )
        }
        compose.onNodeWithText("Registrar estudo").assertIsDisplayed()
        compose.onNodeWithText("Padrão: terminei agora.").assertIsDisplayed()
        compose.onNodeWithText("45\u00A0min").performClick()
        assertEquals(45, chip)
        compose.onNodeWithText("Outra").performClick()
        assertEquals(null, chip)
    }

    @Test
    fun withoutSubjectsTheSheetSaysToCreateOne() {
        show {
            SessionSheetContent(
                state = SessionUiState(loading = false),
                fields = SessionFields("", "", canSave = false),
                events = SessionEvents({}, {}, {}, {}, {}, {}, {}, {}),
            )
        }
        compose.onNodeWithText("Crie uma matéria antes de registrar estudo.").assertIsDisplayed()
    }

    @Test
    fun aBadLinkIsShownUnderItsField() {
        show {
            SubjectSheetContent(
                FormUiState(
                    areas = StudiesPreview.areas,
                    areaId = "a-dev",
                    saveError = AppError.Api(422, "studies.invalid_obsidian_uri"),
                ),
                SubjectFields("FastAPI", "https://x.dev"),
                SubjectFormEvents({}, {}, {}, {}, {}),
            )
        }
        compose.onNodeWithText("Use um link do Obsidian, que começa com obsidian://.").assertIsDisplayed()
    }

    @Test
    fun aBlockWithoutDaysCannotBeSaved() {
        show {
            PlanEditorScreen(
                state = PlanEditorUiState(
                    loading = false,
                    subjects = listOf(StudiesPreview.english),
                    today = LocalDate.of(2026, 11, 17),
                    validFrom = LocalDate.of(2026, 11, 23),
                ),
                blocks = listOf(BlockDraft(1, StudiesPreview.english.id, duration = "30")),
                canSave = false,
                events = PlanEditorEvents({}, {}, {}, { _, _ -> }, {}, {}),
                onClose = {},
            )
        }
        compose.onNodeWithText("Vale a partir de").assertIsDisplayed()
        compose.onNodeWithContentDescription("segunda-feira").assertIsDisplayed()
        compose.onNodeWithText("Salvar plano").performScrollTo().assertIsNotEnabled()
    }
}

/** At 360 dp with the largest font the app supports, the values still fit (D23). */
@Config(application = Application::class, qualifiers = "w360dp-h740dp", fontScale = 1.3f)
@RunWith(AndroidJUnit4::class)
class StudiesSmallScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theWeekFitsAtLargeFont() {
        compose.setContent {
            PrumoTheme(dynamicColor = false) {
                ModuleTheme(StudiesFeature.SEED) {
                    StudiesHomeScreen(
                        tab = StudiesTab.Week,
                        onTab = {},
                        tabs = StudiesTabs(
                            TodayUiState(),
                            WeekUiState(
                                loading = false,
                                week = StudiesPreview.week,
                                selected = StudiesPreview.today.on,
                            ),
                            SubjectsUiState(),
                            PlanUiState(),
                        ),
                        events = StudiesPreview.events,
                        actions = StudiesActions({}, {}),
                        onBack = {},
                    )
                }
            }
        }
        compose.onNodeWithText("2h de 2h\u00A045").assertIsDisplayed()
        compose.onNodeWithContentDescription("domingo, 22").assertIsDisplayed()
    }
}
