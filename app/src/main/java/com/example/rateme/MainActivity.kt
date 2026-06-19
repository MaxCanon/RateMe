package com.example.rateme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rateme.data.AlbumWithArtistAndSongs
import com.example.rateme.data.AlbumWithAvgRating
import com.example.rateme.ui.screens.*
import com.example.rateme.ui.theme.RateMeTheme
import com.example.rateme.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }

            RateMeTheme(darkTheme = isDarkTheme) {
                RateMeApp(isDarkTheme = isDarkTheme, onThemeToggle = { isDarkTheme = !isDarkTheme })
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

    LaunchedEffect(Unit) {
        viewModel.allAlbums.collect { albums = it }
    }
    LaunchedEffect(Unit) {
        viewModel.ratedAlbums.collect { ratedAlbums = it }
    }
    LaunchedEffect(Unit) {
        viewModel.albumsByRating.collect { albumsByRating = it }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                albums = albums,
                onAlbumClick = { albumId -> navController.navigate("album/$albumId") },
                onAddClick = { navController.navigate("add") },
                onDeleteClick = { album -> viewModel.deleteAlbum(album) },
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                onRatedClick = { navController.navigate("rated") },
                onRatingClick = { navController.navigate("rating") },
                onHomeClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                showActions = true
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
                    navController.context.startActivity(Intent.createChooser(intent, "Поделиться"))
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
                    navController.context.startActivity(Intent.createChooser(intent, "Поделиться"))
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

        composable("rated") {
            HomeScreen(
                albums = ratedAlbums,
                onAlbumClick = { albumId -> navController.navigate("album/$albumId") },
                onAddClick = {},
                onDeleteClick = {},
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                onRatedClick = {},
                onRatingClick = {},
                showActions = false
            )
        }

        composable("rating") {
            RatingScreen(
                albums = albumsByRating,
                onBack = { navController.popBackStack() },
                onAlbumClick = { albumId -> navController.navigate("album_view/$albumId") }
            )
        }
    }
}