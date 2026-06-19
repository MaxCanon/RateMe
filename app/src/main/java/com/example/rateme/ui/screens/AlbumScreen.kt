package com.example.rateme.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.model.Song

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
                title = { Text(albumWithSongs.album.title) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Назад") }
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
        }
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$trackNumber. ${song.title}")

            Spacer(modifier = Modifier.height(12.dp))

            // Ползунок
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onRatingChanged(sliderValue.toInt()) },
                valueRange = 0f..10f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )

            // Подписи цифр под ползунком
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..10) {
                    Text(
                        text = "$i",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Текущая оценка по центру
            Text(
                text = "Оценка: ${sliderValue.toInt()}/10",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}