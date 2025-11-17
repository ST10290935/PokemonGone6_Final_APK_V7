package com.example.pokemon.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CreatureDao {
    @Insert
    suspend fun insert(creature: Creature)

    @Update
    suspend fun update(creature: Creature)

    @Query("SELECT * FROM Creature")
    suspend fun getAll(): List<Creature>

    //Get creatures for specific user
    @Query("SELECT * FROM Creature WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<Creature>

    //Get creatures not yet synced to Firebase
    @Query("SELECT * FROM Creature WHERE syncedToFirebase = 0")
    suspend fun getUnsyncedToFirebase(): List<Creature>

    //Mark creature as synced to Firebase
    @Query("UPDATE Creature SET syncedToFirebase = 1 WHERE id = :id")
    suspend fun markFirebaseSynced(id: Int)

    //Delete all creatures (for testing/logout)
    @Query("DELETE FROM Creature")
    suspend fun deleteAll()
}