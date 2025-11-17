package com.example.pokemon.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val userId: String,
    val avatarName: String = "You",
    val avatarSize: String = "medium",
    val mapTheme: String = "default",
    val mapRotation: Boolean = true,
    val language: String = "en",
    val syncedToServer: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)