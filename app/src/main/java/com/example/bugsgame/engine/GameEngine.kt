package com.example.bugsgame.engine

import com.example.bugsgame.model.Bug
import com.example.bugsgame.viewmodel.GameViewModel
import java.util.Random

class GameEngine(var width: Int = 1000, var height: Int = 1000) {
    val bugs = mutableListOf<Bug>()
    private val random = Random()

    val bugImageNames = listOf("bug", "bug1", "bug2", "bug3")

    var maxBugs: Int = 10
    private var currentSpeed: Float = 1.0f

    var topOffset: Int = 0

    fun updateSize(newWidth: Int, newHeight: Int, newTopOffset: Int = topOffset) {
        if (newWidth > 0 && newHeight > 0) {
            this.width = newWidth
            this.height = newHeight
            this.topOffset = newTopOffset
        }
    }

    fun startGame(viewModel: GameViewModel) {
        this.currentSpeed = viewModel.gameSpeed
        this.maxBugs = viewModel.maxBugs
        this.bugs.clear()
    }

    fun updateBugs(speed: Float) {
        for (bug in bugs) {
            bug.x += bug.dx * speed
            bug.y += bug.dy * speed

            if (bug.x <= 0) {
                bug.x = 0f
                bug.dx = kotlin.math.abs(bug.dx)
            } else if (bug.x >= width - bug.size) {
                bug.x = (width - bug.size).toFloat().coerceAtLeast(0f)
                bug.dx = -kotlin.math.abs(bug.dx)
            }

            if (bug.y <= topOffset) {
                bug.y = topOffset.toFloat()
                bug.dy = kotlin.math.abs(bug.dy)
            } else if (bug.y >= height - bug.size) {
                bug.y = (height - bug.size).toFloat().coerceAtLeast(topOffset.toFloat())
                bug.dy = -kotlin.math.abs(bug.dy)
            }
        }
    }

    fun spawnBug() {
        if (bugs.size < maxBugs) {
            val size = 100
            val maxX = (width - size).coerceAtLeast(1)
            val availableHeight = (height - size - topOffset).coerceAtLeast(1)

            val x = random.nextInt(maxX).toFloat()
            val y = (random.nextInt(availableHeight) + topOffset).toFloat()

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
