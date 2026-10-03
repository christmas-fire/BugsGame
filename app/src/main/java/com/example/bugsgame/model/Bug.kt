package com.example.bugsgame.model

import java.util.UUID

data class Bug(
    val id: String = UUID.randomUUID().toString(),
    val type: BugType,
    var x: Float,
    var y: Float,
    val size: Int = type.size,
    val points: Int = type.points,
    var speed: Float = type.baseSpeed,
    var dx: Float = 0f,
    var dy: Float = 0f,
    var isAlive: Boolean = true
) {
    fun contains(touchX: Float, touchY: Float): Boolean {
        return touchX >= x && touchX <= x + size && touchY >= y && touchY <= y + size
    }
}
