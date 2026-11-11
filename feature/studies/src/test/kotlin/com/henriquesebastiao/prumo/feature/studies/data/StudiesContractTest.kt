package com.henriquesebastiao.prumo.feature.studies.data

import com.henriquesebastiao.prumo.core.network.testing.OpenApiContract
import com.henriquesebastiao.prumo.feature.studies.model.DayStatus
import com.henriquesebastiao.prumo.feature.studies.model.PlanState
import com.henriquesebastiao.prumo.feature.studies.model.StudyColor
import org.junit.Assert.assertEquals
import org.junit.Test

/** The hand-written DTOs of Estudos against the server's openapi.json (D04). */
class StudiesContractTest {
    @Test
    fun theReadDtosFollowTheContract() {
        OpenApiContract.assertReads(SubjectRefDto.serializer().descriptor, "SubjectRef")
        OpenApiContract.assertReads(AreaDto.serializer().descriptor, "AreaRead")
        OpenApiContract.assertReads(SubjectDto.serializer().descriptor, "SubjectRead")
        OpenApiContract.assertReads(SubjectDetailDto.serializer().descriptor, "SubjectDetail")
        OpenApiContract.assertReads(TopicRefDto.serializer().descriptor, "TopicRef")
        OpenApiContract.assertReads(TopicDto.serializer().descriptor, "TopicRead")
        OpenApiContract.assertReads(WeekMinutesDto.serializer().descriptor, "WeekMinutes")
        OpenApiContract.assertReads(SessionDto.serializer().descriptor, "StudySessionRead")
        OpenApiContract.assertReads(BlockDto.serializer().descriptor, "BlockRead")
        OpenApiContract.assertReads(PlanDto.serializer().descriptor, "PlanRead")
        OpenApiContract.assertReads(PlanListDto.serializer().descriptor, "PlanList")
        OpenApiContract.assertReads(DayBlockDto.serializer().descriptor, "DayBlock")
        OpenApiContract.assertReads(DayRowDto.serializer().descriptor, "DayRow")
        OpenApiContract.assertReads(DayDto.serializer().descriptor, "DayRead")
        OpenApiContract.assertReads(WeekSubjectDto.serializer().descriptor, "WeekSubject")
        OpenApiContract.assertReads(WeekDto.serializer().descriptor, "WeekRead")
        OpenApiContract.assertReads(OverviewDto.serializer().descriptor, "StudiesOverview")
    }

    @Test
    fun theWriteDtosFollowTheContract() {
        OpenApiContract.assertWrites(AreaCreateDto.serializer().descriptor, "AreaCreate")
        OpenApiContract.assertWrites(AreaUpdateDto.serializer().descriptor, "AreaUpdate")
        OpenApiContract.assertWrites(SubjectCreateDto.serializer().descriptor, "SubjectCreate")
        OpenApiContract.assertWrites(SubjectUpdateDto.serializer().descriptor, "SubjectUpdate")
        OpenApiContract.assertWrites(BlockWriteDto.serializer().descriptor, "BlockWrite")
        OpenApiContract.assertWrites(PlanWriteDto.serializer().descriptor, "PlanWrite")
        OpenApiContract.assertWrites(PlanBlocksWriteDto.serializer().descriptor, "PlanBlocksWrite")
        OpenApiContract.assertWrites(SessionCreateDto.serializer().descriptor, "StudySessionCreate")
        OpenApiContract.assertWrites(SessionUpdateDto.serializer().descriptor, "StudySessionUpdate")
    }

    @Test
    fun theEnumsAreTheServers() {
        assertEquals(OpenApiContract.enumValues("DayStatus"), DayStatus.entries.map { it.wire }.toSet())
        assertEquals(OpenApiContract.enumValues("AreaCreate", "color"), StudyColor.entries.map { it.wire }.toSet())
        assertEquals(OpenApiContract.enumValues("PlanRead", "state"), PlanState.entries.map { it.wire }.toSet())
    }
}
