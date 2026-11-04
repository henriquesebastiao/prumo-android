package com.henriquesebastiao.prumo.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.henriquesebastiao.prumo.core.data.di.SessionPreferences
import java.security.GeneralSecurityException
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * The session on disk (DataStore `session`): the server, the user and the token, encrypted by
 * [TokenCipher]. The server and the username stay after signing out, to sign in again quickly.
 */
class SessionStore @Inject constructor(
    @SessionPreferences private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
) {
    /** What is stored: a [Session] when there is a token that opens, else the last server and user. */
    suspend fun read(): SessionState {
        val prefs = dataStore.data.first()
        val baseUrl = prefs[BASE_URL]
        val username = prefs[USERNAME]
        val token = prefs[TOKEN]?.let { encrypted ->
            try {
                cipher.decrypt(encrypted)
            } catch (_: GeneralSecurityException) {
                // A lost or rotated Keystore key (a backup restored on another device): sign in again.
                null
            }
        }
        return if (baseUrl != null && username != null && token != null) {
            SessionState.SignedIn(Session(baseUrl, username, prefs[DISPLAY_NAME] ?: username, token))
        } else {
            SessionState.SignedOut(SignOutReason.None, baseUrl, username)
        }
    }

    suspend fun save(session: Session) {
        val encrypted = cipher.encrypt(session.token)
        dataStore.edit {
            it[BASE_URL] = session.baseUrl
            it[USERNAME] = session.username
            it[DISPLAY_NAME] = session.displayName
            it[TOKEN] = encrypted
        }
    }

    /** Drops the token, keeping the server and the username. */
    suspend fun clearToken() {
        dataStore.edit { it.remove(TOKEN) }
    }

    /** Forgets everything: another server. */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val BASE_URL = stringPreferencesKey("base_url")
        val USERNAME = stringPreferencesKey("username")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val TOKEN = stringPreferencesKey("token")
    }
}
