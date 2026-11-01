package com.example.bugsgame.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bugsgame.data.GameRepository
import com.example.bugsgame.data.PlayerEntity
import com.example.bugsgame.data.RecordDisplayItem
import com.example.bugsgame.engine.GameEngine
import com.example.bugsgame.model.Bug
import com.example.bugsgame.model.SkillType
import com.example.bugsgame.network.CbrClient
import kotlinx.coroutines.launch

class GameViewModel(
    val repository: GameRepository
) : ViewModel() {

    val gameEngine: GameEngine = GameEngine()

    var currentPlayerId: Long = 0L
    var currentUserName: String = "Гость"
    var currentUserZodiac: String = ""
    var currentPlayerCoins: Int = 0

    var freezeCharges: Int = 0
    var bombCharges: Int = 0
    var slowCharges: Int = 0
    var multitouchCharges: Int = 0

    var selectedSkill: SkillType? = null
    var usedSkillTitle: String = "Без умений"

    var score: Int = 0
    var hits: Int = 0
    var misses: Int = 0

    var gameSpeed: Float = 1.0f
    var maxBugs: Int = 10
    var bonusInterval: Int = 15
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

    val earnedCoinsThisRound: Int
        get() = (score / 100) * 10

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
        usedSkillTitle = "Без умений"
        lastTickTime = System.currentTimeMillis()
        gameEngine.startGame(this)
    }

    fun resetGame() {
        score = 0
        hits = 0
        misses = 0
        remainingTime = roundDuration
        isGameRunning = false
        usedSkillTitle = "Без умений"
        gameEngine.bugs.clear()
        gameEngine.bonus = null
        gameEngine.isTiltModeActive = false
        gameEngine.isFrozen = false
        gameEngine.isSlowed = false
        gameEngine.isMultitouchActive = false
        gameEngine.maxBugs = maxBugs
    }

    fun syncPlayer(player: PlayerEntity) {
        currentPlayerId = player.id
        currentUserName = player.name
        currentUserZodiac = player.zodiac
        currentPlayerCoins = player.coins
        freezeCharges = player.freezeCharges
        bombCharges = player.bombCharges
        slowCharges = player.slowCharges
        multitouchCharges = player.multitouchCharges
    }

    fun getChargesForSkill(skill: SkillType): Int = when (skill) {
        SkillType.FREEZE -> freezeCharges
        SkillType.BOMB -> bombCharges
        SkillType.SLOW -> slowCharges
        SkillType.MULTITOUCH -> multitouchCharges
    }

    fun getOwnedSkills(): List<SkillType> {
        val list = mutableListOf<SkillType>()
        if (freezeCharges > 0) list.add(SkillType.FREEZE)
        if (bombCharges > 0) list.add(SkillType.BOMB)
        if (slowCharges > 0) list.add(SkillType.SLOW)
        if (multitouchCharges > 0) list.add(SkillType.MULTITOUCH)
        return list
    }

    suspend fun buySkill(skill: SkillType): Result<PlayerEntity> {
        val res = repository.buySkill(currentPlayerId, skill)
        res.getOrNull()?.let { syncPlayer(it) }
        return res
    }

    suspend fun activateSkill(skill: SkillType): Result<String> {
        if (!isGameRunning) return Result.failure(Exception("Игра не запущена!"))
        val chargeRes = repository.consumeSkillCharge(currentPlayerId, skill)
        if (chargeRes.isFailure) return Result.failure(Exception("Нет зарядов!"))

        syncPlayer(chargeRes.getOrThrow())
        usedSkillTitle = skill.title

        when (skill) {
            SkillType.FREEZE -> gameEngine.applyFreeze(5000L)
            SkillType.SLOW -> gameEngine.applySlow(7000L)
            SkillType.BOMB -> {
                val destroyed = gameEngine.triggerBomb()
                val earned = destroyed.sumOf { it.points }
                hits += destroyed.size
                score += earned
            }
            SkillType.MULTITOUCH -> gameEngine.applyMultitouch(8000L)
        }
        return Result.success("Умение '${skill.title}' активировано!")
    }

    suspend fun saveCurrentGameSession(): Long {
        val coins = earnedCoinsThisRound
        val sessionId = repository.saveGameSession(
            playerId = currentPlayerId,
            playerName = currentUserName,
            playerZodiac = currentUserZodiac,
            score = score,
            difficulty = difficultyName,
            hits = hits,
            misses = misses,
            durationSeconds = roundDuration,
            usedSkill = usedSkillTitle,
            coinsEarned = coins
        )
        val p = repository.getPlayerById(currentPlayerId)
        if (p != null) syncPlayer(p)
        return sessionId
    }

    suspend fun registerOrGetPlayer(name: String, zodiac: String): PlayerEntity {
        val p = repository.registerOrGetPlayer(name, zodiac)
        syncPlayer(p)
        return p
    }

    suspend fun getAllPlayers(): List<PlayerEntity> = repository.getAllPlayers()

    suspend fun getFilteredRecords(playerId: Long?, sortByScore: Boolean): List<RecordDisplayItem> {
        return repository.getFilteredRecords(playerId, null, sortByScore)
    }
}