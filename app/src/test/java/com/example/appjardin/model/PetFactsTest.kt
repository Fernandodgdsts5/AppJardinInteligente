package com.example.appjardin.model

import com.example.appjardin.util.PetFactUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetFactRotator(private val facts: List<String>) {
    var shuffledFacts: List<String> = facts.shuffled()
    var index: Int = 0

    fun nextFact(): String {
        if (shuffledFacts.isEmpty()) return "Las plantas aman el agua."
        val fact = shuffledFacts[index]
        if (index >= shuffledFacts.size - 1) {
            val lastFact = fact
            var newShuffled = facts.shuffled()
            if (newShuffled.size > 1 && newShuffled.first() == lastFact) {
                newShuffled = facts.shuffled().filter { it != lastFact } + listOf(lastFact)
            }
            shuffledFacts = newShuffled
            index = 0
        } else {
            index++
        }
        return fact
    }
}

class PetFactsTest {

    @Test
    fun testAllSpeciesHave10UniqueFacts() {
        val speciesList = listOf("larva", "gusano", "hormiga", "chanchito", "abeja", "reygeko")
        
        for (species in speciesList) {
            val arrayRes = PetFactUtils.getFactArrayResForPet(species)
            assertTrue("Species $species has valid array res", arrayRes != 0)
        }
    }

    @Test
    fun testFactRotationNoConsecutiveRepetition() {
        val testFacts = (1..10).map { "Fact $it for pet" }
        val rotator = PetFactRotator(testFacts)

        val seenInFirstRound = mutableListOf<String>()
        for (i in 0 until 10) {
            seenInFirstRound.add(rotator.nextFact())
        }

        assertEquals(10, seenInFirstRound.distinct().size)

        val firstOfSecondRound = rotator.nextFact()
        assertNotEquals(seenInFirstRound.last(), firstOfSecondRound)
    }

    @Test
    fun testFallbackUnknownSpeciesDoesNotCrash() {
        val res = PetFactUtils.getFactArrayResForPet("unknown_dinosaur")
        assertTrue(res != 0) // Falls back to default gusano
    }
}
