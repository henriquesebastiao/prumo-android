package com.henriquesebastiao.prumo.core.network.testing

/**
 * Real responses of the server, recorded by `scripts/record-fixtures.py` from the
 * design-board data (`backend/scripts/e2e_server.py` of the server). Never written by hand.
 */
object Fixtures {
    /** The text of `fixtures/[name]` (`dashboard.json`, `login.json`…). */
    fun read(name: String): String = resource("/fixtures/$name")

    internal fun resource(path: String): String =
        checkNotNull(Fixtures::class.java.getResource(path)) { "Missing test resource $path" }.readText()
}
