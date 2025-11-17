//Code Attribution
// For the below code I have used these sources to guide me:
//Foxandroid, 2020. Firebase Cloud Firestore - Android studio tutorial | #1. [video online] Available at: < https://youtu.be/lz6euLh6zAM?si=e-IrhVTdUjDi-5Rx> [Accessed 13 November 2025].
//Hey Delphi, 2023. Android : Android local SQLite sync with Firebase. [video online] Available at: < https://youtu.be/svof9u4wPb4?si=cZX0klU7aK6lCO0S> [Accessed 13 November 2025].
//Phillip Lackner, 2023. Local Notifications in Android - The Full Guide (Android Studio Tutorial). [video online] Available at: < https://youtu.be/LP623htmWcI?si=ehlmF0X8SlagTwHO> [Accessed 16 November 2025].
//Verify Beta, 2025. How to Send Push Notifications with Firebase in Android Studio (Step-by-Step Guide) | Android Mate. [video online] Available at: < https://youtu.be/bD-SGZT7ruc?si=PSgnVzcdRr0VCMya> [Accessed 16 November 2025].


package com.example.pokemon.data

import android.content.Context
import android.util.Log
import com.example.pokemon.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object CreatureRepository {
    private const val TAG = "CreatureRepository"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    //server sync
    suspend fun syncRoomWithServer(context: Context) = withContext(Dispatchers.IO) {
        println("🔄 Sync started…")

        val db = AppDatabase.getDatabase(context)
        val dao = db.creatureDao()
        val api = ApiClient.apiService

        try {
            val localCreatures = dao.getAll()
            val apiCreatures = localCreatures.map { CreatureRequest(it.name, it.sprite) }
            val payload = SyncPayload(apiCreatures)

            api.syncCreatures(payload)
            println("Creatures pushed to server.")

            val serverCreatures = api.getCreatures()
            serverCreatures.forEach { dao.insert(it) }

            println("Sync complete! ${serverCreatures.size} creatures synced.")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Sync failed: ${e.message}")
        }
    }

    //Firebase sync for offline mode
    suspend fun syncToFirebase(context: Context): Boolean {
        val userId = auth.currentUser?.uid
        Log.e("CREATURE_SYNC", "Starting Firebase sync for user: $userId")

        if (userId == null) {
            Log.e("CREATURE_SYNC", "No user logged in, skipping Firebase sync")
            return false
        }

        val db = AppDatabase.getDatabase(context)

        return try {
            val unsyncedCreatures = db.creatureDao().getUnsyncedToFirebase()
            Log.e("CREATURE_SYNC", "Found ${unsyncedCreatures.size} creatures to sync to Firebase")

            unsyncedCreatures.forEach { creature ->
                val creatureData = hashMapOf(
                    "name" to creature.name,
                    "sprite" to creature.sprite,
                    "capturedAt" to creature.capturedAt,
                    "userId" to userId
                )

                // Upload to Firebase
                firestore.collection("users")
                    .document(userId)
                    .collection("captured_creatures")
                    .add(creatureData)
                    .await()

                // Mark as synced
                db.creatureDao().markFirebaseSynced(creature.id)
                Log.e("CREATURE_SYNC", " Synced ${creature.name} to Firebase")
            }

            //Show notification after successful sync
            withContext(Dispatchers.Main) {
                NotificationHelper.showSyncCompleteNotification(context, unsyncedCreatures.size)
            }
            Log.e("CREATURE_SYNC", " Firebase sync complete!")
            true
        } catch (e: Exception) {
            Log.e("CREATURE_SYNC", " Firebase sync failed: ${e.message}", e)
            false
        }
    }

    //Download creatures from Firebase
    suspend fun downloadFromFirebase(context: Context): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val db = AppDatabase.getDatabase(context)

        return try {
            Log.e(" CREATURE_SYNC", "Downloading creatures from Firebase...")

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("captured_creatures")
                .get()
                .await()

            Log.e(" CREATURE_SYNC", "Downloaded ${snapshot.documents.size} creatures from Firebase")

            snapshot.documents.forEach { doc ->
                val creature = Creature(
                    name = doc.getString("name") ?: "",
                    sprite = doc.getLong("sprite")?.toInt() ?: 0,
                    capturedAt = doc.getLong("capturedAt") ?: System.currentTimeMillis(),
                    syncedToFirebase = true,
                    userId = userId
                )
                db.creatureDao().insert(creature)
            }

            Log.e(" CREATURE_SYNC", " Download complete!")
            true
        } catch (e: Exception) {
            Log.e(" CREATURE_SYNC", " Download failed: ${e.message}", e)
            false
        }
    }
}
