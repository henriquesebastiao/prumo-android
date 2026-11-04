package com.henriquesebastiao.prumo.core.designsystem.color

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The schemes against the values the design system was generated with (its tokens/colors.css,
 * seed #136B79): the same algorithm gives the Prumo's from its seeds.
 */
class TonalSchemesTest {
    private val netrik = 0xFF136B79.toInt()

    private fun hex(argb: Int) = "#%06X".format(argb and 0xFFFFFF)

    @Test
    fun theLightSchemeMatchesTheDesignSystemsTokens() {
        val scheme = tonalScheme(netrik, dark = false)
        assertEquals("#006877", hex(scheme.argb { primary }))
        assertEquals("#A2EEFF", hex(scheme.argb { primaryContainer }))
        assertEquals("#F5FAFC", hex(scheme.argb { surface }))
        assertEquals("#E9EFF0", hex(scheme.argb { surfaceContainer }))
        assertEquals("#3F484A", hex(scheme.argb { onSurfaceVariant }))
    }

    @Test
    fun theDarkSchemeMatchesTheDesignSystemsTokens() {
        val scheme = tonalScheme(netrik, dark = true)
        assertEquals("#83D2E3", hex(scheme.argb { primary }))
        assertEquals("#0E1416", hex(scheme.argb { surface }))
        assertEquals("#303637", hex(scheme.argb { surfaceContainerHighest }))
    }

    @Test
    fun successAndWarningAreHarmonizedWithTheSeed() {
        assertEquals("#006D43", hex(extendedTones(Seeds.SUCCESS, netrik, dark = false).color))
        assertEquals("#8AF8BA", hex(extendedTones(Seeds.SUCCESS, netrik, dark = false).container))
        assertEquals("#FFB95E", hex(extendedTones(Seeds.WARNING, netrik, dark = true).color))
    }
}
