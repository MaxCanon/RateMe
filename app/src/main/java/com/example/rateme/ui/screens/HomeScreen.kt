package com.example.rateme.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.model.Album

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    albums: List<AlbumWithArtistAndSongs>,
    onAlbumClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (Album) -> Unit
) {
    var albumToDelete by remember { mutableStateOf<Album?>(null) }

    // Диалог подтверждения удаления
    if (albumToDelete != null) {
        AlertDialog(
            onDismissRequest = { albumToDelete = null },
            title = { Text("Удалить альбом?") },
            text = { Text("Вы уверены, что хотите удалить «${albumToDelete!!.title}»?\nВсе оценки будут потеряны.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick(albumToDelete!!)
                    albumToDelete = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { albumToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("RateMe 🎵") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Text("+")
            }
        }
    ) { padding ->
        if (albums.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Нет альбомов. Нажми + чтобы добавить!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(albums) { item ->
                    val avgRating = item.songs
                        .mapNotNull { it.rating }
                        .takeIf { it.isNotEmpty() }
                        ?.average()
                        ?.let { String.format("%.1f", it) }
                        ?: "—"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .combinedClickable(
                                onClick = { onAlbumClick(item.album.id) },
                                onLongClick = { albumToDelete = item.album }
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.artist.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.album.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Песен: ${item.songs.size}  |  Рейтинг: $avgRating / 10",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}