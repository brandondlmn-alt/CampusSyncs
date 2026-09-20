package com.campussync.app.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility for handling date normalization, display formatting, and countdowns.
 * API 24 compatible.
 */
object DateUtils {

    /**
     * Normalizes dates from various formats to ISO "YYYY-MM-DD" for sorting.
     * Handles: "15-Sep-26", "15 September 2026", "2026-09-15", "15/09/2026"
     */
    fun normalizeDate(raw: String): String {
        if (raw.isBlank() || raw == "N/A") return "N/A"
        
        // If already ISO, return as-is
        if (raw.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return raw

        val formats = listOf(
            "dd-MMM-yy",      // 15-Sep-26
            "dd/MM/yyyy",     // 15/09/2026
            "dd MMMM yyyy",   // 15 September 2026
            "d-MMM-yy"        // 1-Sep-26
        )

        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.isLenient = false
                val date = sdf.parse(raw)
                if (date != null) return isoFormat.format(date)
            } catch (e: Exception) {
                continue
            }
        }
        return raw // Return as-is if no match
    }

    /**
     * Formats ISO date and time for user display.
     * e.g., "2026-09-15" + "23:50" -> "Tue, 15 Sep 2026 at 23:50"
     */
    fun formatForDisplay(isoDate: String, time: String): String {
        if (isoDate == "N/A") return "Date TBC"
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate)
            val displaySdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
            "${displaySdf.format(date!!)} at $time"
        } catch (e: Exception) {
            "$isoDate $time"
        }
    }

    /**
     * Returns a relative countdown string from the current date.
     */
    fun getCountdown(isoDate: String): String {
        if (isoDate == "N/A") return "Pending"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val targetDate = sdf.parse(isoDate)
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val diffMillis = targetDate!!.time - today.time
            val diffDays = diffMillis / (1000 * 60 * 60 * 24)

            when {
                diffDays < 0 -> "Overdue by ${Math.abs(diffDays)} day${if (Math.abs(diffDays) > 1) "s" else ""}"
                diffDays == 0L -> "Due Today"
                diffDays == 1L -> "Due Tomorrow"
                else -> "In $diffDays days"
            }
        } catch (e: Exception) {
            ""
        }
    }
}
