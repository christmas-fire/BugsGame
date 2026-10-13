package com.example.bugsgame.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score_records")
data class ScoreRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userName: String,
    val score: Int,
    val difficulty: String,
    val hits: Int,
    val misses: Int,
    val accuracy: Float,
    val date: String
)
