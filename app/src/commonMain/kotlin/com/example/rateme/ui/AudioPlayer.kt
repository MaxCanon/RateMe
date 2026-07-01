package com.example.rateme.ui

import com.example.rateme.data.model.Song
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    fun play(song: Song)
    fun pause()
    fun stop()
    fun seekTo(position: Long)
    fun release()
    
    val isPlaying: StateFlow<Boolean>
    val isPreparing: StateFlow<Boolean>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    var onPlaybackFinished: () -> Unit
}

expect fun createAudioPlayer(): AudioPlayer
