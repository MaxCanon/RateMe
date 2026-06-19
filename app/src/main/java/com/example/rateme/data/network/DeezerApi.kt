package com.example.rateme.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface DeezerApi {

    @GET("search/track")
    suspend fun searchTrack(
        @Query("q") query: String
    ): DeezerSearchResponse

    @GET("search/artist")
    suspend fun searchArtist(
        @Query("q") query: String
    ): DeezerArtistResponse

    data class DeezerArtistResponse(
        val data: List<DeezerArtist>?
    )

    data class DeezerArtist(
        val id: Long,
        val name: String?,
        val picture_big: String?
    )
}

data class DeezerSearchResponse(
    val data: List<DeezerTrack>?
)

data class DeezerTrack(
    val id: Long,
    val title: String?,
    val preview: String?  // ссылка на 30-секундное превью
)