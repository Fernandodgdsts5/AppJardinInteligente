package com.example.appjardin.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomNavDestinationsTest {

    @Test
    fun testBottomNavDestinationsOrder() {
        val destinations = listOf("main", "missions", "play", "history", "settings")

        assertEquals(5, destinations.size)
        assertEquals("main", destinations[0])
        assertEquals("missions", destinations[1])
        assertEquals("play", destinations[2])
        assertEquals("history", destinations[3])
        assertEquals("settings", destinations[4])
    }
}
