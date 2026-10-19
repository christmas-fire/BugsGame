package com.example.bugsgame.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GameSessionDao {
    @Insert
    suspend fun insertSession(session: GameSessionEntity): Long

    @Query("""
        SELECT 
            s.id AS id,
            p.name AS playerName,
            p.zodiac AS playerZodiac,
            s.score AS score,
            s.difficulty AS difficulty,
            s.hits AS hits,
            s.misses AS misses,
            s.durationSeconds AS durationSeconds,
            s.finishedAtEpochMillis AS finishedAtEpochMillis
        FROM game_sessions s
        INNER JOIN players p ON s.playerId = p.id
        WHERE (:playerId IS NULL OR s.playerId = :playerId)
          AND (:difficulty IS NULL OR s.difficulty = :difficulty)
        ORDER BY s.score DESC, s.finishedAtEpochMillis DESC, s.id DESC
    """)
    suspend fun getTopRecordsByScore(
        playerId: Long?,
        difficulty: String?
    ): List<RecordDisplayItem>

    @Query("""
        SELECT 
            s.id AS id,
            p.name AS playerName,
            p.zodiac AS playerZodiac,
            s.score AS score,
            s.difficulty AS difficulty,
            s.hits AS hits,
            s.misses AS misses,
            s.durationSeconds AS durationSeconds,
            s.finishedAtEpochMillis AS finishedAtEpochMillis
        FROM game_sessions s
        INNER JOIN players p ON s.playerId = p.id
        WHERE (:playerId IS NULL OR s.playerId = :playerId)
          AND (:difficulty IS NULL OR s.difficulty = :difficulty)
        ORDER BY s.finishedAtEpochMillis DESC, s.id DESC
    """)
    suspend fun getRecentRecordsByDate(
        playerId: Long?,
        difficulty: String?
    ): List<RecordDisplayItem>
}
