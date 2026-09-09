package com.vivaanenterprise.app.domain.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts a monetary amount (expressed in paise) to its Indian-English word representation.
 *
 * Output format (ALL CAPS, consistent with PDF_SPEC.md):
 * - Whole rupees, no paise: `RUPEES SEVENTEEN THOUSAND SEVEN HUNDRED ONLY`
 * - With non-zero paise:    `RUPEES ONE HUNDRED TWENTY THREE AND FORTY FIVE PAISE ONLY`
 * - Zero amount:            `RUPEES ZERO ONLY`
 *
 * Numbering system: Indian (Crore → Lakh → Thousand → Hundred), not Western Million/Billion.
 *
 * Examples:
 * - 2_065_000 paise (₹20,650.00) → `RUPEES TWENTY THOUSAND SIX HUNDRED FIFTY ONLY`
 * - 4_885_200 paise (₹48,852.00) → `RUPEES FORTY EIGHT THOUSAND EIGHT HUNDRED FIFTY TWO ONLY`
 * - 1_00_00_000 × 100 paise (₹1 Crore) → `RUPEES ONE CRORE ONLY`
 *
 * No Double or Float is used anywhere in this class.
 */
@Singleton
class IndianCurrencyFormatter @Inject constructor() {

    /**
     * Converts [paise] to its word representation.
     *
     * @param paise Non-negative amount in paise (Long). Negative values throw [IllegalArgumentException].
     */
    fun formatAmountInWords(paise: Long): String {
        require(paise >= 0) { "Amount must be non-negative, got $paise" }

        val rupees = paise / 100L
        val remainingPaise = (paise % 100L).toInt()

        val sb = StringBuilder("RUPEES ")

        when {
            rupees == 0L && remainingPaise == 0 -> sb.append("ZERO")
            rupees == 0L -> {
                // Paise only — append ZERO rupees before the paise suffix
                sb.append("ZERO AND ")
                sb.append(convertBelow100(remainingPaise))
                sb.append(" PAISE")
            }
            else -> {
                sb.append(convertToWords(rupees))
                if (remainingPaise > 0) {
                    sb.append(" AND ")
                    sb.append(convertBelow100(remainingPaise))
                    sb.append(" PAISE")
                }
            }
        }

        sb.append(" ONLY")
        return sb.toString()
    }

    // ── Internal word-building ──────────────────────────────────────────────────

    private fun convertToWords(n: Long): String {
        if (n == 0L) return ""

        val sb = StringBuilder()
        var remaining = n

        val crore = remaining / 1_00_00_000L
        remaining %= 1_00_00_000L
        if (crore > 0L) {
            sb.append(convertToWords(crore))
            sb.append(" CRORE")
        }

        val lakh = remaining / 1_00_000L
        remaining %= 1_00_000L
        if (lakh > 0L) {
            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(convertBelow100(lakh.toInt()))
            sb.append(" LAKH")
        }

        val thousand = remaining / 1_000L
        remaining %= 1_000L
        if (thousand > 0L) {
            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(convertBelow100(thousand.toInt()))
            sb.append(" THOUSAND")
        }

        val hundred = remaining / 100L
        remaining %= 100L
        if (hundred > 0L) {
            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(ONES[hundred.toInt()])
            sb.append(" HUNDRED")
        }

        if (remaining > 0L) {
            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(convertBelow100(remaining.toInt()))
        }

        return sb.toString()
    }

    /**
     * Converts an integer in [0, 99] to its word representation.
     * [n] must be in range; this is a private invariant-guarded helper.
     */
    private fun convertBelow100(n: Int): String = when {
        n < 20 -> ONES[n]
        n % 10 == 0 -> TENS[n / 10]
        else -> "${TENS[n / 10]} ${ONES[n % 10]}"
    }

    companion object {
        private val ONES = arrayOf(
            "ZERO", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE",
            "TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN",
            "SEVENTEEN", "EIGHTEEN", "NINETEEN"
        )

        private val TENS = arrayOf(
            "", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"
        )
    }
}
