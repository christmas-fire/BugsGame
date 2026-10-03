package com.example.bugsgame.model

enum class BugType(
    val title: String,
    val baseSpeed: Float,
    val size: Int,
    val points: Int,
    val drawableResName: String
) {
    COMMON("Обычный", baseSpeed = 3.5f, size = 110, points = 10, drawableResName = "bug"),
    FAST("Быстрый", baseSpeed = 7.5f, size = 80, points = 25, drawableResName = "bug1"),
    RARE("Редкий", baseSpeed = 2.0f, size = 200, points = 50, drawableResName = "bug2")
}
