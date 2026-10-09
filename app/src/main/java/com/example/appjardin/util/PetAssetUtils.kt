package com.example.appjardin.util

import com.example.appjardin.model.PetMood

sealed class PetVisualTarget {
    data class FrameAnimation(val folderName: String) : PetVisualTarget()
    data class StaticAsset(val assetPath: String) : PetVisualTarget()
}

object PetAssetUtils {

    fun getPetStaticAssetPath(speciesId: String, mood: PetMood): String {
        val cleanSpecies = when (speciesId.lowercase().trim()) {
            "larva" -> "larva"
            "gusano" -> "gusano"
            "hormiga" -> "hormiga"
            "chanchito" -> "chanchito"
            "abeja" -> "abeja"
            "reygeko" -> "reygeko"
            else -> "gusano"
        }

        val prefix = when (cleanSpecies) {
            "larva" -> "l"
            "gusano" -> "g"
            "hormiga" -> "h"
            "chanchito" -> "c"
            "abeja" -> "a"
            "reygeko" -> "r"
            else -> "g"
        }

        val moodChar = when (mood) {
            PetMood.FELIZ -> "f"
            PetMood.NEUTRAL -> "n"
            PetMood.TRISTE -> "t"
            PetMood.ENOJADO -> "e"
            PetMood.ASUSTADO -> "a"
        }

        return "mascotas/$cleanSpecies/$prefix$moodChar.png"
    }

    fun resolvePetVisualTarget(speciesId: String, mood: PetMood): PetVisualTarget {
        val isLarva = speciesId.equals("larva", ignoreCase = true)
        return if (isLarva && mood != PetMood.ASUSTADO) {
            val folder = when (mood) {
                PetMood.FELIZ -> "lf"
                PetMood.NEUTRAL -> "ln"
                PetMood.TRISTE -> "lt"
                PetMood.ENOJADO -> "le"
                PetMood.ASUSTADO -> "lt"
            }
            PetVisualTarget.FrameAnimation(folder)
        } else {
            val staticPath = getPetStaticAssetPath(speciesId, mood)
            PetVisualTarget.StaticAsset(staticPath)
        }
    }
}
