package com.example.rateme.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlbumScreen(
    onSave: (String, String, List<String>, String?) -> Unit,
    onBack: () -> Unit,
    onSearchClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить альбом") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Назад") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Добавить новый альбом",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Найдите альбом через Last.fm — все песни и обложка загрузятся автоматически",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onSearchClick,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("🔍 Найти альбом и оценить", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}