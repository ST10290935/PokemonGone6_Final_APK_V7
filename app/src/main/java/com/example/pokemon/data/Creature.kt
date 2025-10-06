package com.example.pokemon.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Creature(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sprite: Int
)
