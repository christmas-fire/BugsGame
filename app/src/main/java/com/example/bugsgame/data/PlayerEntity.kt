package com.example.bugsgame.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    indices = [
        Index(value = ["normalizedName"], unique = true)
    ]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val zodiac: String = ""
)
