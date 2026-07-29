package com.example.util

fun generateRecoveryKey(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    fun chunk() = (1..4).map { chars.random() }.joinToString("")
    return "${chunk()}-${chunk()}-${chunk()}-${chunk()}"
}

fun normalizeKey(key: String): String {
    return key.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
}
