package com.henriquesebastiao.prumo.core.model

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatsTest {
    // The space between the symbol and the value is a no-break space (U+00A0), as in pt-BR.
    private val nbsp = ' '

    @Test
    fun moneyIsWrittenInReaisWithThousandsAndCents() {
        assertEquals("R$${nbsp}8.742,35", Formats.money(874_235))
        assertEquals("R$${nbsp}0,05", Formats.money(5))
        assertEquals("-R$${nbsp}1.482,90", Formats.money(-148_290))
    }

    @Test
    fun moneyFollowsTheUsersCurrencyAndFallsBackToReais() {
        assertEquals("US$${nbsp}10,00", Formats.money(1_000, "USD"))
        assertEquals("R$${nbsp}10,00", Formats.money(1_000, "XYZ"))
    }

    @Test
    fun distancesAndVolumes() {
        assertEquals("12.345 km", Formats.km(12_345))
        assertEquals("180 km", Formats.km(180))
        assertEquals("40,5 L", Formats.litres(40_500))
        assertEquals("9,8 L", Formats.litres(9_800))
        assertEquals("39,49 L", Formats.litres(39_490))
        assertEquals("40 L", Formats.litres(40_000))
    }

    @Test
    fun datesAndTimes() {
        assertEquals("02/11/2026", Formats.date(LocalDate.of(2026, 11, 2)))
        assertEquals("14:30", Formats.time(LocalTime.of(14, 30)))
        assertEquals("novembro", Formats.month(LocalDate.of(2026, 11, 2)))
    }

    @Test
    fun theHomesDateLineDropsFeiraAndCapitalises() {
        assertEquals("Segunda, 2 de novembro", Formats.weekdayAndDate(LocalDate.of(2026, 11, 2)))
        assertEquals("Terça, 17 de novembro", Formats.weekdayAndDate(LocalDate.of(2026, 11, 17)))
        assertEquals("Sábado, 21 de novembro", Formats.weekdayAndDate(LocalDate.of(2026, 11, 21)))
    }

    @Test
    fun pricesPerLitreAndConsumptionKeepTheServersDecimals() {
        assertEquals("R\$\u00A06,351", Formats.unitPrice(6.351))
        assertEquals("R\$\u00A06,29", Formats.unitPrice(6.29))
        assertEquals("40,3", Formats.decimal(40.3))
        assertEquals("39,73", Formats.decimal(39.73))
        assertEquals("1.234,5", Formats.decimal(1234.5))
        assertEquals("29,7", Formats.decimal(29.66, maxDecimals = 1))
    }
}
