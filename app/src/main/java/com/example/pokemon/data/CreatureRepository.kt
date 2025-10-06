package com.example.pokemon.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CreatureRepository {

    suspend fun syncRoomWithServer(context: Context) = withContext(Dispatchers.IO) {
        //this line is used to show in the logcat if the sync has started on launch
        println("🔄 Sync started…")

        val db = AppDatabase.getDatabase(context)
        val dao = db.creatureDao()
        val api = ApiClient.apiService

        try {
            // Pull all local creatures
            val localCreatures = dao.getAll()

            // Map to API request model
            val apiCreatures = localCreatures.map { CreatureRequest(it.name, it.sprite) }


            val payload = SyncPayload(apiCreatures)

            // Push to server
            api.syncCreatures(payload)
            println("✅ Creatures pushed to server.")

            // Fetch updated creatures from server
            val serverCreatures = api.getCreatures()

            // Insert into Room
            serverCreatures.forEach { dao.insert(it) }

            println("✅ Sync complete! ${serverCreatures.size} creatures synced.")
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Sync failed: ${e.message}")
        }
    }
}
