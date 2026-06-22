package com.example.rateme.viewmodel

import android.app.Application
import android.content.Context
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: AlbumDao = AppDatabase.getDatabase(application).albumDao()

    val allAlbums: Flow<List<AlbumWithArtistAndSongs>> = dao.getAllAlbumsWithSongs()
    val albumsByRating: Flow<List<AlbumWithAvgRating>> = dao.getAlbumsByRating()
    val ratedAlbums: Flow<List<AlbumWithArtistAndSongs>> = dao.getRatedAlbums()

    private val _newAchievement = MutableLiveData<Achievement?>(null)
    val newAchievement: LiveData<Achievement?> = _newAchievement

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

    fun updateRating(songId: Long, rating: Int, context: Context, isDarkTheme: Boolean) = viewModelScope.launch {
        android.util.Log.d("RATEME", "updateRating called: songId=$songId, rating=$rating")
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
            context = context,
            totalRated = totalRated,
            totalAlbums = totalAlbums,
            uniqueArtists = uniqueArtists,
            perfect10 = perfect10,
            fullAlbums = fullAlbums,
            isDarkTheme = isDarkTheme,
            albumsList = albumsList
        ) { ach -> _newAchievement.postValue(ach) }
    }

    fun incrementAchievement(context: Context, achId: String) {
        AchievementManager.increment(context, achId) { ach ->
            _newAchievement.postValue(ach)
        }
    }

    fun deleteAlbum(album: Album) = viewModelScope.launch { dao.deleteAlbum(album) }
}