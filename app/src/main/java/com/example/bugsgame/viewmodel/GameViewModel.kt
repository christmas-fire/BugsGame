package com.example.bugsgame.viewmodel

import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    var score = 0
    var gameSpeed = 1.0f
    var maxBugs = 10
    var bonusInterval = 5
    var roundDuration = 60

    var currentTab = 0

    fun hitBug() { score += 10 }
    fun missBug() { score -= 5 }
}