package com.example.rateme.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rateme.R
import com.example.rateme.data.network.*
import com.example.rateme.data.ApiKey
import com.example.rateme.data.SearchHistory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onAlbumSelected: (artist: String, album: String, coverUrl: String?, tracks: List<String>, previews: Map<String, String>, year: String?) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<LastFmAlbumSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var history by remember { mutableStateOf(SearchHistory.getHistory(context)) }

    val lastFmApi = remember {
        Retrofit.Builder()
            .baseUrl("https://ws.audioscrobbler.com/2.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LastFmApi::class.java)
    }
    val deezerApi = remember {
        Retrofit.Builder()
            .baseUrl("https://api.deezer.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeezerApi::class.java)
    }
    val scope = rememberCoroutineScope()

    fun doSearch(q: String) {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(400)
            isLoading = true
            isSearching = true
            errorMessage = null
            try {
                val response = lastFmApi.searchAlbum(q, ApiKey.LAST_FM_API_KEY)
                results = response.results?.albumMatches?.album ?: emptyList()
                if (results.isEmpty()) errorMessage = context.getString(R.string.nothing_found)
                else {
                    SearchHistory.addToHistory(context, q)
                    history = SearchHistory.getHistory(context)
                }
            } catch (e: UnknownHostException) {
                Log.e("SearchScreen", "Нет интернета", e)
                errorMessage = context.getString(R.string.no_internet)
                results = emptyList()
            } catch (e: SocketTimeoutException) {
                Log.e("SearchScreen", "Таймаут", e)
                errorMessage = context.getString(R.string.server_error)
                results = emptyList()
            } catch (e: Exception) {
                Log.e("SearchScreen", "Ошибка: ${e.message}", e)
                errorMessage = "${context.getString(R.string.network_error)}: ${e.message}"
                results = emptyList()
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                }
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
                        isSearching = false
                    }
                },
                label = { Text(stringResource(R.string.search_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (!isSearching && query.isBlank() && history.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.search_history), style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
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
                    Text(
                        stringResource(R.string.clear_history),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
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
                OutlinedButton(onClick = { doSearch(query) }) {
                    Text(stringResource(R.string.retry))
                }
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
                                        val info = lastFmApi.getAlbumInfo(
                                            artist = album.artist ?: "",
                                            album = album.name ?: "",
                                            apiKey = ApiKey.LAST_FM_API_KEY
                                        )
                                        val tracks = info.album?.tracks?.track?.mapNotNull { it.name } ?: emptyList()
                                        val cover = info.album?.image?.lastOrNull()?.url
                                        val year = info.album?.wiki?.published?.let { published ->
                                            // Ищем 4 цифры подряд (год)
                                            Regex("\\d{4}").find(published)?.value
                                        }

                                        val previews = mutableMapOf<String, String>()
                                        tracks.forEach { track ->
                                            try {
                                                val artistName = album.artist ?: ""
                                                val deezerResponse = deezerApi.searchTrack("$artistName $track")
                                                val previewUrl = deezerResponse.data?.firstOrNull()?.preview
                                                if (previewUrl != null) {
                                                    previews[track] = previewUrl
                                                }
                                            } catch (_: Exception) {}
                                        }

                                        onAlbumSelected(
                                            album.artist ?: "",
                                            album.name ?: "",
                                            cover,
                                            tracks,
                                            previews,
                                            null
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