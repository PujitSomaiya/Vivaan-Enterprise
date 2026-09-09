package com.vivaanenterprise.app.domain.model

/**
 * Pure input contract for [DocumentCalculator].
 *
 * All monetary values use Long paise (1 rupee = 100 paise).
 * All GST rates use Int basis points (10,000 bp = 100 %).
 *
 * This type has no dependency on Android, Room, Firebase, or Compose.
 * It must remain a plain Kotlin data class to allow pure JVM unit testing.
 */
data class DocumentCalculationInput(
    /**
     * Two-digit numeric state code of the seller, derived from the first two characters of
     * their GSTIN (e.g. "24" for Gujarat from GSTIN "24CHWPG0910J1ZB").
     *
     * Must be exactly two ASCII digit characters. The calculator rejects any other value
     * with [CalculationError.InvalidStateCode].
     */
    val sellerStateCode: String,

    /**
     * Two-digit numeric state code indicating where the supply is deemed to occur.
     *
     * Compared against [sellerStateCode] to determine [TaxTreatment]:
     * - equal → [TaxTreatment.INTRA_STATE] → CGST + SGST
     * - different → [TaxTreatment.INTER_STATE] → IGST
     *
     * The client's state code may be used as a UI default, but the final place-of-supply
     * must be explicitly confirmed by the user before finalization.
     */
    val placeOfSupplyStateCode: String,

    /** Line items to calculate. Must be non-empty. */
    val lines: List<LineInput>
) {

    /**
     * Input for a single document line item.
     *
     * Constraints enforced by [DocumentCalculator]:
     * - [quantity] must be > 0
     * - [ratePaise] must be ≥ 0 (zero-rate items are permitted)
     * - [gstRateBasisPoints] must be in 0..10,000 (inclusive)
     *
     * V1 note: No discount semantics are defined. Taxable amount = quantity × ratePaise exactly.
     */
    data class LineInput(
        /** Matches the ID of the corresponding [DocumentLineItem] entity for result correlation. */
        val id: String,
        val quantity: Long,
        val ratePaise: Long,
        val gstRateBasisPoints: Int
    )
}
