package com.henriquesebastiao.prumo.core.data.session

/** This device's session on a Prumo server: where it is, who signed in and the bearer token. */
data class Session(val baseUrl: String, val username: String, val displayName: String, val token: String)

/** Why the app is signed out. */
enum class SignOutReason {
    /** First run, or the user chose another server. */
    None,

    /** The user signed out ("Sair"). */
    SignedOut,

    /** The server refused the token (`401`): revoked from the web, or expired. */
    Expired,
}

/**
 * Where the session is. [Loading] while the encrypted session is read at start (the splash stays);
 * [SignedOut] remembers the last server and username, so signing in again needs only the password.
 */
sealed interface SessionState {
    data object Loading : SessionState

    data class SignedOut(val reason: SignOutReason, val lastServer: String?, val lastUsername: String?) : SessionState

    data class SignedIn(val session: Session) : SessionState
}
