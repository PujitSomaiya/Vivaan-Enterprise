package com.vivaanenterprise.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for [IndianCurrencyFormatter].
 *
 * No Android, Room, Firebase, or Compose dependencies — pure JVM.
 *
 * Verified output format: `RUPEES [WORDS] ONLY` (ALL CAPS).
 * With non-zero paise: `RUPEES [WORDS] AND [PAISE WORDS] PAISE ONLY`.
 * Special cases: zero → `RUPEES ZERO ONLY`, paise-only → `RUPEES ZERO AND XX PAISE ONLY`.
 *
 * PDF_SPEC.md verified examples:
 * - ₹17,700.00 → `RUPEES SEVENTEEN THOUSAND SEVEN HUNDRED ONLY`
 * - ₹2,700.00  → `RUPEES TWO THOUSAND SEVEN HUNDRED ONLY`
 * - ₹20,650.00 → `RUPEES TWENTY THOUSAND SIX HUNDRED FIFTY ONLY`
 * - ₹48,852.00 → `RUPEES FORTY EIGHT THOUSAND EIGHT HUNDRED FIFTY TWO ONLY`
 */
class IndianCurrencyFormatterTest {

    private lateinit var formatter: IndianCurrencyFormatter

    @Before
    fun setUp() {
        formatter = IndianCurrencyFormatter()
    }

    private fun format(paise: Long) = formatter.formatAmountInWords(paise)

    // ── Zero and trivial ──────────────────────────────────────────────────────────

    @Test fun testZero()        { assertEquals("RUPEES ZERO ONLY",  format(0L)) }
    @Test fun testOneRupee()    { assertEquals("RUPEES ONE ONLY",   format(100L)) }
    @Test fun testTwoRupees()   { assertEquals("RUPEES TWO ONLY",   format(200L)) }
    @Test fun testNineRupees()  { assertEquals("RUPEES NINE ONLY",  format(900L)) }

    // ── Teens ─────────────────────────────────────────────────────────────────────

    @Test fun testTenRupees()       { assertEquals("RUPEES TEN ONLY",       format(1_000L)) }
    @Test fun testElevenRupees()    { assertEquals("RUPEES ELEVEN ONLY",    format(1_100L)) }
    @Test fun testFifteenRupees()   { assertEquals("RUPEES FIFTEEN ONLY",   format(1_500L)) }
    @Test fun testNineteenRupees()  { assertEquals("RUPEES NINETEEN ONLY",  format(1_900L)) }

    // ── Tens ──────────────────────────────────────────────────────────────────────

    @Test fun testTwentyRupees()    { assertEquals("RUPEES TWENTY ONLY",    format(2_000L)) }
    @Test fun testTwentyOneRupees() { assertEquals("RUPEES TWENTY ONE ONLY",format(2_100L)) }
    @Test fun testNinetyNineRupees(){ assertEquals("RUPEES NINETY NINE ONLY",format(9_900L)) }

    // ── Hundreds ──────────────────────────────────────────────────────────────────

    @Test fun testOneHundred()      { assertEquals("RUPEES ONE HUNDRED ONLY",            format(10_000L)) }
    @Test fun testOneHundredOne()   { assertEquals("RUPEES ONE HUNDRED ONE ONLY",        format(10_100L)) }
    @Test fun testNineHundredNinetyNine() {
        assertEquals("RUPEES NINE HUNDRED NINETY NINE ONLY", format(99_900L))
    }

    // ── Thousands ─────────────────────────────────────────────────────────────────

    @Test fun testOneThousand()     { assertEquals("RUPEES ONE THOUSAND ONLY",              format(100_000L)) }
    @Test fun testOneThousandOne()  { assertEquals("RUPEES ONE THOUSAND ONE ONLY",          format(100_100L)) }
    @Test fun testTenThousand()     { assertEquals("RUPEES TEN THOUSAND ONLY",              format(1_000_000L)) }
    @Test fun testNinetyNineThousandNineHundredNinetyNine() {
        assertEquals("RUPEES NINETY NINE THOUSAND NINE HUNDRED NINETY NINE ONLY", format(9_999_900L))
    }

    // ── Lakh ──────────────────────────────────────────────────────────────────────

    @Test fun testOneLakh()          { assertEquals("RUPEES ONE LAKH ONLY",            format(10_000_000L)) }
    @Test fun testOneLakhOne()       { assertEquals("RUPEES ONE LAKH ONE ONLY",        format(10_000_100L)) }
    @Test fun testTenLakh()          { assertEquals("RUPEES TEN LAKH ONLY",            format(100_000_000L)) }
    @Test fun testOneLakhFiftyThousand() {
        assertEquals("RUPEES ONE LAKH FIFTY THOUSAND ONLY", format(15_000_000L))
    }

    // ── Crore ─────────────────────────────────────────────────────────────────────

    @Test fun testOneCrore()         { assertEquals("RUPEES ONE CRORE ONLY",           format(1_000_000_000L)) }   // ₹1,00,00,000 × 100p
    @Test fun testOneCroreOne()      { assertEquals("RUPEES ONE CRORE ONE ONLY",       format(1_000_000_100L)) }   // +₹1.00
    @Test fun testTenCrore()         { assertEquals("RUPEES TEN CRORE ONLY",           format(10_000_000_000L)) }  // ₹10,00,00,000 × 100p
    @Test fun testHundredCrore()     { assertEquals("RUPEES ONE HUNDRED CRORE ONLY",   format(100_000_000_000L)) } // ₹1,00,00,00,000 × 100p

    // ── PDF_SPEC.md verified examples ─────────────────────────────────────────────

    @Test fun testPdfSpec17700() {
        assertEquals(
            "RUPEES SEVENTEEN THOUSAND SEVEN HUNDRED ONLY",
            format(1_770_000L)   // ₹17,700.00 = 1,770,000 paise
        )
    }

    @Test fun testPdfSpec2700() {
        assertEquals(
            "RUPEES TWO THOUSAND SEVEN HUNDRED ONLY",
            format(270_000L)    // ₹2,700.00 = 270,000 paise
        )
    }

    @Test fun testPdfSpec20650() {
        assertEquals(
            "RUPEES TWENTY THOUSAND SIX HUNDRED FIFTY ONLY",
            format(2_065_000L)  // ₹20,650.00 = 2,065,000 paise
        )
    }

    @Test fun testPdfSpec48852() {
        assertEquals(
            "RUPEES FORTY EIGHT THOUSAND EIGHT HUNDRED FIFTY TWO ONLY",
            format(4_885_200L)  // ₹48,852.00 = 4,885,200 paise
        )
    }

    // ── With paise component ──────────────────────────────────────────────────────

    @Test fun testOneRupeeAndFiftyPaise() {
        assertEquals("RUPEES ONE AND FIFTY PAISE ONLY", format(150L))
    }

    @Test fun testOneHundredTwentyThreeAndFortyFivePaise() {
        assertEquals("RUPEES ONE HUNDRED TWENTY THREE AND FORTY FIVE PAISE ONLY", format(12_345L))
    }

    @Test fun testOneAndOnePaise() {
        assertEquals("RUPEES ONE AND ONE PAISE ONLY", format(101L))
    }

    @Test fun testOneAndNinetyNinePaise() {
        assertEquals("RUPEES ONE AND NINETY NINE PAISE ONLY", format(199L))
    }

    @Test fun testZeroRupeesAndFiftyPaise() {
        assertEquals("RUPEES ZERO AND FIFTY PAISE ONLY", format(50L))
    }

    @Test fun testZeroRupeesAndOnePaise() {
        assertEquals("RUPEES ZERO AND ONE PAISE ONLY", format(1L))
    }

    // ── Boundary — first paise amount that has a rupee component ─────────────────

    @Test fun testExactlyOneRupeeZeroPaise() {
        assertEquals("RUPEES ONE ONLY", format(100L))
    }

    @Test fun testNinetyNinePaiseOnly() {
        assertEquals("RUPEES ZERO AND NINETY NINE PAISE ONLY", format(99L))
    }

    // ── Negative input ────────────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun testNegativeThrows() { format(-1L) }

    // ── Invoice amount-in-words spot checks ───────────────────────────────────────

    @Test fun testVe06GrandTotal206500Paise() {
        assertEquals(
            "RUPEES TWO THOUSAND SIXTY FIVE ONLY",
            format(206_500L)    // ₹2,065.00
        )
    }

    @Test fun testIntraStateTaxAmount27000Paise() {
        assertEquals(
            "RUPEES TWO HUNDRED SEVENTY ONLY",
            format(27_000L)     // ₹270.00 (tax on ₹1,500 at 18%)
        )
    }
}
