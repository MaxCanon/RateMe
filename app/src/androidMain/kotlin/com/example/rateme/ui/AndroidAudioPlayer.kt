package com.example.rateme.ui

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import com.example.rateme.RateMeApp
import com.example.rateme.data.model.Song
import com.example.rateme.showToast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidAudioPlayer : AudioPlayer {
    private val exoPlayer = ExoPlayer.Builder(RateMeApp.context).build()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var currentSongId: Long? = null

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPreparing = MutableStateFlow(false)
    override val isPreparing: StateFlow<Boolean> = _isPreparing.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    override val duration: StateFlow<Long> = _duration.asStateFlow()

    override var onPlaybackFinished: () -> Unit = {}

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressPolling() else stopProgressPolling()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isPreparing.value = playbackState == Player.STATE_BUFFERING
                
                when (playbackState) {
                    Player.STATE_READY -> {
                        _duration.value = exoPlayer.duration.coerceAtLeast(0L)
                        _isPreparing.value = false
                    }
                    Player.STATE_ENDED -> {
                        onPlaybackFinished()
                        _isPlaying.value = false
                        _isPreparing.value = false
                    }
                    Player.STATE_IDLE -> {
                        _isPreparing.value = false
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                println("ExoPlayer Error: ${error.errorCodeName} - ${error.message}")
                showToast("Failed to load track")
                _isPreparing.value = false
                _isPlaying.value = false
            }
        })
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                _currentPosition.value = exoPlayer.currentPosition
                delay(500)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
    }

    override fun play(song: Song) {
        val url = song.previewUrl
        if (url.isNullOrBlank()) {
            showToast("No preview available for this track")
            return
        }

        if (currentSongId == song.id && exoPlayer.playbackState != Player.STATE_IDLE) {
            exoPlayer.play()
        } else {
            currentSongId = song.id
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        currentSongId = null
        _isPlaying.value = false
        _isPreparing.value = false
        _currentPosition.value = 0L
        stopProgressPolling()
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        _currentPosition.value = position
    }

    override fun release() {
        stopProgressPolling()
        scope.cancel()
        exoPlayer.release()
    }
}

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()
