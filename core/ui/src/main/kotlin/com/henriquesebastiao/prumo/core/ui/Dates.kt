package com.henriquesebastiao.prumo.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.henriquesebastiao.prumo.core.model.Formats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * When something happened, at the start of a list's line, as the web writes it: "Hoje, 14:30",
 * "Ontem, 21:40", "12/11, 08:12". [today] and [zone] are the user's (S3), not the device's.
 */
@Composable
@ReadOnlyComposable
fun relativeDateTime(instant: Instant, zone: ZoneId, today: LocalDate): String {
    val at = instant.atZone(zone)
    return stringResource(R.string.ui_date_at, relativeDate(at.toLocalDate(), today), Formats.time(at.toLocalTime()))
}

/** A day the same way, without the time: "Hoje", "Ontem", "12/11". */
@Composable
@ReadOnlyComposable
fun relativeDate(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.ui_today)
    today.minusDays(1) -> stringResource(R.string.ui_yesterday)
    else -> Formats.shortDate(date)
}
