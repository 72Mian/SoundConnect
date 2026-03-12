package com.example.firebase.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Modelos de respuesta específicos de la API de iTunes
data class ITunesResponse(val results: List<ITunesTrack>)
data class ITunesTrack(
    val trackId: Long,
    val trackName: String?,
    val artistName: String?,
    val artworkUrl100: String?,
    val previewUrl: String?
)

interface MusicApiService {
    @GET("search")
    suspend fun searchSongs(
        @Query("term") query: String,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 20
    ): ITunesResponse
}

// Objeto Singleton para instanciar Retrofit
object RetrofitInstance {
    private const val BASE_URL = "https://itunes.apple.com/"

    val api: MusicApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicApiService::class.java)
    }
}