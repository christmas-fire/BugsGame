package com.example.bugsgame.data

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
                zodiac = zodiac
            )
            val generatedId = playerDao.insertPlayer(newPlayer)
            return@withContext newPlayer.copy(id = generatedId)
        }

    suspend fun saveGameSession(
        playerName: String,
        playerZodiac: String,
        score: Int,
        difficulty: String,
        hits: Int,
        misses: Int,
        durationSeconds: Int
    ): Long = withContext(Dispatchers.IO) {
        val player = registerOrGetPlayer(playerName, playerZodiac)
        val session = GameSessionEntity(
            playerId = player.id,
            score = score,
            difficulty = difficulty,
            hits = hits,
            misses = misses,
            durationSeconds = durationSeconds,
            finishedAtEpochMillis = System.currentTimeMillis()
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
