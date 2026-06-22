package com.example.rateme.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rateme.R
import com.example.rateme.data.AlbumWithArtistAndSongs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    albums: List<AlbumWithArtistAndSongs>,
    onBack: () -> Unit
) {
    val totalAlbums = albums.size
    val totalSongs = albums.sumOf { it.songs.size }
    val totalRated = albums.sumOf { it.songs.count { s -> s.rating != null } }
    val averageRating = albums.flatMap { it.songs }
        .mapNotNull { it.rating }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.let { String.format("%.1f", it) }
        ?: "—"
    val topArtist = albums
        .flatMap { album -> album.songs.map { album.artist.name to it.rating } }
        .filter { it.second != null }
        .groupBy { it.first }
        .mapValues { entry -> entry.value.mapNotNull { it.second }.average() }
        .maxByOrNull { it.value }
        ?.key ?: "—"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats), style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { StatCard(stringResource(R.string.total_albums), "$totalAlbums") }
            item { StatCard(stringResource(R.string.total_songs), "$totalSongs") }
            item { StatCard(stringResource(R.string.rated_songs), stringResource(R.string.rated_count, totalRated, totalSongs)) }
            item { StatCard(stringResource(R.string.avg_rating), "$averageRating / 10") }
            item { StatCard(stringResource(R.string.top_artist), topArtist) }
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}