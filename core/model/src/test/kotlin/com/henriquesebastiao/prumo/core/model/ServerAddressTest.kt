package com.henriquesebastiao.prumo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAddressTest {
    @Test
    fun aPublicNameTriesHttpsFirst() {
        assertEquals(
            listOf("https://prumo.minhacasa.net", "http://prumo.minhacasa.net"),
            ServerAddress.candidates(" prumo.minhacasa.net/ "),
        )
    }

    @Test
    fun aHomeNetworkAddressTriesHttpFirst() {
        assertEquals(
            listOf("http://192.168.0.10:8000", "https://192.168.0.10:8000"),
            ServerAddress.candidates("192.168.0.10:8000"),
        )
        assertEquals(
            listOf("http://nas.local:8000", "https://nas.local:8000"),
            ServerAddress.candidates("nas.local:8000"),
        )
        assertEquals(listOf("http://10.0.2.2:8000", "https://10.0.2.2:8000"), ServerAddress.candidates("10.0.2.2:8000"))
    }

    @Test
    fun aSchemeIsKeptAndOnlyThatOneIsTried() {
        assertEquals(listOf("https://prumo.exemplo.com"), ServerAddress.candidates("HTTPS://Prumo.Exemplo.com/"))
        assertEquals(listOf("http://10.0.2.2:8000"), ServerAddress.candidates("http://10.0.2.2:8000"))
    }

    @Test
    fun aPathIsKeptAndApiIsDropped() {
        assertEquals(listOf("https://exemplo.com/prumo"), ServerAddress.candidates("https://exemplo.com/prumo/"))
        assertEquals(listOf("https://exemplo.com"), ServerAddress.candidates("https://exemplo.com/api"))
    }

    @Test
    fun textThatIsNotAnAddressGivesNothing() {
        assertEquals(emptyList<String>(), ServerAddress.candidates(""))
        assertEquals(emptyList<String>(), ServerAddress.candidates("meu servidor"))
        assertEquals(emptyList<String>(), ServerAddress.candidates("ftp://exemplo.com"))
        assertEquals(emptyList<String>(), ServerAddress.candidates("https://exemplo.com/?x=1"))
    }

    @Test
    fun displayDropsTheScheme() {
        assertEquals("prumo.minhacasa.net", ServerAddress.display("https://prumo.minhacasa.net"))
        assertEquals("10.0.2.2:8000", ServerAddress.display("http://10.0.2.2:8000"))
    }

    @Test
    fun plainHttpIsOnlyFlaggedOutsideTheHomeNetwork() {
        assertTrue(ServerAddress.isInsecurePublic("http://prumo.exemplo.com"))
        assertFalse(ServerAddress.isInsecurePublic("https://prumo.exemplo.com"))
        assertFalse(ServerAddress.isInsecurePublic("http://192.168.0.10:8000"))
        assertFalse(ServerAddress.isInsecurePublic("http://nas"))
    }
}
