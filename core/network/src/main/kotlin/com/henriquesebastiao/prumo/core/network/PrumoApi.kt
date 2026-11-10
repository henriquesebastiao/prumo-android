package com.henriquesebastiao.prumo.core.network

import com.henriquesebastiao.prumo.core.network.dto.DashboardDto
import com.henriquesebastiao.prumo.core.network.dto.LoginRequestDto
import com.henriquesebastiao.prumo.core.network.dto.LoginResponseDto
import com.henriquesebastiao.prumo.core.network.dto.ModuleListDto
import com.henriquesebastiao.prumo.core.network.dto.PasswordChangeDto
import com.henriquesebastiao.prumo.core.network.dto.PreferencesDto
import com.henriquesebastiao.prumo.core.network.dto.PreferencesUpdateDto
import com.henriquesebastiao.prumo.core.network.dto.SessionListDto
import com.henriquesebastiao.prumo.core.network.dto.UserDto
import com.henriquesebastiao.prumo.core.network.dto.UserUpdateDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * The system routes of the API (`docs/api.md`, "Autenticação" and "Sistema"). Paths are relative
 * to the server's base URL, which may have a path of its own. Modules have their own interfaces in
 * their `:feature:*`.
 */
interface PrumoApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    @POST("api/v1/auth/logout")
    suspend fun logout()

    @GET("api/v1/auth/sessions")
    suspend fun sessions(): SessionListDto

    @DELETE("api/v1/auth/sessions/{id}")
    suspend fun revokeSession(@Path("id") id: String)

    @GET("api/v1/me")
    suspend fun me(): UserDto

    @PATCH("api/v1/me")
    suspend fun updateMe(@Body body: UserUpdateDto): UserDto

    @PUT("api/v1/me/password")
    suspend fun changePassword(@Body body: PasswordChangeDto)

    /** D38 of the server: a `.zip` with `data.json`, a CSV per table and the uploaded files. */
    @Streaming
    @GET("api/v1/me/export")
    suspend fun export(): ResponseBody

    @GET("api/v1/me/preferences")
    suspend fun preferences(): PreferencesDto

    @PATCH("api/v1/me/preferences")
    suspend fun updatePreferences(@Body body: PreferencesUpdateDto): PreferencesDto

    @GET("api/v1/modules")
    suspend fun modules(): ModuleListDto

    @GET("api/v1/dashboard")
    suspend fun dashboard(): DashboardDto

    companion object {
        /** The path of the login, whose `401` is about what was typed, not about a session. */
        const val LOGIN_PATH = "api/v1/auth/login"
    }
}
