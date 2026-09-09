package com.vivaanenterprise.app.core.pdf

import com.vivaanenterprise.app.domain.util.IndianCurrencyFormatter

/**
 * Common formatting utilities for PDF rendering using exact minor unit arithmetic (Paise) and deterministic strings.
 */
object PdfFormattingUtils {

    /**
     * Formats paise (Long) into Indian grouping format e.g. 1750000 -> "17,500.00", 315000 -> "3,150.00".
     */
    fun formatPaiseToCurrency(paise: Long): String {
        val isNegative = paise < 0
        val absPaise = if (isNegative) -paise else paise
        val rupees = absPaise / 100L
        val remainingPaise = absPaise % 100L

        val rupeesStr = formatIndianRupeesNumber(rupees)
        val paiseStr = String.format("%02d", remainingPaise)
        val result = "$rupeesStr.$paiseStr"
        return if (isNegative) "-$result" else result
    }

    /**
     * Formats a raw Long integer (rupees) using Indian numbering system comma separation (e.g. 1234567 -> "12,34,567").
     */
    private fun formatIndianRupeesNumber(n: Long): String {
        if (n < 1000) return n.toString()
        val str = n.toString()
        val lastThree = str.substring(str.length - 3)
        val remaining = str.substring(0, str.length - 3)

        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            if (count == 2) {
                sb.insert(0, ',')
                count = 0
            }
            sb.insert(0, remaining[i])
            count++
        }
        return if (sb.isNotEmpty()) "$sb,$lastThree" else lastThree
    }

    /**
     * Converts GST rate basis points (e.g. 1800 for 18%, 250 for 2.5%, 900 for 9%) into human-readable percentage string.
     */
    fun formatGstRateBasisPoints(basisPoints: Int): String {
        val whole = basisPoints / 100
        val fraction = basisPoints % 100
        return if (fraction == 0) {
            "$whole%"
        } else if (fraction % 10 == 0) {
            "$whole.${fraction / 10}%"
        } else {
            String.format("%d.%02d%%", whole, fraction)
        }
    }
}
