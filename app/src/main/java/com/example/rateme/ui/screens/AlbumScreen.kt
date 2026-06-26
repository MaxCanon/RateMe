package com.example.rateme.ui.screens

import android.content.Intent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    readOnly: Boolean = false,
    currentlyPlayingId: Long? = null,
    isPlaying: Boolean = false,
    onTogglePlayback: (Song, String, Color?) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    if (albumWithSongs == null) {
        Box(modifier = Modifier.fillMaxSize()) { CircularProgressIndicator() }
        return
    }

    var seedColor by remember { mutableStateOf<Color?>(null) }
    var showFullScreenCover by remember { mutableStateOf(false) }
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalAnimatedContentScope.current

    if (showFullScreenCover && !albumWithSongs.album.coverUrl.isNullOrBlank()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullScreenCover = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false // This makes it full screen
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { showFullScreenCover = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = albumWithSongs.album.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    RateMeTheme(seedColor = seedColor) {
        val themeColor = MaterialTheme.colorScheme.primary

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
            
            // Gradient overlay
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
                                .height(56.dp)
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
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
                            
                            // Use Button instead of TextButton for better contrast against backgrounds
                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = themeColor,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp),
                                shape = MaterialTheme.shapes.medium
                            ) { 
                                Text(
                                    text = stringResource(R.string.done),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                ) 
                            }
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
                                                .clickable { showFullScreenCover = true }
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
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Album, contentDescription = null, modifier = Modifier.size(100.dp), tint = themeColor)
                                        }
                                    }
                                }
                            } else {
                                AsyncImage(model = albumWithSongs.album.coverUrl, contentDescription = null, modifier = Modifier.size(200.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(albumWithSongs.artist.name, style = MaterialTheme.typography.titleMedium, color = themeColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.tracks_count, albumWithSongs.songs.size),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (!albumWithSongs.album.year.isNullOrBlank()) {
                                    Text(" • ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(albumWithSongs.album.year, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (readOnly) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(R.string.readonly_warning), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    itemsIndexed(albumWithSongs.songs) { index, song ->
                        SongRow(
                            song = song, 
                            trackNumber = index + 1, 
                            onRatingChanged = { rating: Int -> onRatingChanged(song.id, rating) }, 
                            readOnly = readOnly,
                            isPlaying = currentlyPlayingId == song.id && isPlaying,
                            onTogglePlay = { 
                                onTrackListen()
                                onTogglePlayback(song, albumWithSongs.artist.name, seedColor)
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
    var sliderValue by remember { mutableFloatStateOf(song.rating?.toFloat() ?: 0f) }
    val roundedValue = sliderValue.roundToInt()

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$trackNumber. ${song.title}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = roundedValue.toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onRatingChanged(roundedValue) },
                valueRange = 0f..10f,
                steps = 9,
                enabled = !readOnly,
                colors = SliderDefaults.colors(
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )

            // Perfect Alignment Scale: 0 • 2 • 4 • 6 • 8 • 10
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..10).forEach { i ->
                    val isSelected = i == roundedValue
                    if (i % 2 == 0) {
                        Text(
                            text = i.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.width(18.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 6.dp else 3.dp)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            if (!song.previewUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onTogglePlay() },
                    modifier = if (isPlaying) Modifier.size(40.dp) else Modifier.fillMaxWidth().height(40.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = if (isPlaying) PaddingValues(0.dp) else ButtonDefaults.ContentPadding,
                    enabled = !readOnly || isPlaying
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, 
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    if (!isPlaying) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Слушать отрывок")
                    }
                }
            }
        }
    }
}
