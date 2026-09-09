package com.vivaanenterprise.app.domain.model

/**
 * Calculated financial values for a single document line item.
 *
 * Rate fields (e.g. [igstRateBasisPoints]) are stored alongside amounts so that the PDF
 * renderer can display the applicable rate without re-deriving it from the document totals.
 *
 * All monetary amounts are in Long paise. All rates are in Int basis points (10,000 = 100 %).
 */
data class DocumentLineCalculation(
    val lineItemId: String,
    val taxableAmountPaise: Long,
    /** Full GST rate applied as IGST; 0 for intra-state transactions. */
    val igstRateBasisPoints: Int = 0,
    val igstAmountPaise: Long,
    /** Half of the total GST rate applied as CGST; 0 for inter-state transactions. */
    val cgstRateBasisPoints: Int = 0,
    val cgstAmountPaise: Long,
    /** Remaining half of the total GST rate applied as SGST; 0 for inter-state transactions. */
    val sgstRateBasisPoints: Int = 0,
    val sgstAmountPaise: Long,
    val totalTaxPaise: Long,
    val lineTotalPaise: Long
)

/**
 * Complete financial calculation result for a business document.
 *
 * Produced exclusively by [DocumentCalculator]. This is the single source of truth for all
 * financial figures — UI, persistence (Room), and PDF rendering all consume this result.
 * Formula logic must never be duplicated in ViewModels, Composables, DAOs, or the PDF renderer.
 *
 * Document totals are sums of [lineCalculations] results. They are never re-derived from
 * aggregate taxable values to avoid rounding divergence from per-line rounding.
 */
data class DocumentCalculationResult(
    val lineCalculations: List<DocumentLineCalculation>,
    /** Whether IGST or CGST+SGST applies; determined by seller vs place-of-supply state code. */
    val taxTreatment: TaxTreatment,
    val taxableAmountPaise: Long,
    val cgstAmountPaise: Long,
    val sgstAmountPaise: Long,
    val igstAmountPaise: Long,
    val totalTaxAmountPaise: Long,
    val grandTotalPaise: Long,
    /** Grand total in words per Indian numbering system; e.g. "RUPEES TWENTY THOUSAND SIX HUNDRED FIFTY ONLY". */
    val amountInWords: String? = null,
    /** Total tax amount in words using the same formatter. */
    val taxAmountInWords: String? = null
)
