package com.vivaanenterprise.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExactCurrencyParserTest {

    @Test fun testZeroRupees() { assertEquals(0L, ExactCurrencyParser.parseToPaise("0")) }
    @Test fun testOneRupee() { assertEquals(100L, ExactCurrencyParser.parseToPaise("1")) }
    @Test fun testSingleDecimalDigit() { assertEquals(150L, ExactCurrencyParser.parseToPaise("1.5")) }
    @Test fun testTwoDecimalDigits() { assertEquals(150L, ExactCurrencyParser.parseToPaise("1.50")) }
    @Test fun testFractionalPaiseOnly() { assertEquals(5L, ExactCurrencyParser.parseToPaise("0.05")) }
    @Test fun testLargeRupees() { assertEquals(350000L, ExactCurrencyParser.parseToPaise("3500")) }
    @Test fun testLargeRupeesWithPaise() { assertEquals(350050L, ExactCurrencyParser.parseToPaise("3500.50")) }
    @Test fun testVe06Rate() { assertEquals(1750000L, ExactCurrencyParser.parseToPaise("17500")) }
    @Test fun testLeadingAndTrailingWhitespace() { assertEquals(250000L, ExactCurrencyParser.parseToPaise("  2500.00  ")) }

    @Test fun testRejectEmptyString() { assertNull(ExactCurrencyParser.parseToPaise("")) }
    @Test fun testRejectWhitespaceOnly() { assertNull(ExactCurrencyParser.parseToPaise("   ")) }
    @Test fun testRejectNegativeAmount() { assertNull(ExactCurrencyParser.parseToPaise("-100")) }
    @Test fun testRejectAlphabeticString() { assertNull(ExactCurrencyParser.parseToPaise("abc")) }
    @Test fun testRejectMoreThanTwoDecimals() { assertNull(ExactCurrencyParser.parseToPaise("10.123")) }
    @Test fun testRejectMultipleDecimalPoints() { assertNull(ExactCurrencyParser.parseToPaise("10.5.2")) }
    @Test fun testRejectSingleDecimalPointOnly() { assertNull(ExactCurrencyParser.parseToPaise(".")) }
    @Test fun testRejectLeadingDecimalPointOnly() { assertNull(ExactCurrencyParser.parseToPaise(".5")) }
    @Test fun testRejectLongOverflow() { assertNull(ExactCurrencyParser.parseToPaise("9999999999999999999999999999")) }
}
