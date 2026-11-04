package com.henriquesebastiao.prumo.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ServerIconsTest {
    @Test
    fun everyIconTheServerCreatesHasItsOwnSymbol() {
        // ACCOUNT_ICONS and DEFAULT_CATEGORIES of the server's finance module.
        val created = listOf(
            "landmark", "vault", "wallet", "trending-up", "utensils", "house", "bus", "fuel", "heart-pulse",
            "graduation-cap", "ticket", "shopping-bag", "repeat", "tag", "briefcase", "code-xml",
        )
        created.forEach { assertNotEquals(it, PrumoIcons.Category, ServerIcons.of(it)) }
    }

    @Test
    fun anUnknownNameIsTheGenericIcon() {
        assertEquals(PrumoIcons.AccountBalance, ServerIcons.of("landmark"))
        assertEquals(PrumoIcons.Category, ServerIcons.of("rocket"))
        assertEquals(PrumoIcons.Category, ServerIcons.of(null))
    }
}
