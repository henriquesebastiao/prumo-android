package com.henriquesebastiao.prumo.system.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.ServerRepository
import com.henriquesebastiao.prumo.core.model.ServerCheck
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The server step: testing the address shows what the server answered; [check] null is untested. */
data class ServerUiState(val checking: Boolean = false, val check: ServerCheck? = null) {
    /** "Continuar" opens only with a Prumo server found. */
    val found: ServerCheck.Found? get() = check as? ServerCheck.Found
}

@HiltViewModel
class ServerViewModel @Inject constructor(private val server: ServerRepository) : ViewModel() {
    /** What the user types: synchronous snapshot state, so no keystroke is lost. */
    var address by mutableStateOf("")
        private set

    private val mutableState = MutableStateFlow(ServerUiState())
    val state: StateFlow<ServerUiState> = mutableState.asStateFlow()
    private var job: Job? = null

    fun onAddressChange(value: String) {
        address = value
        job?.cancel()
        mutableState.value = ServerUiState()
    }

    /** Tests what was typed (`GET /api/health`). Nothing typed, nothing tested. */
    fun test() {
        val typed = address.trim()
        if (typed.isEmpty()) return
        job?.cancel()
        mutableState.update { it.copy(checking = true) }
        job = viewModelScope.launch {
            val result = server.check(typed)
            mutableState.value = ServerUiState(checking = false, check = result)
        }
    }
}
