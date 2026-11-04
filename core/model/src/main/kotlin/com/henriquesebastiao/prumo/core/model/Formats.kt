package com.henriquesebastiao.prumo.core.model

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/**
 * Values as the design writes them, in pt-BR: `R$ 1.234,56 · 02/11/2026 · 14:30 · 12.345 km · 40,5 L`.
 * Money and volume come in integers from the server (cents, millilitres); nothing here computes a
 * value, it only writes one.
 */
object Formats {
    val Locale: Locale = java.util.Locale.forLanguageTag("pt-BR")

    private const val DEFAULT_CURRENCY = "BRL"
    private const val CENTS_SCALE = 2
    private const val UNIT_PRICE_SCALE = 3
    private const val ML_PER_LITRE = 1000.0

    private val dayMonthYear = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale)
    private val hourMinute = DateTimeFormatter.ofPattern("HH:mm", Locale)
    private val dayOfMonth = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale)
    private val monthName = DateTimeFormatter.ofPattern("MMMM", Locale)
    private val shortMonthName = DateTimeFormatter.ofPattern("MMM", Locale)
    private val dayMonth = DateTimeFormatter.ofPattern("dd/MM", Locale)

    /**
     * [cents] in [currency] (ISO 4217, the user's preference; F9 of the server): `R$ 1.234,56`.
     * An unknown code falls back to BRL. The space after the symbol is a no-break space.
     */
    fun money(cents: Long, currency: String = DEFAULT_CURRENCY): String {
        val format = NumberFormat.getCurrencyInstance(Locale)
        format.currency = currencyOf(currency)
        format.minimumFractionDigits = CENTS_SCALE
        format.maximumFractionDigits = CENTS_SCALE
        return format.format(BigDecimal.valueOf(cents, CENTS_SCALE))
    }

    /**
     * A price per unit with two or three decimals, as the server rounds it (the price per litre,
     * G7): `R$ 6,351`, `R$ 6,29`.
     */
    fun unitPrice(value: Double, currency: String = DEFAULT_CURRENCY): String {
        val format = NumberFormat.getCurrencyInstance(Locale)
        format.currency = currencyOf(currency)
        format.minimumFractionDigits = CENTS_SCALE
        format.maximumFractionDigits = UNIT_PRICE_SCALE
        return format.format(value)
    }

    /** A month with its year, capitalized, as a period title: `Novembro 2026`. */
    fun monthYear(month: YearMonth): String =
        "${monthName.format(month.atDay(1)).replaceFirstChar { it.titlecase(Locale) }} ${month.year}"

    /** A month in three letters, for a chart's axis: `nov`. */
    fun shortMonth(month: YearMonth): String = shortMonthName.format(month.atDay(1)).trimEnd('.')

    /** Day and month in digits: `02/11`. */
    fun shortDate(date: LocalDate): String = dayMonth.format(date)

    /** A decimal with up to [maxDecimals] places (a consumption): `40,3`, `39,73`, `1.234,5`. */
    fun decimal(value: Double, maxDecimals: Int = 2): String {
        val format = NumberFormat.getNumberInstance(Locale)
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = maxDecimals
        return format.format(value)
    }

    /** A whole number with thousands separators: `12.345`. */
    fun integer(value: Long): String = NumberFormat.getIntegerInstance(Locale).format(value)

    /** A distance: `12.345 km`. */
    fun km(km: Long): String = "${integer(km)} km"

    /** A volume in litres from millilitres, with up to two decimals: `40,5 L`, `9,8 L`, `40 L`. */
    fun litres(ml: Long): String {
        val litres = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale)).format(ml / ML_PER_LITRE)
        return "$litres L"
    }

    /** `02/11/2026`. */
    fun date(date: LocalDate): String = dayMonthYear.format(date)

    /** `14:30`. */
    fun time(time: LocalTime): String = hourMinute.format(time)

    /** `2 de novembro`. */
    fun dayAndMonth(date: LocalDate): String = dayOfMonth.format(date)

    /** `novembro`. */
    fun month(date: LocalDate): String = monthName.format(date)

    /** The Home's date line: `Segunda, 2 de novembro` (the weekday without "-feira", capitalised). */
    fun weekdayAndDate(date: LocalDate): String {
        val weekday = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale)
            .substringBefore('-')
            .replaceFirstChar { it.titlecase(Locale) }
        return "$weekday, ${dayAndMonth(date)}"
    }

    private fun currencyOf(code: String): Currency = try {
        Currency.getInstance(code)
    } catch (_: IllegalArgumentException) {
        Currency.getInstance(DEFAULT_CURRENCY)
    }
}
