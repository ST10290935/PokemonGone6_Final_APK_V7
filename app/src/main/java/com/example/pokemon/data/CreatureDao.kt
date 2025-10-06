package com.example.pokemon.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CreatureDao {
    @Insert
    suspend fun insert(creature: Creature)

    @Query("SELECT * FROM Creature")
    suspend fun getAll(): List<Creature>
}
