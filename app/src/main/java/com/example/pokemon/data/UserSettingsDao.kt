package com.example.pokemon.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE userId = :userId")
    suspend fun getSettings(userId: String): UserSettings?

    @Query("SELECT * FROM user_settings WHERE syncedToServer = 0")
    suspend fun getUnsyncedSettings(): List<UserSettings>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: UserSettings)

    @Update
    suspend fun update(settings: UserSettings)

    @Query("UPDATE user_settings SET syncedToServer = 1 WHERE userId = :userId")
    suspend fun markAsSynced(userId: String)

    @Query("UPDATE user_settings SET syncedToServer = 0 WHERE userId = :userId")
    suspend fun markAsUnsynced(userId: String)
}