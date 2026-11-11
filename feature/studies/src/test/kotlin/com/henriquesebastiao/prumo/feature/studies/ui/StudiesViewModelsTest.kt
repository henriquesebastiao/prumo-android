package com.henriquesebastiao.prumo.feature.studies.ui

import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.testing.FakeAccountRepository
import com.henriquesebastiao.prumo.core.testing.MainDispatcherRule
import com.henriquesebastiao.prumo.feature.studies.model.ArchivedFilter
import com.henriquesebastiao.prumo.feature.studies.model.AreaInput
import com.henriquesebastiao.prumo.feature.studies.model.BlockInput
import com.henriquesebastiao.prumo.feature.studies.model.SessionInput
import com.henriquesebastiao.prumo.feature.studies.model.StudyColor
import com.henriquesebastiao.prumo.feature.studies.model.SubjectInput
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StudiesViewModelsTest {
    @get:Rule
    val main = MainDispatcherRule()

    // 17/11/2026 at 14:30 in São Paulo, as the demo server's clock.
    private val clock = Clock.fixed(Instant.parse("2026-11-17T17:30:00Z"), ZoneOffset.UTC)
    private val account = FakeAccountRepository()
    private val repository = FakeStudiesRepository()

    @Test
    fun todayShowsTheServersDayAndPullingAsksTheServer() = runTest {
        val vm = TodayViewModel(repository)
        assertEquals(StudiesPreview.today, vm.state.value.day)

        vm.pull()
        assertEquals(1, repository.expired)
        assertFalse(vm.state.value.refreshing)
    }

    @Test
    fun aWriteLoadsTodayAgain() = runTest {
        val vm = TodayViewModel(repository)
        repository.day = StudiesPreview.today.copy(doneMinutes = 75, met = true)
        repository.createArea(AreaInput("Música", StudyColor.Pink))
        assertEquals(true, vm.state.value.day?.met)
    }

    @Test
    fun theWeekPicksTodayAndMovesByTheServersNeighbours() = runTest {
        val vm = WeekViewModel(repository)
        assertEquals(StudiesPreview.today.on, vm.state.value.selected)
        assertNull(repository.weeks.single())

        vm.select(LocalDate.of(2026, 11, 16))
        assertEquals(LocalDate.of(2026, 11, 16), vm.state.value.selectedDay?.on)

        vm.previous()
        assertEquals(StudiesPreview.week.previousStart, repository.weeks.last())
        vm.next()
        assertEquals(StudiesPreview.week.nextStart, repository.weeks.last())
    }

    @Test
    fun withoutANextWeekNextDoesNothing() = runTest {
        repository.week = StudiesPreview.week.copy(nextStart = null)
        val vm = WeekViewModel(repository)
        vm.next()
        assertEquals(1, repository.weeks.size)
    }

    @Test
    fun theSubjectsShowTheArchivedOnDemandAndARefusedDelete() = runTest {
        val vm = SubjectsViewModel(repository, account, clock)
        assertEquals(3, vm.state.value.areas?.size)
        assertEquals(2, vm.state.value.subjectsOf(StudiesPreview.areas[0]).size)
        assertEquals(ArchivedFilter.Active, repository.filters.last())

        vm.toggleArchived()
        assertEquals(ArchivedFilter.All, repository.filters.last())

        val inUse = AppError.Api(409, "studies.area_in_use", "")
        repository.failure = inUse
        vm.delete(StudiesPreview.areas[0])
        assertEquals(inUse, vm.state.value.actionError)
    }

    @Test
    fun thePlanShowsTheVersionInForceWhichDoesNotChange() = runTest {
        val vm = PlanViewModel(repository)
        assertEquals("p2", vm.state.value.plan?.id)
        assertFalse(vm.state.value.editable)

        vm.select("p3")
        assertTrue(vm.state.value.editable)
        vm.delete(vm.state.value.plan!!)
        assertEquals("delete plan p3", repository.sent.last())
    }

    @Test
    fun aNewVersionStartsNextMondayFromTheBlocksInForce() = runTest {
        val vm = PlanEditorViewModel(null, repository)
        assertEquals(LocalDate.of(2026, 11, 23), vm.state.value.validFrom)
        assertEquals(3, vm.blocks.size)
        assertTrue(vm.canSave)

        vm.changeBlock(vm.blocks[0].key) { it.copy(weekdays = emptySet()) }
        assertFalse(vm.canSave)
        vm.removeBlock(vm.blocks[0].key)
        vm.addBlock("s-api")
        val added = vm.blocks.last().key
        vm.changeBlock(added) { it.copy(weekdays = setOf(DayOfWeek.SATURDAY), duration = "90") }
        vm.save()

        @Suppress("UNCHECKED_CAST")
            val (from, blocks) = repository.sent.single() as Pair<LocalDate, List<BlockInput>>
        assertEquals(LocalDate.of(2026, 11, 23), from)
        assertEquals(BlockInput("s-api", listOf(DayOfWeek.SATURDAY), null, 90), blocks.last())
        assertTrue(vm.state.value.done)
    }

    @Test
    fun editingAVersionKeepsItsDayAndSendsOnlyTheBlocks() = runTest {
        val vm = PlanEditorViewModel("p3", repository)
        assertEquals(LocalDate.of(2026, 11, 23), vm.state.value.validFrom)
        assertEquals(2, vm.blocks.size)
        vm.save()
        assertEquals("p3", (repository.sent.single() as Pair<*, *>).first)
    }

    @Test
    fun aSessionEndsNowByDefaultAndFollowsTheDuration() = runTest {
        repository.lastSubjectId = "s-api"
        val vm = SessionViewModel(repository, account, clock)
        vm.start(1, null, null)

        assertEquals("s-api", vm.state.value.subjectId)
        assertEquals(3, vm.state.value.topics.size)
        // 14:30 in São Paulo less the 30 minutes of the default chip.
        assertEquals(LocalDateTime.of(2026, 11, 17, 14, 0), vm.state.value.startedAt)
        vm.selectChip(45)
        assertEquals(LocalDateTime.of(2026, 11, 17, 13, 45), vm.state.value.startedAt)

        vm.onTopicChange("  pydantic ")
        vm.save()
        val sent = repository.sent.single() as SessionInput
        assertEquals("t2", sent.topicId)
        assertEquals(45, sent.durationMinutes)
        assertEquals(Instant.parse("2026-11-17T16:45:00Z"), sent.startedAt)
        assertTrue(vm.state.value.done)
        assertEquals("s-api", repository.lastSubjectId)
    }

    @Test
    fun aNewTopicGoesByNameAndAPickedStartStays() = runTest {
        val vm = SessionViewModel(repository, account, clock)
        vm.start(1, "s-en", null)
        assertEquals("s-en", vm.state.value.subjectId)

        vm.onStartChange(LocalDateTime.of(2026, 11, 17, 7, 0))
        vm.selectChip(null)
        assertFalse(vm.canSave)
        vm.onCustomMinutesChange("50")
        assertEquals(LocalDateTime.of(2026, 11, 17, 7, 0), vm.state.value.startedAt)
        vm.onTopicChange("Phrasal verbs")
        vm.save()

        val sent = repository.sent.single() as SessionInput
        assertNull(sent.topicId)
        assertEquals("Phrasal verbs", sent.topicName)
        assertEquals(50, sent.durationMinutes)
        assertEquals(Instant.parse("2026-11-17T10:00:00Z"), sent.startedAt)
    }

    @Test
    fun editingASessionLoadsItAndCanDeleteIt() = runTest {
        val vm = SessionViewModel(repository, account, clock)
        vm.start(1, null, "ss1")

        assertEquals("Testes", vm.topic)
        assertEquals(45, vm.state.value.chip)
        assertTrue(vm.state.value.startPicked)
        vm.delete()
        assertEquals("delete session ss1", repository.sent.single())
    }

    @Test
    fun aRefusedSessionStaysOpenWithTheError() = runTest {
        val archived = AppError.Api(422, "studies.subject_archived", "")
        repository.failure = archived
        val vm = SessionViewModel(repository, account, clock)
        vm.start(1, null, null)
        vm.save()
        assertEquals(archived, vm.state.value.saveError)
        assertFalse(vm.state.value.done)
    }

    @Test
    fun theSubjectSaysWhenNoAppOpensItsNote() = runTest {
        val vm = SubjectDetailViewModel("s-api", repository, account, clock)
        assertEquals("FastAPI", vm.state.value.detail?.subject?.name)

        vm.onNoObsidianApp()
        assertTrue(vm.state.value.noObsidianApp)
        vm.onNoticeShown()
        assertFalse(vm.state.value.noObsidianApp)

        vm.delete()
        assertTrue(vm.state.value.gone)
    }

    @Test
    fun theFormsSendWhatWasTyped() = runTest {
        val area = AreaViewModel(repository)
        area.start(1, null)
        area.onNameChange(" Música ")
        area.onColorChange(StudyColor.Pink)
        area.save()
        assertEquals(AreaInput("Música", StudyColor.Pink), repository.sent.last())

        val subject = SubjectFormViewModel(repository)
        subject.start(1, null, "a-dev")
        assertEquals("a-dev", subject.state.value.areaId)
        subject.onNameChange("Docker")
        subject.onUriChange("  ")
        subject.save()
        assertEquals(SubjectInput("a-dev", "Docker", null), repository.sent.last())

        val edit = SubjectFormViewModel(repository)
        edit.start(2, "s-api", null)
        assertEquals("FastAPI", edit.name)
        assertEquals("obsidian://open?vault=Estudos&file=Dev%2FFastAPI", edit.uri)
    }
}
