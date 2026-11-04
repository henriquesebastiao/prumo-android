package com.henriquesebastiao.prumo.core.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemModelsTest {
    @Test
    fun todayIsInTheUsersZoneNotTheDevices() {
        // 01:30 UTC on the 18th is still the 17th in São Paulo (UTC-3).
        val now = Instant.parse("2026-11-18T01:30:00Z")
        val prefs = Preferences.Default.copy(timezone = "America/Sao_Paulo")
        assertEquals(LocalDate.of(2026, 11, 17), prefs.today(now))
        assertEquals(LocalDate.of(2026, 11, 18), prefs.copy(timezone = "Europe/Lisbon").today(now))
    }

    @Test
    fun anUnknownZoneFallsBackToTheDevices() {
        assertEquals(java.time.ZoneId.systemDefault(), Preferences.Default.copy(timezone = "Mars/Base").zone)
    }

    @Test
    fun unknownWireValuesDoNotBreak() {
        assertEquals(ThemeMode.Dark, ThemeMode.fromWire("dark"))
        assertEquals(ThemeMode.System, ThemeMode.fromWire("sepia"))
        assertEquals(SessionClient.App, SessionClient.fromWire("app"))
    }

    @Test
    fun theGreetingFollowsTheHour() {
        assertEquals(DayPart.Night, DayPart.of(4))
        assertEquals(DayPart.Morning, DayPart.of(5))
        assertEquals(DayPart.Afternoon, DayPart.of(14))
        assertEquals(DayPart.Night, DayPart.of(18))
    }

    @Test
    fun outcomesMapAndExposeTheirParts() {
        val ok: Outcome<Int> = Outcome.Ok(2)
        val failed: Outcome<Int> = Outcome.Failed(AppError.SessionExpired)
        assertEquals(4, ok.map { it * 2 }.getOrNull())
        assertEquals(AppError.SessionExpired, failed.map { it * 2 }.errorOrNull())
    }
}
