package com.campussync.app

import com.campussync.app.utils.ModuleCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the ModuleCatalog logic.
 */
class ModuleCatalogTest {

    @Test
    fun testGetModulesFor_Year1Semester1() {
        val modules = ModuleCatalog.getModulesFor(1, 1)
        assertEquals(4, modules.size)
        assertTrue(modules.any { it.code == "PROG5121" })
        assertEquals("Programming 1A", modules.find { it.code == "PROG5121" }?.name)
    }

    @Test
    fun testGetModulesFor_Year3Semester2() {
        val modules = ModuleCatalog.getModulesFor(3, 2)
        assertEquals(4, modules.size)
        assertTrue(modules.any { it.code == "OPSC6312" })
        assertEquals("Open Source Coding (Intermediate)", modules.find { it.code == "OPSC6312" }?.name)
    }

    @Test
    fun testGetModulesFor_InvalidYear() {
        val modules = ModuleCatalog.getModulesFor(4, 1)
        assertTrue(modules.isEmpty())
    }

    @Test
    fun testCatalogLists() {
        // Verify campus list is not empty and contains expected values
        assertTrue(ModuleCatalog.campusLocations.contains("Braamfontein"))
        assertEquals(8, ModuleCatalog.campusLocations.size)
        
        // Verify course list
        assertTrue(ModuleCatalog.courses.any { it.contains("DISD") })
    }
}
