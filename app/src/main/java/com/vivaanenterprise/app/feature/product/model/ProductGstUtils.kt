package com.vivaanenterprise.app.feature.product.model

object ProductGstUtils {

    fun parseGstPercentageToBasisPoints(input: String): Int? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val parts = trimmed.split(".")
        if (parts.size > 2) return null

        val integerPartStr = parts[0]
        if (integerPartStr.isEmpty() || !integerPartStr.all { it.isDigit() }) return null

        val integerPart = integerPartStr.toIntOrNull() ?: return null
        if (integerPart < 0 || integerPart > 100) return null

        val fractionalPartStr = if (parts.size == 2) parts[1] else ""
        if (fractionalPartStr.isNotEmpty() && !fractionalPartStr.all { it.isDigit() }) return null
        if (fractionalPartStr.length > 2) return null

        if (integerPart == 100 && fractionalPartStr.isNotEmpty() && (fractionalPartStr.toIntOrNull() ?: 0) > 0) {
            return null
        }

        val cents = when (fractionalPartStr.length) {
            0 -> 0
            1 -> (fractionalPartStr[0] - '0') * 10
            2 -> (fractionalPartStr[0] - '0') * 10 + (fractionalPartStr[1] - '0')
            else -> return null
        }

        val totalBasisPoints = integerPart * 100 + cents
        if (totalBasisPoints < 0 || totalBasisPoints > 10000) return null

        return totalBasisPoints
    }

    fun formatBasisPointsToPercentage(basisPoints: Int?): String {
        if (basisPoints == null) return ""
        val whole = basisPoints / 100
        val remainder = basisPoints % 100
        return when {
            remainder == 0 -> "$whole"
            remainder % 10 == 0 -> "$whole.${remainder / 10}"
            else -> "$whole.${if (remainder < 10) "0$remainder" else "$remainder"}"
        }
    }
}
