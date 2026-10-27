package com.example.bugsgame.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bugsgame.data.GameRepository
import com.example.bugsgame.data.PlayerEntity
import com.example.bugsgame.data.RecordDisplayItem
import com.example.bugsgame.engine.GameEngine
import com.example.bugsgame.network.CbrClient
import kotlinx.coroutines.launch

class GameViewModel(
    val repository: GameRepository
) : ViewModel() {

    val gameEngine: GameEngine = GameEngine()

    var currentUserName: String = "Гость"
    var currentUserZodiac: String = ""

    var score: Int = 0
    var hits: Int = 0
    var misses: Int = 0

    var gameSpeed: Float = 1.0f
    var maxBugs: Int = 10
    var bonusInterval: Int = 5
    var roundDuration: Int = 60
    var remainingTime: Int = 60

    var currentTab: Int = 0
    var isGameRunning: Boolean = false

    var lastTickTime: Long = System.currentTimeMillis()

    init {
        loadGoldPrice()
    }

    fun loadGoldPrice() {
        viewModelScope.launch {
            try {
                val price = CbrClient.fetchGoldPrice()
                gameEngine.goldPrice = price
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val accuracy: Float
        get() {
            val total = hits + misses
            return if (total > 0) (hits.toFloat() / total) * 100f else 0f
        }

    val difficultyName: String
        get() = when {
            gameSpeed <= 1.5f -> "Легкий"
            gameSpeed <= 3.0f -> "Средний"
            else -> "Сложный"
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
        lastTickTime = System.currentTimeMillis()
        gameEngine.startGame(this)
    }

    fun resetGame() {
        score = 0
        hits = 0
        misses = 0
        remainingTime = roundDuration
        isGameRunning = false
        gameEngine.bugs.clear()
        gameEngine.bonus = null
        gameEngine.isTiltModeActive = false
        gameEngine.maxBugs = maxBugs
    }

    suspend fun saveCurrentGameSession(): Long {
        return repository.saveGameSession(
            playerName = currentUserName,
            playerZodiac = currentUserZodiac,
            score = score,
            difficulty = difficultyName,
            hits = hits,
            misses = misses,
            durationSeconds = roundDuration
        )
    }

    suspend fun registerOrGetPlayer(name: String, zodiac: String): PlayerEntity {
        return repository.registerOrGetPlayer(name, zodiac)
    }

    suspend fun getAllPlayers(): List<PlayerEntity> {
        return repository.getAllPlayers()
    }

    suspend fun getFilteredRecords(playerId: Long?, sortByScore: Boolean): List<RecordDisplayItem> {
        return repository.getFilteredRecords(playerId, null, sortByScore)
    }
}
