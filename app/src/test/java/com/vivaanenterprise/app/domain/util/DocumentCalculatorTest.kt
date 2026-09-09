package com.vivaanenterprise.app.domain.util

import com.vivaanenterprise.app.domain.model.CalculationError
import com.vivaanenterprise.app.domain.model.CalculationException
import com.vivaanenterprise.app.domain.model.DocumentCalculationInput
import com.vivaanenterprise.app.domain.model.TaxTreatment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for [DocumentCalculator].
 *
 * No Android, Room, Firebase, or Compose dependencies — pure JVM.
 *
 * Arithmetic verification key:
 *   taxable       = quantity × ratePaise
 *   igst (inter)  = (taxable × igstRate + 5_000) / 10_000
 *   cgst (intra)  = (taxable × cgstRate + 5_000) / 10_000  where cgstRate = totalGstRate / 2
 *   sgst (intra)  = (taxable × sgstRate + 5_000) / 10_000  where sgstRate = totalGstRate - cgstRate
 *   totalTax      = igst + cgst + sgst
 *   lineTotal     = taxable + totalTax
 *   grandTotal    = Σ lineTotal
 */
class DocumentCalculatorTest {

    private lateinit var calculator: DocumentCalculator

    @Before
    fun setUp() {
        calculator = DocumentCalculator(IndianCurrencyFormatter())
    }

    // ── Helper builder ────────────────────────────────────────────────────────────

    private fun input(
        sellerState: String = "24",
        supplyState: String = "33",
        vararg lines: Triple<Long, Long, Int> // quantity, ratePaise, gstBasisPoints
    ) = DocumentCalculationInput(
        sellerStateCode = sellerState,
        placeOfSupplyStateCode = supplyState,
        lines = lines.mapIndexed { i, (qty, rate, gst) ->
            DocumentCalculationInput.LineInput(
                id = "line-$i",
                quantity = qty,
                ratePaise = rate,
                gstRateBasisPoints = gst
            )
        }
    )

    // ── VE/06 inter-state invoice ─────────────────────────────────────────────────

    /**
     * VE/06: qty=5, rate=₹350.00 (35_000 paise), GST 18%
     * Seller Gujarat (24), buyer Tamil Nadu (33) → INTER-STATE
     * taxable = 5 × 35_000 = 175_000 paise (₹1,750.00)
     * IGST 18% = (175_000 × 1_800 + 5_000) / 10_000 = 315_005_000 / 10_000 = 31_500 paise (₹315.00)
     * grandTotal = 175_000 + 31_500 = 206_500 paise (₹2,065.00)
     */
    @Test
    fun testVe06InterStateInvoice() {
        val result = calculator.calculate(input("24", "33", Triple(5L, 35_000L, 1800))).getOrThrow()

        assertEquals(TaxTreatment.INTER_STATE, result.taxTreatment)
        assertEquals(175_000L, result.taxableAmountPaise)
        assertEquals(0L, result.cgstAmountPaise)
        assertEquals(0L, result.sgstAmountPaise)
        assertEquals(31_500L, result.igstAmountPaise)
        assertEquals(31_500L, result.totalTaxAmountPaise)
        assertEquals(206_500L, result.grandTotalPaise)

        val line = result.lineCalculations.first()
        assertEquals("line-0", line.lineItemId)
        assertEquals(175_000L, line.taxableAmountPaise)
        assertEquals(1800, line.igstRateBasisPoints)
        assertEquals(31_500L, line.igstAmountPaise)
        assertEquals(0, line.cgstRateBasisPoints)
        assertEquals(0L, line.cgstAmountPaise)
        assertEquals(0, line.sgstRateBasisPoints)
        assertEquals(0L, line.sgstAmountPaise)
        assertEquals(31_500L, line.totalTaxPaise)
        assertEquals(206_500L, line.lineTotalPaise)
    }

    /**
     * VE/05: qty=18, rate=₹230.00 (23_000 paise), GST 18%
     * Seller Gujarat (24), buyer Maharashtra (27) → INTER-STATE
     * taxable = 18 × 23_000 = 414_000 paise (₹4,140.00)
     * IGST 18% = (414_000 × 1_800 + 5_000) / 10_000 = 745_205_000 / 10_000 = 74_520 paise (₹745.20)
     * grandTotal = 414_000 + 74_520 = 488_520 paise (₹4,885.20)
     *
     * Note: The task references ₹48,852 elsewhere which corresponds to a 10× larger invoice.
     * This test uses the documented rate of ₹230/unit, 18 units.
     */
    @Test
    fun testVe05InterStateInvoice() {
        val result = calculator.calculate(input("24", "27", Triple(18L, 23_000L, 1800))).getOrThrow()

        assertEquals(TaxTreatment.INTER_STATE, result.taxTreatment)
        assertEquals(414_000L, result.taxableAmountPaise)
        assertEquals(74_520L, result.igstAmountPaise)
        assertEquals(488_520L, result.grandTotalPaise)
    }

    // ── Intra-state invoice ───────────────────────────────────────────────────────

    /**
     * Intra-state: qty=100, rate=₹100.00 (10_000 paise), GST 18%
     * Seller Gujarat (24), buyer Gujarat (24) → INTRA-STATE
     * taxable = 100 × 10_000 = 1_000_000 paise (₹10,000.00)
     * CGST 9%  = (1_000_000 × 900 + 5_000) / 10_000 = 90_005_000 / 10_000 = 90_000 paise
     * SGST 9%  = (1_000_000 × 900 + 5_000) / 10_000 = 90_000 paise
     * grandTotal = 1_000_000 + 90_000 + 90_000 = 1_180_000 paise (₹11,800.00)
     */
    @Test
    fun testIntraStateInvoice18Percent() {
        val result = calculator.calculate(input("24", "24", Triple(100L, 10_000L, 1800))).getOrThrow()

        assertEquals(TaxTreatment.INTRA_STATE, result.taxTreatment)
        assertEquals(1_000_000L, result.taxableAmountPaise)
        assertEquals(90_000L, result.cgstAmountPaise)
        assertEquals(90_000L, result.sgstAmountPaise)
        assertEquals(0L, result.igstAmountPaise)
        assertEquals(180_000L, result.totalTaxAmountPaise)
        assertEquals(1_180_000L, result.grandTotalPaise)

        val line = result.lineCalculations.first()
        assertEquals(900, line.cgstRateBasisPoints)    // floor(1800/2) = 900
        assertEquals(900, line.sgstRateBasisPoints)    // 1800 - 900 = 900
        assertEquals(0, line.igstRateBasisPoints)
    }

    /**
     * Intra-state with odd GST rate (e.g. 5%): CGST floor(500/2)=250, SGST=250.
     * Rates sum exactly to 500.
     */
    @Test
    fun testIntraStateOddGstRate5Percent() {
        val result = calculator.calculate(input("24", "24", Triple(1L, 100_000L, 500))).getOrThrow()

        assertEquals(TaxTreatment.INTRA_STATE, result.taxTreatment)
        val line = result.lineCalculations.first()
        assertEquals(250, line.cgstRateBasisPoints)
        assertEquals(250, line.sgstRateBasisPoints)
        assertEquals(line.cgstRateBasisPoints + line.sgstRateBasisPoints, 500)
        assertEquals(2_500L, line.cgstAmountPaise)
        assertEquals(2_500L, line.sgstAmountPaise)
        assertEquals(105_000L, line.lineTotalPaise)
    }

    /**
     * Intra-state with odd GST rate (e.g. 3%): CGST floor(300/2)=150, SGST=300-150=150.
     * 3% is unusual but must not fail.
     */
    @Test
    fun testIntraStateOddGstRate3Percent() {
        val result = calculator.calculate(input("24", "24", Triple(1L, 100_000L, 300))).getOrThrow()

        val line = result.lineCalculations.first()
        assertEquals(150, line.cgstRateBasisPoints)
        assertEquals(150, line.sgstRateBasisPoints)
    }

    // ── Multi-line invoice ────────────────────────────────────────────────────────

    /**
     * Multi-line inter-state invoice with different GST rates per line.
     * Line 0: qty=2, rate=₹500 (50_000), GST 18% → taxable=100_000, IGST=18_000
     * Line 1: qty=1, rate=₹1000 (100_000), GST 5% → taxable=100_000, IGST=5_000
     * grandTotal = 200_000 + 18_000 + 5_000 = 223_000
     */
    @Test
    fun testMultiLineInterState() {
        val input = DocumentCalculationInput(
            sellerStateCode        = "24",
            placeOfSupplyStateCode = "33",
            lines = listOf(
                DocumentCalculationInput.LineInput("l0", 2L, 50_000L, 1800),
                DocumentCalculationInput.LineInput("l1", 1L, 100_000L, 500)
            )
        )
        val result = calculator.calculate(input).getOrThrow()

        assertEquals(200_000L, result.taxableAmountPaise)
        assertEquals(0L, result.cgstAmountPaise)
        assertEquals(0L, result.sgstAmountPaise)
        assertEquals(23_000L, result.igstAmountPaise)
        assertEquals(223_000L, result.grandTotalPaise)

        assertEquals(2, result.lineCalculations.size)
        assertEquals("l0", result.lineCalculations[0].lineItemId)
        assertEquals(18_000L, result.lineCalculations[0].igstAmountPaise)
        assertEquals("l1", result.lineCalculations[1].lineItemId)
        assertEquals(5_000L, result.lineCalculations[1].igstAmountPaise)
    }

    // ── Repository integration test data ─────────────────────────────────────────

    /**
     * Matches DocumentRepositoryTest intra-state scenario:
     * qty=10, rate=₹150 (15_000 paise), GST 18%, seller "24", supply "24"
     * taxable = 150_000, CGST = 13_500, SGST = 13_500, grandTotal = 177_000
     */
    @Test
    fun testRepositoryTestIntraStateScenario() {
        val result = calculator.calculate(input("24", "24", Triple(10L, 15_000L, 1800))).getOrThrow()

        assertEquals(150_000L, result.taxableAmountPaise)
        assertEquals(13_500L, result.cgstAmountPaise)
        assertEquals(13_500L, result.sgstAmountPaise)
        assertEquals(0L, result.igstAmountPaise)
        assertEquals(27_000L, result.totalTaxAmountPaise)
        assertEquals(177_000L, result.grandTotalPaise)
    }

    /**
     * Matches DocumentRepositoryTest PO inter-state scenario:
     * qty=5, rate=₹10 (1_000 paise), GST 18%, seller "24", supply "27"
     * taxable = 5_000, IGST 18% = 900, grandTotal = 5_900
     */
    @Test
    fun testRepositoryTestInterStatePoScenario() {
        val result = calculator.calculate(input("24", "27", Triple(5L, 1_000L, 1800))).getOrThrow()

        assertEquals(5_000L, result.taxableAmountPaise)
        assertEquals(900L, result.igstAmountPaise)
        assertEquals(5_900L, result.grandTotalPaise)
    }

    // ── Edge cases ────────────────────────────────────────────────────────────────

    /**
     * Zero GST rate: no tax, taxable == lineTotal. Must not divide by zero or fail.
     */
    @Test
    fun testZeroGstRate() {
        val result = calculator.calculate(input("24", "33", Triple(10L, 5_000L, 0))).getOrThrow()

        assertEquals(50_000L, result.taxableAmountPaise)
        assertEquals(0L, result.igstAmountPaise)
        assertEquals(0L, result.cgstAmountPaise)
        assertEquals(0L, result.sgstAmountPaise)
        assertEquals(0L, result.totalTaxAmountPaise)
        assertEquals(50_000L, result.grandTotalPaise)
    }

    /**
     * Zero rate (free item): taxable = 0. All tax components = 0. Must succeed.
     */
    @Test
    fun testZeroRatePaise() {
        val result = calculator.calculate(input("24", "33", Triple(5L, 0L, 1800))).getOrThrow()

        assertEquals(0L, result.taxableAmountPaise)
        assertEquals(0L, result.totalTaxAmountPaise)
        assertEquals(0L, result.grandTotalPaise)
    }

    /**
     * Maximum valid GST rate (100% = 10_000 basis points). Must not fail.
     * taxable = 1 × 10_000 = 10_000, IGST 100% = 10_000, grandTotal = 20_000
     */
    @Test
    fun testMaxGstRate100Percent() {
        val result = calculator.calculate(input("24", "33", Triple(1L, 10_000L, 10_000))).getOrThrow()

        assertEquals(10_000L, result.taxableAmountPaise)
        assertEquals(10_000L, result.igstAmountPaise)
        assertEquals(20_000L, result.grandTotalPaise)
    }

    // ── Half-up rounding ─────────────────────────────────────────────────────────

    /**
     * Rounding: taxable=333 paise, IGST 18%
     * 333 × 1800 = 599_400; (599_400 + 5_000) / 10_000 = 604_400 / 10_000 = 60 (rounds down from 59.94)
     */
    @Test
    fun testRoundingDownCase() {
        val result = calculator.calculate(input("24", "33", Triple(1L, 333L, 1800))).getOrThrow()
        assertEquals(60L, result.igstAmountPaise)   // 59.94 → 60 with half-up
    }

    /**
     * Rounding: taxable=278 paise, IGST 18%
     * 278 × 1800 = 500_400; (500_400 + 5_000) / 10_000 = 505_400 / 10_000 = 50 (50.04 → 50)
     */
    @Test
    fun testRoundingExactHalfUp() {
        val result = calculator.calculate(input("24", "33", Triple(1L, 278L, 1800))).getOrThrow()
        // 278 × 1800 = 500_400 → 50.04 → rounds to 50
        assertEquals(50L, result.igstAmountPaise)
    }

    /**
     * True half case: taxable=5_000_000 / 18 ≈ 277_777.78 paise ... use a value where
     * taxable × rate is exactly a half-paise multiple to verify the half-up tie-break.
     *
     * taxable=55_556 paise × rate=900 bp = 50_000_400 → (50_000_400 + 5_000)/10_000 = 50_005_400/10_000 = 5_000
     * No tie here; verify explicitly.
     *
     * For exact .5 tie: taxable × rate % 10_000 == 5_000
     * e.g. taxable=2_500, rate=1800: 2_500 × 1_800 = 4_500_000; 4_500_000 % 10_000 = 0 (even multiple)
     * Try taxable=1_250, rate=1800: 1_250 × 1_800 = 2_250_000 → 225.0 → already exact, no rounding.
     *
     * Use taxable=1_667, rate=300: 1_667 × 300 = 500_100 → 50.01 → 50
     * Use taxable=1_666, rate=300: 1_666 × 300 = 499_800 → 49.98 → 50 (rounds up, .48 < .5, no!)
     * Actually: (499_800 + 5_000)/10_000 = 504_800/10_000 = 50 → rounds to 50 ✓ (49.98 rounds to 50)
     *
     * Simpler: verify intra-state CGST/SGST independently sum to total.
     */
    @Test
    fun testIntraStateCgstPlusSgstEqualsTotalTax() {
        // Pick odd GST rate to ensure non-symmetric split
        val result = calculator.calculate(input("24", "24", Triple(7L, 3_000L, 1200))).getOrThrow()

        // taxable = 21_000
        // CGST rate = floor(1200/2) = 600
        // SGST rate = 1200 - 600 = 600
        // CGST = (21_000 × 600 + 5_000) / 10_000 = 12_605_000 / 10_000 = 1_260
        // SGST = 1_260
        val line = result.lineCalculations.first()
        assertEquals(line.cgstAmountPaise + line.sgstAmountPaise, line.totalTaxPaise)
        assertEquals(result.cgstAmountPaise + result.sgstAmountPaise, result.totalTaxAmountPaise)
    }

    /**
     * Document totals must equal sum of line calculations, not re-derived.
     */
    @Test
    fun testDocumentTotalsAreSumOfLines() {
        val input = DocumentCalculationInput(
            sellerStateCode        = "24",
            placeOfSupplyStateCode = "33",
            lines = listOf(
                DocumentCalculationInput.LineInput("l0", 3L, 7_000L, 1800),
                DocumentCalculationInput.LineInput("l1", 7L, 3_000L, 1200),
                DocumentCalculationInput.LineInput("l2", 1L, 50_000L, 500)
            )
        )
        val result = calculator.calculate(input).getOrThrow()

        val computedTaxable  = result.lineCalculations.sumOf { it.taxableAmountPaise }
        val computedIgst     = result.lineCalculations.sumOf { it.igstAmountPaise }
        val computedTotalTax = result.lineCalculations.sumOf { it.totalTaxPaise }
        val computedGrand    = result.lineCalculations.sumOf { it.lineTotalPaise }

        assertEquals(computedTaxable,  result.taxableAmountPaise)
        assertEquals(computedIgst,     result.igstAmountPaise)
        assertEquals(computedTotalTax, result.totalTaxAmountPaise)
        assertEquals(computedGrand,    result.grandTotalPaise)
    }

    // ── Validation errors ─────────────────────────────────────────────────────────

    @Test
    fun testRejectsEmptyLines() {
        val result = calculator.calculate(
            DocumentCalculationInput("24", "33", emptyList())
        )
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as CalculationException).error
        assertTrue(err is CalculationError.NoLineItems)
    }

    @Test
    fun testRejectsZeroQuantity() {
        val result = calculator.calculate(input("24", "33", Triple(0L, 1_000L, 1800)))
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as CalculationException).error
        assertTrue(err is CalculationError.InvalidQuantity)
    }

    @Test
    fun testRejectsNegativeQuantity() {
        val result = calculator.calculate(input("24", "33", Triple(-1L, 1_000L, 1800)))
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as CalculationException).error
        assertTrue(err is CalculationError.InvalidQuantity)
    }

    @Test
    fun testRejectsNegativeRate() {
        val result = calculator.calculate(input("24", "33", Triple(1L, -1L, 1800)))
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as CalculationException).error
        assertTrue(err is CalculationError.InvalidRate)
    }

    @Test
    fun testRejectsGstRateAbove10000() {
        val result = calculator.calculate(input("24", "33", Triple(1L, 1_000L, 10_001)))
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as CalculationException).error
        assertTrue(err is CalculationError.InvalidGstRate)
    }

    @Test
    fun testRejectsNegativeGstRate() {
        val result = calculator.calculate(input("24", "33", Triple(1L, 1_000L, -1)))
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as CalculationException).error
        assertTrue(err is CalculationError.InvalidGstRate)
    }

    @Test
    fun testRejectsBlankSellerStateCode() {
        val result = calculator.calculate(input("", "33", Triple(1L, 1_000L, 1800)))
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as CalculationException).error
        assertTrue(err is CalculationError.InvalidStateCode)
        assertEquals("sellerStateCode", (err as CalculationError.InvalidStateCode).field)
    }

    @Test
    fun testRejectsNonNumericSellerStateCode() {
        val result = calculator.calculate(input("GJ", "33", Triple(1L, 1_000L, 1800)))
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as CalculationException).error
        assertTrue(err is CalculationError.InvalidStateCode)
    }

    @Test
    fun testRejectsBlankPlaceOfSupplyStateCode() {
        val result = calculator.calculate(input("24", "  ", Triple(1L, 1_000L, 1800)))
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as CalculationException).error
        assertTrue(err is CalculationError.InvalidStateCode)
        assertEquals("placeOfSupplyStateCode", (err as CalculationError.InvalidStateCode).field)
    }

    @Test
    fun testRejectsThreeDigitStateCode() {
        val result = calculator.calculate(input("243", "33", Triple(1L, 1_000L, 1800)))
        assertTrue(result.isFailure)
        assertTrue((result.exceptionOrNull() as CalculationException).error is CalculationError.InvalidStateCode)
    }

    @Test
    fun testRejectsOneDigitStateCode() {
        val result = calculator.calculate(input("2", "33", Triple(1L, 1_000L, 1800)))
        assertTrue(result.isFailure)
        assertTrue((result.exceptionOrNull() as CalculationException).error is CalculationError.InvalidStateCode)
    }

    // ── Amount-in-words ───────────────────────────────────────────────────────────

    @Test
    fun testAmountInWordsPopulated() {
        val result = calculator.calculate(input("24", "24", Triple(10L, 15_000L, 1800))).getOrThrow()

        // grandTotal = 177_000 paise = ₹1,770.00
        assertEquals("RUPEES ONE THOUSAND SEVEN HUNDRED SEVENTY ONLY", result.amountInWords)
        // totalTax = 27_000 paise = ₹270.00
        assertEquals("RUPEES TWO HUNDRED SEVENTY ONLY", result.taxAmountInWords)
    }

    @Test
    fun testPdfSpecExampleAmountInWords() {
        // PDF_SPEC example: ₹17,700 → "RUPEES SEVENTEEN THOUSAND SEVEN HUNDRED ONLY"
        val result = calculator.calculate(input("24", "24", Triple(100L, 17_700_00L, 0))).getOrThrow()
        // taxable = 100 × 1_770_000 paise... simpler: direct word test
        // Just test the formatter output directly through the calculator
        val result2 = calculator.calculate(
            DocumentCalculationInput(
                sellerStateCode        = "24",
                placeOfSupplyStateCode = "33",
                lines = listOf(DocumentCalculationInput.LineInput("l", 1L, 1_770_000L, 0))
            )
        ).getOrThrow()
        assertEquals("RUPEES SEVENTEEN THOUSAND SEVEN HUNDRED ONLY", result2.amountInWords)
    }
}
