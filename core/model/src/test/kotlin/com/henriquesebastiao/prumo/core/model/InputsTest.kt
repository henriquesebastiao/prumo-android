package com.henriquesebastiao.prumo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputsTest {
    @Test
    fun digitsFieldsKeepOnlyDigits() {
        assertTrue(Inputs.isDigits(""))
        assertTrue(Inputs.isDigits("4636"))
        assertFalse(Inputs.isDigits("46,36"))
        assertFalse(Inputs.isDigits("12345678901"))
        assertEquals(4_636L, Inputs.number("004636"))
        assertEquals(0L, Inputs.number("000"))
        assertNull(Inputs.number(""))
        assertEquals("4636", Inputs.digitsOf(4_636))
        assertEquals("", Inputs.digitsOf(null))
    }

    @Test
    fun litresAreADecimalWithACommaOrAPoint() {
        assertTrue(Inputs.isDecimal("7,3", scale = 3))
        assertTrue(Inputs.isDecimal("7.300", scale = 3))
        assertTrue(Inputs.isDecimal("7,", scale = 3))
        assertFalse(Inputs.isDecimal("7,3001", scale = 3))
        assertFalse(Inputs.isDecimal("7,3,1", scale = 3))
        assertFalse(Inputs.isDecimal("7a", scale = 3))
        assertEquals(7_300L, Inputs.scaled("7,3", 3))
        assertEquals(7_000L, Inputs.scaled("7,", 3))
        assertEquals(350L, Inputs.scaled(",35", 3))
        assertNull(Inputs.scaled(",", 3))
        assertNull(Inputs.scaled("", 3))
        assertEquals("7,3", Inputs.decimalOf(7_300, 3))
        assertEquals("40", Inputs.decimalOf(40_000, 3))
        assertEquals("", Inputs.decimalOf(null, 3))
    }
}
