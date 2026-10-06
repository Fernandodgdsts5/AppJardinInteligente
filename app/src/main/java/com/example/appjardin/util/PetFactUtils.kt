package com.example.appjardin.util

import com.example.appjardin.R

object PetFactUtils {
    fun getFactArrayResForPet(petId: String?): Int {
        return when (petId?.lowercase()?.trim()) {
            "larva" -> R.array.fun_facts_larva
            "gusano" -> R.array.fun_facts_gusano
            "hormiga" -> R.array.fun_facts_hormiga
            "chanchito" -> R.array.fun_facts_chanchito
            "abeja" -> R.array.fun_facts_abeja
            "reygeko" -> R.array.fun_facts_reygeko
            else -> R.array.fun_facts_gusano
        }
    }
}
