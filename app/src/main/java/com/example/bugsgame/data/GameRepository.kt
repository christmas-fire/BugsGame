package com.example.bugsgame.data

import com.example.bugsgame.model.SkillType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository(private val database: AppDatabase) {

    private val playerDao = database.playerDao()
    private val sessionDao = database.gameSessionDao()

    suspend fun registerOrGetPlayer(rawName: String, zodiac: String): PlayerEntity =
        withContext(Dispatchers.IO) {
            val displayName = rawName.trim()
            val normalized = displayName.lowercase()

            val existing = playerDao.getPlayerByNormalizedName(normalized)
            if (existing != null) return@withContext existing

            val newPlayer = PlayerEntity(
                name = displayName,
                normalizedName = normalized,
                zodiac = zodiac,
                coins = 0
            )
            val generatedId = playerDao.insertPlayer(newPlayer)
            return@withContext newPlayer.copy(id = generatedId)
        }

    suspend fun getPlayerById(id: Long): PlayerEntity? = withContext(Dispatchers.IO) {
        playerDao.getPlayerById(id)
    }

    suspend fun addCoinsToPlayer(playerId: Long, coinsToAdd: Int): PlayerEntity? = withContext(Dispatchers.IO) {
        val player = playerDao.getPlayerById(playerId) ?: return@withContext null
        val updated = player.copy(coins = player.coins + coinsToAdd)
        playerDao.updatePlayer(updated)
        updated
    }

    suspend fun buySkill(playerId: Long, skill: SkillType): Result<PlayerEntity> = withContext(Dispatchers.IO) {
        val player = playerDao.getPlayerById(playerId) ?: return@withContext Result.failure(Exception("Игрок не найден"))
        if (player.coins < skill.price) {
            return@withContext Result.failure(Exception("Недостаточно монет! Требуется ${skill.price}"))
        }

        val updated = when (skill) {
            SkillType.FREEZE -> player.copy(
                coins = player.coins - skill.price,
                freezeCharges = player.freezeCharges + skill.chargesPerBuy
            )
            SkillType.BOMB -> player.copy(
                coins = player.coins - skill.price,
                bombCharges = player.bombCharges + skill.chargesPerBuy
            )
            SkillType.SLOW -> player.copy(
                coins = player.coins - skill.price,
                slowCharges = player.slowCharges + skill.chargesPerBuy
            )
            SkillType.MULTITOUCH -> player.copy(
                coins = player.coins - skill.price,
                multitouchCharges = player.multitouchCharges + skill.chargesPerBuy
            )
        }
        playerDao.updatePlayer(updated)
        Result.success(updated)
    }

    suspend fun consumeSkillCharge(playerId: Long, skill: SkillType): Result<PlayerEntity> = withContext(Dispatchers.IO) {
        val player = playerDao.getPlayerById(playerId) ?: return@withContext Result.failure(Exception("Игрок не найден"))
        val updated = when (skill) {
            SkillType.FREEZE -> {
                if (player.freezeCharges <= 0) return@withContext Result.failure(Exception("Нет зарядов"))
                player.copy(freezeCharges = player.freezeCharges - 1)
            }
            SkillType.BOMB -> {
                if (player.bombCharges <= 0) return@withContext Result.failure(Exception("Нет зарядов"))
                player.copy(bombCharges = player.bombCharges - 1)
            }
            SkillType.SLOW -> {
                if (player.slowCharges <= 0) return@withContext Result.failure(Exception("Нет зарядов"))
                player.copy(slowCharges = player.slowCharges - 1)
            }
            SkillType.MULTITOUCH -> {
                if (player.multitouchCharges <= 0) return@withContext Result.failure(Exception("Нет зарядов"))
                player.copy(multitouchCharges = player.multitouchCharges - 1)
            }
        }
        playerDao.updatePlayer(updated)
        Result.success(updated)
    }

    suspend fun saveGameSession(
        playerId: Long,
        playerName: String,
        playerZodiac: String,
        score: Int,
        difficulty: String,
        hits: Int,
        misses: Int,
        durationSeconds: Int,
        usedSkill: String,
        coinsEarned: Int
    ): Long = withContext(Dispatchers.IO) {
        val actualPlayerId = if (playerId > 0) playerId else registerOrGetPlayer(playerName, playerZodiac).id

        // Начисляем монеты на аккаунт игрока
        if (coinsEarned > 0) {
            addCoinsToPlayer(actualPlayerId, coinsEarned)
        }

        val session = GameSessionEntity(
            playerId = actualPlayerId,
            score = score,
            difficulty = difficulty,
            hits = hits,
            misses = misses,
            durationSeconds = durationSeconds,
            finishedAtEpochMillis = System.currentTimeMillis(),
            usedSkill = usedSkill,
            coinsEarned = coinsEarned
        )
        return@withContext sessionDao.insertSession(session)
    }

    suspend fun getFilteredRecords(
        playerId: Long?,
        difficulty: String?,
        sortByScore: Boolean
    ): List<RecordDisplayItem> = withContext(Dispatchers.IO) {
        if (sortByScore) {
            sessionDao.getTopRecordsByScore(playerId, difficulty)
        } else {
            sessionDao.getRecentRecordsByDate(playerId, difficulty)
        }
    }

    suspend fun getAllPlayers(): List<PlayerEntity> = withContext(Dispatchers.IO) {
        playerDao.getAllPlayers()
    }
}