package com.campussync.app

import com.campussync.app.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for date normalization and formatting logic.
 */
class DateUtilsTest {

    @Test
    fun testNormalizeDate_StandardFormat() {
        val input = "15-Sep-26"
        val result = DateUtils.normalizeDate(input)
        assertEquals("2026-09-15", result)
    }

    @Test
    fun testNormalizeDate_AlternativeFormat() {
        val input = "15/09/2026"
        val result = DateUtils.normalizeDate(input)
        assertEquals("2026-09-15", result)
    }

    @Test
    fun testNormalizeDate_Invalid() {
        val input = "InvalidDate"
        val result = DateUtils.normalizeDate(input)
        assertEquals("InvalidDate", result)
    }

    @Test
    fun testFormatForDisplay() {
        val isoDate = "2026-09-15"
        val time = "23:50"
        val result = DateUtils.formatForDisplay(isoDate, time)
        // Note: Expected output assumes US locale formatting as defined in DateUtils
        assertEquals("Tue, 15 Sep 2026 at 23:50", result)
    }

    @Test
    fun testGetCountdown_Future() {
        val futureDate = "2099-01-01"
        val result = DateUtils.getCountdown(futureDate)
        assert(result.startsWith("In"))
    }
}
