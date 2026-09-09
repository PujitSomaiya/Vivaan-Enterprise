package com.vivaanenterprise.app.domain.model

/**
 * Determines which GST branch applies for a document line item.
 *
 * Decision rule: compare the seller's state code (first two digits of their GSTIN) against
 * the document's place-of-supply state code.
 *
 * - [INTRA_STATE] — same state → CGST + SGST split.
 *   CGST rate = floor(totalGstRate / 2); SGST rate = totalGstRate − CGST rate.
 *   This guarantees the two halves always sum exactly to the total rate.
 *
 * - [INTER_STATE] — different states → IGST at the full GST rate; CGST = SGST = 0.
 */
enum class TaxTreatment {
    INTRA_STATE,
    INTER_STATE
}
