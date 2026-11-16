package com.henriquesebastiao.prumo.feature.habits.ui

import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.testing.FakeAccountRepository
import com.henriquesebastiao.prumo.core.testing.MainDispatcherRule
import com.henriquesebastiao.prumo.feature.habits.model.ArchivedFilter
import com.henriquesebastiao.prumo.feature.habits.model.Cadence
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyInput
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyKind
import com.henriquesebastiao.prumo.feature.habits.model.HabitColor
import com.henriquesebastiao.prumo.feature.habits.model.HabitInput
import com.henriquesebastiao.prumo.feature.habits.model.HabitKind
import com.henriquesebastiao.prumo.feature.habits.model.Measure
import com.henriquesebastiao.prumo.feature.habits.model.Period
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModelsTest {
    @get:Rule
    val main = MainDispatcherRule()

    // 17/11/2026 at 14:30 in São Paulo, as the demo server's clock.
    private val clock = Clock.fixed(Instant.parse("2026-11-17T17:30:00Z"), ZoneOffset.UTC)
    private val account = FakeAccountRepository()
    private val repository = FakeHabitsRepository()
    private val day = LocalDate.of(2026, 11, 17)

    @Test
    fun todayMarksAndUnmarksByTheServersDayAndOffersToUndo() = runTest {
        val vm = TodayViewModel(repository)
        val anki = vm.state.value.day!!.all.first { it.habit.name == "Anki" }
        vm.toggle(anki)
        assertEquals("mark h-anki $day 1", repository.sent.last())
        assertEquals(UndoRequest("h-anki", "Anki", day), vm.state.value.undo)
        vm.undo()
        assertEquals("unmark h-anki $day", repository.sent.last())
        assertEquals(null, vm.state.value.undo)
        val bible = vm.state.value.day!!.doneItems.single()
        vm.toggle(bible)
        assertEquals("unmark h-bible $day", repository.sent.last())
        // Unmarking has nothing to undo.
        assertEquals(null, vm.state.value.undo)
    }

    @Test
    fun anEarlierDayAsksTheServerAndTodayGoesBack() = runTest {
        val vm = TodayViewModel(repository)
        vm.onDay(day.minusDays(1))
        assertEquals(day.minusDays(1), repository.days.last())
        // A day from today on is today again.
        vm.onDay(day.plusDays(3))
        assertEquals(null, repository.days.last())
        assertEquals(null, vm.state.value.selected)
    }

    @Test
    fun theHabitsWithoutATimeAreReordered() = runTest {
        val vm = TodayViewModel(repository)
        vm.reorder(listOf("h-water", "h-anki", "h-chore"))
        assertEquals("order [h-water, h-anki, h-chore]", repository.sent.last())
    }

    @Test
    fun aQuantityIsSentOnceAfterARunOfSteps() = runTest {
        val vm = TodayViewModel(repository)
        val water = vm.state.value.day!!.all.first { it.habit.name == "Beber água" }
        vm.step(water, up = true)
        vm.step(water, up = true)
        assertEquals(1750, vm.state.value.drafts[water.habit.id])
        assertTrue(repository.sent.isEmpty())
        advanceTimeBy(600)
        assertEquals(listOf<Any>("mark h-water $day 1750"), repository.sent)
        assertFalse(water.habit.id in vm.state.value.drafts)
    }

    @Test
    fun aRefusedMarkIsShown() = runTest {
        repository.failure = AppError.Api(422, "habits.before_start")
        val vm = TodayViewModel(repository)
        vm.toggle(vm.state.value.day!!.all.first())
        assertEquals("habits.before_start", (vm.state.value.actionError as AppError.Api).code)
        vm.onActionErrorShown()
        assertEquals(null, vm.state.value.actionError)
    }

    @Test
    fun theListSearchesFiltersAndShowsTheArchivedOnDemand() = runTest {
        val vm = ListViewModel(repository)
        assertEquals(8, vm.state.value.shown("").size)
        assertEquals(listOf("Beber água"), vm.state.value.shown(" AGUA").map { it.name })
        vm.onKind(HabitKind.Quit)
        assertEquals(listOf("Redes sociais antes de dormir"), vm.state.value.shown("").map { it.name })
        vm.onKind(null)
        vm.onCadence(Cadence.Fortnightly)
        assertEquals(listOf("Trocar roupa de cama"), vm.state.value.shown("").map { it.name })
        vm.onQuery("anki")
        assertEquals("anki", vm.query)
        vm.reorder(listOf("h-anki", "h-water"))
        assertEquals("order [h-anki, h-water]", repository.sent.last())
        vm.toggleArchived()
        assertEquals(ArchivedFilter.All, repository.filters.last())
        vm.pull()
        assertEquals(1, repository.expired)
    }

    @Test
    fun aHabitLoadsItsMonthAndMarksAPastDay() = runTest {
        val vm = HabitDetailViewModel(HabitsPreview.bible.id, repository, account, clock)
        assertEquals(HabitsPreview.calendar, vm.state.value.calendar)
        vm.toggleDay(LocalDate.of(2026, 11, 3))
        assertEquals("unmark h-bible 2026-11-03", repository.sent.last())
        vm.toggleDay(day)
        assertEquals("mark h-bible $day 1", repository.sent.last())
        vm.showMonth(YearMonth.of(2026, 10))
        assertEquals(YearMonth.of(2026, 10), repository.months.last())
        vm.endPause(HabitsPreview.bibleDetail.pauses.first().copy(endsOn = null))
        assertEquals("update pause p1 2026-10-20 $day", repository.sent.last())
    }

    @Test
    fun aHabitToQuitHasNoCalendarAndCanBeDeleted() = runTest {
        val vm = HabitDetailViewModel(HabitsPreview.social.id, repository, account, clock)
        assertEquals(null, vm.state.value.calendar)
        assertTrue(repository.months.isEmpty())
        vm.delete()
        assertTrue(vm.state.value.gone)
    }

    @Test
    fun aNewHabitSendsItsKindMeasureAndFrequency() = runTest {
        val vm = HabitEditorViewModel(null, repository, account, clock)
        assertFalse(vm.canSave)
        vm.onNameChange(" Beber água ")
        vm.onMeasure(Measure.Quantity)
        assertFalse(vm.canSave)
        vm.onTargetChange("2000")
        vm.onUnitChange("ml")
        vm.onIcon("droplet")
        vm.onColor(HabitColor.Cyan)
        vm.onFrequency(FrequencyInput(kind = FrequencyKind.MonthDays, monthDays = emptySet()))
        assertFalse(vm.canSave)
        val tank = FrequencyInput(kind = FrequencyKind.MonthDays, monthDays = setOf(5), everyNMonths = 3)
        vm.onFrequency(tank)
        vm.onTimeOfDay(LocalTime.of(18, 0))
        assertTrue(vm.canSave)
        vm.save()
        val input = repository.sent.single() as HabitInput
        assertEquals("Beber água", input.name)
        assertEquals(2000, input.targetValue)
        assertEquals("ml", input.unit)
        assertEquals(tank, input.frequency)
        assertEquals(LocalTime.of(18, 0), input.timeOfDay)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun aHabitToQuitLeavesTheMeasureOut() = runTest {
        val vm = HabitEditorViewModel(null, repository, account, clock)
        vm.onKind(HabitKind.Quit)
        vm.onNameChange("Doce")
        vm.save()
        val input = repository.sent.single() as HabitInput
        assertEquals(HabitKind.Quit, input.kind)
        assertEquals(null, input.measure)
        assertEquals(null, input.frequency)
    }

    @Test
    fun editingKeepsTheKindAndSendsTheQuantityFlag() = runTest {
        repository.details =
            repository.details + (HabitsPreview.water.id to HabitsPreview.bibleDetail.copy(habit = HabitsPreview.water))
        val vm = HabitEditorViewModel(HabitsPreview.water.id, repository, account, clock)
        assertEquals("Beber água", vm.name)
        assertEquals("2000", vm.target)
        vm.onTargetChange("2500")
        vm.save()
        val (id, input, quantity) = repository.sent.single() as Triple<*, *, *>
        assertEquals(HabitsPreview.water.id, id)
        assertEquals(2500, (input as HabitInput).targetValue)
        assertEquals(true, quantity)
    }

    @Test
    fun theSheetsSendFrequencyPauseRelapseAndADaysValue() = runTest {
        val vm = HabitSheetViewModel(repository, account, clock)
        vm.start(1, HabitsPreview.bible.id)
        vm.onRule(FrequencyInput(kind = FrequencyKind.Weekdays, weekdays = DayOfWeek.entries.drop(1).take(4).toSet()))
        vm.onValidFrom(LocalDate.of(2026, 11, 23))
        vm.saveFrequency()
        val (habitId, from, frequency) = repository.sent.last() as Triple<*, *, *>
        assertEquals(HabitsPreview.bible.id, habitId)
        assertEquals(LocalDate.of(2026, 11, 23), from)
        assertEquals(DayOfWeek.entries.drop(1).take(4).toSet(), (frequency as FrequencyInput).weekdays.toSet())

        val relapse = HabitSheetViewModel(repository, account, clock)
        relapse.start(2, HabitsPreview.social.id)
        relapse.onOccurredAt(LocalDateTime.of(2026, 11, 16, 23, 30))
        relapse.onNote("  Insônia ")
        relapse.saveRelapse()
        assertEquals("relapse h-social 2026-11-17T02:30:00Z Insônia", repository.sent.last())

        val entry = HabitSheetViewModel(repository, account, clock)
        entry.start(3, HabitsPreview.bible.id, day = LocalDate.of(2026, 11, 10), dayValue = 1500)
        assertEquals("1500", entry.value)
        entry.onValue("2000")
        entry.saveEntry()
        assertEquals("mark h-bible 2026-11-10 2000", repository.sent.last())
        entry.clearEntry()
        assertEquals("unmark h-bible 2026-11-10", repository.sent.last())
    }

    @Test
    fun aFutureVersionLoadsForItsEdit() = runTest {
        val fortnight = FrequencyInput(kind = FrequencyKind.PerPeriod, times = 4, period = Period.Fortnight)
        val future = HabitsPreview.bible.frequency!!.copy(
            id = "f-next",
            validFrom = LocalDate.of(2026, 11, 23),
            rule = fortnight,
        )
        repository.details =
            mapOf(HabitsPreview.bible.id to HabitsPreview.bibleDetail.copy(frequencies = listOf(future)))
        val vm = HabitSheetViewModel(repository, account, clock)
        vm.start(1, HabitsPreview.bible.id, frequencyId = "f-next")
        assertEquals(fortnight, vm.state.value.rule)
        vm.saveFrequency()
        assertEquals("f-next", (repository.sent.last() as Pair<*, *>).first)
    }
}
