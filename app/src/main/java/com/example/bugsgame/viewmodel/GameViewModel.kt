package com.example.bugsgame.viewmodel

import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    var score = 0
    var gameSpeed = 1.0f
    var maxBugs = 10
    var bonusInterval = 5
    var roundDuration = 60
    var remainingTime = 60

    var currentTab = 0
    var isGameRunning = false

    fun hitBug() { score += 10 }
    fun missBug() { score -= 5 }

    fun startNewGame() {
        score = 0
        remainingTime = roundDuration
        isGameRunning = true
    }

    fun resetGame() {
        score = 0
        remainingTime = roundDuration
        isGameRunning = false
    }
}
