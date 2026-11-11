package com.henriquesebastiao.prumo.feature.studies.ui

import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.feature.studies.data.StudiesRepository
import com.henriquesebastiao.prumo.feature.studies.model.ArchivedFilter
import com.henriquesebastiao.prumo.feature.studies.model.Area
import com.henriquesebastiao.prumo.feature.studies.model.AreaInput
import com.henriquesebastiao.prumo.feature.studies.model.BlockInput
import com.henriquesebastiao.prumo.feature.studies.model.Day
import com.henriquesebastiao.prumo.feature.studies.model.Plan
import com.henriquesebastiao.prumo.feature.studies.model.PlanState
import com.henriquesebastiao.prumo.feature.studies.model.Plans
import com.henriquesebastiao.prumo.feature.studies.model.SessionInput
import com.henriquesebastiao.prumo.feature.studies.model.StudySession
import com.henriquesebastiao.prumo.feature.studies.model.Subject
import com.henriquesebastiao.prumo.feature.studies.model.SubjectDetail
import com.henriquesebastiao.prumo.feature.studies.model.SubjectInput
import com.henriquesebastiao.prumo.feature.studies.model.SubjectRef
import com.henriquesebastiao.prumo.feature.studies.model.Topic
import com.henriquesebastiao.prumo.feature.studies.model.Week
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Estudos in memory, with the demo server's data (StudiesPreview). Nothing is computed: the reads
 * answer what they were given. [failure] makes the next writes fail; what was sent is kept in [sent].
 */
@Suppress("TooManyFunctions")
class FakeStudiesRepository : StudiesRepository {
    private val mutableChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    override val changes = mutableChanges

    var expired = 0

    override fun expireCache() {
        expired++
    }

    override var lastSubjectId: String? = null

    var day: Day = StudiesPreview.today
    var week: Week = StudiesPreview.week
    var areas: List<Area> = StudiesPreview.areas
    var subjects: List<Subject> = StudiesPreview.subjects
    var detail: SubjectDetail = StudiesPreview.detail
    var plans: Plans = StudiesPreview.plans
    var sessions: List<StudySession> = StudiesPreview.detail.recentSessions

    var failure: AppError? = null
    var readFailure: AppError? = null

    val sent = mutableListOf<Any>()
    val weeks = mutableListOf<LocalDate?>()
    val filters = mutableListOf<ArchivedFilter>()

    private fun <T> read(value: () -> T): Outcome<T> = readFailure?.let { Outcome.Failed(it) } ?: Outcome.Ok(value())

    private fun <T> write(body: Any, value: () -> T): Outcome<T> {
        failure?.let { return Outcome.Failed(it) }
        sent += body
        val result = value()
        mutableChanges.tryEmit(Unit)
        return Outcome.Ok(result)
    }

    override suspend fun overview() = read { StudiesPreview.overview }

    override suspend fun day(on: LocalDate?) = read { day }

    override suspend fun week(start: LocalDate?) = read {
        weeks += start
        week
    }

    override suspend fun areas(filter: ArchivedFilter) = read {
        filters += filter
        areas
    }

    override suspend fun createArea(input: AreaInput) = write(input) {
        areas.first().copy(name = input.name, color = input.color)
    }

    override suspend fun updateArea(id: String, input: AreaInput) =
        write(id to input) { areas.first { it.id == id }.copy(name = input.name, color = input.color) }

    override suspend fun setAreaArchived(id: String, archived: Boolean) =
        write("area $id archived=$archived") { areas.first { it.id == id }.copy(archived = archived) }

    override suspend fun deleteArea(id: String) = write("delete area $id") { }

    override suspend fun subjects(filter: ArchivedFilter) = read {
        filters += filter
        subjects
    }

    override suspend fun subject(id: String) = read { detail }

    override suspend fun createSubject(input: SubjectInput) = write(input) { subjects.first() }

    override suspend fun updateSubject(id: String, input: SubjectInput) = write(id to input) { subjects.first() }

    override suspend fun setSubjectArchived(id: String, archived: Boolean) =
        write("subject $id archived=$archived") { subjects.first().copy(archived = archived) }

    override suspend fun deleteSubject(id: String) = write("delete subject $id") { }

    override suspend fun topics(subjectId: String): Outcome<List<Topic>> = read { detail.topics }

    override suspend fun deleteTopic(id: String) = write("delete topic $id") { }

    override suspend fun plans() = read { plans }

    override suspend fun createPlan(validFrom: LocalDate, blocks: List<BlockInput>) =
        write(validFrom to blocks) { Plan("new", validFrom, null, PlanState.Future, emptyList()) }

    override suspend fun updatePlan(id: String, blocks: List<BlockInput>) =
        write(id to blocks) { plans.items.first { it.id == id } }

    override suspend fun deletePlan(id: String) = write("delete plan $id") { }

    override suspend fun session(id: String) = read { sessions.first { it.id == id } }

    override suspend fun createSession(input: SessionInput) = write(input) {
        sessions.first().copy(subject = SubjectRef(input.subjectId, "", sessions.first().subject.color))
    }

    override suspend fun updateSession(id: String, input: SessionInput) = write(id to input) {
        sessions.first {
            it.id ==
                id
        }
    }

    override suspend fun deleteSession(id: String) = write("delete session $id") { }
}
