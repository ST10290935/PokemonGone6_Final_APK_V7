package com.example.pokemon.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Creature::class, UserSettings::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creatureDao(): CreatureDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        //  Add user_settings table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_settings (
                        userId TEXT PRIMARY KEY NOT NULL,
                        avatarName TEXT NOT NULL DEFAULT 'You',
                        avatarSize TEXT NOT NULL DEFAULT 'medium',
                        mapTheme TEXT NOT NULL DEFAULT 'default',
                        mapRotation INTEGER NOT NULL DEFAULT 1,
                        syncedToServer INTEGER NOT NULL DEFAULT 0,
                        lastModified INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        //  Add language column
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_settings ADD COLUMN language TEXT NOT NULL DEFAULT 'en'")
            }
        }

        //  Add Firebase sync fields to Creature
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Creature ADD COLUMN capturedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE Creature ADD COLUMN syncedToFirebase INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE Creature ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "creature_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)  // Added MIGRATION_4_5
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}