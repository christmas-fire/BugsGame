package com.example.bugsgame.viewmodel

import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    var score = 0
    var hits = 0
    var misses = 0

    var gameSpeed = 1.0f
    var maxBugs = 10
    var bonusInterval = 5
    var roundDuration = 60
    var remainingTime = 60

    var currentTab = 0
    var isGameRunning = false

    val accuracy: Float
        get() {
            val total = hits + misses
            return if (total > 0) (hits.toFloat() / total) * 100f else 0f
        }

    fun hitBug(points: Int) {
        hits++
        score += points
    }

    fun missBug() {
        misses++
        score = (score - 5).coerceAtLeast(0)
    }

    fun startNewGame() {
        score = 0
        hits = 0
        misses = 0
        remainingTime = roundDuration
        isGameRunning = true
    }

    fun resetGame() {
        score = 0
        hits = 0
        misses = 0
        remainingTime = roundDuration
        isGameRunning = false
    }
}