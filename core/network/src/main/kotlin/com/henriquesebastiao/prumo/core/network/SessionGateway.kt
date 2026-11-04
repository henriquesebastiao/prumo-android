package com.henriquesebastiao.prumo.core.network

/**
 * The signed-in session as the network layer sees it: where the server is and the token to send.
 * Implemented by `:core:data`'s session repository, which keeps it encrypted on the device.
 */
interface SessionGateway {
    /** The current session, or null when signed out. Read on every request: kept in memory. */
    val activeSession: ActiveSession?

    /**
     * The server refused [token] (`401` to a request that carried it): the session is over, unless
     * a new sign-in replaced it meanwhile.
     */
    fun onUnauthorized(token: String)
}

/** A server's base URL (no trailing slash) and the bearer token of this device's session on it. */
data class ActiveSession(val baseUrl: String, val token: String)
