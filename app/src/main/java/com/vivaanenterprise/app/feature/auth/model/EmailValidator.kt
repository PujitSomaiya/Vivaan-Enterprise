package com.vivaanenterprise.app.feature.auth.model

import java.util.regex.Pattern

object EmailValidator {
    private val EMAIL_PATTERN: Pattern = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
    )

    fun isValidEmail(email: String): Boolean {
        return EMAIL_PATTERN.matcher(email).matches()
    }
}
