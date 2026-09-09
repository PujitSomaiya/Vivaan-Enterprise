package com.vivaanenterprise.app.feature.product.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductGstUtilsTest {

    @Test
    fun testParseGstPercentageToBasisPointsValidCases() {
        assertEquals(1800, ProductGstUtils.parseGstPercentageToBasisPoints("18"))
        assertEquals(1800, ProductGstUtils.parseGstPercentageToBasisPoints("18.0"))
        assertEquals(1800, ProductGstUtils.parseGstPercentageToBasisPoints("18.00"))
        assertEquals(1250, ProductGstUtils.parseGstPercentageToBasisPoints("12.5"))
        assertEquals(1825, ProductGstUtils.parseGstPercentageToBasisPoints("18.25"))
        assertEquals(0, ProductGstUtils.parseGstPercentageToBasisPoints("0"))
        assertEquals(10000, ProductGstUtils.parseGstPercentageToBasisPoints("100"))
        assertEquals(500, ProductGstUtils.parseGstPercentageToBasisPoints("5"))
    }

    @Test
    fun testParseGstPercentageToBasisPointsInvalidCases() {
        assertNull(ProductGstUtils.parseGstPercentageToBasisPoints(""))
        assertNull(ProductGstUtils.parseGstPercentageToBasisPoints("   "))
        assertNull(ProductGstUtils.parseGstPercentageToBasisPoints("-5"))
        assertNull(ProductGstUtils.parseGstPercentageToBasisPoints("101"))
        assertNull(ProductGstUtils.parseGstPercentageToBasisPoints("18.256"))
        assertNull(ProductGstUtils.parseGstPercentageToBasisPoints("abc"))
        assertNull(ProductGstUtils.parseGstPercentageToBasisPoints("18.2.5"))
    }

    @Test
    fun testFormatBasisPointsToPercentage() {
        assertEquals("18", ProductGstUtils.formatBasisPointsToPercentage(1800))
        assertEquals("18.25", ProductGstUtils.formatBasisPointsToPercentage(1825))
        assertEquals("12.5", ProductGstUtils.formatBasisPointsToPercentage(1250))
        assertEquals("0", ProductGstUtils.formatBasisPointsToPercentage(0))
        assertEquals("100", ProductGstUtils.formatBasisPointsToPercentage(10000))
        assertEquals("", ProductGstUtils.formatBasisPointsToPercentage(null))
    }
}
