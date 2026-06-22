package com.example.rateme.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.rateme.R
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.model.Song
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    albumWithSongs: AlbumWithArtistAndSongs?,
    onBack: () -> Unit,
    onRatingChanged: (Long, Int) -> Unit,
    onShareClick: (Intent) -> Unit,
    readOnly: Boolean = false
) {
    if (albumWithSongs == null) {
        Box(modifier = Modifier.fillMaxSize()) { CircularProgressIndicator() }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { MarqueeText(text = albumWithSongs.album.title, modifier = Modifier.fillMaxWidth()) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back), style = MaterialTheme.typography.headlineSmall) } },
                actions = {
                    IconButton(onClick = {
                        val shareText = buildString {
                            appendLine("🎵 ${albumWithSongs.album.title} — ${albumWithSongs.artist.name}")
                            albumWithSongs.songs.forEachIndexed { i, s ->
                                val r = s.rating?.toString() ?: "—"
                                appendLine("${i + 1}. ${s.title} — $r/10")
                            }
                            val avg = albumWithSongs.songs.mapNotNull { it.rating }.average().let {
                                if (it.isNaN()) "—" else String.format("%.1f", it)
                            }
                            appendLine()
                            appendLine("Средний балл: $avg/10")
                        }
                        onShareClick(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) })
                    }) { Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share)) }
                    TextButton(onClick = onBack) { Text(stringResource(R.string.done)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(albumWithSongs.artist.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    if (readOnly) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.readonly_warning), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Треков: ${albumWithSongs.songs.size}", style = MaterialTheme.typography.bodySmall)
                }
            }
            itemsIndexed(albumWithSongs.songs) { index, song ->
                SongRow(song = song, trackNumber = index + 1, onRatingChanged = { rating -> onRatingChanged(song.id, rating) }, readOnly = readOnly)
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun MarqueeText(text: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = -200f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 5000, easing = LinearEasing), repeatMode = RepeatMode.Restart))
    Box(modifier = modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(0.dp))) {
        Text(text = text, maxLines = 1, overflow = TextOverflow.Visible, softWrap = false, modifier = Modifier.offset { IntOffset(offset.toInt(), 0) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongRow(song: Song, trackNumber: Int, onRatingChanged: (Int) -> Unit, readOnly: Boolean = false) {
    var sliderValue by remember { mutableFloatStateOf(song.rating?.toFloat() ?: 0f) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    DisposableEffect(Unit) { onDispose { mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null } }

    fun stopPreview() { mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null; isPlaying = false }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("$trackNumber. ${song.title}", maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!song.previewUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = {
                    if (isPlaying) stopPreview()
                    else try { stopPreview(); mediaPlayer = android.media.MediaPlayer().apply { setDataSource(song.previewUrl); setOnPreparedListener { start() }; setOnCompletionListener { stopPreview() }; prepareAsync() }; isPlaying = true } catch (_: Exception) {}
                }, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp), enabled = !readOnly || isPlaying) {
                    Text(if (isPlaying) stringResource(R.string.stop) else stringResource(R.string.listen_30s), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(value = sliderValue, onValueChange = { sliderValue = it }, onValueChangeFinished = { onRatingChanged(sliderValue.roundToInt()) }, valueRange = 0f..10f, steps = 10, modifier = Modifier.fillMaxWidth(), enabled = !readOnly)
            if (readOnly) Text(stringResource(R.string.error_cant_rate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 0..10) Text("$i", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Оценка: ${sliderValue.roundToInt()}/10", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
        }
    }
}