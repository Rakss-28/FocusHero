package com.example.focushero.data

data class Session(
    val id: Long = System.currentTimeMillis(),
    val subject: String,
    val mood: Mood,
    val durationSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val pointsEarned: Int
)

enum class Mood(val label: String, val focusMinutes: Int, val breakMinutes: Int) {
    HAPPY("Happy", 35, 10),
    NEUTRAL("Neutral", 30, 10),
    TIRED("Tired", 20, 5)
}
