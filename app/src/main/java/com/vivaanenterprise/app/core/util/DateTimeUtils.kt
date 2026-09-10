package com.vivaanenterprise.app.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    /**
     * Formats Unix epoch timestamp in milliseconds to a human-readable local date string (e.g. "09 Sep 2026").
     */
    fun formatDocumentDate(timestampMs: Long): String {
        return dateFormatter.format(Instant.ofEpochMilli(timestampMs))
    }
}
