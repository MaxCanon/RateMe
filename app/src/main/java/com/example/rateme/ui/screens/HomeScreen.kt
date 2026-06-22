package com.example.rateme.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rateme.R
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.model.Album
import com.example.rateme.ui.components.ShimmerLoadingList

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    albums: List<AlbumWithArtistAndSongs>,
    onAlbumClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (Album) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onRatedClick: () -> Unit,
    onRatingClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    showActions: Boolean = true,
    showTopBar: Boolean = true,
    showAddButton: Boolean = true,
    title: String = "RateMe",
    isLoading: Boolean = false
) {
    var albumToDelete by remember { mutableStateOf<Album?>(null) }
    var animationsStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { animationsStarted = true }

    if (albumToDelete != null && showActions) {
        AlertDialog(
            onDismissRequest = { albumToDelete = null },
            title = { Text(stringResource(R.string.delete_album_title)) },
            text = { Text(stringResource(R.string.delete_album_text)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick(albumToDelete!!)
                    albumToDelete = null
                }) {
                    Text(stringResource(R.string.delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { albumToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(title, style = MaterialTheme.typography.titleMedium)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        if (showActions) {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                ShimmerLoadingList()
            }
        } else if (albums.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_albums))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                itemsIndexed(albums) { index, item ->
                    val delay = index * 80
                    AnimatedVisibility(
                        visible = animationsStarted,
                        enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(durationMillis = 400, delayMillis = delay, easing = FastOutSlowInEasing))
                                + fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = delay))
                    ) {
                        AlbumCard(item = item, showActions = showActions, onAlbumClick = onAlbumClick, onDeleteClick = { albumToDelete = item.album }, onEditClick = { onAlbumClick(item.album.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumCard(
    item: AlbumWithArtistAndSongs,
    showActions: Boolean,
    onAlbumClick: (Long) -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val ratedCount = item.songs.count { it.rating != null }
    val totalCount = item.songs.size
    val avgRating = item.songs.mapNotNull { it.rating }.takeIf { it.isNotEmpty() }?.average()?.let { String.format("%.1f", it) } ?: "—"

    val statusText = if (ratedCount == totalCount && totalCount > 0) {
        stringResource(R.string.rated_all, totalCount)
    } else {
        stringResource(R.string.rated_count, ratedCount, totalCount)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .combinedClickable(onClick = { onAlbumClick(item.album.id) }, onLongClick = { if (showActions) onDeleteClick() }),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!item.album.coverUrl.isNullOrBlank()) {
                AsyncImage(model = item.album.coverUrl, contentDescription = null, modifier = Modifier.size(56.dp))
            } else {
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Album, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.album.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(item.artist.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text("⭐ $avgRating/10", style = MaterialTheme.typography.labelSmall)
                Text(statusText, style = MaterialTheme.typography.labelSmall)
            }
            if (showActions) {
                IconButton(onClick = onEditClick) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit), tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDeleteClick) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_confirm), tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}