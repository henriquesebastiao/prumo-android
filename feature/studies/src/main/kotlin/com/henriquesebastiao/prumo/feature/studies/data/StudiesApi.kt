package com.henriquesebastiao.prumo.feature.studies.data

import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Estudos on the server (`/api/v1/studies`, docs/api.md of the server, "Estudos"). */
@Suppress("TooManyFunctions")
interface StudiesApi {
    @GET("api/v1/studies/overview")
    suspend fun overview(): OverviewDto

    /** E4, E5: a day's subjects with goal, minutes and status; today without [on]. */
    @GET("api/v1/studies/day")
    suspend fun day(@Query("on") on: String?): DayDto

    /** E6: the week of [start] (any of its days); this week without it. */
    @GET("api/v1/studies/week")
    suspend fun week(@Query("start") start: String?): WeekDto

    @GET("api/v1/studies/areas")
    suspend fun areas(@Query("archived") archived: String, @Query("limit") limit: Int): PageDto<AreaDto>

    @POST("api/v1/studies/areas")
    suspend fun createArea(@Body body: JsonObject): AreaDto

    @PATCH("api/v1/studies/areas/{id}")
    suspend fun updateArea(@Path("id") id: String, @Body body: JsonObject): AreaDto

    @POST("api/v1/studies/areas/{id}/archive")
    suspend fun archiveArea(@Path("id") id: String): AreaDto

    @POST("api/v1/studies/areas/{id}/unarchive")
    suspend fun unarchiveArea(@Path("id") id: String): AreaDto

    @DELETE("api/v1/studies/areas/{id}")
    suspend fun deleteArea(@Path("id") id: String)

    @GET("api/v1/studies/subjects")
    suspend fun subjects(@Query("archived") archived: String, @Query("limit") limit: Int): PageDto<SubjectDto>

    @GET("api/v1/studies/subjects/{id}")
    suspend fun subject(@Path("id") id: String): SubjectDetailDto

    @POST("api/v1/studies/subjects")
    suspend fun createSubject(@Body body: JsonObject): SubjectDto

    @PATCH("api/v1/studies/subjects/{id}")
    suspend fun updateSubject(@Path("id") id: String, @Body body: JsonObject): SubjectDto

    @POST("api/v1/studies/subjects/{id}/archive")
    suspend fun archiveSubject(@Path("id") id: String): SubjectDto

    @POST("api/v1/studies/subjects/{id}/unarchive")
    suspend fun unarchiveSubject(@Path("id") id: String): SubjectDto

    @DELETE("api/v1/studies/subjects/{id}")
    suspend fun deleteSubject(@Path("id") id: String)

    @GET("api/v1/studies/subjects/{id}/topics")
    suspend fun topics(@Path("id") subjectId: String): ItemsDto<TopicDto>

    @DELETE("api/v1/studies/topics/{id}")
    suspend fun deleteTopic(@Path("id") id: String)

    @GET("api/v1/studies/plans")
    suspend fun plans(): PlanListDto

    @POST("api/v1/studies/plans")
    suspend fun createPlan(@Body body: JsonObject): PlanDto

    @PUT("api/v1/studies/plans/{id}")
    suspend fun updatePlan(@Path("id") id: String, @Body body: JsonObject): PlanDto

    @DELETE("api/v1/studies/plans/{id}")
    suspend fun deletePlan(@Path("id") id: String)

    @GET("api/v1/studies/sessions/{id}")
    suspend fun session(@Path("id") id: String): SessionDto

    @POST("api/v1/studies/sessions")
    suspend fun createSession(@Body body: JsonObject): SessionDto

    @PATCH("api/v1/studies/sessions/{id}")
    suspend fun updateSession(@Path("id") id: String, @Body body: JsonObject): SessionDto

    @DELETE("api/v1/studies/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: String)
}
