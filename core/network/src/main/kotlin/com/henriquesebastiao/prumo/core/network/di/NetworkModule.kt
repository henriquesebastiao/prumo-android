package com.henriquesebastiao.prumo.core.network.di

import com.henriquesebastiao.prumo.core.network.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient

/** The client of the API calls: bearer on the session's server, the usual timeouts. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiClient

/** The client that tests an address: no token, short timeouts (the server screen waits on it). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProbeClient

/** The dispatcher for blocking I/O. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val CONNECT_SECONDS = 10L
    private const val READ_SECONDS = 30L
    private const val PROBE_SECONDS = 10L

    @Provides
    @Singleton
    @ApiClient
    fun apiClient(auth: AuthInterceptor): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(READ_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(auth)
        .build()

    @Provides
    @Singleton
    @ProbeClient
    fun probeClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(PROBE_SECONDS, TimeUnit.SECONDS)
        .readTimeout(PROBE_SECONDS, TimeUnit.SECONDS)
        .callTimeout(PROBE_SECONDS * 2, TimeUnit.SECONDS)
        .build()

    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
