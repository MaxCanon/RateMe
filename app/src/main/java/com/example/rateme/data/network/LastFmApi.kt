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
}