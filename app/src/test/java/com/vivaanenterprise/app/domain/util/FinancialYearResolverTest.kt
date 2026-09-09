package com.vivaanenterprise.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class FinancialYearResolverTest {

    private fun makeKolkataTimestamp(year: Int, month1Based: Int, day: Int, hour: Int = 12, minute: Int = 0): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
            clear()
            set(year, month1Based - 1, day, hour, minute, 0)
        }
        return cal.timeInMillis
    }

    @Test
    fun testFinancialYearAsiaKolkataBoundaries() {
        // 31 March 2026 23:59:59 IST -> 2025-26
        val mar31_2026 = makeKolkataTimestamp(2026, 3, 31, 23, 59)
        assertEquals("2025-26", FinancialYearResolver.resolveFinancialYear(mar31_2026))

        // 1 April 2026 00:00:01 IST -> 2026-27
        val apr1_2026 = makeKolkataTimestamp(2026, 4, 1, 0, 1)
        assertEquals("2026-27", FinancialYearResolver.resolveFinancialYear(apr1_2026))

        // 31 March 2027 23:59:59 IST -> 2026-27
        val mar31_2027 = makeKolkataTimestamp(2027, 3, 31, 23, 59)
        assertEquals("2026-27", FinancialYearResolver.resolveFinancialYear(mar31_2027))

        // 1 April 2027 00:00:01 IST -> 2027-28
        val apr1_2027 = makeKolkataTimestamp(2027, 4, 1, 0, 1)
        assertEquals("2027-28", FinancialYearResolver.resolveFinancialYear(apr1_2027))
    }

    @Test
    fun testTimestampNearUtcBoundaryUsesAsiaKolkata() {
        // 31 March 2026 20:00:00 UTC is 1 April 2026 01:30:00 IST (+5:30)
        // In UTC date it is March 31, but in Asia/Kolkata it is April 1.
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.MARCH, 31, 20, 0, 0)
        }
        val timestamp = cal.timeInMillis
        assertEquals("2026-27", FinancialYearResolver.resolveFinancialYear(timestamp))
    }
}
