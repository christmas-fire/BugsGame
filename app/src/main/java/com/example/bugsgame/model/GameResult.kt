package com.example.bugsgame.model
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class GameResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerName: String,
    val score: Int,
    val difficulty: Int
)
