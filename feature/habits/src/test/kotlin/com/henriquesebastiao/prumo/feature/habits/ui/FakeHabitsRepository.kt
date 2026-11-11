package com.henriquesebastiao.prumo.feature.habits.ui

import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.feature.habits.data.HabitsRepository
import com.henriquesebastiao.prumo.feature.habits.model.ArchivedFilter
import com.henriquesebastiao.prumo.feature.habits.model.CalendarMonth
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyInput
import com.henriquesebastiao.prumo.feature.habits.model.Habit
import com.henriquesebastiao.prumo.feature.habits.model.HabitDetail
import com.henriquesebastiao.prumo.feature.habits.model.HabitInput
import com.henriquesebastiao.prumo.feature.habits.model.HabitsToday
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Hábitos in memory, with the demo server's data (HabitsPreview). Nothing is computed: reads answer
 * what they were given. [failure] makes the next writes fail; what was sent is kept in [sent].
 */
@Suppress("TooManyFunctions")
class FakeHabitsRepository : HabitsRepository {
    private val mutableChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    override val changes = mutableChanges

    var expired = 0

    override fun expireCache() {
        expired++
    }

    var today: HabitsToday = HabitsPreview.today
    var habits: List<Habit> = HabitsPreview.habits
    var details: Map<String, HabitDetail> = mapOf(
        HabitsPreview.bible.id to HabitsPreview.bibleDetail,
        HabitsPreview.social.id to HabitsPreview.quitDetail,
    )
    var calendar: CalendarMonth = HabitsPreview.calendar

    var failure: AppError? = null
    val sent = mutableListOf<Any>()
    val filters = mutableListOf<ArchivedFilter>()
    val months = mutableListOf<YearMonth?>()

    private fun <T> read(value: () -> T): Outcome<T> = Outcome.Ok(value())

    private fun <T> write(body: Any, value: () -> T): Outcome<T> {
        failure?.let { return Outcome.Failed(it) }
        sent += body
        val result = value()
        mutableChanges.tryEmit(Unit)
        return Outcome.Ok(result)
    }

    override suspend fun today() = read { today }

    override suspend fun habits(filter: ArchivedFilter) = read {
        filters += filter
        habits
    }

    override suspend fun habit(id: String) = read { details.getValue(id) }

    override suspend fun calendar(id: String, month: YearMonth?) = read {
        months += month
        calendar
    }

    override suspend fun createHabit(input: HabitInput) = write(input) { habits.first() }

    override suspend fun updateHabit(id: String, input: HabitInput, quantity: Boolean) =
        write(Triple(id, input, quantity)) { habits.first { it.id == id } }

    override suspend fun setArchived(id: String, archived: Boolean) = write("$id archived=$archived") { habits.first() }

    override suspend fun deleteHabit(id: String) = write("delete $id") { }

    override suspend fun mark(id: String, on: LocalDate, value: Int) = write("mark $id $on $value") { habits.first() }

    override suspend fun unmark(id: String, on: LocalDate) = write("unmark $id $on") { habits.first() }

    override suspend fun createFrequency(habitId: String, validFrom: LocalDate, input: FrequencyInput) =
        write(Triple(habitId, validFrom, input)) { }

    override suspend fun updateFrequency(id: String, input: FrequencyInput) = write(id to input) { }

    override suspend fun deleteFrequency(id: String) = write("delete frequency $id") { }

    override suspend fun createPause(habitId: String, startsOn: LocalDate, endsOn: LocalDate?) =
        write("pause $habitId $startsOn $endsOn") { }

    override suspend fun updatePause(id: String, startsOn: LocalDate, endsOn: LocalDate?) =
        write("update pause $id $startsOn $endsOn") { }

    override suspend fun deletePause(id: String) = write("delete pause $id") { }

    override suspend fun logRelapse(habitId: String, occurredAt: Instant, note: String?) =
        write("relapse $habitId $occurredAt $note") { HabitsPreview.social }

    override suspend fun deleteRelapse(id: String) = write("delete relapse $id") { }
}
