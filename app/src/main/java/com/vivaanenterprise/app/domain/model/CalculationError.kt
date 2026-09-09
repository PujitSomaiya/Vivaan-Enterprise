package com.vivaanenterprise.app.domain.model

/**
 * Typed domain errors produced by [DocumentCalculator] for expected input failures.
 *
 * These represent business-rule violations, not programming mistakes. Callers should
 * inspect the concrete type to surface a meaningful error to the user.
 */
sealed interface CalculationError {

    /**
     * A state code (seller or place-of-supply) is missing, blank, or is not exactly two
     * numeric digits as required by the GST state code format.
     *
     * @param field "sellerStateCode" or "placeOfSupplyStateCode"
     */
    data class InvalidStateCode(val field: String) : CalculationError

    /** A line item has [quantity] ≤ 0. Quantity must be a positive integer. */
    data object InvalidQuantity : CalculationError

    /** A line item has a negative [ratePaise]. Zero-rate items are permitted; negative rates are not. */
    data object InvalidRate : CalculationError

    /** A line item has [gstRateBasisPoints] outside the valid 0–10,000 range (0 %–100 %). */
    data object InvalidGstRate : CalculationError

    /** Intermediate arithmetic would overflow a [Long]. The document values are too large. */
    data object ArithmeticOverflow : CalculationError

    /** The calculation input contains no line items. */
    data object NoLineItems : CalculationError
}

/**
 * Wraps a [CalculationError] as a [Throwable] so it can be carried inside [kotlin.Result].
 *
 * Callers should unwrap via `(result.exceptionOrNull() as? CalculationException)?.error`.
 */
class CalculationException(val error: CalculationError) : Exception(error.toString())
