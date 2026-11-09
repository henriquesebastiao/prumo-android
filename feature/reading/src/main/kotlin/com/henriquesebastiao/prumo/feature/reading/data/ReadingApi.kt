package com.henriquesebastiao.prumo.feature.reading.data

import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** Livros on the server (`/api/v1/reading`, docs/api.md of the server, "Livros"). */
@Suppress("TooManyFunctions")
interface ReadingApi {
    @GET("api/v1/reading/books")
    suspend fun books(
        @Query("status") status: String?,
        @Query("owned") owned: Boolean?,
        @Query("q") query: String?,
        @Query("sort") sort: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): PageDto<BookDto>

    @GET("api/v1/reading/books/{id}")
    suspend fun book(@Path("id") id: String): BookDetailDto

    @POST("api/v1/reading/books")
    suspend fun createBook(@Body body: JsonObject): BookDto

    @PATCH("api/v1/reading/books/{id}")
    suspend fun updateBook(@Path("id") id: String, @Body body: JsonObject): BookDto

    @DELETE("api/v1/reading/books/{id}")
    suspend fun deleteBook(@Path("id") id: String)

    /** R2–R4: a new current page. The answer is the book. */
    @POST("api/v1/reading/books/{id}/progress")
    suspend fun addProgress(@Path("id") id: String, @Body body: JsonObject): BookDto

    @Multipart
    @PUT("api/v1/reading/books/{id}/cover")
    suspend fun setCover(@Path("id") id: String, @Part file: MultipartBody.Part): BookDto

    @DELETE("api/v1/reading/books/{id}/cover")
    suspend fun deleteCover(@Path("id") id: String)

    @GET("api/v1/reading/books/{id}/notes")
    suspend fun bookNotes(@Path("id") id: String): ItemsDto<NoteDto>

    @GET("api/v1/reading/notes")
    suspend fun notes(
        @Query("q") query: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): PageDto<NoteDto>

    @POST("api/v1/reading/books/{id}/notes")
    suspend fun createNote(@Path("id") bookId: String, @Body body: JsonObject): NoteDto

    @PATCH("api/v1/reading/books/{id}/notes/{note}")
    suspend fun updateNote(@Path("id") bookId: String, @Path("note") id: String, @Body body: JsonObject): NoteDto

    @DELETE("api/v1/reading/books/{id}/notes/{note}")
    suspend fun deleteNote(@Path("id") bookId: String, @Path("note") id: String)
}
