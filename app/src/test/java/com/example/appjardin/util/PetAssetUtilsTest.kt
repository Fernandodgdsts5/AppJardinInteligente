package com.example.appjardin.util

import com.example.appjardin.model.MoistureState
import com.example.appjardin.model.PetMood
import com.example.appjardin.model.toPetMood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetAssetUtilsTest {

    @Test
    fun testMoistureStateToPetMoodMapping() {
        assertEquals(PetMood.TRISTE, MoistureState.LOW_MOISTURE.toPetMood())
        assertEquals(PetMood.NEUTRAL, MoistureState.MEDIUM_MOISTURE.toPetMood())
        assertEquals(PetMood.FELIZ, MoistureState.GOOD_MOISTURE.toPetMood())
        assertEquals(PetMood.ENOJADO, MoistureState.EXCESS_MOISTURE.toPetMood())
        assertEquals(PetMood.NEUTRAL, MoistureState.NO_PLANT.toPetMood())
    }

    @Test
    fun testStaticAssetPathMappingForOtherPets() {
        val moods = listOf(
            PetMood.ASUSTADO to "a",
            PetMood.ENOJADO to "e",
            PetMood.FELIZ to "f",
            PetMood.NEUTRAL to "n",
            PetMood.TRISTE to "t"
        )

        val speciesPrefixes = mapOf(
            "abeja" to "a",
            "chanchito" to "c",
            "gusano" to "g",
            "hormiga" to "h",
            "larva" to "l",
            "reygeko" to "r"
        )

        for ((species, prefix) in speciesPrefixes) {
            for ((mood, char) in moods) {
                val expectedPath = "mascotas/$species/$prefix$char.png"
                val actualPath = PetAssetUtils.getPetStaticAssetPath(species, mood)
                assertEquals("Path for $species $mood", expectedPath, actualPath)
            }
        }
    }

    @Test
    fun testLarvaTargetResolution() {
        // Larva with FELIZ, NEUTRAL, TRISTE, ENOJADO -> FrameAnimation
        val animTargetFeliz = PetAssetUtils.resolvePetVisualTarget("larva", PetMood.FELIZ)
        assertTrue(animTargetFeliz is PetVisualTarget.FrameAnimation)
        assertEquals("lf", (animTargetFeliz as PetVisualTarget.FrameAnimation).folderName)

        val animTargetNeutral = PetAssetUtils.resolvePetVisualTarget("larva", PetMood.NEUTRAL)
        assertTrue(animTargetNeutral is PetVisualTarget.FrameAnimation)
        assertEquals("ln", (animTargetNeutral as PetVisualTarget.FrameAnimation).folderName)

        val animTargetTriste = PetAssetUtils.resolvePetVisualTarget("larva", PetMood.TRISTE)
        assertTrue(animTargetTriste is PetVisualTarget.FrameAnimation)
        assertEquals("lt", (animTargetTriste as PetVisualTarget.FrameAnimation).folderName)

        val animTargetEnojado = PetAssetUtils.resolvePetVisualTarget("larva", PetMood.ENOJADO)
        assertTrue(animTargetEnojado is PetVisualTarget.FrameAnimation)
        assertEquals("le", (animTargetEnojado as PetVisualTarget.FrameAnimation).folderName)

        // Larva with ASUSTADO -> StaticAsset (no animation folder exists)
        val staticTargetAsustado = PetAssetUtils.resolvePetVisualTarget("larva", PetMood.ASUSTADO)
        assertTrue(staticTargetAsustado is PetVisualTarget.StaticAsset)
        assertEquals("mascotas/larva/la.png", (staticTargetAsustado as PetVisualTarget.StaticAsset).assetPath)
    }

    @Test
    fun testOtherPetsTargetResolutionAlwaysStatic() {
        val nonLarvaPets = listOf("gusano", "hormiga", "chanchito", "abeja", "reygeko")
        val moods = PetMood.entries

        for (pet in nonLarvaPets) {
            for (mood in moods) {
                val target = PetAssetUtils.resolvePetVisualTarget(pet, mood)
                assertTrue("Pet $pet mood $mood must be StaticAsset", target is PetVisualTarget.StaticAsset)
            }
        }
    }
}
