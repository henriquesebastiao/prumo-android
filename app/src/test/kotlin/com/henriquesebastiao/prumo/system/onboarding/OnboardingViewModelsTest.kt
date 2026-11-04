package com.henriquesebastiao.prumo.system.onboarding

import com.henriquesebastiao.prumo.core.data.session.SessionState
import com.henriquesebastiao.prumo.core.data.session.SignOutReason
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.ErrorCodes
import com.henriquesebastiao.prumo.core.model.ServerCheck
import com.henriquesebastiao.prumo.core.model.UnreachableCause
import com.henriquesebastiao.prumo.core.testing.FakeServerRepository
import com.henriquesebastiao.prumo.core.testing.FakeSessionRepository
import com.henriquesebastiao.prumo.core.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelsTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun continueOpensOnlyWithAPrumoServerFound() {
        val server = FakeServerRepository { ServerCheck.Unreachable("https://$it", UnreachableCause.UnknownHost) }
        val vm = ServerViewModel(server)
        vm.onAddressChange("prumo.minhacasa.nte")
        vm.test()
        assertNull(vm.state.value.found)

        server.answer = { ServerCheck.Found("https://$it", "0.1.0", 84) }
        vm.onAddressChange("prumo.minhacasa.net")
        assertNull("typing again clears the result", vm.state.value.check)
        vm.test()
        assertEquals("https://prumo.minhacasa.net", vm.state.value.found?.baseUrl)
        assertEquals(listOf("prumo.minhacasa.nte", "prumo.minhacasa.net"), server.checked)
    }

    @Test
    fun nothingTypedIsNotTested() {
        val server = FakeServerRepository()
        ServerViewModel(server).test()
        assertTrue(server.checked.isEmpty())
    }

    @Test
    fun aWrongPasswordClearsItAndShowsTheError() {
        val sessions = FakeSessionRepository(SessionState.SignedOut(SignOutReason.None, null, null))
        sessions.signInError = AppError.Api(401, ErrorCodes.AUTH_INVALID_CREDENTIALS)
        val vm = LoginViewModel(sessions)
        vm.onUsernameChange("marina")
        vm.onPasswordChange("wrong")
        vm.signIn("https://prumo.minhacasa.net")

        assertEquals(ErrorCodes.AUTH_INVALID_CREDENTIALS, (vm.state.value.error as AppError.Api).code)
        assertEquals("", vm.password)
        vm.onPasswordChange("right")
        assertNull(vm.state.value.error)
    }

    @Test
    fun signingInAgainKeepsTheLastUsername() {
        val sessions =
            FakeSessionRepository(
                SessionState.SignedOut(SignOutReason.Expired, "https://prumo.minhacasa.net", "marina"),
            )
        val vm = LoginViewModel(sessions)
        assertEquals("marina", vm.username)
        vm.onPasswordChange("secret")
        vm.signIn("https://prumo.minhacasa.net")
        assertEquals(listOf(Triple("https://prumo.minhacasa.net", "marina", "secret")), sessions.signIns)
        assertTrue(sessions.state.value is SessionState.SignedIn)
    }
}
