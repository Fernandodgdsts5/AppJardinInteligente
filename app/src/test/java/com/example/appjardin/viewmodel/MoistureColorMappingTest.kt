package com.example.appjardin.viewmodel

import com.example.appjardin.data.local.PlantEntity
import com.example.appjardin.model.MoistureState
import org.junit.Assert.assertEquals
import org.junit.Test

class MoistureColorMappingTest {

    private val testPlant = PlantEntity(
        id = 1,
        name = "Tomate",
        humedadMinima = 30,
        humedadBuena = 60,
        humedadExceso = 80
    )

    private fun getMoistureState(humidity: Float, plant: PlantEntity?): MoistureState {
        if (plant == null) return MoistureState.NO_PLANT
        return when {
            humidity < plant.humedadMinima -> MoistureState.LOW_MOISTURE
            humidity < plant.humedadBuena -> MoistureState.MEDIUM_MOISTURE
            humidity <= plant.humedadExceso -> MoistureState.GOOD_MOISTURE
            else -> MoistureState.EXCESS_MOISTURE
        }
    }

    @Test
    fun testNoPlantFallback() {
        assertEquals(MoistureState.NO_PLANT, getMoistureState(50f, null))
    }

    @Test
    fun testLowMoisture() {
        assertEquals(MoistureState.LOW_MOISTURE, getMoistureState(15f, testPlant))
        assertEquals(MoistureState.LOW_MOISTURE, getMoistureState(29.9f, testPlant))
    }

    @Test
    fun testMediumMoistureBoundary() {
        assertEquals(MoistureState.MEDIUM_MOISTURE, getMoistureState(30f, testPlant))
        assertEquals(MoistureState.MEDIUM_MOISTURE, getMoistureState(45f, testPlant))
        assertEquals(MoistureState.MEDIUM_MOISTURE, getMoistureState(59.9f, testPlant))
    }

    @Test
    fun testGoodMoistureBoundary() {
        assertEquals(MoistureState.GOOD_MOISTURE, getMoistureState(60f, testPlant))
        assertEquals(MoistureState.GOOD_MOISTURE, getMoistureState(70f, testPlant))
        assertEquals(MoistureState.GOOD_MOISTURE, getMoistureState(80f, testPlant))
    }

    @Test
    fun testExcessMoistureBoundary() {
        assertEquals(MoistureState.EXCESS_MOISTURE, getMoistureState(80.1f, testPlant))
        assertEquals(MoistureState.EXCESS_MOISTURE, getMoistureState(95f, testPlant))
    }
}
