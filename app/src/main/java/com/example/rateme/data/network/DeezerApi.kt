package com.example.rateme.data.network

import retrofit2.http.GET
import retrofit2.http.Path
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

    @GET("search/album")
    suspend fun searchAlbum(
        @Query("q") query: String
    ): DeezerAlbumResponse

    @GET("album/{id}/tracks")
    suspend fun getAlbumTracks(
        @Path("id") albumId: Long
    ): DeezerTracksResponse

    data class DeezerArtistResponse(
        val data: List<DeezerArtist>?
    )

    data class DeezerArtist(
        val id: Long,
        val name: String?,
        val picture_big: String?
    )
}

data class DeezerAlbumResponse(
    val data: List<DeezerAlbumSummary>?
)

data class DeezerAlbumSummary(
    val id: Long,
    val title: String?,
    val cover_big: String?,
    val release_date: String?
)

data class DeezerTracksResponse(
    val data: List<DeezerTrack>?
)

data class DeezerSearchResponse(
    val data: List<DeezerTrack>?
)

data class DeezerTrack(
    val id: Long,
    val title: String?,
    val preview: String?  // ссылка на 30-секундное превью
)
