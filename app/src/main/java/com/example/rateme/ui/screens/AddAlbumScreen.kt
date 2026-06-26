package com.example.rateme.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    initialQuery: String? = null,
    onAlbumSelected: (artist: String, album: String, coverUrl: String?, tracks: List<String>, previews: Map<String, String>, year: String?) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf(initialQuery ?: "") }
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

    LaunchedEffect(Unit) {
        if (!initialQuery.isNullOrBlank()) {
            doSearch(initialQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
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
                                            var finalTracks: List<String> = emptyList()
                                            var finalPreviews = mutableMapOf<String, String>()
                                            var finalCover = album.image?.lastOrNull()?.url
                                            var finalYear: String? = null

                                            // 1. Try Last.fm for tracks and info
                                            try {
                                                val info = lastFmApi.getAlbumInfo(
                                                    artist = album.artist ?: "",
                                                    album = album.name ?: "",
                                                    apiKey = ApiKey.LAST_FM_API_KEY
                                                )
                                                finalTracks = info.album?.tracks?.track?.mapNotNull { it.name } ?: emptyList()
                                                finalCover = info.album?.image?.lastOrNull()?.url ?: finalCover
                                                // Last.fm wiki is often inaccurate for original release year, 
                                                // but we'll take it as a last resort if others fail.
                                            } catch (e: Exception) {
                                                android.util.Log.e("AddAlbum", "Last.fm info failed: ${e.message}")
                                            }

                                            // 2. Fallback to Deezer for tracks and accurate release date
                                            try {
                                                val deezerSearch = ApiClient.deezerApi.searchAlbum("${album.artist} ${album.name}")
                                                val deezerAlbum = deezerSearch.data?.firstOrNull { 
                                                    it.title?.contains(album.name ?: "", ignoreCase = true) == true 
                                                }
                                                
                                                if (deezerAlbum != null) {
                                                    finalYear = deezerAlbum.release_date?.take(4)
                                                    
                                                    if (finalTracks.isEmpty()) {
                                                        val deezerTracks = ApiClient.deezerApi.getAlbumTracks(deezerAlbum.id)
                                                        finalTracks = deezerTracks.data?.mapNotNull { it.title } ?: emptyList()
                                                        deezerTracks.data?.forEach { track ->
                                                            if (track.title != null && track.preview != null) {
                                                                finalPreviews[track.title] = track.preview
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("AddAlbum", "Deezer failed: ${e.message}")
                                            }

                                            // 3. iTunes fallback for year and missing previews
                                            try {
                                                val searchTerm = "${album.artist} ${album.name}"
                                                val response = iTunesApi.searchTrack(searchTerm) // Use existing search
                                                val itunesResult = response.results.firstOrNull()
                                                
                                                if (finalYear == null) {
                                                    finalYear = itunesResult?.releaseDate?.take(4)
                                                }

                                                finalTracks.forEach { track ->
                                                    if (!finalPreviews.containsKey(track)) {
                                                        try {
                                                            val trackSearch = "${album.artist} $track"
                                                            val trackResp = iTunesApi.searchTrack(trackSearch)
                                                            val previewUrl = trackResp.results.firstOrNull()?.previewUrl
                                                            if (previewUrl != null) finalPreviews[track] = previewUrl
                                                        } catch (_: Exception) {}
                                                    }
                                                }
                                            } catch (_: Exception) {}

                                            onAlbumSelected(
                                                album.artist ?: "",
                                                album.name ?: "",
                                                finalCover,
                                                finalTracks,
                                                finalPreviews,
                                                finalYear
                                            )
                                        } catch (e: Exception) {
                                            android.util.Log.e("AddAlbum", "Selection failed: ${e.message}")
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
