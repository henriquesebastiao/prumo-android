package com.henriquesebastiao.prumo.system.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.prumo.core.data.repository.AccountRepository
import com.henriquesebastiao.prumo.core.feature.PrumoFeature
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.system.ModuleRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A module in Settings › Módulos: the app's feature and whether the user has it on. */
data class ModuleItem(val feature: PrumoFeature, val enabled: Boolean) {
    val id: String get() = feature.id
}

data class ModulesUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val items: List<ModuleItem> = emptyList(),
    /** Installed on the server but unknown to this version of the app. */
    val unknown: Int = 0,
)

@HiltViewModel
class ModulesViewModel @Inject constructor(
    private val account: AccountRepository,
    private val registry: ModuleRegistry,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ModulesUiState())
    val state: StateFlow<ModulesUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true) }
        viewModelScope.launch {
            when (val outcome = account.modules()) {
                is Outcome.Ok -> {
                    val known = registry.installed(outcome.value)
                    mutableState.value = ModulesUiState(
                        loading = false,
                        items = known.map { (feature, module) -> ModuleItem(feature, module.enabled) },
                        unknown = outcome.value.size - known.size,
                    )
                }

                is Outcome.Failed -> mutableState.update { it.copy(loading = false, error = outcome.error) }
            }
        }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        mutableState.update { state ->
            state.copy(items = state.items.map { if (it.id == id) it.copy(enabled = enabled) else it })
        }
        save()
    }

    /** The modules in a new order (after a drag or a move action). */
    fun reorder(ids: List<String>) {
        mutableState.update { state ->
            val byId = state.items.associateBy { it.id }
            state.copy(items = ids.mapNotNull { byId[it] })
        }
        save()
    }

    /** `PATCH /me/preferences` with the enabled ones in order; the server disables the rest (D20). */
    private fun save() {
        val enabled = state.value.items.filter { it.enabled }.map { it.id }
        viewModelScope.launch {
            val outcome = account.setEnabledModules(enabled)
            if (outcome is Outcome.Failed) mutableState.update { it.copy(error = outcome.error) }
        }
    }
}
