package com.vivaanenterprise.app.domain.util

import com.vivaanenterprise.app.domain.model.CalculationError
import com.vivaanenterprise.app.domain.model.CalculationException
import com.vivaanenterprise.app.domain.model.DocumentCalculationInput
import com.vivaanenterprise.app.domain.model.DocumentCalculationResult
import com.vivaanenterprise.app.domain.model.DocumentLineCalculation
import com.vivaanenterprise.app.domain.model.TaxTreatment
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single deterministic financial calculation engine for Tax Invoices and Purchase Orders.
 *
 * This is the ONLY authority for document financial totals. UI (ViewModel, Composable),
 * Repository, and PDF renderer must all consume the same [DocumentCalculationResult].
 * No monetary formula may be duplicated elsewhere.
 *
 * ## Arithmetic constraints
 * - All monetary amounts: Long paise (1 rupee = 100 paise). No Double or Float.
 * - All GST rates: Int basis points (10,000 bp = 100 %). No Double or Float.
 * - Intermediate multiplications use [Math.multiplyExact] — overflow returns
 *   [CalculationError.ArithmeticOverflow] instead of silently wrapping.
 *
 * ## Tax treatment
 * - [sellerStateCode] == [placeOfSupplyStateCode] → [TaxTreatment.INTRA_STATE] → CGST + SGST.
 * - Different states → [TaxTreatment.INTER_STATE] → IGST.
 * - State codes are trimmed before comparison. Invalid codes produce [CalculationError.InvalidStateCode].
 *
 * ## Rounding rule — half-up to nearest paise
 * For a GST amount = taxableAmountPaise × rateBasisPoints / 10,000:
 * ```
 * rounded = (taxableAmountPaise × rateBasisPoints + 5_000) / 10_000   // integer division
 * ```
 * This rounds 0.5 paise up. It is applied per-line; document totals are sums of line results.
 * Aggregate-level tax is never recomputed on the document total to avoid rounding divergence.
 *
 * ## INTRA-STATE GST split
 * ```
 * cgstRate = totalGstRate / 2          // integer floor
 * sgstRate = totalGstRate - cgstRate   // guarantees cgstRate + sgstRate == totalGstRate exactly
 * ```
 */
@Singleton
class DocumentCalculator @Inject constructor(
    private val formatter: IndianCurrencyFormatter
) {

    /**
     * Calculates all financial values for the given input.
     *
     * @return [Result.success] with [DocumentCalculationResult], or [Result.failure] wrapping
     *         a [CalculationException] whose [CalculationException.error] describes the problem.
     */
    fun calculate(input: DocumentCalculationInput): Result<DocumentCalculationResult> {

        // ── 1. Validate state codes ─────────────────────────────────────────────
        val sellerState = input.sellerStateCode.trim()
        if (!isValidStateCode(sellerState)) {
            return Result.failure(CalculationException(CalculationError.InvalidStateCode("sellerStateCode")))
        }
        val supplyState = input.placeOfSupplyStateCode.trim()
        if (!isValidStateCode(supplyState)) {
            return Result.failure(CalculationException(CalculationError.InvalidStateCode("placeOfSupplyStateCode")))
        }

        // ── 2. Validate lines ───────────────────────────────────────────────────
        if (input.lines.isEmpty()) {
            return Result.failure(CalculationException(CalculationError.NoLineItems))
        }
        for (line in input.lines) {
            if (line.quantity <= 0) {
                return Result.failure(CalculationException(CalculationError.InvalidQuantity))
            }
            if (line.ratePaise < 0) {
                return Result.failure(CalculationException(CalculationError.InvalidRate))
            }
            if (line.gstRateBasisPoints < 0 || line.gstRateBasisPoints > 10_000) {
                return Result.failure(CalculationException(CalculationError.InvalidGstRate))
            }
        }

        // ── 3. Determine tax treatment ──────────────────────────────────────────
        val taxTreatment = if (sellerState == supplyState) {
            TaxTreatment.INTRA_STATE
        } else {
            TaxTreatment.INTER_STATE
        }

        // ── 4. Calculate per line ───────────────────────────────────────────────
        val lineResults = mutableListOf<DocumentLineCalculation>()
        for (line in input.lines) {
            val lineCalc = calculateLine(line, taxTreatment)
                ?: return Result.failure(CalculationException(CalculationError.ArithmeticOverflow))
            lineResults.add(lineCalc)
        }

        // ── 5. Sum line results with overflow protection ────────────────────────
        var totalTaxable = 0L
        var totalCgst = 0L
        var totalSgst = 0L
        var totalIgst = 0L
        var totalTax = 0L
        var grandTotal = 0L
        try {
            for (line in lineResults) {
                totalTaxable = Math.addExact(totalTaxable, line.taxableAmountPaise)
                totalCgst    = Math.addExact(totalCgst,    line.cgstAmountPaise)
                totalSgst    = Math.addExact(totalSgst,    line.sgstAmountPaise)
                totalIgst    = Math.addExact(totalIgst,    line.igstAmountPaise)
                totalTax     = Math.addExact(totalTax,     line.totalTaxPaise)
                grandTotal   = Math.addExact(grandTotal,   line.lineTotalPaise)
            }
        } catch (e: ArithmeticException) {
            return Result.failure(CalculationException(CalculationError.ArithmeticOverflow))
        }

        // ── 6. Populate amount-in-words ─────────────────────────────────────────
        val amountInWords    = formatter.formatAmountInWords(grandTotal)
        val taxAmountInWords = formatter.formatAmountInWords(totalTax)

        return Result.success(
            DocumentCalculationResult(
                lineCalculations     = lineResults,
                taxTreatment         = taxTreatment,
                taxableAmountPaise   = totalTaxable,
                cgstAmountPaise      = totalCgst,
                sgstAmountPaise      = totalSgst,
                igstAmountPaise      = totalIgst,
                totalTaxAmountPaise  = totalTax,
                grandTotalPaise      = grandTotal,
                amountInWords        = amountInWords,
                taxAmountInWords     = taxAmountInWords
            )
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    /**
     * Calculates financial values for one line item.
     * Returns null if any intermediate arithmetic overflows Long.
     */
    private fun calculateLine(
        line: DocumentCalculationInput.LineInput,
        taxTreatment: TaxTreatment
    ): DocumentLineCalculation? {

        // taxable = quantity × ratePaise — checked for overflow
        val taxableAmountPaise = try {
            Math.multiplyExact(line.quantity, line.ratePaise)
        } catch (e: ArithmeticException) {
            return null
        }

        val totalGstRate = line.gstRateBasisPoints

        val igstRateBasisPoints: Int
        val cgstRateBasisPoints: Int
        val sgstRateBasisPoints: Int
        val igstAmountPaise: Long
        val cgstAmountPaise: Long
        val sgstAmountPaise: Long

        when (taxTreatment) {
            TaxTreatment.INTER_STATE -> {
                igstRateBasisPoints = totalGstRate
                cgstRateBasisPoints = 0
                sgstRateBasisPoints = 0
                igstAmountPaise = roundHalfUp(taxableAmountPaise, totalGstRate) ?: return null
                cgstAmountPaise = 0L
                sgstAmountPaise = 0L
            }
            TaxTreatment.INTRA_STATE -> {
                igstRateBasisPoints = 0
                // CGST gets floor(totalRate / 2); SGST gets the remainder
                cgstRateBasisPoints = totalGstRate / 2
                sgstRateBasisPoints = totalGstRate - cgstRateBasisPoints
                igstAmountPaise = 0L
                cgstAmountPaise = roundHalfUp(taxableAmountPaise, cgstRateBasisPoints) ?: return null
                sgstAmountPaise = roundHalfUp(taxableAmountPaise, sgstRateBasisPoints) ?: return null
            }
        }

        val totalTaxPaise    = igstAmountPaise + cgstAmountPaise + sgstAmountPaise
        val lineTotalPaise   = taxableAmountPaise + totalTaxPaise

        return DocumentLineCalculation(
            lineItemId          = line.id,
            taxableAmountPaise  = taxableAmountPaise,
            igstRateBasisPoints = igstRateBasisPoints,
            igstAmountPaise     = igstAmountPaise,
            cgstRateBasisPoints = cgstRateBasisPoints,
            cgstAmountPaise     = cgstAmountPaise,
            sgstRateBasisPoints = sgstRateBasisPoints,
            sgstAmountPaise     = sgstAmountPaise,
            totalTaxPaise       = totalTaxPaise,
            lineTotalPaise      = lineTotalPaise
        )
    }

    /**
     * Computes `taxableAmountPaise × rateBasisPoints / 10,000` with half-up rounding.
     *
     * Formula: `(taxableAmountPaise × rateBasisPoints + 5_000) / 10_000`
     *
     * The `+ 5_000` shifts the floor such that 0.5 rounds up:
     * - `x.4999…` → floor → rounds down  ✓
     * - `x.5000`  → floor → rounds up    ✓ (half-up)
     *
     * Returns null if any intermediate multiplication overflows Long.
     */
    private fun roundHalfUp(taxableAmountPaise: Long, rateBasisPoints: Int): Long? {
        if (rateBasisPoints == 0) return 0L
        val numerator = try {
            Math.multiplyExact(taxableAmountPaise, rateBasisPoints.toLong())
        } catch (e: ArithmeticException) {
            return null
        }
        // Adding 5_000 cannot overflow here for any realistic invoice amount
        // (max numerator before overflow is Long.MAX_VALUE - 5_000, which is ~9.2 × 10^18)
        return (numerator + 5_000L) / 10_000L
    }

    /**
     * A valid GST state code is exactly two ASCII digit characters (e.g. "24", "09", "33").
     */
    private fun isValidStateCode(code: String): Boolean =
        code.length == 2 && code[0].isDigit() && code[1].isDigit()
}
