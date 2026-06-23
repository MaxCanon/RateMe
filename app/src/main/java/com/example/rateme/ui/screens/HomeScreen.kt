package com.example.rateme.ui.screens

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rateme.LocalAnimatedContentScope
import com.example.rateme.LocalSharedTransitionScope
import com.example.rateme.R
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.model.Album
import com.example.rateme.data.network.LastFmAlbumSummary
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
    isLoading: Boolean = false,
    isDashboard: Boolean = true,
    recommendations: List<LastFmAlbumSummary> = emptyList(),
    onRecommendationClick: (LastFmAlbumSummary) -> Unit = {}
) {
    var albumToDelete by remember { mutableStateOf<Album?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredAlbums = remember(albums, searchQuery) {
        if (searchQuery.isBlank()) albums
        else albums.filter { 
            it.album.title.contains(searchQuery, ignoreCase = true) || 
            it.artist.name.contains(searchQuery, ignoreCase = true) 
        }
    }

    if (albumToDelete != null) {
        AlertDialog(
            onDismissRequest = { albumToDelete = null },
            title = { Text(stringResource(R.string.delete_confirm)) },
            text = { Text("Вы действительно хотите удалить альбом \"${albumToDelete?.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        albumToDelete?.let { onDeleteClick(it) }
                        albumToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { albumToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val inProgress = remember(albums) {
        albums.filter { album ->
            val ratedCount = album.songs.count { it.rating != null }
            ratedCount > 0 && (ratedCount < album.songs.size)
        }
    }

    val bestRated = remember(albums) {
        albums.filter { album ->
            val ratedCount = album.songs.count { it.rating != null }
            ratedCount == album.songs.size && album.songs.isNotEmpty()
        }.sortedByDescending { it.songs.mapNotNull { s -> s.rating }.average() }.take(5)
    }

    val newAlbums = remember(albums) {
        albums.filter { album -> album.songs.all { it.rating == null } }
            .sortedByDescending { it.album.id }
            .take(5)
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        if (showActions) {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                ShimmerLoadingList()
            }
        } else if (albums.isEmpty() && recommendations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_albums))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (!isDashboard) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("Поиск альбома или артиста") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large
                        )
                    }
                }

                if (isDashboard) {
                    if (newAlbums.isNotEmpty()) {
                        item {
                            SectionHeader("Новые альбомы")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(newAlbums) { item ->
                                    DashboardAlbumCard(item, onAlbumClick, { albumToDelete = it.album })
                                }
                            }
                        }
                    }

                    if (inProgress.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            SectionHeader("В процессе")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(inProgress) { item ->
                                    DashboardAlbumCard(item, onAlbumClick, { albumToDelete = it.album })
                                }
                            }
                        }
                    }

                    if (bestRated.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            SectionHeader("Лучшие альбомы")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(bestRated) { item ->
                                    DashboardAlbumCard(item, onAlbumClick, { albumToDelete = it.album })
                                }
                            }
                        }
                    }

                    if (recommendations.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            SectionHeader("Рекомендации для вас")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(recommendations) { item ->
                                    RecommendationCard(item, onRecommendationClick)
                                }
                            }
                        }
                    }
                } else {
                    items(filteredAlbums) { item ->
                        AlbumCard(
                            item = item,
                            showActions = showActions,
                            onAlbumClick = onAlbumClick,
                            onDeleteClick = { albumToDelete = item.album },
                            onEditClick = { onAlbumClick(item.album.id) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardAlbumCard(
    item: AlbumWithArtistAndSongs,
    onClick: (Long) -> Unit,
    onLongClick: (AlbumWithArtistAndSongs) -> Unit
) {
    val avgRating = item.songs.mapNotNull { it.rating }.takeIf { it.isNotEmpty() }?.average()?.let { String.format("%.1f", it) } ?: "—"
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalAnimatedContentScope.current

    Column(
        modifier = Modifier
            .width(140.dp)
            .combinedClickable(
                onClick = { onClick(item.album.id) },
                onLongClick = { onLongClick(item) }
            )
    ) {
        if (sharedTransitionScope != null && animatedContentScope != null) {
            with(sharedTransitionScope) {
                if (!item.album.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.album.coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .sharedElement(
                                rememberSharedContentState(key = "album-cover-${item.album.id}"),
                                animatedVisibilityScope = animatedContentScope
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .sharedElement(
                                rememberSharedContentState(key = "album-cover-${item.album.id}"),
                                animatedVisibilityScope = animatedContentScope
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Album, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } else {
             // Fallback if scope is missing
            AsyncImage(
                model = item.album.coverUrl,
                contentDescription = null,
                modifier = Modifier.size(140.dp).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(item.album.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.artist.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("⭐ $avgRating", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun RecommendationCard(item: LastFmAlbumSummary, onClick: (LastFmAlbumSummary) -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick(item) }
    ) {
        val imageUrl = item.image?.lastOrNull()?.url
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(140.dp).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(140.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Album, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(item.name ?: "?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.artist ?: "?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
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
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalAnimatedContentScope.current

    val isFullyRated = ratedCount == totalCount && totalCount > 0
    val backgroundColor = if (isFullyRated) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    }

    val statusText = if (isFullyRated) {
        stringResource(R.string.rated_all, totalCount)
    } else {
        stringResource(R.string.rated_count, ratedCount, totalCount)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .combinedClickable(onClick = { onAlbumClick(item.album.id) }, onLongClick = { onDeleteClick() }),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (sharedTransitionScope != null && animatedContentScope != null) {
                with(sharedTransitionScope) {
                    if (!item.album.coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.album.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .sharedElement(
                                    rememberSharedContentState(key = "album-cover-${item.album.id}"),
                                    animatedVisibilityScope = animatedContentScope
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .sharedElement(
                                    rememberSharedContentState(key = "album-cover-${item.album.id}"),
                                    animatedVisibilityScope = animatedContentScope
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Album, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                AsyncImage(model = item.album.coverUrl, contentDescription = null, modifier = Modifier.size(56.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.album.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(item.artist.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text("⭐ $avgRating/10", style = MaterialTheme.typography.labelSmall)
                Text(statusText, style = MaterialTheme.typography.labelSmall)
            }

            // Status Icon
            if (isFullyRated) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50), // Green
                    modifier = Modifier.size(24.dp).padding(horizontal = 4.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Pending,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp).padding(horizontal = 4.dp)
                )
            }

            if (showActions) {
                IconButton(onClick = onEditClick) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit), tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDeleteClick) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_confirm), tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
