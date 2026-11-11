package com.henriquesebastiao.prumo.feature.studies.data

import com.henriquesebastiao.prumo.core.data.repository.DataChanges
import com.henriquesebastiao.prumo.core.data.repository.ServerCalls
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.map
import com.henriquesebastiao.prumo.core.model.onOk
import com.henriquesebastiao.prumo.core.network.requestBody
import com.henriquesebastiao.prumo.feature.studies.model.ArchivedFilter
import com.henriquesebastiao.prumo.feature.studies.model.Area
import com.henriquesebastiao.prumo.feature.studies.model.AreaInput
import com.henriquesebastiao.prumo.feature.studies.model.BlockInput
import com.henriquesebastiao.prumo.feature.studies.model.Day
import com.henriquesebastiao.prumo.feature.studies.model.Plan
import com.henriquesebastiao.prumo.feature.studies.model.Plans
import com.henriquesebastiao.prumo.feature.studies.model.STUDIES_MODULE_ID
import com.henriquesebastiao.prumo.feature.studies.model.SessionInput
import com.henriquesebastiao.prumo.feature.studies.model.StudiesOverview
import com.henriquesebastiao.prumo.feature.studies.model.StudySession
import com.henriquesebastiao.prumo.feature.studies.model.Subject
import com.henriquesebastiao.prumo.feature.studies.model.SubjectDetail
import com.henriquesebastiao.prumo.feature.studies.model.SubjectInput
import com.henriquesebastiao.prumo.feature.studies.model.Topic
import com.henriquesebastiao.prumo.feature.studies.model.Week
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Estudos on the server. Every write tells [changes] (and the Home, through [DataChanges]), so open
 * screens load again; nothing is computed here (D02): goals, minutes and statuses come ready.
 */
@Suppress("TooManyFunctions")
interface StudiesRepository {
    /** Emits after each successful write of Estudos. */
    val changes: Flow<Unit>

    /** Pull to refresh: the next reads ask the server before the device's copy (D21). */
    fun expireCache()

    /** The subject last used in "Registrar estudo" during this run of the app, or null (D15). */
    var lastSubjectId: String?

    suspend fun overview(): Outcome<StudiesOverview>

    /** A day of the plan (E4, E5): today without [on]. */
    suspend fun day(on: LocalDate? = null): Outcome<Day>

    /** The week with [start] (any of its days, E6): this week without it. */
    suspend fun week(start: LocalDate? = null): Outcome<Week>

    suspend fun areas(filter: ArchivedFilter = ArchivedFilter.Active): Outcome<List<Area>>

    suspend fun createArea(input: AreaInput): Outcome<Area>

    suspend fun updateArea(id: String, input: AreaInput): Outcome<Area>

    suspend fun setAreaArchived(id: String, archived: Boolean): Outcome<Area>

    /** Refused with `studies.area_in_use` while the area has subjects. */
    suspend fun deleteArea(id: String): Outcome<Unit>

    /** The subjects by name, every area. */
    suspend fun subjects(filter: ArchivedFilter = ArchivedFilter.Active): Outcome<List<Subject>>

    suspend fun subject(id: String): Outcome<SubjectDetail>

    suspend fun createSubject(input: SubjectInput): Outcome<Subject>

    suspend fun updateSubject(id: String, input: SubjectInput): Outcome<Subject>

    suspend fun setSubjectArchived(id: String, archived: Boolean): Outcome<Subject>

    /** Deletes the subject with its topics, sessions and blocks. */
    suspend fun deleteSubject(id: String): Outcome<Unit>

    /** E10: a subject's topics, by name. */
    suspend fun topics(subjectId: String): Outcome<List<Topic>>

    /** Refused with `studies.topic_in_use` while a session names it. */
    suspend fun deleteTopic(id: String): Outcome<Unit>

    /** Every version of the plan, newest first (E1). */
    suspend fun plans(): Outcome<Plans>

    /** A new version from [validFrom], today or later (E1). */
    suspend fun createPlan(validFrom: LocalDate, blocks: List<BlockInput>): Outcome<Plan>

    /** The blocks of a version not yet in force. */
    suspend fun updatePlan(id: String, blocks: List<BlockInput>): Outcome<Plan>

    suspend fun deletePlan(id: String): Outcome<Unit>

    suspend fun session(id: String): Outcome<StudySession>

    suspend fun createSession(input: SessionInput): Outcome<StudySession>

    suspend fun updateSession(id: String, input: SessionInput): Outcome<StudySession>

    suspend fun deleteSession(id: String): Outcome<Unit>

    companion object {
        /** Areas and subjects come whole: the server's maximum page. */
        const val ALL = 200
    }
}

@Suppress("TooManyFunctions")
@Singleton
class DefaultStudiesRepository @Inject constructor(
    private val calls: ServerCalls,
    private val dataChanges: DataChanges,
) : StudiesRepository {
    override val changes: Flow<Unit> = dataChanges.changes.filter { it == STUDIES_MODULE_ID }.map { }

    override fun expireCache() = calls.expireCache()

    @Volatile
    override var lastSubjectId: String? = null

    private suspend fun <T> api(block: suspend StudiesApi.() -> T): Outcome<T> =
        calls.module(StudiesApi::class.java, block)

    /** A write: tells the screens and the Home when it worked. */
    private suspend fun <T> write(block: suspend StudiesApi.() -> T): Outcome<T> =
        api(block).onOk { dataChanges.changed(STUDIES_MODULE_ID) }

    override suspend fun overview() = api { overview() }.map { it.toModel() }

    override suspend fun day(on: LocalDate?) = api { day(on?.toString()) }.map { it.toModel() }

    override suspend fun week(start: LocalDate?) = api { week(start?.toString()) }.map { it.toModel() }

    override suspend fun areas(filter: ArchivedFilter) = api {
        areas(filter.wire, StudiesRepository.ALL)
    }.map { page -> page.items.map { it.toModel() } }

    override suspend fun createArea(input: AreaInput) =
        write { createArea(requestBody(AreaCreateDto.serializer(), input.toCreateDto())) }.map { it.toModel() }

    override suspend fun updateArea(id: String, input: AreaInput) =
        write { updateArea(id, requestBody(AreaUpdateDto.serializer(), input.toUpdateDto())) }.map { it.toModel() }

    override suspend fun setAreaArchived(id: String, archived: Boolean) = write {
        if (archived) archiveArea(id) else unarchiveArea(id)
    }.map { it.toModel() }

    override suspend fun deleteArea(id: String) = write { deleteArea(id) }

    override suspend fun subjects(filter: ArchivedFilter) = api {
        subjects(filter.wire, StudiesRepository.ALL)
    }.map { page -> page.items.map { it.toModel() } }

    override suspend fun subject(id: String) = api { subject(id) }.map { it.toModel() }

    override suspend fun createSubject(input: SubjectInput) = write {
        createSubject(requestBody(SubjectCreateDto.serializer(), input.toCreateDto()))
    }.map { it.toModel() }

    override suspend fun updateSubject(id: String, input: SubjectInput) = write {
        updateSubject(id, requestBody(SubjectUpdateDto.serializer(), input.toUpdateDto()))
    }.map { it.toModel() }

    override suspend fun setSubjectArchived(id: String, archived: Boolean) = write {
        if (archived) archiveSubject(id) else unarchiveSubject(id)
    }.map { it.toModel() }

    override suspend fun deleteSubject(id: String) = write { deleteSubject(id) }

    override suspend fun topics(subjectId: String) = api {
        topics(subjectId)
    }.map { list -> list.items.map { it.toModel() } }

    override suspend fun deleteTopic(id: String) = write { deleteTopic(id) }

    override suspend fun plans() = api { plans() }.map { it.toModel() }

    override suspend fun createPlan(validFrom: LocalDate, blocks: List<BlockInput>) = write {
        createPlan(
            requestBody(
                PlanWriteDto.serializer(),
                PlanWriteDto(
                    validFrom.toString(),
                    blocks.map {
                        it.toDto()
                    },
                ),
            ),
        )
    }.map { it.toModel() }

    override suspend fun updatePlan(id: String, blocks: List<BlockInput>) = write {
        updatePlan(id, requestBody(PlanBlocksWriteDto.serializer(), PlanBlocksWriteDto(blocks.map { it.toDto() })))
    }.map { it.toModel() }

    override suspend fun deletePlan(id: String) = write { deletePlan(id) }

    override suspend fun session(id: String) = api { session(id) }.map { it.toModel() }

    override suspend fun createSession(input: SessionInput) = write {
        createSession(requestBody(SessionCreateDto.serializer(), input.toCreateDto()))
    }.map { it.toModel() }

    override suspend fun updateSession(id: String, input: SessionInput) = write {
        updateSession(id, requestBody(SessionUpdateDto.serializer(), input.toUpdateDto()))
    }.map { it.toModel() }

    override suspend fun deleteSession(id: String) = write { deleteSession(id) }
}
