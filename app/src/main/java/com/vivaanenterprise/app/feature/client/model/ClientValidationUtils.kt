package com.vivaanenterprise.app.feature.client.model

import java.util.regex.Pattern

object ClientValidationUtils {
    private val GSTIN_PATTERN: Pattern = Pattern.compile(
        "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}\$"
    )

    private val PAN_PATTERN: Pattern = Pattern.compile(
        "^[A-Z]{5}[0-9]{4}[A-Z]{1}\$"
    )

    private val STATE_CODE_PATTERN: Pattern = Pattern.compile(
        "^[0-9]{2}\$"
    )

    private val EMAIL_PATTERN: Pattern = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
    )

    private val PHONE_PATTERN: Pattern = Pattern.compile(
        "^[+]?[0-9\\s-]{7,15}\$"
    )

    fun isValidGstin(gstin: String): Boolean {
        return GSTIN_PATTERN.matcher(gstin.trim().uppercase()).matches()
    }

    fun isValidPan(pan: String): Boolean {
        return PAN_PATTERN.matcher(pan.trim().uppercase()).matches()
    }

    fun isValidStateCode(stateCode: String): Boolean {
        return STATE_CODE_PATTERN.matcher(stateCode.trim()).matches()
    }

    fun isValidEmail(email: String): Boolean {
        return EMAIL_PATTERN.matcher(email.trim()).matches()
    }

    fun isValidPhone(phone: String): Boolean {
        return PHONE_PATTERN.matcher(phone.trim()).matches()
    }
}
