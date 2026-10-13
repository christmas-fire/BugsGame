package com.example.bugsgame.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Query("SELECT * FROM players WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun getPlayerByNormalizedName(normalizedName: String): PlayerEntity?

    @Query("SELECT * FROM players ORDER BY name ASC")
    suspend fun getAllPlayers(): List<PlayerEntity>
}