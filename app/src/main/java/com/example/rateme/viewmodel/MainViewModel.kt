package com.example.rateme.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

    // Playback State using Media3 ExoPlayer
    private var exoPlayer: ExoPlayer? = null
    
    private val _currentlyPlaying = MutableStateFlow<Song?>(null)
    val currentlyPlaying: StateFlow<Song?> = _currentlyPlaying.asStateFlow()

    private val _playingArtistName = MutableStateFlow<String?>(null)
    val playingArtistName: StateFlow<String?> = _playingArtistName.asStateFlow()

    private val _playingColor = MutableStateFlow<androidx.compose.ui.graphics.Color?>(null)
    val playingColor: StateFlow<androidx.compose.ui.graphics.Color?> = _playingColor.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPreparing = MutableStateFlow(false)
    val isPreparing: StateFlow<Boolean> = _isPreparing.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _playbackDuration = MutableStateFlow(0L)
    val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()

    private var progressJob: kotlinx.coroutines.Job? = null

    init {
        exoPlayer = ExoPlayer.Builder(application).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) startProgressPolling() else stopProgressPolling()
                }

                override fun onPlaybackStateChanged(state: Int) {
                    _isPreparing.value = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_READY) {
                        _playbackDuration.value = duration.coerceAtLeast(0L)
                    }
                    if (state == Player.STATE_ENDED) {
                        stopPlayback()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e("ExoPlayer", "Error: ${error.message}")
                    stopPlayback()
                }
            })
        }
    }

    fun togglePlayback(song: Song, artistName: String? = null, color: androidx.compose.ui.graphics.Color? = null) {
        val player = exoPlayer ?: return
        
        if (_currentlyPlaying.value?.id == song.id) {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        } else {
            _playingArtistName.value = artistName
            _playingColor.value = color
            _currentlyPlaying.value = song
            val mediaItem = MediaItem.fromUri(song.previewUrl ?: "")
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                exoPlayer?.let {
                    _playbackPosition.value = it.currentPosition.coerceAtLeast(0L)
                }
                kotlinx.coroutines.delay(50)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        _playbackPosition.value = position
    }

    fun stopPlayback() {
        stopProgressPolling()
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _currentlyPlaying.value = null
        _playingArtistName.value = null
        _playingColor.value = null
        _isPlaying.value = false
        _isPreparing.value = false
        _playbackPosition.value = 0L
        _playbackDuration.value = 0L
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }

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

    fun refreshAllMetadata() = viewModelScope.launch {
        val albums = dao.getAllAlbumsWithSongs().first()
        albums.forEach { albumWithSongs ->
            try {
                var newYear: String? = null
                
                // Try Deezer first (most accurate)
                val deezerSearch = ApiClient.deezerApi.searchAlbum("${albumWithSongs.artist.name} ${albumWithSongs.album.title}")
                val deezerAlbum = deezerSearch.data?.firstOrNull { 
                    it.title?.contains(albumWithSongs.album.title, ignoreCase = true) == true 
                }
                newYear = deezerAlbum?.release_date?.take(4)

                // Try iTunes fallback
                if (newYear == null) {
                    val itunesResp = ApiClient.iTunesApi.searchTrack("${albumWithSongs.artist.name} ${albumWithSongs.album.title}")
                    newYear = itunesResp.results.firstOrNull()?.releaseDate?.take(4)
                }

                if (newYear != null && newYear != albumWithSongs.album.year) {
                    dao.updateAlbum(albumWithSongs.album.copy(year = newYear))
                }
            } catch (e: Exception) {
                android.util.Log.e("MetadataRefresh", "Failed for ${albumWithSongs.album.title}: ${e.message}")
            }
        }
    }
}
