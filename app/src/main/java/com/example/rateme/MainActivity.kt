package com.example.rateme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rateme.ui.screens.*
import com.example.rateme.ui.theme.RateMeTheme
import com.example.rateme.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // rememberSaveable сохраняет состояние при повороте экрана
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }

            RateMeTheme(darkTheme = isDarkTheme) {
                RateMeApp(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}

@Composable
fun RateMeApp(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val albums by viewModel.allAlbums.collectAsStateWithLifecycle(initialValue = emptyList())

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                albums = albums,
                onAlbumClick = { albumId -> navController.navigate("album/$albumId") },
                onAddClick = { navController.navigate("add") },
                onDeleteClick = { album -> viewModel.deleteAlbum(album) },
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle
            )
        }
        composable("album/{albumId}") { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId")?.toLongOrNull()
            val albumWithSongs = albums.find { it.album.id == albumId }
            AlbumScreen(
                albumWithSongs = albumWithSongs,
                onBack = { navController.popBackStack() },
                onRatingChanged = { songId, rating -> viewModel.updateRating(songId, rating) }
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
                onAlbumSelected = { artist, album, coverUrl, tracks, previews ->
                    viewModel.addAlbumWithSongs(artist, album, tracks, coverUrl, previews)
                    navController.popBackStack()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}