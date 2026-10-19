package com.example.bugsgame.model

data class Bonus(
    var x: Float,
    var y: Float,
    val size: Int = 110,
    val durationMs: Long = 5000L
) {
    fun contains(touchX: Float, touchY: Float): Boolean {
        return touchX >= x && touchX <= x + size && touchY >= y && touchY <= y + size
    }
}