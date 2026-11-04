package com.henriquesebastiao.prumo.core.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * What the form fields keep as typed text and send as whole numbers: money as digits of cents
 * (calculator style, "4636" is R$ 46,36), km as digits, litres as a decimal with a comma ("7,3").
 * The text a field shows is a visual transformation of what is kept (CLAUDE.md, "Campos de texto").
 */
object Inputs {
    /** The longest number a digits field takes (R$ 99.999.999,99; 9.999.999.999 km). */
    const val MAX_DIGITS = 10

    /** Whether [text] can be kept by a digits field: only digits, at most [maxDigits]. */
    fun isDigits(text: String, maxDigits: Int = MAX_DIGITS): Boolean =
        text.length <= maxDigits && text.all(Char::isDigit)

    /** The number of a digits field ("004636" is 4636), or null when empty. */
    fun number(digits: String): Long? = if (digits.isEmpty()) null else digits.toLongOrNull()

    /** The digits a field starts with for [value] (an edit): "4636" for 4636, "" for null. */
    fun digitsOf(value: Long?): String = value?.toString().orEmpty()

    /**
     * Whether [text] can be kept by a decimal field: digits with one comma (or point, from some
     * keyboards) and at most [scale] decimals.
     */
    fun isDecimal(text: String, scale: Int, maxIntegerDigits: Int = MAX_DIGITS - scale): Boolean {
        val parts = text.split(',', '.')
        if (parts.size > 2 || !parts.all { part -> part.all(Char::isDigit) }) return false
        return parts[0].length <= maxIntegerDigits && (parts.size == 1 || parts[1].length <= scale)
    }

    /** [text] of a decimal field in units of 10^-[scale] ("7,3" with 3 is 7300), or null when it has no number. */
    fun scaled(text: String, scale: Int): Long? {
        val normalized = text.replace(',', '.').trimEnd('.')
        if (normalized.isEmpty() || normalized == ".") return null
        return normalized.toBigDecimalOrNull()?.movePointRight(scale)?.setScale(0, RoundingMode.HALF_UP)?.toLong()
    }

    /** The text a decimal field starts with for [value] in units of 10^-[scale] (7300 with 3 is "7,3"). */
    fun decimalOf(value: Long?, scale: Int): String {
        if (value == null) return ""
        return BigDecimal.valueOf(value, scale).stripTrailingZeros().toPlainString().replace('.', ',')
    }
}
