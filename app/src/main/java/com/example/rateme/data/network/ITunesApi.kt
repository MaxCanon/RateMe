package com.example.rateme.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface ITunesApi {
    @GET("search")
    suspend fun searchTrack(
        @Query("term") term: String,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 1
    ): ITunesSearchResponse
}

data class ITunesSearchResponse(
    val resultCount: Int,
    val results: List<ITunesTrack>
)

data class ITunesTrack(
    val trackName: String?,
    val previewUrl: String?,
    val releaseDate: String?
)
