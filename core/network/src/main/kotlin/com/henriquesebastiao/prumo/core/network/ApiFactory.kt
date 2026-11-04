package com.henriquesebastiao.prumo.core.network

import com.henriquesebastiao.prumo.core.network.di.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Builds Retrofit services for a server's base URL. The address is the user's (first run,
 * Settings › Servidor), so it can't be fixed at build time; the last one is kept. Modules create
 * their own interfaces with [create].
 */
@Singleton
class ApiFactory @Inject constructor(@ApiClient private val client: OkHttpClient) {
    private val converter = NetworkJson.asConverterFactory("application/json".toMediaType())

    @Volatile
    private var cached: Pair<String, Retrofit>? = null

    /** The system routes on [baseUrl]. */
    fun system(baseUrl: String): PrumoApi = create(baseUrl, PrumoApi::class.java)

    /** Any Retrofit interface [service] on [baseUrl] (a module's API). */
    fun <T> create(baseUrl: String, service: Class<T>): T = retrofit(baseUrl).create(service)

    private fun retrofit(baseUrl: String): Retrofit {
        cached?.let { (url, retrofit) -> if (url == baseUrl) return retrofit }
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(converter)
            .build()
        cached = baseUrl to retrofit
        return retrofit
    }
}
