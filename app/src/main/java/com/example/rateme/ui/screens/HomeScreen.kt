package com.example.rateme.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.model.Album

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
    onHomeClick: () -> Unit = {},
    showActions: Boolean = true
) {
    var albumToDelete by remember { mutableStateOf<Album?>(null) }

    if (albumToDelete != null && showActions) {
        AlertDialog(
            onDismissRequest = { albumToDelete = null },
            title = { Text("Удалить альбом?") },
            text = { Text("Удалить «${albumToDelete!!.title}»?\nВсе оценки будут потеряны.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick(albumToDelete!!)
                    albumToDelete = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { albumToDelete = null }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextButton(onClick = onHomeClick) {
                        Text(
                            "RateMe 🎵",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (showActions) {
                        IconButton(onClick = onRatedClick) { Text("📁") }
                        IconButton(onClick = onRatingClick) { Text("🏆") }
                        IconButton(onClick = onThemeToggle) { Text(if (isDarkTheme) "☀️" else "🌙") }
                    }
                }
            )
        },
        floatingActionButton = {
            if (showActions) {
                FloatingActionButton(onClick = onAddClick) { Text("+") }
            }
        }
    ) { padding ->
        if (albums.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет альбомов. Нажми + чтобы добавить!")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(albums) { item ->
                    val ratedCount = item.songs.count { it.rating != null }
                    val totalCount = item.songs.size
                    val avgRating = item.songs
                        .mapNotNull { it.rating }
                        .takeIf { it.isNotEmpty() }
                        ?.average()
                        ?.let { String.format("%.1f", it) }
                        ?: "—"

                    val statusText = if (ratedCount == totalCount && totalCount > 0) {
                        "Оценены все $totalCount треков"
                    } else {
                        "Оценено $ratedCount из $totalCount"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .combinedClickable(
                                onClick = { onAlbumClick(item.album.id) },
                                onLongClick = { if (showActions) albumToDelete = item.album }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Обложка или иконка
                            if (!item.album.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.album.coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(56.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎵", style = MaterialTheme.typography.headlineMedium)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Информация об альбоме
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.album.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    item.artist.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    "⭐ $avgRating/10",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    statusText,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // Кнопки действий
                            if (showActions) {
                                IconButton(onClick = { onAlbumClick(item.album.id) }) {
                                    Text("✏️")
                                }
                                IconButton(onClick = { albumToDelete = item.album }) {
                                    Text("❌")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}