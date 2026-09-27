package com.example.bugsgame.model

data class Bug(
    var x: Float,
    var y: Float,
    val size: Int,
    var isAlive: Boolean = true
) {
    fun contains(touchX: Float, touchY: Float): Boolean {
        return touchX >= x && touchX <= x + size && touchY >= y && touchY <= y + size
    }
}