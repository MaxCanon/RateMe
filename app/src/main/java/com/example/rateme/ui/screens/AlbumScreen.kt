package com.example.rateme.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rateme.LocalAnimatedContentScope
import com.example.rateme.LocalSharedTransitionScope
import com.example.rateme.R
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.model.Song
import com.example.rateme.ui.theme.RateMeTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumScreen(
    albumWithSongs: AlbumWithArtistAndSongs?,
    onBack: () -> Unit,
    onRatingChanged: (Long, Int) -> Unit,
    onShareClick: (Intent) -> Unit,
    onTrackListen: () -> Unit,
    readOnly: Boolean = false
) {
    val context = LocalContext.current
    if (albumWithSongs == null) {
        Box(modifier = Modifier.fillMaxSize()) { CircularProgressIndicator() }
        return
    }

    var playingSongId by remember { mutableStateOf<Long?>(null) }
    var seedColor by remember { mutableStateOf<Color?>(null) }
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalAnimatedContentScope.current

    RateMeTheme(seedColor = seedColor) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Blurred Background
            if (!albumWithSongs.album.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = albumWithSongs.album.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(40.dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.4f
                )
            }
            
            // Gradient overlay to ensure readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth().statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                            Text(
                                text = albumWithSongs.album.title,
                                modifier = Modifier.weight(1f).basicMarquee(),
                                maxLines = 1,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = {
                                val shareText = buildString {
                                    appendLine("🎵 ${albumWithSongs.album.title} — ${albumWithSongs.artist.name}")
                                    albumWithSongs.songs.forEachIndexed { i, s ->
                                        val r = s.rating?.toString() ?: "—"
                                        appendLine("${i + 1}. ${s.title} — $r/10")
                                    }
                                    val avg = albumWithSongs.songs.mapNotNull { it.rating }.average().let {
                                        if (it.isNaN()) "—" else String.format(java.util.Locale.getDefault(), "%.1f", it)
                                    }
                                    appendLine()
                                    appendLine(context.getString(R.string.avg_rating_value, avg))
                                }
                                onShareClick(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) })
                            }) { Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share)) }
                            TextButton(onClick = onBack) { Text(stringResource(R.string.done)) }
                        }
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (sharedTransitionScope != null && animatedContentScope != null) {
                                with(sharedTransitionScope) {
                                    if (!albumWithSongs.album.coverUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                .data(albumWithSongs.album.coverUrl)
                                                .allowHardware(false)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(200.dp)
                                                .sharedElement(
                                                    rememberSharedContentState(key = "album-cover-${albumWithSongs.album.id}"),
                                                    animatedVisibilityScope = animatedContentScope
                                                ),
                                            onSuccess = { result ->
                                                val bitmap = (result.result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                                bitmap?.let {
                                                    val palette = androidx.palette.graphics.Palette.from(it).generate()
                                                    val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
                                                    swatch?.rgb?.let { rgb ->
                                                        seedColor = Color(rgb)
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(200.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .sharedElement(
                                                    rememberSharedContentState(key = "album-cover-${albumWithSongs.album.id}"),
                                                    animatedVisibilityScope = animatedContentScope
                                                ),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Album, contentDescription = null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            } else {
                                AsyncImage(model = albumWithSongs.album.coverUrl, contentDescription = null, modifier = Modifier.size(200.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(albumWithSongs.artist.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            if (readOnly) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(R.string.readonly_warning), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.tracks_count, albumWithSongs.songs.size), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    itemsIndexed(albumWithSongs.songs) { index, song ->
                        SongRow(
                            song = song, 
                            trackNumber = index + 1, 
                            onRatingChanged = { rating: Int -> onRatingChanged(song.id, rating) }, 
                            readOnly = readOnly,
                            isPlaying = playingSongId == song.id,
                            onTogglePlay = { 
                                if (playingSongId == song.id) playingSongId = null
                                else {
                                    onTrackListen()
                                    playingSongId = song.id
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongRow(
    song: Song, 
    trackNumber: Int, 
    onRatingChanged: (Int) -> Unit, 
    readOnly: Boolean = false,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var sliderValue by remember { mutableFloatStateOf(song.rating?.toFloat() ?: 0f) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    fun releasePlayer() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Exception) {}
            it.release()
        }
        mediaPlayer = null
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            releasePlayer()
        } else if (mediaPlayer == null) {
            try {
                var url = song.previewUrl ?: ""
                if (url.startsWith("http://")) url = url.replace("http://", "https://")
                
                val mp = android.media.MediaPlayer().apply {
                    setDataSource(url)
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setOnPreparedListener { it.start() }
                    setOnCompletionListener { onTogglePlay() }
                    setOnErrorListener { _, _, _ -> onTogglePlay(); true }
                    prepareAsync()
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                android.util.Log.e("MediaPlayer", "Error: ${e.message}")
                onTogglePlay()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { releasePlayer() }
    }

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
                Button(
                    onClick = { onTogglePlay() },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    enabled = !readOnly || isPlaying
                ) {
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
            Text(stringResource(R.string.rating_value, sliderValue.roundToInt()), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
        }
    }
}
