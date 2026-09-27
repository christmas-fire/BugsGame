package com.example.bugsgame.model

import java.util.Random

data class Bug(
    var x: Float,
    var y: Float,
    val size: Int,
    var isAlive: Boolean = true,
    var dx: Float = (Random().nextFloat() * 10 - 5),
    var dy: Float = (Random().nextFloat() * 10 - 5)
) {
    fun contains(touchX: Float, touchY: Float): Boolean {
        return touchX >= x && touchX <= x + size && touchY >= y && touchY <= y + size
    }
}