package com.henriquesebastiao.prumo.core.data.repository

import com.henriquesebastiao.prumo.core.data.session.SessionRepository
import com.henriquesebastiao.prumo.core.model.DeviceSession
import com.henriquesebastiao.prumo.core.model.InstalledModule
import com.henriquesebastiao.prumo.core.model.Outcome
import com.henriquesebastiao.prumo.core.model.Preferences
import com.henriquesebastiao.prumo.core.model.ThemeMode
import com.henriquesebastiao.prumo.core.model.User
import com.henriquesebastiao.prumo.core.model.map
import com.henriquesebastiao.prumo.core.model.onOk
import com.henriquesebastiao.prumo.core.network.dto.PasswordChangeDto
import com.henriquesebastiao.prumo.core.network.dto.PreferencesUpdateDto
import com.henriquesebastiao.prumo.core.network.dto.UserUpdateDto
import com.henriquesebastiao.prumo.core.network.toModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The account on the server: the user, the preferences (theme, time zone, currency, modules and
 * their order), the installed modules and the active sessions. What the screens observe is kept in
 * [preferences] and [user]; a refresh replaces it.
 */
interface AccountRepository {
    val user: StateFlow<User?>
    val preferences: StateFlow<Preferences?>

    suspend fun refreshUser(): Outcome<User>

    suspend fun refreshPreferences(): Outcome<Preferences>

    suspend fun updateName(name: String): Outcome<User>

    /** `PUT /me/password`: the server ends the other sessions (S5). */
    suspend fun changePassword(current: String, new: String): Outcome<Unit>

    suspend fun setTheme(theme: ThemeMode): Outcome<Preferences>

    /** The enabled modules in their order; the installed ones left out are disabled. */
    suspend fun setEnabledModules(ids: List<String>): Outcome<Preferences>

    suspend fun modules(): Outcome<List<InstalledModule>>

    suspend fun sessions(): Outcome<List<DeviceSession>>

    suspend fun revokeSession(id: String): Outcome<Unit>

    /** Clears what was kept of the account (signed out, another user may come). */
    fun clear()
}

@Singleton
class DefaultAccountRepository @Inject constructor(
    private val calls: ServerCalls,
    private val session: SessionRepository,
) : AccountRepository {
    private val mutableUser = MutableStateFlow<User?>(null)
    private val mutablePreferences = MutableStateFlow<Preferences?>(null)
    override val user: StateFlow<User?> = mutableUser.asStateFlow()
    override val preferences: StateFlow<Preferences?> = mutablePreferences.asStateFlow()

    override suspend fun refreshUser(): Outcome<User> =
        calls.system { me() }.map { it.toModel() }.onOk { mutableUser.value = it }

    override suspend fun refreshPreferences(): Outcome<Preferences> =
        calls.system { preferences() }.map { it.toModel() }.onOk { mutablePreferences.value = it }

    override suspend fun updateName(name: String): Outcome<User> =
        calls.system { updateMe(UserUpdateDto(name.trim())) }.map { it.toModel() }.onOk {
            mutableUser.value = it
            session.updateDisplayName(it.displayName)
        }

    override suspend fun changePassword(current: String, new: String): Outcome<Unit> =
        calls.system { changePassword(PasswordChangeDto(current, new)) }

    override suspend fun setTheme(theme: ThemeMode): Outcome<Preferences> =
        calls.system { updatePreferences(PreferencesUpdateDto(theme = theme.wire)) }
            .map { it.toModel() }
            .onOk { mutablePreferences.value = it }

    override suspend fun setEnabledModules(ids: List<String>): Outcome<Preferences> =
        calls.system { updatePreferences(PreferencesUpdateDto(enabledModules = ids)) }
            .map { it.toModel() }
            .onOk { mutablePreferences.value = it }

    override suspend fun modules(): Outcome<List<InstalledModule>> =
        calls.system { modules() }.map { list -> list.items.map { it.toModel() }.sortedBy { it.position } }

    override suspend fun sessions(): Outcome<List<DeviceSession>> =
        calls.system { sessions() }.map { list -> list.items.map { it.toModel() } }

    override suspend fun revokeSession(id: String): Outcome<Unit> = calls.system { revokeSession(id) }

    override fun clear() {
        mutableUser.value = null
        mutablePreferences.value = null
    }
}
