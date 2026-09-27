package com.example.bugsgame.viewmodel

import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    var score = 0
    var lives = 3
    var gameSpeed = 1.0f
    var maxBugs = 10

    fun hitBug() {
        score += 10
    }

    fun missBug() {
        score -= 5
    }
}