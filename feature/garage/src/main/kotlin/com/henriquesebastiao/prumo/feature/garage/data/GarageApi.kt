package com.henriquesebastiao.prumo.feature.garage.data

import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The routes of Veículos (`docs/api.md` of the server, "Veículos"), relative to the server's base
 * URL. Bodies go as [JsonObject] encoded by [GarageWriteJson]: a `PATCH` clears a field with an
 * explicit `null`, which the app's lenient JSON leaves out.
 */
@Suppress("TooManyFunctions")
interface GarageApi {
    @GET("api/v1/garage/vehicles")
    suspend fun vehicles(@Query("archived") archived: Boolean, @Query("limit") limit: Int): PageDto<VehicleDto>

    @GET("api/v1/garage/vehicles/{id}")
    suspend fun vehicle(@Path("id") id: String): VehicleDto

    @POST("api/v1/garage/vehicles")
    suspend fun createVehicle(@Body body: JsonObject): VehicleDto

    @PATCH("api/v1/garage/vehicles/{id}")
    suspend fun updateVehicle(@Path("id") id: String, @Body body: JsonObject): VehicleDto

    @DELETE("api/v1/garage/vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") id: String)

    @POST("api/v1/garage/vehicles/{id}/archive")
    suspend fun archiveVehicle(@Path("id") id: String): VehicleDto

    @POST("api/v1/garage/vehicles/{id}/unarchive")
    suspend fun unarchiveVehicle(@Path("id") id: String): VehicleDto

    @GET("api/v1/garage/vehicles/{id}/odometer")
    suspend fun odometer(
        @Path("id") vehicleId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): PageDto<OdometerReadingDto>

    @POST("api/v1/garage/vehicles/{id}/odometer")
    suspend fun addReading(@Path("id") vehicleId: String, @Body body: JsonObject): OdometerReadingDto

    @DELETE("api/v1/garage/vehicles/{id}/odometer/{reading}")
    suspend fun deleteReading(@Path("id") vehicleId: String, @Path("reading") readingId: String)

    @GET("api/v1/garage/maintenances")
    suspend fun maintenances(
        @Query("vehicle_id") vehicleId: String?,
        @Query("kind") kind: String?,
        @Query("workshop_id") workshopId: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): PageDto<MaintenanceDto>

    @GET("api/v1/garage/vehicles/{id}/maintenances/{maintenance}")
    suspend fun maintenance(@Path("id") vehicleId: String, @Path("maintenance") id: String): MaintenanceDto

    @POST("api/v1/garage/vehicles/{id}/maintenances")
    suspend fun createMaintenance(@Path("id") vehicleId: String, @Body body: JsonObject): MaintenanceDto

    @PATCH("api/v1/garage/vehicles/{id}/maintenances/{maintenance}")
    suspend fun updateMaintenance(
        @Path("id") vehicleId: String,
        @Path("maintenance") id: String,
        @Body body: JsonObject,
    ): MaintenanceDto

    @DELETE("api/v1/garage/vehicles/{id}/maintenances/{maintenance}")
    suspend fun deleteMaintenance(@Path("id") vehicleId: String, @Path("maintenance") id: String)

    @GET("api/v1/garage/vehicles/{id}/oil-change-suggestion")
    suspend fun oilChangeSuggestion(
        @Path("id") vehicleId: String,
        @Query("km") km: Long?,
        @Query("on") on: String?,
    ): OilChangeSuggestionDto

    @GET("api/v1/garage/fuelings")
    suspend fun fuelings(
        @Query("vehicle_id") vehicleId: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): PageDto<FuelingDto>

    @GET("api/v1/garage/vehicles/{id}/fuelings/{fueling}")
    suspend fun fueling(@Path("id") vehicleId: String, @Path("fueling") id: String): FuelingDto

    @POST("api/v1/garage/vehicles/{id}/fuelings")
    suspend fun createFueling(@Path("id") vehicleId: String, @Body body: JsonObject): FuelingDto

    @PATCH("api/v1/garage/vehicles/{id}/fuelings/{fueling}")
    suspend fun updateFueling(
        @Path("id") vehicleId: String,
        @Path("fueling") id: String,
        @Body body: JsonObject,
    ): FuelingDto

    @DELETE("api/v1/garage/vehicles/{id}/fuelings/{fueling}")
    suspend fun deleteFueling(@Path("id") vehicleId: String, @Path("fueling") id: String)

    @GET("api/v1/garage/workshops")
    suspend fun workshops(@Query("q") query: String?, @Query("limit") limit: Int): PageDto<WorkshopDto>

    @POST("api/v1/garage/workshops")
    suspend fun createWorkshop(@Body body: JsonObject): WorkshopDto

    @PATCH("api/v1/garage/workshops/{id}")
    suspend fun updateWorkshop(@Path("id") id: String, @Body body: JsonObject): WorkshopDto

    @DELETE("api/v1/garage/workshops/{id}")
    suspend fun deleteWorkshop(@Path("id") id: String)
}
