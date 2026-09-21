package com.campussync.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for AI response processing logic.
 */
class GeminiRepositoryTest {

    /**
     * Helper to simulate the private cleanJsonResponse logic from GeminiRepository.
     */
    private fun cleanJsonResponse(raw: String): String {
        return raw.trim().removeSurrounding("```json", "```").trim()
    }

    @Test
    fun testCleanJsonResponse_WithFences() {
        val rawJson = """
            ```json
            [{"moduleCode": "OPSC6312"}]
            ```
        """.trimIndent()
        
        val cleaned = cleanJsonResponse(rawJson)
        assertEquals("[{\"moduleCode\": \"OPSC6312\"}]", cleaned)
    }

    @Test
    fun testCleanJsonResponse_Plain() {
        val rawJson = "[{\"moduleCode\": \"OPSC6312\"}]"
        val cleaned = cleanJsonResponse(rawJson)
        assertEquals(rawJson, cleaned)
    }
}
