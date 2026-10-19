package com.example.bugsgame.data

data class RecordDisplayItem(
    val id: Long,
    val playerName: String,
    val playerZodiac: String,
    val score: Int,
    val difficulty: String,
    val hits: Int,
    val misses: Int,
    val durationSeconds: Int,
    val finishedAtEpochMillis: Long
) {
    val accuracy: Float
        get() {
            val total = hits + misses
            return if (total > 0) (hits.toFloat() / total) * 100f else 0f
        }
}
