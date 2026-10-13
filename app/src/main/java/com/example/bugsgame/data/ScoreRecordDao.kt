package com.example.bugsgame.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScoreRecordDao {
    @Insert
    suspend fun insertRecord(record: ScoreRecord): Long

    @Query("SELECT * FROM score_records ORDER BY score DESC, id DESC")
    suspend fun getAllRecords(): List<ScoreRecord>
}
