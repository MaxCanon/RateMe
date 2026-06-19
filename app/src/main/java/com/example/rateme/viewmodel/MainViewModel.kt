package com.example.rateme.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.rateme.data.AlbumDao
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.AppDatabase
import com.example.rateme.data.model.Album
import com.example.rateme.data.model.Artist
import com.example.rateme.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: AlbumDao = AppDatabase.getDatabase(application).albumDao()

    val allAlbums: Flow<List<AlbumWithArtistAndSongs>> = dao.getAllAlbumsWithSongs()

    fun addAlbumWithSongs(
        artistName: String,
        albumTitle: String,
        songTitles: List<String>,
        coverUrl: String? = null
    ) = viewModelScope.launch {
        val artistId = dao.insertArtist(Artist(name = artistName.trim()))
        val albumId = dao.insertAlbum(
            Album(title = albumTitle.trim(), artistId = artistId, coverUrl = coverUrl)
        )
        songTitles.forEachIndexed { index, title ->
            if (title.isNotBlank()) {
                dao.insertSong(
                    Song(title = title.trim(), albumId = albumId, trackNumber = index + 1)
                )
            }
        }
    }

    fun updateRating(songId: Long, rating: Int) = viewModelScope.launch {
        dao.updateRating(songId, rating)
    }

    fun deleteAlbum(album: Album) = viewModelScope.launch {
        dao.deleteAlbum(album)
    }
}