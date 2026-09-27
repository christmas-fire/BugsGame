package com.example.bugsgame.engine

import com.example.bugsgame.model.Bug
import java.util.*

class GameEngine(private val width: Int, private val height: Int) {
    val bugs = mutableListOf<Bug>()
    private val random = Random()

    fun updateBugs() {
        for (bug in bugs) {
            bug.x += (random.nextFloat() * 10 - 5)
            bug.y += (random.nextFloat() * 10 - 5)
        }
    }

    fun spawnBug() {
        if (bugs.size < 10) {
            bugs.add(Bug(random.nextInt(width).toFloat(), random.nextInt(height).toFloat(), 100))
        }
    }
}