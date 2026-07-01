package com.example.rateme

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rateme.data.getDatabase
import com.example.rateme.data.getDatabaseBuilder
import com.example.rateme.ui.components.AppBackground
import com.example.rateme.ui.screens.*
import com.example.rateme.ui.theme.RateMeTheme
import com.example.rateme.viewmodel.MainViewModel
import org.jetbrains.compose.resources.stringResource
import rateme.app.generated.resources.Res
import rateme.app.generated.resources.*

// Specific icon imports
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.automirrored.filled.OpenInNew

@Composable
fun App() {
    val settings = com.example.rateme.data.getSettings()
    val isDark = settings.getBoolean("darkTheme", true)
    var isDarkTheme by remember { mutableStateOf(isDark) }

    RateMeTheme(darkTheme = isDarkTheme) {
        AppBackground(isDarkTheme = isDarkTheme) {
            RateMeApp(
                isDarkTheme = isDarkTheme,
                onThemeToggle = {
                    val newValue = !isDarkTheme
                    isDarkTheme = newValue
                    settings.putBoolean("darkTheme", newValue)
                }
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RateMeApp(isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    val database = remember { getDatabase(getDatabaseBuilder()) }
    val viewModel: MainViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: kotlin.reflect.KClass<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(database.albumDao()) as T
        }
    })
    val navController = rememberNavController()

    val albums by viewModel.allAlbums.collectAsState(emptyList())
    val ratedAlbums by viewModel.ratedAlbums.collectAsState(emptyList())
    val albumsByRating by viewModel.albumsByRating.collectAsState(emptyList())
    
    val recommendations by viewModel.recommendations.collectAsState()
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val playingArtistName by viewModel.playingArtistName.collectAsState()
    val playingColor by viewModel.playingColor.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isPreparing by viewModel.isPreparing.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()
    val playbackDuration by viewModel.playbackDuration.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    val newAchievement by viewModel.newAchievement.collectAsState()

    LaunchedEffect(newAchievement) {
        newAchievement?.let { ach ->
            showToast(ach.title)
            viewModel.clearAchievement()
        }
    }

    LaunchedEffect(albums) {
        isLoading = false
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isAlbumScreen = currentRoute?.startsWith("album") == true
    val showBottomBar = !isAlbumScreen

    Box(modifier = Modifier.fillMaxSize()) {
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavHost(
                    navController = navController, 
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("home") {
                        CompositionLocalProvider(LocalAnimatedContentScope provides this) {
                            HomeScreen(albums = albums, onAlbumClick = { navController.navigate("album/$it") }, onAddClick = {}, onDeleteClick = { viewModel.deleteAlbum(it) },
                                isDarkTheme = isDarkTheme, onThemeToggle = {}, onRatedClick = {}, onRatingClick = {},
                                showActions = true, showTopBar = true, showAddButton = false, isLoading = isLoading,
                                onSettingsClick = { navController.navigate("settings") },
                                recommendations = recommendations,
                                onRecommendationClick = { item ->
                                    navController.navigate("add?query=${item.artist} ${item.name}")
                                })
                        }
                    }
                    composable("rated") {
                        CompositionLocalProvider(LocalAnimatedContentScope provides this) {
                            HomeScreen(albums = ratedAlbums, onAlbumClick = { navController.navigate("album/$it") }, onAddClick = {}, onDeleteClick = { viewModel.deleteAlbum(it) },
                                isDarkTheme = isDarkTheme, onThemeToggle = {}, onRatedClick = {}, onRatingClick = {},
                                showActions = false, showTopBar = true, showAddButton = false, title = stringResource(Res.string.evaluated_albums), isLoading = false, isDashboard = false)
                        }
                    }
                    composable("rating") {
                        RatingScreen(
                            albums = albumsByRating, 
                            onBack = { navController.navigate("home") }, 
                            onAlbumClick = { navController.navigate("album_view/$it") },
                            onDeleteClick = { id ->
                                albums.find { it.album.id == id }?.let { viewModel.deleteAlbum(it.album) }
                            }
                        )
                    }
                    composable("album/{albumId}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("albumId")?.toLongOrNull()
                        CompositionLocalProvider(LocalAnimatedContentScope provides this) {
                            AlbumScreen(
                                albumWithSongs = albums.find { it.album.id == id }, 
                                onBack = { navController.popBackStack() },
                                onRatingChanged = { songId, rating -> viewModel.updateRating(songId, rating, isDarkTheme) },
                                onShareClick = { text -> shareText(text) }, 
                                onTrackListen = { viewModel.incrementAchievement("27") },
                                readOnly = false,
                                currentlyPlayingId = currentlyPlaying?.id,
                                isPlaying = isPlaying,
                                isPreparing = isPreparing,
                                onTogglePlayback = { song, artist, color -> viewModel.togglePlayback(song, artist, color) }
                            )
                        }
                    }
                    composable("album_view/{albumId}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("albumId")?.toLongOrNull()
                        CompositionLocalProvider(LocalAnimatedContentScope provides this) {
                            AlbumScreen(
                                albumWithSongs = albums.find { it.album.id == id }, 
                                onBack = { navController.popBackStack() },
                                onRatingChanged = { _, _ -> },
                                onShareClick = { text -> shareText(text) }, 
                                onTrackListen = { viewModel.incrementAchievement("27") },
                                readOnly = true,
                                currentlyPlayingId = currentlyPlaying?.id,
                                isPlaying = isPlaying,
                                isPreparing = isPreparing,
                                onTogglePlayback = { song, artist, color -> viewModel.togglePlayback(song, artist, color) }
                            )
                        }
                    }
                    composable("add?query={query}") { backStackEntry ->
                        val initialQuery = backStackEntry.arguments?.getString("query")
                        AddAlbumScreen(
                            initialQuery = initialQuery,
                            onAlbumSelected = { artist, album, coverUrl, tracks, previews, year ->
                                viewModel.addAlbumWithSongs(artist, album, tracks, coverUrl, previews, year)
                                navController.navigate("home") { popUpTo("home") { inclusive = true } }
                            }, 
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("stats") { AchievementsScreen(onBack = { navController.popBackStack() }) }
                    composable("settings") {
                        SettingsScreen(isDarkTheme = isDarkTheme, onThemeToggle = onThemeToggle,
                            onBack = { navController.popBackStack() }, onStatsClick = { navController.navigate("oldstats") },
                            onLanguageChange = { viewModel.incrementAchievement("7") },
                            onRefreshMetadata = { viewModel.refreshAllMetadata() })
                    }
                    composable("oldstats") { StatsScreen(albums = albums, onBack = { navController.popBackStack() }) }
                }
            }
        }

        // CONTROL PANEL (Overlay)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // MINI PLAYER
            AnimatedVisibility(
                visible = currentlyPlaying != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                currentlyPlaying?.let { song ->
                    var showLinks by remember { mutableStateOf(false) }
                    val bgColor = playingColor ?: MaterialTheme.colorScheme.primaryContainer
                    val contentColor = if (isDarkTheme) Color.White else Color.Black
                    
                    if (showLinks) {
                        AlertDialog(
                            onDismissRequest = { showLinks = false },
                            title = { Text(stringResource(Res.string.listen_full)) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val query = "${playingArtistName ?: ""} ${song.title}"
                                    val links = listOf(
                                        "YouTube" to "https://www.youtube.com/results?search_query=$query",
                                        "Spotify" to "https://open.spotify.com/search/$query",
                                        "Apple Music" to "https://music.apple.com/search?term=$query",
                                        "Yandex Music" to "https://music.yandex.ru/search?text=$query"
                                    )
                                    links.forEach { (name, url) ->
                                        Button(onClick = { openUrl(url); showLinks = false }, modifier = Modifier.fillMaxWidth()) {
                                            Text(name)
                                        }
                                    }
                                }
                            },
                            confirmButton = { TextButton(onClick = { showLinks = false }) { Text(stringResource(Res.string.cancel)) } }
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .shadow(25.dp, RoundedCornerShape(28.dp)),
                        color = bgColor.copy(alpha = 0.85f), // Меньше яркости, больше прозрачности
                        shape = RoundedCornerShape(28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)) // Мягкий контур
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, null, tint = contentColor.copy(alpha = 0.9f), modifier = Modifier.size(30.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, color = contentColor, fontWeight = FontWeight.Bold)
                                    Text(playingArtistName ?: "", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.5f))
                                }
                                IconButton(onClick = { viewModel.togglePlayback(song, playingArtistName, playingColor) }) {
                                    if (isPreparing) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = contentColor)
                                    } else {
                                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = contentColor)
                                    }
                                }
                                IconButton(onClick = { viewModel.stopPlayback() }) {
                                    Icon(Icons.Default.Close, null, tint = contentColor.copy(alpha = 0.2f))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Кнопка с мягким контуром
                            Button(
                                onClick = { showLinks = true },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = contentColor.copy(alpha = 0.05f), // Почти прозрачная
                                    contentColor = contentColor
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.1f)) // Мягкий контур
                            ) {
                                Text(stringResource(Res.string.listen_full).uppercase(), fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Ползунок стал мягче
                            Slider(
                                value = playbackPosition.toFloat(),
                                onValueChange = { viewModel.seekTo(it.toLong()) },
                                valueRange = 0f..playbackDuration.toFloat().coerceAtLeast(1f),
                                modifier = Modifier.fillMaxWidth().height(10.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = contentColor,
                                    activeTrackColor = contentColor.copy(alpha = 0.7f),
                                    inactiveTrackColor = contentColor.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }
            }

            // NAVIGATION
            if (showBottomBar) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        .height(72.dp)
                        .shadow(25.dp, CircleShape),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavItem(Icons.Default.Home, stringResource(Res.string.home), currentRoute == "home") {
                            navController.navigate("home") { popUpTo("home") { inclusive = true }; launchSingleTop = true }
                        }
                        NavItem(Icons.Default.ThumbUp, stringResource(Res.string.ratings), currentRoute == "rated") {
                            navController.navigate("rated") { launchSingleTop = true; restoreState = true }
                        }
                        FloatingActionButton(
                            onClick = { navController.navigate("add?query=") },
                            modifier = Modifier.size(54.dp).offset(y = (-2).dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(30.dp))
                        }
                        NavItem(Icons.Default.Favorite, stringResource(Res.string.rating), currentRoute == "rating") {
                            navController.navigate("rating") { launchSingleTop = true; restoreState = true }
                        }
                        NavItem(Icons.Default.EmojiEvents, stringResource(Res.string.achievements), currentRoute == "stats") {
                            navController.navigate("stats") { launchSingleTop = true; restoreState = true }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(26.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal), color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}
