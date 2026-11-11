package com.henriquesebastiao.prumo.feature.habits.data

import com.henriquesebastiao.prumo.core.data.repository.DataChanges
import com.henriquesebastiao.prumo.core.data.repository.ServerCalls
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.map
import com.henriquesebastiao.prumo.core.model.onOk
import com.henriquesebastiao.prumo.core.network.requestBody
import com.henriquesebastiao.prumo.feature.habits.model.ArchivedFilter
import com.henriquesebastiao.prumo.feature.habits.model.CalendarMonth
import com.henriquesebastiao.prumo.feature.habits.model.FrequencyInput
import com.henriquesebastiao.prumo.feature.habits.model.HABITS_MODULE_ID
import com.henriquesebastiao.prumo.feature.habits.model.Habit
import com.henriquesebastiao.prumo.feature.habits.model.HabitDetail
import com.henriquesebastiao.prumo.feature.habits.model.HabitInput
import com.henriquesebastiao.prumo.feature.habits.model.HabitsToday
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Hábitos on the server. Every write tells [changes] (and the Home, through [DataChanges]), so open
 * screens load again; nothing is computed here (D02): streaks, rates and the time without come ready.
 */
@Suppress("TooManyFunctions")
interface HabitsRepository {
    /** Emits after each successful write of Hábitos. */
    val changes: Flow<Unit>

    /** Pull to refresh: the next reads ask the server before the device's copy (D21). */
    fun expireCache()

    /** H2: what today asks, with the user's today. */
    suspend fun today(): Outcome<HabitsToday>

    /** By name, both kinds. */
    suspend fun habits(filter: ArchivedFilter = ArchivedFilter.Active): Outcome<List<Habit>>

    suspend fun habit(id: String): Outcome<HabitDetail>

    /** A month of a habit to practice, this month without [month]. */
    suspend fun calendar(id: String, month: YearMonth? = null): Outcome<CalendarMonth>

    suspend fun createHabit(input: HabitInput): Outcome<Habit>

    /** [quantity]: the habit counts a quantity, so its target and unit go too. */
    suspend fun updateHabit(id: String, input: HabitInput, quantity: Boolean): Outcome<Habit>

    suspend fun setArchived(id: String, archived: Boolean): Outcome<Habit>

    suspend fun deleteHabit(id: String): Outcome<Unit>

    /** H1, H3: marks [on] with [value] (1 in a `check`); the answer is the habit. */
    suspend fun mark(id: String, on: LocalDate, value: Int): Outcome<Habit>

    suspend fun unmark(id: String, on: LocalDate): Outcome<Habit>

    /** H2: a new version from [validFrom], today or later. */
    suspend fun createFrequency(habitId: String, validFrom: LocalDate, input: FrequencyInput): Outcome<Unit>

    suspend fun updateFrequency(id: String, input: FrequencyInput): Outcome<Unit>

    suspend fun deleteFrequency(id: String): Outcome<Unit>

    suspend fun createPause(habitId: String, startsOn: LocalDate, endsOn: LocalDate?): Outcome<Unit>

    suspend fun updatePause(id: String, startsOn: LocalDate, endsOn: LocalDate?): Outcome<Unit>

    suspend fun deletePause(id: String): Outcome<Unit>

    /** H7: the answer is the habit, with its new time without. */
    suspend fun logRelapse(habitId: String, occurredAt: Instant, note: String?): Outcome<Habit>

    suspend fun deleteRelapse(id: String): Outcome<Unit>

    companion object {
        /** The list comes whole: the server's maximum page. */
        const val ALL = 200
    }
}

@Suppress("TooManyFunctions")
@Singleton
class DefaultHabitsRepository @Inject constructor(
    private val calls: ServerCalls,
    private val dataChanges: DataChanges,
) : HabitsRepository {
    override val changes: Flow<Unit> = dataChanges.changes.filter { it == HABITS_MODULE_ID }.map { }

    override fun expireCache() = calls.expireCache()

    private suspend fun <T> api(block: suspend HabitsApi.() -> T): Outcome<T> =
        calls.module(HabitsApi::class.java, block)

    /** A write: tells the screens and the Home when it worked. */
    private suspend fun <T> write(block: suspend HabitsApi.() -> T): Outcome<T> =
        api(block).onOk { dataChanges.changed(HABITS_MODULE_ID) }

    override suspend fun today() = api { today() }.map { it.toModel() }

    override suspend fun habits(filter: ArchivedFilter) = api {
        habits(filter.wire, HabitsRepository.ALL)
    }.map { page -> page.items.map { it.toModel() } }

    override suspend fun habit(id: String) = api { habit(id) }.map { it.toModel() }

    override suspend fun calendar(id: String, month: YearMonth?) = api {
        calendar(id, month?.toString())
    }.map { it.toModel() }

    override suspend fun createHabit(input: HabitInput) =
        write { createHabit(requestBody(HabitCreateDto.serializer(), input.toCreateDto())) }.map { it.toModel() }

    override suspend fun updateHabit(id: String, input: HabitInput, quantity: Boolean) = write {
        updateHabit(id, requestBody(HabitUpdateDto.serializer(), input.toUpdateDto(quantity)))
    }.map { it.toModel() }

    override suspend fun setArchived(id: String, archived: Boolean) = write {
        if (archived) archiveHabit(id) else unarchiveHabit(id)
    }.map { it.toModel() }

    override suspend fun deleteHabit(id: String) = write { deleteHabit(id) }

    override suspend fun mark(id: String, on: LocalDate, value: Int) = write {
        putEntry(id, on.toString(), requestBody(EntryWriteDto.serializer(), EntryWriteDto(value)))
    }.map { it.toModel() }

    override suspend fun unmark(id: String, on: LocalDate) = write {
        deleteEntry(id, on.toString())
    }.map { it.toModel() }

    override suspend fun createFrequency(habitId: String, validFrom: LocalDate, input: FrequencyInput) = write {
        val fields = input.toFields()
        val body = FrequencyCreateDto(validFrom.toString(), fields.kind, fields.timesPerWeek, fields.weekdays)
        createFrequency(habitId, requestBody(FrequencyCreateDto.serializer(), body))
    }.map { }

    override suspend fun updateFrequency(id: String, input: FrequencyInput) = write {
        val fields = input.toFields()
        val body = FrequencyUpdateDto(fields.kind, fields.timesPerWeek, fields.weekdays)
        updateFrequency(id, requestBody(FrequencyUpdateDto.serializer(), body))
    }.map { }

    override suspend fun deleteFrequency(id: String) = write { deleteFrequency(id) }

    override suspend fun createPause(habitId: String, startsOn: LocalDate, endsOn: LocalDate?) = write {
        createPause(
            habitId,
            requestBody(PauseCreateDto.serializer(), PauseCreateDto(startsOn.toString(), endsOn?.toString())),
        )
    }.map { }

    override suspend fun updatePause(id: String, startsOn: LocalDate, endsOn: LocalDate?) = write {
        updatePause(
            id,
            requestBody(PauseUpdateDto.serializer(), PauseUpdateDto(startsOn.toString(), endsOn?.toString())),
        )
    }.map { }

    override suspend fun deletePause(id: String) = write { deletePause(id) }

    override suspend fun logRelapse(habitId: String, occurredAt: Instant, note: String?) = write {
        createRelapse(
            habitId,
            requestBody(RelapseCreateDto.serializer(), RelapseCreateDto(occurredAt.toString(), note)),
        )
    }.map { it.toModel() }

    override suspend fun deleteRelapse(id: String) = write { deleteRelapse(id) }
}
