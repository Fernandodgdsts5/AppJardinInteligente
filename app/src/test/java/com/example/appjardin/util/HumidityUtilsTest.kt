package com.example.appjardin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HumidityUtilsTest {

    @Test
    fun testParseFinalHumidityEmpty() {
        assertNull(parseFinalHumidity(null))
        assertNull(parseFinalHumidity(""))
        assertNull(parseFinalHumidity("   "))
    }

    @Test
    fun testParseFinalHumiditySingleValue() {
        assertEquals(45.5f, parseFinalHumidity("45.5")!!, 1e-4f)
    }

    @Test
    fun testParseFinalHumidityMultipleValues() {
        assertEquals(78.5f, parseFinalHumidity("45.5, 62.0, 78.5")!!, 1e-4f)
    }

    @Test
    fun testParseFinalHumidityMalformedValues() {
        assertNull(parseFinalHumidity("abc"))
        assertEquals(62.0f, parseFinalHumidity("45.5, invalid, 62.0")!!, 1e-4f)
        assertEquals(62.0f, parseFinalHumidity("45.5, 62.0, ")!!, 1e-4f)
    }
}
