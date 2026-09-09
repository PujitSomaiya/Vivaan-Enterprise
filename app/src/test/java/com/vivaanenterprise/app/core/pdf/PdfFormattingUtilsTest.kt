package com.vivaanenterprise.app.core.pdf

import com.vivaanenterprise.app.domain.util.IndianCurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfFormattingUtilsTest {

    @Test
    fun testFormatPaiseToCurrency_standardValues() {
        assertEquals("17,500.00", PdfFormattingUtils.formatPaiseToCurrency(1_750_000L))
        assertEquals("3,150.00", PdfFormattingUtils.formatPaiseToCurrency(315_000L))
        assertEquals("20,650.00", PdfFormattingUtils.formatPaiseToCurrency(2_065_000L))
        assertEquals("48,852.00", PdfFormattingUtils.formatPaiseToCurrency(4_885_200L))
        assertEquals("0.00", PdfFormattingUtils.formatPaiseToCurrency(0L))
        assertEquals("0.50", PdfFormattingUtils.formatPaiseToCurrency(50L))
        assertEquals("10,00,00,000.00", PdfFormattingUtils.formatPaiseToCurrency(100_000_000_00L))
    }

    @Test
    fun testFormatGstRateBasisPoints() {
        assertEquals("18%", PdfFormattingUtils.formatGstRateBasisPoints(1800))
        assertEquals("9%", PdfFormattingUtils.formatGstRateBasisPoints(900))
        assertEquals("2.5%", PdfFormattingUtils.formatGstRateBasisPoints(250))
        assertEquals("0%", PdfFormattingUtils.formatGstRateBasisPoints(0))
        assertEquals("5%", PdfFormattingUtils.formatGstRateBasisPoints(500))
    }
}
