package com.example.rateme.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rateme.R
import com.example.rateme.data.ApiKey
import com.example.rateme.data.SearchHistory
import com.example.rateme.data.network.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlbumScreen(
    onAlbumSelected: (artist: String, album: String, coverUrl: String?, tracks: List<String>, previews: Map<String, String>, year: String?) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<LastFmAlbumSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var history by remember { mutableStateOf(SearchHistory.getHistory(context)) }

    val lastFmApi = ApiClient.lastFmApi
    val iTunesApi = ApiClient.iTunesApi
    val scope = rememberCoroutineScope()

    fun doSearch(q: String) {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(400)
            isLoading = true
            hasSearched = true
            errorMessage = null
            try {
                val response = lastFmApi.searchAlbum(q, ApiKey.LAST_FM_API_KEY)
                results = response.results?.albumMatches?.album ?: emptyList()
                if (results.isEmpty()) errorMessage = "Ничего не найдено"
                else {
                    SearchHistory.addToHistory(context, q)
                    history = SearchHistory.getHistory(context)
                }
            } catch (e: Exception) {
                errorMessage = "Ошибка сети"
                results = emptyList()
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.length >= 2) doSearch(it)
                    else {
                        results = emptyList()
                        hasSearched = false
                    }
                },
                label = { Text(stringResource(R.string.search_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (!hasSearched && query.isBlank() && history.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.history) + ":", style = MaterialTheme.typography.labelMedium)
                history.take(5).forEach { item ->
                    TextButton(onClick = {
                        query = item
                        doSearch(item)
                    }) {
                        Text(item, style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = {
                    SearchHistory.clearHistory(context)
                    history = emptyList()
                }) {
                    Text(stringResource(R.string.clear), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (errorMessage != null && results.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { doSearch(query) }) { Text(stringResource(R.string.retry)) }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (results.isEmpty() && hasSearched && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ничего не найдено. Попробуйте другой запрос.")
                }
            } else {
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
                                            val info = lastFmApi.getAlbumInfo(
                                                artist = album.artist ?: "",
                                                album = album.name ?: "",
                                                apiKey = ApiKey.LAST_FM_API_KEY
                                            )
                                            val tracks = info.album?.tracks?.track?.mapNotNull { it.name } ?: emptyList()
                                            val cover = info.album?.image?.lastOrNull()?.url
                                            val year = info.album?.wiki?.published?.let {
                                                Regex("\\d{4}").find(it)?.value
                                            }

                                            val previews = mutableMapOf<String, String>()
                                            tracks.forEach { track ->
                                                try {
                                                    val searchTerm = "${album.artist} $track"
                                                    val response = iTunesApi.searchTrack(searchTerm)
                                                    val previewUrl = response.results.firstOrNull()?.previewUrl
                                                    if (previewUrl != null) previews[track] = previewUrl
                                                } catch (_: Exception) {}
                                            }

                                            onAlbumSelected(
                                                album.artist ?: "",
                                                album.name ?: "",
                                                cover,
                                                tracks,
                                                previews,
                                                year
                                            )
                                        } catch (_: Exception) {
                                            onAlbumSelected(
                                                album.artist ?: "",
                                                album.name ?: "",
                                                album.image?.lastOrNull()?.url,
                                                emptyList(),
                                                emptyMap(),
                                                null
                                            )
                                        }
                                        isLoading = false
                                    }
                                },
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!imageUrl.isNullOrBlank()) {
                                    AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.size(56.dp))
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
}