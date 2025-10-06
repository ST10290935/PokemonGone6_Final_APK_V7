package com.example.pokemon.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @GET("creatures")
    suspend fun getCreatures(): List<Creature>

    @POST("creatures/sync")
    suspend fun syncCreatures(@Body payload: SyncPayload)
}
