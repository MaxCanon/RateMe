package com.example.rateme.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("←") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            SettingsItem(
                icon = Icons.Filled.Star,
                title = "Тема",
                subtitle = if (isDarkTheme) "Тёмная ☀️" else "Светлая 🌙",
                onClick = onThemeToggle
            )
            SettingsItem(
                icon = Icons.Filled.Person,
                title = "Регистрация",
                subtitle = "Войти / создать аккаунт",
                onClick = { /* потом */ }
            )
            SettingsItem(
                icon = Icons.Filled.Info,
                title = "О программе",
                subtitle = "Версия 1.0",
                onClick = { /* потом */ }
            )
            SettingsItem(
                icon = Icons.Filled.Email,
                title = "Связаться с автором",
                subtitle = "Написать на почту",
                onClick = { /* потом */ }
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}