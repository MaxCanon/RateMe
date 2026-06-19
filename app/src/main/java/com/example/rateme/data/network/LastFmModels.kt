package com.example.rateme.data.network

import com.google.gson.annotations.SerializedName

data class LastFmAlbumSearchResponse(
    val results: AlbumSearchResults?
)
data class AlbumSearchResults(
    @SerializedName("albummatches")
    val albumMatches: AlbumMatches?
)
data class AlbumMatches(
    val album: List<LastFmAlbumSummary>?
)
data class LastFmAlbumSummary(
    val name: String?,
    val artist: String?,
    val image: List<LastFmImage>?
)

data class LastFmAlbumInfoResponse(
    val album: LastFmAlbumDetail?
)
data class LastFmAlbumDetail(
    val name: String?,
    val artist: String?,
    val image: List<LastFmImage>?,
    val tracks: LastFmTracks?,
    val wiki: LastFmWiki?
)
data class LastFmTracks(
    @SerializedName("track")
    val track: List<LastFmTrack>?
)
data class LastFmTrack(
    val name: String?,
    @SerializedName("duration")
    val duration: String?
)
data class LastFmWiki(
    val published: String?
)
data class LastFmImage(
    @SerializedName("#text")
    val url: String?,
    val size: String?
)