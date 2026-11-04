package com.henriquesebastiao.prumo.system.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.session.SessionRepository
import com.henriquesebastiao.prumo.core.data.session.SessionState
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Signing in: [error] is shown on the password field ("Usuário ou senha incorretos"). */
data class LoginUiState(val submitting: Boolean = false, val error: AppError? = null)

@HiltViewModel
class LoginViewModel @Inject constructor(private val sessions: SessionRepository) : ViewModel() {
    /** The username of the last session on this device: signing in again only needs the password. */
    var username by mutableStateOf((sessions.state.value as? SessionState.SignedOut)?.lastUsername.orEmpty())
        private set
    var password by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = mutableState.asStateFlow()

    fun onUsernameChange(value: String) {
        username = value
        mutableState.value = mutableState.value.copy(error = null)
    }

    fun onPasswordChange(value: String) {
        password = value
        mutableState.value = mutableState.value.copy(error = null)
    }

    /** Signs in on [baseUrl]. On success the session changes and the app leaves the first run. */
    fun signIn(baseUrl: String) {
        if (username.isBlank() || password.isEmpty() || mutableState.value.submitting) return
        mutableState.value = LoginUiState(submitting = true)
        viewModelScope.launch {
            when (val outcome = sessions.signIn(baseUrl, username, password)) {
                is Outcome.Ok -> mutableState.value = LoginUiState()

                is Outcome.Failed -> {
                    password = ""
                    mutableState.value = LoginUiState(error = outcome.error)
                }
            }
        }
    }
}
