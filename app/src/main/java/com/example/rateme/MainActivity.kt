package com.example.rateme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.AlbumWithAvgRating
import com.example.rateme.ui.screens.*
import com.example.rateme.ui.theme.RateMeTheme
import com.example.rateme.viewmodel.MainViewModel
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }

            RateMeTheme(darkTheme = isDarkTheme) {
                RateMeApp(isDarkTheme = isDarkTheme, onThemeToggle = { isDarkTheme = !isDarkTheme })
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val emoji: String
)

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

    val navItems = listOf(
        BottomNavItem("home", "Главная", "🏠"),
        BottomNavItem("rated", "Оценённые", "⭐"),
        BottomNavItem("rating", "Рейтинг", "🏆")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Единая цветовая схема
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = surfaceColor.copy(alpha = 0.5f),
                contentColor = onSurfaceColor,
                tonalElevation = 0.dp
            ) {
                navItems.forEach { item ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = { Text(item.emoji, style = MaterialTheme.typography.titleLarge) },
                        label = { Text(item.label) },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                NavigationBarItem(
                    icon = {
                        Text(
                            if (isDarkTheme) "☀️" else "🌙",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    label = { Text("Тема") },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        indicatorColor = Color.Transparent
                    ),
                    onClick = onThemeToggle
                )
            }
        },
        floatingActionButton = {
            if (currentDestination?.route == "home") {
                FloatingActionButton(
                    onClick = { navController.navigate("add") },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("+", style = MaterialTheme.typography.headlineSmall)
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
                    onAlbumClick = { albumId -> navController.navigate("album/$albumId") },
                    onAddClick = {},
                    onDeleteClick = { album -> viewModel.deleteAlbum(album) },
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = {},
                    onRatedClick = {},
                    onRatingClick = {},
                    onHomeClick = {},
                    showActions = true,
                    showTopBar = false,
                    showAddButton = false
                )
            }

            composable("rated") {
                HomeScreen(
                    albums = ratedAlbums,
                    onAlbumClick = { albumId -> navController.navigate("album/$albumId") },
                    onAddClick = {},
                    onDeleteClick = {},
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = {},
                    onRatedClick = {},
                    onRatingClick = {},
                    onHomeClick = {},
                    showActions = false,
                    showTopBar = false,
                    showAddButton = false,
                    title = "Оценённые альбомы"
                )
            }

            composable("rating") {
                RatingScreen(
                    albums = albumsByRating,
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { albumId -> navController.navigate("album_view/$albumId") }
                )
            }

            composable("album/{albumId}") { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId")?.toLongOrNull()
                val albumWithSongs = albums.find { it.album.id == albumId }
                AlbumScreen(
                    albumWithSongs = albumWithSongs,
                    onBack = { navController.popBackStack() },
                    onRatingChanged = { songId, rating -> viewModel.updateRating(songId, rating) },
                    onShareClick = { intent ->
                        navController.context.startActivity(
                            Intent.createChooser(
                                intent,
                                "Поделиться"
                            )
                        )
                    },
                    readOnly = false
                )
            }

            composable("album_view/{albumId}") { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId")?.toLongOrNull()
                val albumWithSongs = albums.find { it.album.id == albumId }
                AlbumScreen(
                    albumWithSongs = albumWithSongs,
                    onBack = { navController.popBackStack() },
                    onRatingChanged = { _, _ -> },
                    onShareClick = { intent ->
                        navController.context.startActivity(
                            Intent.createChooser(
                                intent,
                                "Поделиться"
                            )
                        )
                    },
                    readOnly = true
                )
            }

            composable("add") {
                AddAlbumScreen(
                    onSave = { artist, album, songs, coverUrl ->
                        viewModel.addAlbumWithSongs(artist, album, songs, coverUrl, emptyMap())
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                    onSearchClick = { navController.navigate("search") }
                )
            }

            composable("search") {
                SearchScreen(
                    onAlbumSelected = { artist, album, coverUrl, tracks, previews, year ->
                        viewModel.addAlbumWithSongs(artist, album, tracks, coverUrl, previews, year)
                        navController.popBackStack()
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}}