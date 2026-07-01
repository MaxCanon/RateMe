package com.example.rateme.ui

import com.example.rateme.data.model.Song
import platform.AVFoundation.*
import platform.Foundation.NSURL
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IosAudioPlayer : AudioPlayer {
    private var avPlayer: AVPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPreparing = MutableStateFlow(false)
    override val isPreparing: StateFlow<Boolean> = _isPreparing.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(30000L) // Default for previews
    override val duration: StateFlow<Long> = _duration.asStateFlow()

    override var onPlaybackFinished: () -> Unit = {}

    override fun play(song: Song) {
        val url = NSURL.URLWithString(song.previewUrl ?: "") ?: return
        val playerItem = AVPlayerItem(url)
        avPlayer = AVPlayer(playerItem)
        
        _isPreparing.value = true
        avPlayer?.play()
        _isPlaying.value = true
        _isPreparing.value = false // Simplified for iOS
        startProgressPolling()
    }

    override fun pause() {
        avPlayer?.pause()
        _isPlaying.value = false
        stopProgressPolling()
    }

    override fun stop() {
        avPlayer?.pause()
        avPlayer = null
        _isPlaying.value = false
        _isPreparing.value = false
        _currentPosition.value = 0L
        stopProgressPolling()
    }

    override fun seekTo(position: Long) {
        // iOS seek implementation
    }

    override fun release() {
        stop()
        scope.cancel()
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                // Implementation to get current time from avPlayer
                delay(500)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
    }
}

actual fun createAudioPlayer(): AudioPlayer = IosAudioPlayer()
