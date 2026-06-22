package com.example.rateme.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmApi {

    @GET("?method=album.search")
    suspend fun searchAlbum(
        @Query("album") album: String,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json"
    ): LastFmAlbumSearchResponse

    @GET("?method=album.getinfo")
    suspend fun getAlbumInfo(
        @Query("artist") artist: String,
        @Query("album") album: String,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json"
    ): LastFmAlbumInfoResponse

    @GET("?method=artist.getsimilar")
    suspend fun getSimilarArtists(
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 10,
        @Query("format") format: String = "json"
    ): SimilarArtistsResponse

    @GET("?method=artist.gettopalbums")
    suspend fun getTopAlbums(
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 5,
        @Query("format") format: String = "json"
    ): TopAlbumsResponse
}