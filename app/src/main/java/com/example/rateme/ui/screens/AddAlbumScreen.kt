package com.example.rateme.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlbumScreen(
    onSave: (String, String, List<String>, String?) -> Unit,
    onBack: () -> Unit,
    onSearchClick: () -> Unit = {}
) {
    var artistName by remember { mutableStateOf("") }
    var albumTitle by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf("") }
    var songTitles by remember { mutableStateOf(mutableListOf("", "", "")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить альбом") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Назад") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                Button(
                    onClick = onSearchClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔍 Найти в Last.fm")
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = artistName,
                    onValueChange = { artistName = it },
                    label = { Text("Исполнитель") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = albumTitle,
                    onValueChange = { albumTitle = it },
                    label = { Text("Название альбома") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = coverUrl,
                    onValueChange = { coverUrl = it },
                    label = { Text("Ссылка на обложку") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Песни:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            itemsIndexed(songTitles) { index, title ->
                OutlinedTextField(
                    value = title,
                    onValueChange = { newValue ->
                        songTitles = songTitles.toMutableList().also { it[index] = newValue }
                    },
                    label = { Text("Песня ${index + 1}") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = {
                        songTitles = (songTitles + "").toMutableList()
                    }) {
                        Text("+ Песня")
                    }
                    Button(onClick = {
                        if (artistName.isNotBlank() && albumTitle.isNotBlank()) {
                            onSave(
                                artistName,
                                albumTitle,
                                songTitles.filter { it.isNotBlank() },
                                coverUrl.ifBlank { null }
                            )
                        }
                    }) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}