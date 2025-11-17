package com.example.pokemon.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Creature(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sprite: Int,
    val capturedAt: Long = System.currentTimeMillis(),  // Track when captured
    val syncedToFirebase: Boolean = false,  // Track if synced to Firebase
    val userId: String = ""  // Track which user captured it
)