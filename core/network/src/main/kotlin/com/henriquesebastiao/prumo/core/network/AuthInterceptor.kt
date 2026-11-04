package com.henriquesebastiao.prumo.core.network

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Sends `Authorization: Bearer <token>` on every request to the session's server, and only to it,
 * so the token never leaks to another host. A `401` to a request that carried the token (anything
 * but the login, whose `401` is about the password typed) ends the session.
 *
 * The gateway comes through a [Provider]: the session repository signs in through the client this
 * interceptor is part of.
 */
@Singleton
class AuthInterceptor @Inject constructor(private val gatewayProvider: Provider<SessionGateway>) : Interceptor {
    private val gateway: SessionGateway get() = gatewayProvider.get()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val session = gateway.activeSession
        if (session == null || request.header(AUTHORIZATION) != null || !belongsTo(request.url, session.baseUrl)) {
            return chain.proceed(request)
        }
        val response = chain.proceed(request.newBuilder().header(AUTHORIZATION, "Bearer ${session.token}").build())
        val signingIn = request.url.encodedPath.endsWith("/${PrumoApi.LOGIN_PATH}")
        if (response.code == HTTP_UNAUTHORIZED && !signingIn) {
            gateway.onUnauthorized(session.token)
        }
        return response
    }

    companion object {
        const val AUTHORIZATION = "Authorization"
        private const val HTTP_UNAUTHORIZED = 401

        /** Whether [url] is on the server at [baseUrl]: same scheme, host and port, under its path. */
        fun belongsTo(url: HttpUrl, baseUrl: String): Boolean {
            val base = baseUrl.toHttpUrlOrNull() ?: return false
            if (url.scheme != base.scheme || url.host != base.host || url.port != base.port) return false
            val basePath = base.encodedPath.trimEnd('/')
            return basePath.isEmpty() || url.encodedPath == basePath || url.encodedPath.startsWith("$basePath/")
        }
    }
}
