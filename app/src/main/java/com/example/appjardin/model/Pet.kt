package com.example.appjardin.model

import androidx.annotation.DrawableRes
import com.example.appjardin.R

enum class PetMood {
    ASUSTADO, ENOJADO, FELIZ, TRISTE, NEUTRAL
}

enum class Pet(
    val id: String,
    val speciesName: String,
    val defaultName: String,
    private val moodDrawables: Map<PetMood, Int>
) {
    GUSANO(
        id = "gusano",
        speciesName = "Gusano",
        defaultName = "Coco",
        moodDrawables = mapOf(
            PetMood.ASUSTADO to R.drawable.pet_gusano_asustado,
            PetMood.ENOJADO to R.drawable.pet_gusano_enojado,
            PetMood.FELIZ to R.drawable.pet_gusano_feliz,
            PetMood.TRISTE to R.drawable.pet_gusano_triste,
            PetMood.NEUTRAL to R.drawable.pet_gusano_neutral
        )
    ),
    ABEJA(
        id = "abeja",
        speciesName = "Abeja",
        defaultName = "Miel",
        moodDrawables = mapOf(
            PetMood.ASUSTADO to R.drawable.pet_abeja_asustado,
            PetMood.ENOJADO to R.drawable.pet_abeja_enojado,
            PetMood.FELIZ to R.drawable.pet_abeja_feliz,
            PetMood.TRISTE to R.drawable.pet_abeja_triste,
            PetMood.NEUTRAL to R.drawable.pet_abeja_neutral
        )
    ),
    CHANCHITO(
        id = "chanchito",
        speciesName = "Chanchito",
        defaultName = "Troll",
        moodDrawables = mapOf(
            PetMood.ASUSTADO to R.drawable.pet_chanchito_asustado,
            PetMood.ENOJADO to R.drawable.pet_chanchito_enojado,
            PetMood.FELIZ to R.drawable.pet_chanchito_feliz,
            PetMood.TRISTE to R.drawable.pet_chanchito_triste,
            PetMood.NEUTRAL to R.drawable.pet_chanchito_neutral
        )
    ),
    HORMIGA(
        id = "hormiga",
        speciesName = "Hormiga",
        defaultName = "Luna",
        moodDrawables = mapOf(
            PetMood.ASUSTADO to R.drawable.pet_hormiga_asustado,
            PetMood.ENOJADO to R.drawable.pet_hormiga_enojado,
            PetMood.FELIZ to R.drawable.pet_hormiga_feliz,
            PetMood.TRISTE to R.drawable.pet_hormiga_triste,
            PetMood.NEUTRAL to R.drawable.pet_hormiga_neutral
        )
    ),
    LARVA(
        id = "larva",
        speciesName = "Larva",
        defaultName = "Gringo",
        moodDrawables = mapOf(
            PetMood.ASUSTADO to R.drawable.pet_larva_asustado,
            PetMood.ENOJADO to R.drawable.pet_larva_enojado,
            PetMood.FELIZ to R.drawable.pet_larva_feliz,
            PetMood.TRISTE to R.drawable.pet_larva_triste,
            PetMood.NEUTRAL to R.drawable.pet_larva_neutral
        )
    );

    @DrawableRes
    fun getDrawable(mood: PetMood): Int {
        return moodDrawables[mood] ?: moodDrawables[PetMood.FELIZ]!!
    }

    companion object {
        fun fromId(id: String?): Pet {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: GUSANO
        }
    }
}

fun MoistureState.toPetMood(): PetMood {
    return when (this) {
        MoistureState.LOW_MOISTURE -> PetMood.TRISTE
        MoistureState.MEDIUM_MOISTURE -> PetMood.NEUTRAL
        MoistureState.GOOD_MOISTURE -> PetMood.FELIZ
        MoistureState.EXCESS_MOISTURE -> PetMood.ENOJADO
        else -> PetMood.NEUTRAL
    }
}
