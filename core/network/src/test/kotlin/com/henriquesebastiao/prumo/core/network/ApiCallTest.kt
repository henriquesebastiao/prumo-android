package com.henriquesebastiao.prumo.core.network

import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.ErrorCodes
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.UnreachableCause
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiCallTest {
    @Test
    fun aWrongPasswordIsAnApiErrorWithItsCode() {
        val error = parseError(401, Fixtures.read("error_invalid_credentials.json"))
        assertEquals(ErrorCodes.AUTH_INVALID_CREDENTIALS, (error as AppError.Api).code)
        assertEquals(401, error.status)
    }

    @Test
    fun aRefusedTokenEndsTheSession() {
        assertEquals(AppError.SessionExpired, parseError(401, Fixtures.read("error_unauthorized.json")))
    }

    @Test
    fun validationErrorsKeepTheirFields() {
        val error = parseError(422, Fixtures.read("error_validation.json")) as AppError.Api
        assertEquals(ErrorCodes.VALIDATION, error.code)
        assertTrue(error.fields.map { it.path }.containsAll(listOf("username", "password")))
    }

    @Test
    fun detailsAndRetryAfterAreKept() {
        val body = """{"error":{"code":"auth.rate_limited","message":"Too many","details":{"retry_after":900}}}"""
        val error = parseError(429, body, retryAfterSeconds = 900) as AppError.Api
        assertEquals("900", error.details["retry_after"])
        assertEquals(900L, error.retryAfterSeconds)
    }

    @Test
    fun anAnswerWithoutTheErrorFormatIsUnexpected() {
        assertEquals(AppError.Unexpected(502), parseError(502, "<html>Bad Gateway</html>"))
        assertEquals(AppError.Unexpected(500), parseError(500, null))
    }

    @Test
    fun eachKindOfSilenceHasItsCause() {
        assertEquals(UnreachableCause.UnknownHost, unreachableCause(UnknownHostException("prumo.minhacasa.nte")))
        assertEquals(UnreachableCause.Timeout, unreachableCause(SocketTimeoutException()))
        assertEquals(UnreachableCause.Refused, unreachableCause(ConnectException()))
        assertEquals(UnreachableCause.Certificate, unreachableCause(SSLHandshakeException("bad cert")))
        assertEquals(UnreachableCause.Other, unreachableCause(IOException("reset")))
    }

    @Test
    fun exceptionsNeverReachTheCaller() = runTest {
        assertEquals(Outcome.Ok(1), apiCall { 1 })
        assertEquals(
            Outcome.Failed(AppError.Unreachable(UnreachableCause.Refused)),
            apiCall<Int> { throw ConnectException() },
        )
    }
}
