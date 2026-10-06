package com.example.appjardin.util

fun parseFinalHumidity(humidities: String?): Float? {
    if (humidities.isNullOrBlank()) return null
    val parts = humidities.split(",")
    for (i in parts.indices.reversed()) {
        val trimmed = parts[i].trim()
        val num = trimmed.toFloatOrNull()
        if (num != null) return num
    }
    return null
}
