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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.rateme.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onBack: () -> Unit,
    onStatsClick: () -> Unit = {},
    onLanguageChange: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    val savedLang = prefs.getString("language", "ru") ?: "ru"

    var expandedLanguage by remember { mutableStateOf(false) }
    val languageMap = mapOf(
        "ru" to stringResource(R.string.language_ru),
        "en" to stringResource(R.string.language_en),
        "de" to stringResource(R.string.language_de),
        "fr" to stringResource(R.string.language_fr)
    )
    var selectedLanguage by remember { mutableStateOf(languageMap[savedLang] ?: "Русский") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Box {
                SettingsItem(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.language),
                    subtitle = selectedLanguage,
                    onClick = { expandedLanguage = true }
                )
                DropdownMenu(expanded = expandedLanguage, onDismissRequest = { expandedLanguage = false }) {
                    languageMap.forEach { (code, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            if (code != savedLang) {
                                onLanguageChange()
                            }

                            expandedLanguage = false
                            prefs.edit()
                                .putString("language", code)
                                .putBoolean("darkTheme", isDarkTheme)
                                .apply()
                            val intent = android.content.Intent(context, context::class.java)
                            context.startActivity(intent)
                            (context as android.app.Activity).finish()
                        })
                    }
                }
            }

            SettingsItem(
                icon = Icons.Filled.BarChart,
                title = "Статистика",
                subtitle = "Альбомы, треки, рейтинг",
                onClick = onStatsClick
            )

            SettingsItem(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.version),
                subtitle = "0.7",
                onClick = { }
            )

            SettingsItem(
                icon = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                title = stringResource(R.string.theme),
                subtitle = if (isDarkTheme) stringResource(R.string.dark_theme) else stringResource(R.string.light_theme),
                onClick = onThemeToggle
            )

            SettingsItem(
                icon = Icons.Filled.Person,
                title = stringResource(R.string.author),
                subtitle = "@brikiton",
                onClick = { }
            )
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}