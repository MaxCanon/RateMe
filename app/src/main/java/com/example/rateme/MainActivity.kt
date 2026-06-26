package com.example.rateme

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rateme.R
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.AlbumWithAvgRating
import com.example.rateme.ui.components.AppBackground
import com.example.rateme.ui.screens.*
import com.example.rateme.ui.theme.RateMeTheme
import com.example.rateme.viewmodel.MainViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalAnimatedContentScope = compositionLocalOf<AnimatedContentScope?> { null }

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDark = prefs.getBoolean("darkTheme", true)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(isDark) }
            val context = LocalContext.current
            RateMeTheme(darkTheme = isDarkTheme) {
                AppBackground(isDarkTheme = isDarkTheme) {
                    RateMeApp(isDarkTheme = isDarkTheme, onThemeToggle = {
                        val newValue = !isDarkTheme
                        isDarkTheme = newValue
                        context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("darkTheme", newValue).apply()
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RateMeApp(isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val appContext = LocalContext.current

    var albums by remember { mutableStateOf<List<AlbumWithArtistAndSongs>>(emptyList()) }
    var ratedAlbums by remember { mutableStateOf<List<AlbumWithArtistAndSongs>>(emptyList()) }
    var albumsByRating by remember { mutableStateOf<List<AlbumWithAvgRating>>(emptyList()) }
    val recommendations by viewModel.recommendations.collectAsState()
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val playingArtistName by viewModel.playingArtistName.collectAsState()
    val playingColor by viewModel.playingColor.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isPreparing by viewModel.isPreparing.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()
    val playbackDuration by viewModel.playbackDuration.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    val newAchievement by viewModel.newAchievement.observeAsState(initial = null)

    LaunchedEffect(newAchievement) {
        newAchievement?.let { ach ->
            android.widget.Toast.makeText(appContext, appContext.getString(R.string.achievement_unlocked, ach.title), android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearAchievement()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.allAlbums.collect { 
            albums = it
            isLoading = false 
            if (it.isNotEmpty() && recommendations.isEmpty()) {
                viewModel.loadRecommendations()
            }
        }
    }
    LaunchedEffect(Unit) { viewModel.ratedAlbums.collect { ratedAlbums = it } }
    LaunchedEffect(Unit) { viewModel.albumsByRating.collect { albumsByRating = it } }

    LaunchedEffect(Unit) {
        viewModel.incrementAchievement("14")
        viewModel.checkStreakReset()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    BackHandler {
        if (currentRoute != "home") {
            navController.navigate("home") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true; restoreState = true
            }
        } else {
            (appContext as? android.app.Activity)?.moveTaskToBack(true)
        }
    }

    val showBottomBar = currentRoute in listOf("home", "rated", "rating", "add", "stats", "settings", "oldstats")

    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                // Mini Player
                currentlyPlaying?.let { song ->
                    var showLinks by remember { mutableStateOf(false) }
                    
                    // Logic to ensure the player is visible even for black/dark covers
                    val bgColor = playingColor ?: MaterialTheme.colorScheme.primaryContainer
                    val isBgTooDark = (0.2126f * bgColor.red + 0.7152f * bgColor.green + 0.0722f * bgColor.blue) < 0.1f
                    val playerContentColor = if (isBgTooDark) Color.White else Color.Black.copy(alpha = 0.8f)
                    
                    if (showLinks) {
                        AlertDialog(
                            onDismissRequest = { showLinks = false },
                            title = { Text("Послушать полностью") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val query = "${playingArtistName ?: ""} ${song.title}"
                                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                                    
                                    val links = listOf(
                                        "YouTube" to "https://www.youtube.com/results?search_query=$encodedQuery",
                                        "Apple Music" to "https://music.apple.com/search?term=$encodedQuery",
                                        "Yandex Music" to "https://music.yandex.ru/search?text=$encodedQuery",
                                        "Last.fm" to "https://www.last.fm/search?q=$encodedQuery"
                                    )

                                    links.forEach { (name, url) ->
                                        Button(
                                            onClick = {
                                                appContext.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                                                showLinks = false
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(name)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showLinks = false }) { Text("Закрыть") }
                            }
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .shadow(8.dp, MaterialTheme.shapes.medium),
                        color = bgColor.copy(alpha = 0.95f),
                        shape = MaterialTheme.shapes.medium,
                        border = if (isBgTooDark) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = playerContentColor,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        color = playerContentColor
                                    )
                                    if (isPreparing) {
                                        Text(
                                            text = "Загрузка...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = playerContentColor.copy(alpha = 0.7f)
                                        )
                                    } else {
                                        Button(
                                            onClick = { showLinks = true },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp),
                                            shape = MaterialTheme.shapes.small,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = playerContentColor.copy(alpha = 0.15f),
                                                contentColor = playerContentColor
                                            )
                                        ) {
                                            Text(
                                                text = "Слушать полностью ↗",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.togglePlayback(song) },
                                    enabled = !isPreparing
                                ) {
                                    if (isPreparing) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = playerContentColor)
                                    } else {
                                        Icon(
                                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = playerContentColor
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.stopPlayback() }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = playerContentColor.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            
                            // Progress Bar / Slider
                            Slider(
                                value = playbackPosition.toFloat(),
                                onValueChange = { viewModel.seekTo(it.toLong()) },
                                valueRange = 0f..playbackDuration.toFloat().coerceAtLeast(1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .padding(horizontal = 12.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = playerContentColor,
                                    activeTrackColor = playerContentColor,
                                    inactiveTrackColor = playerContentColor.copy(alpha = 0.24f)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                if (showBottomBar) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                IconButton(onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true }; launchSingleTop = true } }) {
                                    Icon(Icons.Filled.Home, contentDescription = null, tint = if (currentRoute == "home") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Text(stringResource(R.string.home), style = MaterialTheme.typography.labelSmall, color = if (currentRoute == "home") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                IconButton(onClick = { navController.navigate("rated") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }) {
                                    Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = if (currentRoute == "rated") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Text(stringResource(R.string.ratings), style = MaterialTheme.typography.labelSmall, color = if (currentRoute == "rated") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                FloatingActionButton(onClick = { navController.navigate("add?query=") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                    modifier = Modifier.size(48.dp).shadow(8.dp, CircleShape), containerColor = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                                    Icon(Icons.Filled.Add, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                                Text(stringResource(R.string.add), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                IconButton(onClick = { navController.navigate("rating") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }) {
                                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = if (currentRoute == "rating") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Text(stringResource(R.string.rating), style = MaterialTheme.typography.labelSmall, color = if (currentRoute == "rating") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                IconButton(onClick = { navController.navigate("stats") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = if (currentRoute == "stats") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Text(stringResource(R.string.achievements), style = MaterialTheme.typography.labelSmall, color = if (currentRoute == "stats") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(top = innerPadding.calculateTopPadding())) {
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
                                showActions = false, showTopBar = true, showAddButton = false, title = stringResource(R.string.evaluated_albums), isLoading = false, isDashboard = false)
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
                                onBack = { navController.navigate("home") },
                                onRatingChanged = { songId, rating -> viewModel.updateRating(songId, rating, isDarkTheme) },
                                onShareClick = { navController.context.startActivity(Intent.createChooser(it, "Поделиться")) }, 
                                onTrackListen = { viewModel.incrementAchievement("27") },
                                readOnly = false,
                                currentlyPlayingId = currentlyPlaying?.id,
                                isPlaying = isPlaying,
                                onTogglePlayback = { song, artist, color -> viewModel.togglePlayback(song, artist, color) }
                            )
                        }
                    }
                    composable("album_view/{albumId}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("albumId")?.toLongOrNull()
                        CompositionLocalProvider(LocalAnimatedContentScope provides this) {
                            AlbumScreen(
                                albumWithSongs = albums.find { it.album.id == id }, 
                                onBack = { navController.navigate("rating") },
                                onRatingChanged = { _, _ -> },
                                onShareClick = { navController.context.startActivity(Intent.createChooser(it, "Поделиться")) }, 
                                onTrackListen = { viewModel.incrementAchievement("27") },
                                readOnly = true,
                                currentlyPlayingId = currentlyPlaying?.id,
                                isPlaying = isPlaying,
                                onTogglePlayback = { song, artist, color -> viewModel.togglePlayback(song, artist, color) }
                            )
                        }
                    }
                    composable(
                        "add?query={query}",
                        arguments = listOf(androidx.navigation.navArgument("query") { defaultValue = ""; nullable = true })
                    ) { backStackEntry ->
                        val initialQuery = backStackEntry.arguments?.getString("query")
                        AddAlbumScreen(
                            initialQuery = initialQuery,
                            onAlbumSelected = { artist, album, coverUrl, tracks, previews, year ->
                                viewModel.addAlbumWithSongs(artist, album, tracks, coverUrl, previews, year,
                                    onDuplicate = { android.widget.Toast.makeText(appContext, appContext.getString(R.string.duplicate_album), android.widget.Toast.LENGTH_SHORT).show() })
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }, 
                            onBack = { navController.navigate("home") }
                        )
                    }
                    composable("stats") {
                        LaunchedEffect(Unit) { viewModel.incrementAchievement("19") }
                        AchievementsScreen(onBack = { navController.navigate("home") })
                    }
                    composable("settings") {
                        SettingsScreen(isDarkTheme = isDarkTheme, onThemeToggle = onThemeToggle,
                            onBack = { navController.navigate("home") }, onStatsClick = { 
                                navController.navigate("oldstats") 
                            }, onLanguageChange = {
                                viewModel.incrementAchievement("7")
                            }, onRefreshMetadata = {
                                viewModel.refreshAllMetadata()
                            })
                    }
                    composable("oldstats") {
                        LaunchedEffect(Unit) { viewModel.incrementAchievement("19") }
                        StatsScreen(albums = albums, onBack = { navController.navigate("settings") })
                    }
                }
            }
        }
    }
}
