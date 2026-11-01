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
    val zodiac: String = "",
    val coins: Int = 0,
    val freezeCharges: Int = 0,
    val bombCharges: Int = 0,
    val slowCharges: Int = 0,
    val multitouchCharges: Int = 0
)