package com.example.rateme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.AlbumWithAvgRating
import com.example.rateme.ui.components.AppBackground
import com.example.rateme.ui.screens.*
import com.example.rateme.ui.theme.RateMeTheme
import com.example.rateme.viewmodel.MainViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }
            RateMeTheme(darkTheme = isDarkTheme) {
                AppBackground(isDarkTheme = isDarkTheme) {
                    RateMeApp(isDarkTheme = isDarkTheme, onThemeToggle = { isDarkTheme = !isDarkTheme })
                }
            }
        }
    }
}

@Composable
fun RateMeApp(isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    var albums by remember { mutableStateOf<List<AlbumWithArtistAndSongs>>(emptyList()) }
    var ratedAlbums by remember { mutableStateOf<List<AlbumWithArtistAndSongs>>(emptyList()) }
    var albumsByRating by remember { mutableStateOf<List<AlbumWithAvgRating>>(emptyList()) }

    LaunchedEffect(Unit) { viewModel.allAlbums.collect { albums = it } }
    LaunchedEffect(Unit) { viewModel.ratedAlbums.collect { ratedAlbums = it } }
    LaunchedEffect(Unit) { viewModel.albumsByRating.collect { albumsByRating = it } }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf("home", "rated", "rating", "add", "search")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .navigationBarsPadding()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Главная
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = null,
                                tint = if (currentRoute == "home") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Text(
                            "Главная",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentRoute == "home") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    // Оценки
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = {
                            navController.navigate("rated") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = if (currentRoute == "rated") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Text(
                            "Оценки",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentRoute == "rated") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    // Добавить (выделенная)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                navController.navigate("add") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(8.dp, CircleShape),
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Text(
                            "Добавить",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Рейтинг
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = {
                            navController.navigate("rating") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (currentRoute == "rating") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Text(
                            "Рейтинг",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentRoute == "rating") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    // Тема
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onThemeToggle) {
                            Text(
                                if (isDarkTheme) "☀️" else "🌙",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            "Тема",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    albums = albums,
                    onAlbumClick = { navController.navigate("album/$it") },
                    onAddClick = {},
                    onDeleteClick = { viewModel.deleteAlbum(it) },
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = {},
                    onRatedClick = {},
                    onRatingClick = {},
                    showActions = true,
                    showTopBar = false,
                    showAddButton = false
                )
            }
            composable("rated") {
                HomeScreen(
                    albums = ratedAlbums,
                    onAlbumClick = { navController.navigate("album/$it") },
                    onAddClick = {},
                    onDeleteClick = {},
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = {},
                    onRatedClick = {},
                    onRatingClick = {},
                    showActions = false,
                    showTopBar = false,
                    showAddButton = false,
                    title = "Оценённые альбомы"
                )
            }
            composable("rating") {
                RatingScreen(
                    albums = albumsByRating,
                    onBack = {},
                    onAlbumClick = { navController.navigate("album_view/$it") }
                )
            }
            composable("album/{albumId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("albumId")?.toLongOrNull()
                val album = albums.find { it.album.id == id }
                AlbumScreen(
                    albumWithSongs = album,
                    onBack = { navController.navigate("home") },
                    onRatingChanged = { songId, rating -> viewModel.updateRating(songId, rating) },
                    onShareClick = { intent ->
                        navController.context.startActivity(Intent.createChooser(intent, "Поделиться"))
                    },
                    readOnly = false
                )
            }
            composable("album_view/{albumId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("albumId")?.toLongOrNull()
                val album = albums.find { it.album.id == id }
                AlbumScreen(
                    albumWithSongs = album,
                    onBack = { navController.navigate("rating") },
                    onRatingChanged = { _, _ -> },
                    onShareClick = { intent ->
                        navController.context.startActivity(Intent.createChooser(intent, "Поделиться"))
                    },
                    readOnly = true
                )
            }
            composable("add") {
                AddAlbumScreen(
                    onSave = { artist, album, songs, coverUrl ->
                        viewModel.addAlbumWithSongs(artist, album, songs, coverUrl)
                        navController.navigate("home")
                    },
                    onBack = {},
                    onSearchClick = { navController.navigate("search") }
                )
            }
            composable("search") {
                SearchScreen(
                    onAlbumSelected = { artist, album, coverUrl, tracks, previews, year ->
                        viewModel.addAlbumWithSongs(artist, album, tracks, coverUrl, previews, year)
                        navController.navigate("home")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}