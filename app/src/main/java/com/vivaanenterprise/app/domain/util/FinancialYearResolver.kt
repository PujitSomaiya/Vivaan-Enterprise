package com.vivaanenterprise.app.domain.util

import java.util.Calendar
import java.util.TimeZone

object FinancialYearResolver {
    /**
     * Resolves the Indian Financial Year string (e.g., "2026-27") for a given epoch timestamp in milliseconds.
     * Indian Financial Year runs from April 1 to March 31.
     */
    fun resolveFinancialYear(timestampMs: Long): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
            timeInMillis = timestampMs
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) // 0-indexed: Jan=0, Mar=2, Apr=3, Dec=11

        val startYear = if (month >= Calendar.APRIL) {
            year
        } else {
            year - 1
        }
        val endYearTwoDigits = (startYear + 1) % 100
        return String.format("%04d-%02d", startYear, endYearTwoDigits)
    }
}
