package com.example.appjardin.model

import androidx.compose.ui.graphics.Color
import com.example.appjardin.ui.theme.ColorExcessMoisture
import com.example.appjardin.ui.theme.ColorGoodMoisture
import com.example.appjardin.ui.theme.ColorLowMoisture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeTest {

    @Test
    fun testFromIdResolutionAndFallback() {
        assertEquals(AppTheme.SELVA, AppTheme.fromId("selva"))
        assertEquals(AppTheme.ATARDECER, AppTheme.fromId("atardecer"))
        assertEquals(AppTheme.ANOCHECER, AppTheme.fromId("anochecer"))
        assertEquals(AppTheme.PLAYA, AppTheme.fromId("playa"))
        assertEquals(AppTheme.SEMAFORO, AppTheme.fromId("semaforo"))

        // Fallback for null or unknown string -> SELVA
        assertEquals(AppTheme.SELVA, AppTheme.fromId(null))
        assertEquals(AppTheme.SELVA, AppTheme.fromId("unknown_theme"))
        assertEquals(AppTheme.SELVA, AppTheme.fromId(""))
    }

    @Test
    fun testFixedThemesReturnSameHeaderAndButtonColorRegardlessOfMoisture() {
        val fixedThemes = listOf(AppTheme.SELVA, AppTheme.ATARDECER, AppTheme.ANOCHECER, AppTheme.PLAYA)
        val sampleMoistureColors = listOf(ColorLowMoisture, ColorGoodMoisture, ColorExcessMoisture)

        for (theme in fixedThemes) {
            assertFalse("Theme ${theme.id} must not be dynamic", theme.isDynamic)
            val baseHeader = theme.getHeaderColor(ColorLowMoisture)
            val baseButton = theme.getButtonColor(ColorLowMoisture)

            for (moistureColor in sampleMoistureColors) {
                assertEquals(
                    "Header color for ${theme.id} must remain fixed",
                    baseHeader,
                    theme.getHeaderColor(moistureColor)
                )
                assertEquals(
                    "Button color for ${theme.id} must remain fixed",
                    baseButton,
                    theme.getButtonColor(moistureColor)
                )
            }
        }
    }

    @Test
    fun testSemaforoThemeReturnsDynamicColors() {
        val semaforo = AppTheme.SEMAFORO
        assertTrue(semaforo.isDynamic)

        val sampleMoistureColors = listOf(ColorLowMoisture, ColorGoodMoisture, ColorExcessMoisture)

        for (moistureColor in sampleMoistureColors) {
            assertEquals(moistureColor, semaforo.getHeaderColor(moistureColor))
            assertEquals(moistureColor, semaforo.getButtonColor(moistureColor))
        }
    }

    @Test
    fun testButtonContentColorIsWhite() {
        for (theme in AppTheme.entries) {
            assertEquals(Color.White, theme.buttonContentColor)
        }
    }

    @Test
    fun testComputeBottomBarColorsOnMainAndOtherRoutes() {
        val sampleMoistureColor = ColorGoodMoisture
        val nonMainRoutes = listOf("missions", "play", "history", "settings", null)

        for (theme in AppTheme.entries) {
            for (route in nonMainRoutes) {
                val colors = computeBottomBarColors(theme, route, sampleMoistureColor)
                assertEquals("Container must be white on non-main routes", Color.White, colors.containerColor)
            }
        }

        val selvaColors = computeBottomBarColors(AppTheme.SELVA, "main", sampleMoistureColor)
        assertEquals(Color.White, selvaColors.containerColor)

        val semaforoColors = computeBottomBarColors(AppTheme.SEMAFORO, "main", sampleMoistureColor)
        assertEquals(Color.White, semaforoColors.containerColor)
        assertEquals(sampleMoistureColor, semaforoColors.estadoSelectedTextColor)

        val atardecerColors = computeBottomBarColors(AppTheme.ATARDECER, "main", sampleMoistureColor)
        assertEquals(AppTheme.ATARDECER.buttonColorFixed!!, atardecerColors.containerColor)

        val anochecerColors = computeBottomBarColors(AppTheme.ANOCHECER, "main", sampleMoistureColor)
        assertEquals(AppTheme.ANOCHECER.buttonColorFixed!!, anochecerColors.containerColor)

        val playaColors = computeBottomBarColors(AppTheme.PLAYA, "main", sampleMoistureColor)
        assertEquals(AppTheme.PLAYA.buttonColorFixed!!, playaColors.containerColor)
    }
}
