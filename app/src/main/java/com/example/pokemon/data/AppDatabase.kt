package com.example.pokemon.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Creature::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creatureDao(): CreatureDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "creature_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}