package com.vivaanenterprise.app.domain.util

/**
 * Pure utility to parse human currency strings into exact Long paise without Double/Float.
 *
 * Examples:
 * - "3500" -> 350000L
 * - "3500.5" -> 350050L
 * - "3500.50" -> 350050L
 * - "0" -> 0L
 * - "0.05" -> 5L
 *
 * Rejects:
 * - Blank / empty
 * - Negative numbers ("-100")
 * - Non-numeric strings ("abc")
 * - More than 2 decimal digits ("10.123")
 * - Multiple decimal points ("10.5.2")
 * - Overflow beyond Long.MAX_VALUE
 */
object ExactCurrencyParser {

    fun parseToPaise(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("-")) return null

        val parts = trimmed.split(".")
        if (parts.size > 2) return null

        val rupeesPart = parts[0]
        if (rupeesPart.isEmpty()) return null
        if (!rupeesPart.all { it.isDigit() }) return null

        val rupees = try {
            rupeesPart.toLong()
        } catch (e: NumberFormatException) {
            return null
        }

        val paisePart = if (parts.size == 2) parts[1] else ""
        if (!paisePart.all { it.isDigit() }) return null
        if (paisePart.length > 2) return null

        val paiseInt = when (paisePart.length) {
            0 -> 0
            1 -> (paisePart[0] - '0') * 10
            2 -> (paisePart[0] - '0') * 10 + (paisePart[1] - '0')
            else -> return null
        }

        return try {
            val rupeePaise = Math.multiplyExact(rupees, 100L)
            Math.addExact(rupeePaise, paiseInt.toLong())
        } catch (e: ArithmeticException) {
            null
        }
    }
}
