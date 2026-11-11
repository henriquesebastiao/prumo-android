package com.henriquesebastiao.prumo.feature.habits.data

import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Hábitos on the server (`/api/v1/habits`, docs/api.md of the server, "Hábitos"). */
@Suppress("TooManyFunctions")
interface HabitsApi {
    /** H2: the habits that ask something of today, without the paused ones. */
    @GET("api/v1/habits/today")
    suspend fun today(): TodayDto

    @GET("api/v1/habits")
    suspend fun habits(@Query("archived") archived: String, @Query("limit") limit: Int): PageDto<HabitDto>

    @GET("api/v1/habits/{id}")
    suspend fun habit(@Path("id") id: String): HabitDetailDto

    /** The days of [month] (`YYYY-MM`) with their state; this month without it. */
    @GET("api/v1/habits/{id}/calendar")
    suspend fun calendar(@Path("id") id: String, @Query("month") month: String?): CalendarDto

    @POST("api/v1/habits")
    suspend fun createHabit(@Body body: JsonObject): HabitDto

    @PATCH("api/v1/habits/{id}")
    suspend fun updateHabit(@Path("id") id: String, @Body body: JsonObject): HabitDto

    @POST("api/v1/habits/{id}/archive")
    suspend fun archiveHabit(@Path("id") id: String): HabitDto

    @POST("api/v1/habits/{id}/unarchive")
    suspend fun unarchiveHabit(@Path("id") id: String): HabitDto

    @DELETE("api/v1/habits/{id}")
    suspend fun deleteHabit(@Path("id") id: String)

    /** Marks a day or changes its value; repeating it is safe (D47 of the server). */
    @PUT("api/v1/habits/{id}/entries/{on}")
    suspend fun putEntry(@Path("id") id: String, @Path("on") on: String, @Body body: JsonObject): HabitDto

    @DELETE("api/v1/habits/{id}/entries/{on}")
    suspend fun deleteEntry(@Path("id") id: String, @Path("on") on: String): HabitDto

    @POST("api/v1/habits/{id}/frequencies")
    suspend fun createFrequency(@Path("id") id: String, @Body body: JsonObject): FrequencyDto

    @PATCH("api/v1/habits/frequencies/{id}")
    suspend fun updateFrequency(@Path("id") id: String, @Body body: JsonObject): FrequencyDto

    @DELETE("api/v1/habits/frequencies/{id}")
    suspend fun deleteFrequency(@Path("id") id: String)

    @POST("api/v1/habits/{id}/pauses")
    suspend fun createPause(@Path("id") id: String, @Body body: JsonObject): PauseDto

    @PATCH("api/v1/habits/pauses/{id}")
    suspend fun updatePause(@Path("id") id: String, @Body body: JsonObject): PauseDto

    @DELETE("api/v1/habits/pauses/{id}")
    suspend fun deletePause(@Path("id") id: String)

    @POST("api/v1/habits/{id}/relapses")
    suspend fun createRelapse(@Path("id") id: String, @Body body: JsonObject): HabitDto

    @DELETE("api/v1/habits/relapses/{id}")
    suspend fun deleteRelapse(@Path("id") id: String)
}
