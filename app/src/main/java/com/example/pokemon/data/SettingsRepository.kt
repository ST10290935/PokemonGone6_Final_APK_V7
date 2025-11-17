package com.example.pokemon.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.SetOptions

object SettingsRepository {
    private const val TAG = "SettingsRepository"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Save settings to both SharedPreferences and Room (offline-first)
     */
    suspend fun saveSettings(
        context: Context,
        avatarName: String? = null,
        avatarSize: String? = null,
        mapTheme: String? = null,
        mapRotation: Boolean? = null,
        language: String? = null
    ) {
        val userId = auth.currentUser?.uid ?: return
        val db = AppDatabase.getDatabase(context)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        // Get current settings from Room OR SharedPreferences as fallback
        var settings = db.userSettingsDao().getSettings(userId) ?: UserSettings(
            userId = userId,
            avatarName = prefs.getString("pref_avatar_name", "You") ?: "You",
            avatarSize = prefs.getString("pref_avatar_size", "medium") ?: "medium",
            mapTheme = prefs.getString("pref_map_theme", "default") ?: "default",
            mapRotation = prefs.getBoolean("pref_map_rotation", true),
            language = prefs.getString("selected_language", "en") ?: "en"
        )

        // Update only the fields that were provided
        settings = settings.copy(
            avatarName = avatarName ?: settings.avatarName,
            avatarSize = avatarSize ?: settings.avatarSize,
            mapTheme = mapTheme ?: settings.mapTheme,
            mapRotation = mapRotation ?: settings.mapRotation,
            language = language ?: settings.language,
            syncedToServer = false,  // Mark as needing sync
            lastModified = System.currentTimeMillis()
        )

        // Save to Room database (offline backup)
        db.userSettingsDao().insert(settings)

        // Update SharedPreferences for immediate UI response
        with(prefs.edit()) {
            putString("pref_avatar_name", settings.avatarName)
            putString("pref_avatar_size", settings.avatarSize)
            putString("pref_map_theme", settings.mapTheme)
            putBoolean("pref_map_rotation", settings.mapRotation)
            putString("selected_language", settings.language)
            apply()
        }

        Log.d(TAG, "Settings saved locally")
    }

    /**
     * Sync settings to Firebase
     */
    suspend fun syncToFirebase(context: Context): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val db = AppDatabase.getDatabase(context)

        return try {
            val unsyncedSettings = db.userSettingsDao().getUnsyncedSettings()

            unsyncedSettings.forEach { settings ->
                Log.e(" SYNC", "Syncing settings - Language: ${settings.language}, Avatar: ${settings.avatarName}")

                val settingsData = hashMapOf(
                    "avatarName" to settings.avatarName,
                    "avatarSize" to settings.avatarSize,
                    "mapTheme" to settings.mapTheme,
                    "mapRotation" to settings.mapRotation,
                    "language" to settings.language,
                    "lastModified" to settings.lastModified
                )

                firestore.collection("users")
                    .document(userId)
                    .collection("settings")
                    .document("preferences")
                    .set(settingsData, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                db.userSettingsDao().markAsSynced(userId)
                Log.d(TAG, "Settings synced to Firebase")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync settings to Firebase", e)
            false
        }
    }

    /**
     * Download settings from Firebase
     */
    suspend fun downloadFromFirebase(context: Context): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val db = AppDatabase.getDatabase(context)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("settings")
                .document("preferences")
                .get()
                .await()

            if (doc.exists()) {
                val serverLastModified = doc.getLong("lastModified") ?: 0L
                val localSettings = db.userSettingsDao().getSettings(userId)
                val localLastModified = localSettings?.lastModified ?: 0L

                // Conflict resolution: Keep the most recent settings
                if (serverLastModified > localLastModified) {
                    // Server is newer, use server settings
                    Log.d(TAG, "Server settings are newer, downloading...")
                    val settings = UserSettings(
                        userId = userId,
                        avatarName = doc.getString("avatarName") ?: "You",
                        avatarSize = doc.getString("avatarSize") ?: "medium",
                        mapTheme = doc.getString("mapTheme") ?: "default",
                        mapRotation = doc.getBoolean("mapRotation") ?: true,
                        language = doc.getString("language") ?: "en",
                        syncedToServer = true,
                        lastModified = serverLastModified
                    )

                    // Save to Room
                    db.userSettingsDao().insert(settings)

                    // Save to SharedPreferences
                    with(prefs.edit()) {
                        putString("pref_avatar_name", settings.avatarName)
                        putString("pref_avatar_size", settings.avatarSize)
                        putString("pref_map_theme", settings.mapTheme)
                        putBoolean("pref_map_rotation", settings.mapRotation)
                        putString("selected_language", settings.language)
                        apply()
                    }

                    Log.d(TAG, "Server settings downloaded and applied")
                } else {
                    // Local is newer or same, keep local and sync to server
                    Log.d(TAG, "Local settings are newer or same, keeping local")
                    if (localSettings != null && !localSettings.syncedToServer) {
                        syncToFirebase(context)
                    }
                }
                true
            } else {
                // No server settings exist, upload local if we have any
                Log.d(TAG, "No server settings found")
                val localSettings = db.userSettingsDao().getSettings(userId)
                if (localSettings != null) {
                    Log.d(TAG, "Uploading local settings to server...")
                    syncToFirebase(context)
                }
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download settings from Firebase", e)
            false
        }
    }

    /**
     * Load settings from Room on app start (works offline)
     */
    suspend fun loadLocalSettings(context: Context) {
        val userId = auth.currentUser?.uid ?: return
        val db = AppDatabase.getDatabase(context)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val settings = db.userSettingsDao().getSettings(userId)

        settings?.let {
            with(prefs.edit()) {
                putString("pref_avatar_name", it.avatarName)
                putString("pref_avatar_size", it.avatarSize)
                putString("pref_map_theme", it.mapTheme)
                putBoolean("pref_map_rotation", it.mapRotation)
                putString("selected_language", it.language)
                apply()
            }
            Log.d(TAG, "Local settings loaded")
        }
    }
}