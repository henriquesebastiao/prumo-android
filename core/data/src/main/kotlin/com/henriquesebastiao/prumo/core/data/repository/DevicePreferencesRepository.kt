package com.henriquesebastiao.prumo.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.henriquesebastiao.prumo.core.data.di.DevicePreferencesStore
import com.henriquesebastiao.prumo.core.model.DevicePreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What this device keeps for itself (docs/decisoes.md, D12): pure black, dynamic color, pinned actions. */
interface DevicePreferencesRepository {
    val preferences: Flow<DevicePreferences>

    suspend fun setPureBlack(enabled: Boolean)

    suspend fun setDynamicColor(enabled: Boolean)

    /** The ids of the actions pinned to the Home's toolbar, in order (four at most). */
    suspend fun setPinnedActions(ids: List<String>)
}

class DataStoreDevicePreferences @Inject constructor(
    @DevicePreferencesStore private val dataStore: DataStore<Preferences>,
) : DevicePreferencesRepository {
    override val preferences: Flow<DevicePreferences> = dataStore.data.map { prefs ->
        DevicePreferences(
            pureBlack = prefs[PURE_BLACK] ?: false,
            dynamicColor = prefs[DYNAMIC_COLOR] ?: true,
            pinnedActions = prefs[PINNED]?.split(SEPARATOR)?.filter { it.isNotBlank() },
        )
    }

    override suspend fun setPureBlack(enabled: Boolean) {
        dataStore.edit { it[PURE_BLACK] = enabled }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setPinnedActions(ids: List<String>) {
        dataStore.edit { it[PINNED] = ids.take(MAX_PINNED).joinToString(SEPARATOR) }
    }

    private companion object {
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PINNED = stringPreferencesKey("pinned_actions")
        const val SEPARATOR = ","
        const val MAX_PINNED = 4
    }
}
