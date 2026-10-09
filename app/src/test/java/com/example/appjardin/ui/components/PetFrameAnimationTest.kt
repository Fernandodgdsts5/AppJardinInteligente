package com.example.appjardin.ui.components

import com.example.appjardin.model.PetMood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

fun shouldAnimatePet(speciesId: String, mood: PetMood): Boolean {
    return speciesId.equals("larva", ignoreCase = true) && mood == PetMood.NEUTRAL
}

class PetFrameAnimationTest {

    @Test
    fun testGetFolderForPetMood() {
        assertEquals("lf", getFolderForPetMood(PetMood.FELIZ))
        assertEquals("ln", getFolderForPetMood(PetMood.NEUTRAL))
        assertEquals("lt", getFolderForPetMood(PetMood.TRISTE))
        assertEquals("lt", getFolderForPetMood(PetMood.ASUSTADO))
        assertEquals("le", getFolderForPetMood(PetMood.ENOJADO))
    }

    @Test
    fun testCalculateFrameIndexLoop() {
        val frameDurationNanos = 83_333_333L // ~12 FPS

        // Start (0ms) -> Frame 1
        assertEquals(1, calculateFrameIndex(0L, frameDurationNanos, 120))

        // First frame (83.33ms) -> Frame 2
        assertEquals(2, calculateFrameIndex(83_333_333L, frameDurationNanos, 120))

        // Frame 119 (119 * 83.33ms) -> Frame 120
        assertEquals(120, calculateFrameIndex(119 * 83_333_333L, frameDurationNanos, 120))

        // Frame 120 (120 * 83.33ms) -> Loops back to Frame 1 (001.png)!
        assertEquals(1, calculateFrameIndex(120 * 83_333_333L, frameDurationNanos, 120))

        // Frame 121 -> Frame 2
        assertEquals(2, calculateFrameIndex(121 * 83_333_333L, frameDurationNanos, 120))
    }

    @Test
    fun testCalculateFrameIndexSkippingDelay() {
        val frameDurationNanos = 83_333_333L

        // If CPU/decoding delays by 250ms (~3 frames elapsed), skips directly to Frame 4!
        val elapsedNanos = 250_000_000L
        val frameIndex = calculateFrameIndex(elapsedNanos, frameDurationNanos, 120)
        assertEquals(4, frameIndex)
    }

    @Test
    fun testCalculateContainDstSizeAndOffsetSquare() {
        val (dstSize, dstOffset) = calculateContainDstSizeAndOffset(540, 960, 270f, 270f)
        assertEquals(151, dstSize.width)
        assertEquals(270, dstSize.height)
        assertEquals(59, dstOffset.x)
        assertEquals(0, dstOffset.y)
    }

    @Test
    fun testCalculateContainDstSizeAndOffsetWider() {
        val (dstSize, dstOffset) = calculateContainDstSizeAndOffset(540, 960, 400f, 200f)
        assertEquals(112, dstSize.width)
        assertEquals(200, dstSize.height)
        assertEquals(144, dstOffset.x)
        assertEquals(0, dstOffset.y)
    }

    @Test
    fun testCalculateContainDstSizeAndOffsetTaller() {
        val (dstSize, dstOffset) = calculateContainDstSizeAndOffset(540, 960, 200f, 500f)
        assertEquals(200, dstSize.width)
        assertEquals(355, dstSize.height)
        assertEquals(0, dstOffset.x)
        assertEquals(72, dstOffset.y)
    }

    @Test
    fun testShouldAnimatePetOnlyLarvaNeutral() {
        assertTrue(shouldAnimatePet("larva", PetMood.NEUTRAL))
        assertFalse(shouldAnimatePet("larva", PetMood.FELIZ))
        assertFalse(shouldAnimatePet("larva", PetMood.TRISTE))
        assertFalse(shouldAnimatePet("larva", PetMood.ENOJADO))
        assertFalse(shouldAnimatePet("gusano", PetMood.NEUTRAL))
        assertFalse(shouldAnimatePet("abeja", PetMood.NEUTRAL))
        assertFalse(shouldAnimatePet("hormiga", PetMood.NEUTRAL))
    }
}
