package com.example.bugsgame.engine

import com.example.bugsgame.model.Bug
import com.example.bugsgame.model.BugType
import com.example.bugsgame.model.Bonus
import com.example.bugsgame.viewmodel.GameViewModel
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

class GameEngine(var width: Int = 1000, var height: Int = 1000) {
    val bugs = mutableListOf<Bug>()
    private val random = Random()

    var maxBugs: Int = 10
    var topOffset: Int = 0

    var bonus: Bonus? = null
    var isTiltModeActive: Boolean = false
    var tiltModeEndTime: Long = 0L
    var tiltX: Float = 0f
    var tiltY: Float = 0f
    private var lastBonusSpawnTime: Long = 0L

    fun updateSize(newWidth: Int, newHeight: Int, newTopOffset: Int = 0) {
        if (newWidth > 0 && newHeight > 0) {
            this.width = newWidth
            this.height = newHeight
            this.topOffset = newTopOffset
        }
    }

    fun startGame(viewModel: GameViewModel) {
        this.maxBugs = viewModel.maxBugs
        this.bugs.clear()
        this.bonus = null
        this.isTiltModeActive = false
        this.lastBonusSpawnTime = System.currentTimeMillis()
    }

    fun updateBugs(gameSpeedMultiplier: Float) {
        val now = System.currentTimeMillis()

        if (isTiltModeActive && now > tiltModeEndTime) {
            isTiltModeActive = false
        }

        if (bonus == null && now - lastBonusSpawnTime >= 15000L) {
            spawnBonus()
            lastBonusSpawnTime = now
        }

        for (bug in bugs) {
            if (isTiltModeActive) {
                val gravityForce = 4.5f * gameSpeedMultiplier
                bug.x += (bug.dx * 0.8f) + (tiltX * gravityForce)
                bug.y += (bug.dy * 0.8f) + (tiltY * gravityForce)
            } else {
                val totalSpeed = bug.speed * gameSpeedMultiplier
                bug.x += bug.dx * totalSpeed
                bug.y += bug.dy * totalSpeed
            }

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
            val roll = random.nextInt(100)
            val type = when {
                roll < 60 -> BugType.COMMON
                roll < 90 -> BugType.FAST
                else -> BugType.RARE
            }

            val size = type.size
            val maxX = (width - size).coerceAtLeast(1)
            val availableHeight = (height - size - topOffset).coerceAtLeast(1)

            val x = random.nextInt(maxX).toFloat()
            val y = (random.nextInt(availableHeight) + topOffset).toFloat()

            val angle = random.nextDouble() * 2 * Math.PI
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()

            bugs.add(
                Bug(
                    type = type,
                    x = x,
                    y = y,
                    size = size,
                    points = type.points,
                    speed = type.baseSpeed,
                    dx = dx,
                    dy = dy
                )
            )
        }
    }

    private fun spawnBonus() {
        val size = 110
        val maxX = (width - size).coerceAtLeast(1)
        val availableHeight = (height - size - topOffset).coerceAtLeast(1)

        val x = random.nextInt(maxX).toFloat()
        val y = (random.nextInt(availableHeight) + topOffset).toFloat()

        bonus = Bonus(x, y, size)
    }

    fun checkBonusHit(touchX: Float, touchY: Float): Boolean {
        val currentBonus = bonus ?: return false
        if (currentBonus.contains(touchX, touchY)) {
            bonus = null
            isTiltModeActive = true
            tiltModeEndTime = System.currentTimeMillis() + currentBonus.durationMs
            return true
        }
        return false
    }

    fun checkHit(touchX: Float, touchY: Float): Bug? {
        for (i in bugs.indices.reversed()) {
            if (bugs[i].contains(touchX, touchY)) {
                return bugs.removeAt(i)
            }
        }
        return null
    }
}
