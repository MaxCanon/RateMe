package com.example.rateme.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rateme.data.network.LastFmAlbumSummary
import com.example.rateme.data.network.LastFmApi
import com.example.rateme.data.ApiKey
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onAlbumSelected: (artist: String, album: String, coverUrl: String?, tracks: List<String>) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<LastFmAlbumSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val api = remember {
        Retrofit.Builder()
            .baseUrl("https://ws.audioscrobbler.com/2.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LastFmApi::class.java)
    }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поиск альбома") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Назад") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Название альбома или исполнитель") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val response = api.searchAlbum(query, ApiKey.LAST_FM_API_KEY)
                            results = response.results?.albumMatches?.album ?: emptyList()
                            if (results.isEmpty()) errorMessage = "Ничего не найдено"
                        } catch (e: Exception) {
                            errorMessage = "Ошибка: ${e.message}"
                            results = emptyList()
                        }
                        isLoading = false
                    }
                },
                enabled = query.isNotBlank() && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Искать")
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(results) { album ->
                    val imageUrl = album.image?.lastOrNull()?.url
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val info = api.getAlbumInfo(
                                            artist = album.artist ?: "",
                                            album = album.name ?: "",
                                            apiKey = ApiKey.LAST_FM_API_KEY
                                        )
                                        val tracks = info.album?.tracks?.track?.mapNotNull { it.name } ?: emptyList()
                                        val cover = info.album?.image?.lastOrNull()?.url
                                        onAlbumSelected(
                                            album.artist ?: "",
                                            album.name ?: "",
                                            cover,
                                            tracks
                                        )
                                    } catch (e: Exception) {
                                        // Если не получилось загрузить треки — всё равно добавляем
                                        val cover = album.image?.lastOrNull()?.url
                                        onAlbumSelected(
                                            album.artist ?: "",
                                            album.name ?: "",
                                            cover,
                                            emptyList()
                                        )
                                    }
                                    isLoading = false
                                }
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            if (!imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column {
                                Text(album.name ?: "?", style = MaterialTheme.typography.titleSmall)
                                Text(album.artist ?: "?", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}