package com.example.bugsgame.engine

import com.example.bugsgame.model.Bug
import com.example.bugsgame.viewmodel.GameViewModel
import java.util.Random

class GameEngine(private val width: Int, private val height: Int) {
    val bugs = mutableListOf<Bug>()
    private val random = Random()

    val bugImageNames = listOf("bug", "bug1", "bug2", "bug3")

    var maxBugs: Int = 10
    private var currentSpeed: Float = 1.0f

    fun startGame(viewModel: GameViewModel) {
        this.currentSpeed = viewModel.gameSpeed
        this.maxBugs = viewModel.maxBugs
        this.bugs.clear()
    }

    fun updateBugs(speed: Float) {
        for (bug in bugs) {
            bug.x += bug.dx * speed
            bug.y += bug.dy * speed

            if (bug.x <= 0 || bug.x >= width - bug.size) bug.dx *= -1
            if (bug.y <= 0 || bug.y >= height - bug.size) bug.dy *= -1
        }
    }

    fun spawnBug() {
        if (bugs.size < maxBugs) {
            val size = 100
            val x = random.nextInt(width - size).toFloat()
            val y = random.nextInt(height - size).toFloat()

            val randomImage = bugImageNames[random.nextInt(bugImageNames.size)]

            bugs.add(Bug(x, y, size, imageName = randomImage))
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
