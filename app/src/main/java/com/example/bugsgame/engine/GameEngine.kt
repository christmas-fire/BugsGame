package com.example.bugsgame.engine

import com.example.bugsgame.model.Bug
import java.util.Random

class GameEngine(private val width: Int, private val height: Int) {
    val bugs = mutableListOf<Bug>()
    private val random = Random()

    fun updateBugs() {
        for (bug in bugs) {
            bug.x += (random.nextFloat() * 20 - 10)
            bug.y += (random.nextFloat() * 20 - 10)

            if (bug.x < 0) bug.x = 0f
            if (bug.x > width - bug.size) bug.x = (width - bug.size).toFloat()
            if (bug.y < 0) bug.y = 0f
            if (bug.y > height - bug.size) bug.y = (height - bug.size).toFloat()
        }
    }

    fun spawnBug() {
        if (bugs.size < 10) {
            val size = 100
            val x = random.nextInt(width - size).toFloat()
            val y = random.nextInt(height - size).toFloat()
            bugs.add(Bug(x, y, size))
        }
    }

    fun checkHit(touchX: Float, touchY: Float): Boolean {
        for (i in bugs.indices.reversed()) {
            if (bugs[i].contains(touchX, touchY)) {
                bugs.removeAt(i)
                return true
            }
        }
        return false
    }
}
