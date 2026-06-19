package com.example.rateme.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.model.Song
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    albumWithSongs: AlbumWithArtistAndSongs?,
    onBack: () -> Unit,
    onRatingChanged: (Long, Int) -> Unit
) {
    if (albumWithSongs == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    MarqueeText(
                        text = albumWithSongs.album.title,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.headlineSmall)
                    }
                },
                actions = {
                    TextButton(onClick = onBack) { Text("Готово ✓") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            itemsIndexed(albumWithSongs.songs) { index, song ->
                SongRow(
                    song = song,
                    trackNumber = index + 1,
                    onRatingChanged = { rating -> onRatingChanged(song.id, rating) }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(0.dp))) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false,
            modifier = Modifier.offset { IntOffset(offset.toInt(), 0) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongRow(
    song: Song,
    trackNumber: Int,
    onRatingChanged: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(song.rating?.toFloat() ?: 0f) }
    var isPlaying by remember { mutableStateOf(false) }

    // Простой MediaPlayer (ExoPlayer был бы лучше, но сложнее)
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    fun stopPreview() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "$trackNumber. ${song.title}",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Кнопка прослушивания
            if (!song.previewUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (isPlaying) {
                            stopPreview()
                        } else {
                            try {
                                stopPreview()
                                mediaPlayer = android.media.MediaPlayer().apply {
                                    setDataSource(song.previewUrl)
                                    setOnPreparedListener { start() }
                                    setOnCompletionListener { stopPreview() }
                                    prepareAsync()
                                }
                                isPlaying = true
                            } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        if (isPlaying) "⏹ Стоп" else "▶ Прослушать 30с",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onRatingChanged(sliderValue.roundToInt()) },
                valueRange = 0f..10f,
                steps = 10,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0..10) {
                    Text("$i", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Оценка: ${sliderValue.roundToInt()}/10",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}