package com.example.rateme.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.rateme.data.AchievementManager
import com.example.rateme.data.AlbumDao
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.AlbumWithAvgRating
import com.example.rateme.data.AppDatabase
import com.example.rateme.data.model.Achievement
import com.example.rateme.data.model.Album
import com.example.rateme.data.model.Artist
import com.example.rateme.data.model.Song
import com.example.rateme.data.network.ApiClient
import com.example.rateme.data.network.LastFmAlbumSummary
import com.example.rateme.data.ApiKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: AlbumDao = AppDatabase.getDatabase(application).albumDao()

    val allAlbums: Flow<List<AlbumWithArtistAndSongs>> = dao.getAllAlbumsWithSongs()
    val albumsByRating: Flow<List<AlbumWithAvgRating>> = dao.getAlbumsByRating()
    val ratedAlbums: Flow<List<AlbumWithArtistAndSongs>> = dao.getRatedAlbums()

    private val _recommendations = MutableStateFlow<List<LastFmAlbumSummary>>(emptyList())
    val recommendations: StateFlow<List<LastFmAlbumSummary>> = _recommendations.asStateFlow()

    private val _newAchievement = MutableLiveData<Achievement?>(null)
    val newAchievement: LiveData<Achievement?> = _newAchievement

    fun loadRecommendations() = viewModelScope.launch {
        try {
            val albums = dao.getAllAlbumsWithSongs().first()
            if (albums.isEmpty()) return@launch

            // Find top rated artists
            val topArtists = albums
                .filter { it.songs.any { s -> s.rating != null } }
                .groupBy { it.artist.name }
                .mapValues { entry -> 
                    entry.value.flatMap { it.songs }.mapNotNull { it.rating }.average()
                }
                .filterValues { it >= 7.0 }
                .keys.toList()

            if (topArtists.isEmpty()) return@launch

            val randomArtist = topArtists.random()
            val lastFmApi = ApiClient.lastFmApi

            // Get similar artists
            val similarResponse = lastFmApi.getSimilarArtists(randomArtist, ApiKey.LAST_FM_API_KEY, limit = 5)
            val similarArtists = similarResponse.similarartists?.artist?.mapNotNull { it.name } ?: emptyList()

            if (similarArtists.isEmpty()) return@launch

            val recommendedAlbums = mutableListOf<LastFmAlbumSummary>()
            similarArtists.shuffled().take(3).forEach { artist ->
                val topAlbumsResponse = lastFmApi.getTopAlbums(artist, ApiKey.LAST_FM_API_KEY, limit = 3)
                topAlbumsResponse.topalbums?.album?.let { recommendedAlbums.addAll(it) }
            }

            // Filter out already added albums
            val existingTitles = albums.map { it.album.title.lowercase() }.toSet()
            _recommendations.value = recommendedAlbums
                .filter { it.name != null && it.name.lowercase() !in existingTitles }
                .distinctBy { it.name?.lowercase() }
                .take(10)

        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to load recommendations: ${e.message}")
        }
    }

    fun clearAchievement() { _newAchievement.value = null }

    fun addAlbumWithSongs(
        artistName: String, albumTitle: String, songTitles: List<String>,
        coverUrl: String? = null, previewUrls: Map<String, String> = emptyMap(),
        year: String? = null, onDuplicate: () -> Unit = {}
    ) = viewModelScope.launch {
        val trimmedArtist = artistName.trim()
        val trimmedAlbum = albumTitle.trim()
        val existingArtist = dao.getArtistByName(trimmedArtist)
        val artistId = existingArtist?.id ?: dao.insertArtist(Artist(name = trimmedArtist))
        if (dao.albumExists(trimmedAlbum, artistId) > 0) { onDuplicate(); return@launch }
        val newAlbumId = dao.insertAlbum(Album(title = trimmedAlbum, artistId = artistId, coverUrl = coverUrl, year = year))
        songTitles.forEachIndexed { index, title ->
            if (title.isNotBlank()) dao.insertSong(Song(title = title.trim(), albumId = newAlbumId, trackNumber = index + 1, previewUrl = previewUrls[title]))
        }
    }

    fun updateRating(songId: Long, rating: Int, isDarkTheme: Boolean) = viewModelScope.launch {
        dao.updateRating(songId, rating)

        val albumsList = withContext(Dispatchers.IO) {
            dao.getAllAlbumsWithSongs().first()
        }

        val totalRated = albumsList.sumOf { it.songs.count { s -> s.rating != null } }
        val totalAlbums = albumsList.size
        val uniqueArtists = albumsList.map { it.artist.name }.distinct().size
        val perfect10 = albumsList.flatMap { it.songs }.count { it.rating == 10 }
        val fullAlbums = albumsList.count { it.songs.all { s -> s.rating != null } }

        AchievementManager.checkAndUpdate(
            context = getApplication<Application>().applicationContext,
            totalRated = totalRated,
            totalAlbums = totalAlbums,
            uniqueArtists = uniqueArtists,
            perfect10 = perfect10,
            fullAlbums = fullAlbums,
            isDarkTheme = isDarkTheme,
            albumsList = albumsList
        ) { ach -> _newAchievement.postValue(ach) }
    }

    fun incrementAchievement(achId: String) {
        AchievementManager.increment(getApplication<Application>().applicationContext, achId) { ach ->
            _newAchievement.postValue(ach)
        }
    }

    fun checkStreakReset() {
        AchievementManager.checkAndUpdate(getApplication<Application>().applicationContext, totalRated = -2) { ach ->
            _newAchievement.postValue(ach)
        }
    }

    fun deleteAlbum(album: Album) = viewModelScope.launch { dao.deleteAlbum(album) }
}